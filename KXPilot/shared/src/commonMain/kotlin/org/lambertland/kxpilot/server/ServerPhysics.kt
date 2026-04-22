package org.lambertland.kxpilot.server

import org.lambertland.kxpilot.common.ClickConst
import org.lambertland.kxpilot.common.EnergyDrain
import org.lambertland.kxpilot.common.GameConst
import org.lambertland.kxpilot.common.Key
import org.lambertland.kxpilot.common.Vector
import kotlin.math.sqrt

// ---------------------------------------------------------------------------
// ServerPhysics
// ---------------------------------------------------------------------------
//
// Per-tick physics update for all ALIVE players.
//
// Ported from server/update.c (`Player_update` + `update_object_speed`) and
// server/collision.c (wall collision).
//
// Coordinate systems:
//   - Click-space: integers, 64 clicks per pixel (ClickConst.CLICK).
//   - Pixel-space: floats, used for velocity / acceleration.
//   - Direction: integer 0..RES-1, where RES = 128 (full circle).
//
// Physics step (matches C server with timeStep = 1.0 / FPS × FPS = 1):
//   1. Decode keyboard → thrust on/off, left/right turn.
//   2. Apply turn: advance floatDir by (turnspeed × turn_dir) − turnvel × turnresistance.
//   3. Set acceleration from thrust: acc = (cos(dir), sin(dir)) × power / mass.
//   4. Integrate velocity: vel += (acc + gravity) × 1.0 (timeStep = 1 tick).
//   5. Clamp speed to SPEED_LIMIT.
//   6. Integrate position: pos += vel × CLICK.
//   7. Wrap position to world bounds.
//   8. Wall collision: if new block is solid, bounce and optionally kill.

/**
 * Stateless physics engine.  Call [tickPlayer] once per game tick for each
 * ALIVE player.
 */
object ServerPhysics {
    // Bounce velocity multiplier on wall hit (energy loss ≈ 70%).
    internal const val BOUNCE_FACTOR = -0.3

    // Wall kill: speed above this on impact kills the player.
    // Default for maxUnshieldedWallBounceSpeed from xpilot-ng cmdline.c: "90.0"
    internal const val WALL_KILL_SPEED = 90.0

    // Fuel consumption coefficient: f = power * FUEL_BURN_COEFF per thrust tick.
    // Value 0.0008 from update.c (derived empirically from C source gameplay tuning).
    internal const val FUEL_BURN_COEFF = 0.0008

    /**
     * Advance physics for [pl] by one tick.
     *
     * @param pl     The player to update.
     * @param world  The game world (for gravity and wall queries).
     * @param frameLoop Current frame counter (used for wall-touch tracking).
     * @return [WallHitResult] if the player collided with a wall this tick.
     */
    fun tickPlayer(
        pl: Player,
        world: World,
        frameLoop: Long,
    ): WallHitResult {
        if (!pl.isAlive()) return WallHitResult.NONE

        // ------------------------------------------------------------------
        // 1. Decode keyboard
        // ------------------------------------------------------------------
        val thrusting = pl.lastKeyv[Key.KEY_THRUST.ordinal]
        val turnLeft = pl.lastKeyv[Key.KEY_TURN_LEFT.ordinal]
        val turnRight = pl.lastKeyv[Key.KEY_TURN_RIGHT.ordinal]

        // Update THRUSTING status bit
        val statusInt = pl.objStatus.toInt()
        pl.objStatus =
            if (thrusting) {
                (statusInt or ObjStatus.THRUSTING).toUShort()
            } else {
                (statusInt and ObjStatus.THRUSTING.inv()).toUShort()
            }

        // ------------------------------------------------------------------
        // 2. Turning
        // ------------------------------------------------------------------
        // C model (event.c + update.c Players_turn):
        //   turnacc is SET each frame from key state (not accumulated):
        //     turnacc = 0; if left: turnacc += turnspeed; if right: turnacc -= turnspeed
        //   Then: turnvel += turnacc * timeStep  (timeStep = 1.0)
        //   If turnresistance != 0: turnvel *= turnresistance  (direct multiplier, NOT 1-r)
        //   float_dir += turnvel
        //   If turnresistance == 0: turnvel = 0  (linear mode)
        //
        // Unit convention in Kotlin port:
        //   turnvel / turnacc are in RES units (same as C).
        //   floatDir is in radians → convert: floatDir += turnvel / RES * 2π
        pl.turnacc =
            when {
                turnLeft && turnRight -> 0.0

                // both pressed → no turn
                turnLeft -> pl.turnspeed

                turnRight -> -pl.turnspeed

                else -> 0.0
            }
        pl.turnvel += pl.turnacc // timeStep = 1.0
        if (pl.turnresistance != 0.0) {
            pl.turnvel *= pl.turnresistance
        } else {
            pl.turnvel = 0.0
        }

        val twoPi = 2.0 * GameConst.PI_VALUE
        val newDir = ((pl.floatDir + pl.turnvel / GameConst.RES * twoPi) % twoPi + twoPi) % twoPi
        pl.setFloatDir(newDir)
        pl.dir = ServerGameWorld.radToDir(newDir)

        // ------------------------------------------------------------------
        // 3. Thrust acceleration
        // ------------------------------------------------------------------
        val gravBlock = world.gravity
        val bx = (pl.pos.cx / ClickConst.BLOCK_CLICKS).coerceIn(0, world.x - 1)
        val by = (pl.pos.cy / ClickConst.BLOCK_CLICKS).coerceIn(0, world.y - 1)
        val grav: Vector = if (gravBlock.isNotEmpty()) gravBlock[bx][by] else Vector.ZERO

        if (thrusting) {
            val thrustForce = pl.power / pl.mass
            pl.acc =
                Vector(
                    (pl.floatDirCos * thrustForce).toFloat(),
                    (pl.floatDirSin * thrustForce).toFloat(),
                )
            // Consume fuel: f = power * 0.0008  (from update.c)
            // When out of fuel C uses minimal power (but still thrusts), here we
            // simply stop consuming so fuel never goes below zero.
            if (pl.fuel.sum > 0.0) {
                pl.fuel.sum = (pl.fuel.sum - pl.power * FUEL_BURN_COEFF).coerceAtLeast(0.0)
            }
        } else {
            pl.acc = Vector.ZERO
        }

        // ------------------------------------------------------------------
        // 3b. Per-tick item fuel drain and timer decrements (from update.c Use_items)
        // ------------------------------------------------------------------
        useItems(pl)

        // ------------------------------------------------------------------
        // 4. Velocity integration (timeStep = 1.0)
        // ------------------------------------------------------------------
        val gravityEnabled = (pl.objStatus.toInt() and ObjStatus.GRAVITY) != 0
        if (gravityEnabled) {
            pl.vel =
                Vector(
                    pl.vel.x + (pl.acc.x + grav.x),
                    pl.vel.y + (pl.acc.y + grav.y),
                )
        } else {
            pl.vel =
                Vector(
                    pl.vel.x + pl.acc.x,
                    pl.vel.y + pl.acc.y,
                )
        }

        // ------------------------------------------------------------------
        // 5. Speed: record pre-clamp speed for wall kill check (C does not
        //    clamp player speed before the wall collision test), then clamp
        //    for physics stability.
        // ------------------------------------------------------------------
        val rawSpeed = sqrt((pl.vel.x * pl.vel.x + pl.vel.y * pl.vel.y).toDouble())
        if (rawSpeed > GameConst.SPEED_LIMIT) {
            val scale = GameConst.SPEED_LIMIT / rawSpeed
            pl.vel = Vector((pl.vel.x * scale).toFloat(), (pl.vel.y * scale).toFloat())
            pl.velocity = GameConst.SPEED_LIMIT
        } else {
            pl.velocity = rawSpeed
        }

        // ------------------------------------------------------------------
        // 6. Position integration  (vel is pixels/tick; pos is clicks)
        // ------------------------------------------------------------------
        val newCx = pl.pos.cx + (pl.vel.x * ClickConst.CLICK).toInt()
        val newCy = pl.pos.cy + (pl.vel.y * ClickConst.CLICK).toInt()

        // ------------------------------------------------------------------
        // 7. World wrap
        // ------------------------------------------------------------------
        val wrappedCx = world.wrapXClick(newCx)
        val wrappedCy = world.wrapYClick(newCy)

        // ------------------------------------------------------------------
        // 8. Wall collision
        // ------------------------------------------------------------------
        val newBx = (wrappedCx / ClickConst.BLOCK_CLICKS).coerceIn(0, world.x - 1)
        val newBy = (wrappedCy / ClickConst.BLOCK_CLICKS).coerceIn(0, world.y - 1)

        return if (world.isSolid(newBx, newBy)) {
            handleWallCollision(pl, bx, by, newBx, newBy, frameLoop, rawSpeed)
        } else {
            // Free movement — update position
            pl.pos =
                org.lambertland.kxpilot.common
                    .ClPos(wrappedCx, wrappedCy)
            WallHitResult.NONE
        }
    }

    // -----------------------------------------------------------------------
    // Per-tick item processing (update.c Use_items, lines 654–715)
    // -----------------------------------------------------------------------

    /**
     * Apply per-tick fuel drain for active items and decrement their timers.
     * Called once per tick from [tickPlayer].
     *
     * Fuel drain amounts from [EnergyDrain]; timer constants from [GameConst].
     * Matches C server `Use_items()` in server/update.c.
     */
    private fun useItems(pl: Player) {
        // Per-tick fuel drain for active items
        if ((pl.used and PlayerAbility.SHIELD) != 0L) {
            pl.fuel.sum = (pl.fuel.sum + EnergyDrain.SHIELD).coerceAtLeast(0.0)
        }
        if (pl.isPhasing()) {
            pl.fuel.sum = (pl.fuel.sum + EnergyDrain.PHASING_DEVICE).coerceAtLeast(0.0)
        }
        if (pl.isCloaked()) {
            pl.fuel.sum = (pl.fuel.sum + EnergyDrain.CLOAKING_DEVICE).coerceAtLeast(0.0)
        }
        if ((pl.used and PlayerAbility.DEFLECTOR) != 0L) {
            pl.fuel.sum = (pl.fuel.sum + EnergyDrain.DEFLECTOR).coerceAtLeast(0.0)
        }

        // Timer decrements — item deactivates when its timer reaches zero
        if (pl.shieldTime > 0.0) {
            pl.shieldTime -= 1.0
            if (pl.shieldTime <= 0.0) {
                pl.shieldTime = 0.0
                pl.used = pl.used and PlayerAbility.SHIELD.inv()
            }
        }
        if (pl.isPhasing() && pl.phasingLeft > 0.0) {
            pl.phasingLeft -= 1.0
            if (pl.phasingLeft <= 0.0) {
                pl.phasingLeft = 0.0
                pl.used = pl.used and PlayerAbility.PHASING_DEVICE.inv()
            }
        }
        if ((pl.used and PlayerAbility.EMERGENCY_THRUST) != 0L && pl.emergencyThrustLeft > 0.0) {
            pl.emergencyThrustLeft -= 1.0
            if (pl.emergencyThrustLeft <= 0.0) {
                pl.emergencyThrustLeft = 0.0
                pl.used = pl.used and PlayerAbility.EMERGENCY_THRUST.inv()
            }
        }
        if ((pl.used and PlayerAbility.EMERGENCY_SHIELD) != 0L && pl.emergencyShieldLeft > 0.0) {
            pl.emergencyShieldLeft -= 1.0
            if (pl.emergencyShieldLeft <= 0.0) {
                pl.emergencyShieldLeft = 0.0
                pl.used = pl.used and PlayerAbility.EMERGENCY_SHIELD.inv()
            }
        }
    }

    // -----------------------------------------------------------------------
    // Wall collision response
    // -----------------------------------------------------------------------

    private fun handleWallCollision(
        pl: Player,
        oldBx: Int,
        oldBy: Int,
        newBx: Int,
        newBy: Int,
        frameLoop: Long,
        impactSpeed: Double = pl.velocity,
    ): WallHitResult {
        // Determine which axis changed block — bounce on that axis
        val xBlocked = newBx != oldBx
        val yBlocked = newBy != oldBy

        if (xBlocked) pl.vel = Vector(pl.vel.x * BOUNCE_FACTOR.toFloat(), pl.vel.y)
        if (yBlocked) pl.vel = Vector(pl.vel.x, pl.vel.y * BOUNCE_FACTOR.toFloat())

        // If neither axis changed (moved diagonally into corner), reverse both
        if (!xBlocked && !yBlocked) {
            pl.vel = Vector(pl.vel.x * BOUNCE_FACTOR.toFloat(), pl.vel.y * BOUNCE_FACTOR.toFloat())
        }

        // Clamp position back to the last valid block centre
        val safeBx = if (xBlocked || (!xBlocked && !yBlocked)) oldBx else newBx
        val safeBy = if (yBlocked || (!xBlocked && !yBlocked)) oldBy else newBy
        val bc = ClickConst.BLOCK_CLICKS
        pl.pos =
            org.lambertland.kxpilot.common
                .ClPos(safeBx * bc + bc / 2, safeBy * bc + bc / 2)
        pl.lastWallTouch = frameLoop

        // Kill if hitting wall too fast
        return if (impactSpeed > WALL_KILL_SPEED) {
            WallHitResult.KILLED
        } else {
            WallHitResult.BOUNCED
        }
    }
}

// ---------------------------------------------------------------------------
// WallHitResult
// ---------------------------------------------------------------------------

/** Outcome of a per-tick wall collision check. */
enum class WallHitResult {
    /** No wall collision this tick. */
    NONE,

    /** Ship bounced off a wall (low speed). */
    BOUNCED,

    /** Ship hit a wall at lethal speed and should be killed. */
    KILLED,
}
