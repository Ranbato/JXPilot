package org.lambertland.kxpilot.server

import org.lambertland.kxpilot.common.ClPos
import org.lambertland.kxpilot.common.ClickConst
import org.lambertland.kxpilot.common.EnergyDrain
import org.lambertland.kxpilot.common.GameConst
import org.lambertland.kxpilot.common.Item
import org.lambertland.kxpilot.common.Key
import org.lambertland.kxpilot.common.Vector
import org.lambertland.kxpilot.common.toPixel
import kotlin.math.cos
import kotlin.math.sin
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
     * Delegates to [tickPlayer(PhysicsState, GameObjectBase, World, Long)].
     * Preserved for all existing call sites that pass a [Player] directly.
     */
    fun tickPlayer(
        pl: Player,
        world: World,
        frameLoop: Long,
    ): WallHitResult = tickPlayer(pl.physics, pl, world, frameLoop)

    /**
     * Advance physics for a [PhysicsState] + [GameObjectBase] pair by one tick.
     *
     * This overload does not require a full [Player] object — create a bare
     * [PhysicsState] and any [GameObjectBase] subclass in unit tests to verify
     * physics without any network/identity scaffolding.
     *
     * @param phys       The per-tick physics state (turn, thrust, fuel, …).
     * @param base       The spatial state (pos, vel, acc, mass, objStatus, …).
     * @param world      The game world (for gravity and wall queries).
     * @param frameLoop  Current frame counter (used for wall-touch tracking).
     * @return [WallHitResult] if the player collided with a wall this tick.
     */
    fun tickPlayer(
        phys: PhysicsState,
        base: GameObjectBase,
        world: World,
        frameLoop: Long,
    ): WallHitResult {
        if (!phys.isAlive()) return WallHitResult.NONE

        // ------------------------------------------------------------------
        // 0. Update ship mass: empty mass + fuel mass (I4)
        //    Matches C server: mass = emptyMass + FUEL_MASS(fuel.sum)
        // ------------------------------------------------------------------
        base.mass = (phys.emptyMass + GameConst.fuelMass(phys.fuel.sum)).toFloat()

        // ------------------------------------------------------------------
        // 1. Decode keyboard
        // ------------------------------------------------------------------
        val thrusting = phys.lastKeyv[Key.KEY_THRUST.ordinal]
        val turnLeft = phys.lastKeyv[Key.KEY_TURN_LEFT.ordinal]
        val turnRight = phys.lastKeyv[Key.KEY_TURN_RIGHT.ordinal]

        // Update THRUSTING status bit
        val statusInt = base.objStatus.toInt()
        base.objStatus =
            if (thrusting) {
                (statusInt or ObjStatus.THRUSTING).toUShort()
            } else {
                (statusInt and ObjStatus.THRUSTING.inv()).toUShort()
            }

        // ------------------------------------------------------------------
        // 2. Turning
        // ------------------------------------------------------------------
        phys.turnacc =
            when {
                turnLeft && turnRight -> 0.0
                turnLeft -> phys.turnspeed
                turnRight -> -phys.turnspeed
                else -> 0.0
            }
        phys.turnvel += phys.turnacc
        if (phys.turnresistance != 0.0) {
            phys.turnvel *= phys.turnresistance
        } else {
            phys.turnvel = 0.0
        }

        val twoPi = 2.0 * GameConst.PI_VALUE
        val newDir = ((phys.floatDir + phys.turnvel / GameConst.RES * twoPi) % twoPi + twoPi) % twoPi
        phys.setFloatDir(newDir)
        phys.dir = ServerGameWorld.radToDir(newDir)

        // ------------------------------------------------------------------
        // 3. Thrust acceleration
        // ------------------------------------------------------------------
        val gravBlock = world.gravity
        val bx = (base.pos.cx / ClickConst.BLOCK_CLICKS).coerceIn(0, world.x - 1)
        val by = (base.pos.cy / ClickConst.BLOCK_CLICKS).coerceIn(0, world.y - 1)
        val grav: Vector = if (gravBlock.isNotEmpty()) gravBlock[bx][by] else Vector.ZERO

        if (thrusting) {
            val thrustForce = phys.power / base.mass
            base.acc =
                Vector(
                    (phys.floatDirCos * thrustForce).toFloat(),
                    (phys.floatDirSin * thrustForce).toFloat(),
                )
            if (phys.fuel.sum > 0.0) {
                phys.fuel.sum = (phys.fuel.sum - phys.power * FUEL_BURN_COEFF).coerceAtLeast(0.0)
            }
        } else {
            base.acc = Vector.ZERO
        }

        // ------------------------------------------------------------------
        // 3b. Per-tick item fuel drain and timer decrements
        // ------------------------------------------------------------------
        useItems(phys, base)

        // ------------------------------------------------------------------
        // 4. Velocity integration (timeStep = 1.0)
        // ------------------------------------------------------------------
        val gravityEnabled = (base.objStatus.toInt() and ObjStatus.GRAVITY) != 0
        if (gravityEnabled) {
            base.vel =
                Vector(
                    base.vel.x + (base.acc.x + grav.x),
                    base.vel.y + (base.acc.y + grav.y),
                )
        } else {
            base.vel =
                Vector(
                    base.vel.x + base.acc.x,
                    base.vel.y + base.acc.y,
                )
        }

        // ------------------------------------------------------------------
        // 5. Speed clamp
        // ------------------------------------------------------------------
        val rawSpeed = sqrt((base.vel.x * base.vel.x + base.vel.y * base.vel.y).toDouble())
        if (rawSpeed > GameConst.SPEED_LIMIT) {
            val scale = GameConst.SPEED_LIMIT / rawSpeed
            base.vel = Vector((base.vel.x * scale).toFloat(), (base.vel.y * scale).toFloat())
            phys.velocity = GameConst.SPEED_LIMIT
        } else {
            phys.velocity = rawSpeed
        }

        // ------------------------------------------------------------------
        // 6. Position integration
        // ------------------------------------------------------------------
        val newCx = base.pos.cx + (base.vel.x * ClickConst.CLICK).toInt()
        val newCy = base.pos.cy + (base.vel.y * ClickConst.CLICK).toInt()

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
            handleWallCollision(phys, base, bx, by, newBx, newBy, frameLoop, rawSpeed)
        } else {
            val newPos = ClPos(wrappedCx, wrappedCy)
            phys.lastSafePos = base.pos
            base.pos = newPos
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
    private fun useItems(
        phys: PhysicsState,
        base: GameObjectBase,
    ) {
        // Per-tick fuel drain for active items
        if ((phys.used and PlayerAbility.SHIELD) != 0L) {
            phys.fuel.sum = (phys.fuel.sum + EnergyDrain.SHIELD).coerceAtLeast(0.0)
            // Deactivate shield when fuel is exhausted (no timer-based override needed)
            if (phys.fuel.sum <= 0.0) {
                phys.used = phys.used and PlayerAbility.SHIELD.inv()
            }
        }
        if (phys.isPhasing()) {
            phys.fuel.sum = (phys.fuel.sum + EnergyDrain.PHASING_DEVICE).coerceAtLeast(0.0)
        }
        if (phys.isCloaked()) {
            phys.fuel.sum = (phys.fuel.sum + EnergyDrain.CLOAKING_DEVICE).coerceAtLeast(0.0)
        }
        if ((phys.used and PlayerAbility.DEFLECTOR) != 0L) {
            phys.fuel.sum = (phys.fuel.sum + EnergyDrain.DEFLECTOR).coerceAtLeast(0.0)
        }

        // Timer decrements — item deactivates when its timer reaches zero
        if (phys.shieldTime > 0.0) {
            phys.shieldTime -= 1.0
            if (phys.shieldTime <= 0.0) {
                phys.shieldTime = 0.0
                phys.used = phys.used and PlayerAbility.SHIELD.inv()
            }
        }
        if (phys.isPhasing() && phys.phasingLeft > 0.0) {
            phys.phasingLeft -= 1.0
            if (phys.phasingLeft <= 0.0) {
                phys.phasingLeft = 0.0
                phys.used = phys.used and PlayerAbility.PHASING_DEVICE.inv()
            }
        }
        if ((phys.used and PlayerAbility.EMERGENCY_THRUST) != 0L && phys.emergencyThrustLeft > 0.0) {
            phys.emergencyThrustLeft -= 1.0
            if (phys.emergencyThrustLeft <= 0.0) {
                phys.emergencyThrustLeft = 0.0
                phys.used = phys.used and PlayerAbility.EMERGENCY_THRUST.inv()
            }
        }
        if ((phys.used and PlayerAbility.EMERGENCY_SHIELD) != 0L && phys.emergencyShieldLeft > 0.0) {
            phys.emergencyShieldLeft -= 1.0
            if (phys.emergencyShieldLeft <= 0.0) {
                phys.emergencyShieldLeft = 0.0
                phys.used = phys.used and PlayerAbility.EMERGENCY_SHIELD.inv()
            }
        }

        // Refueling — drain fuel from the docked depot into the player's tank
        if ((phys.used and PlayerAbility.REFUEL) != 0L) {
            doRefuel(phys, base)
        }
    }

    private fun doRefuel(
        phys: PhysicsState,
        base: GameObjectBase,
    ) {
        val depot = phys.refuelTarget
        if (depot == null) {
            phys.used = phys.used and PlayerAbility.REFUEL.inv()
            return
        }

        // Distance check: abort if player has drifted more than 90 clicks away
        val dx = (base.pos.cx - depot.pos.cx).toLong()
        val dy = (base.pos.cy - depot.pos.cy).toLong()
        if (dx * dx + dy * dy > 90L * 90L) {
            phys.used = phys.used and PlayerAbility.REFUEL.inv()
            phys.refuelTarget = null
            return
        }

        // Full-tank check: stop refueling when tank is at capacity.
        if (phys.fuel.sum >= phys.fuel.max) {
            phys.used = phys.used and PlayerAbility.REFUEL.inv()
            phys.refuelTarget = null
            return
        }

        // Transfer up to REFUEL_RATE units from depot to player.
        val available = depot.withdraw(GameConst.REFUEL_RATE)
        if (available <= 0.0) {
            phys.used = phys.used and PlayerAbility.REFUEL.inv()
            phys.refuelTarget = null
            return
        }
        phys.fuel.sum = (phys.fuel.sum + available).coerceAtMost(phys.fuel.max)
    }

    // -----------------------------------------------------------------------
    // Depot proximity scan — wire refueling when close enough
    // -----------------------------------------------------------------------

    /**
     * For each ALIVE player, check proximity to all fuel depots.  If within
     * 90 clicks of a non-empty depot (and not already refueling), set the
     * [PlayerAbility.REFUEL] bit and assign [Player.refuelTarget].
     *
     * Mirrors the collision detection in C collision.c that triggers
     * `Do_refuel` via `USES_REFUEL`.
     *
     * @param players All active players.
     * @param fuels   The world's fuel depot list.
     */
    fun tickDepotProximity(
        players: Map<Int, Player>,
        fuels: List<Fuel>,
    ) {
        if (fuels.isEmpty()) return
        for ((_, pl) in players) {
            if (!pl.isAlive()) continue
            if ((pl.used and PlayerAbility.REFUEL) != 0L) continue // already refueling
            if (pl.fuel.sum >= pl.fuel.max) continue // tank already full

            val plCx = pl.pos.cx
            val plCy = pl.pos.cy
            for (depot in fuels) {
                if (depot.fuel <= 0.0) continue
                // Bounding-box pre-filter: skip depots whose X or Y distance alone
                // exceeds the 90-click radius (avoids the multiply for distant depots).
                val adx = (plCx - depot.pos.cx)
                val ady = (plCy - depot.pos.cy)
                if (adx > 90 || adx < -90 || ady > 90 || ady < -90) continue
                val dx = adx.toLong()
                val dy = ady.toLong()
                if (dx * dx + dy * dy <= 90L * 90L) {
                    pl.refuelTarget = depot
                    pl.used = pl.used or PlayerAbility.REFUEL
                    break
                }
            }
        }
    }
    // -----------------------------------------------------------------------

    private fun handleWallCollision(
        phys: PhysicsState,
        base: GameObjectBase,
        oldBx: Int,
        oldBy: Int,
        newBx: Int,
        newBy: Int,
        frameLoop: Long,
        impactSpeed: Double = phys.velocity,
    ): WallHitResult {
        // Determine which axis changed block — bounce on that axis
        val xBlocked = newBx != oldBx
        val yBlocked = newBy != oldBy

        if (xBlocked) base.vel = Vector(base.vel.x * BOUNCE_FACTOR.toFloat(), base.vel.y)
        if (yBlocked) base.vel = Vector(base.vel.x, base.vel.y * BOUNCE_FACTOR.toFloat())

        // If neither axis changed (moved diagonally into corner), reverse both.
        if (!xBlocked && !yBlocked) {
            base.vel = Vector(base.vel.x * BOUNCE_FACTOR.toFloat(), base.vel.y * BOUNCE_FACTOR.toFloat())
            base.pos = phys.lastSafePos
            phys.lastWallTouch = frameLoop
            return if (impactSpeed > WALL_KILL_SPEED) WallHitResult.KILLED else WallHitResult.BOUNCED
        }

        // Single-axis collision — snap to the safe side's block centre.
        val safeBx = if (xBlocked) oldBx else newBx
        val safeBy = if (yBlocked) oldBy else newBy
        val bc = ClickConst.BLOCK_CLICKS
        base.pos = ClPos(safeBx * bc + bc / 2, safeBy * bc + bc / 2)
        phys.lastWallTouch = frameLoop

        return if (impactSpeed > WALL_KILL_SPEED) {
            WallHitResult.KILLED
        } else {
            WallHitResult.BOUNCED
        }
    }

    // -----------------------------------------------------------------------
    // Shot firing
    // -----------------------------------------------------------------------

    /**
     * Fire a shot for [pl] if the player's fire key is held and the cooldown
     * has elapsed.  The shot is allocated from [pools] and initialised with
     * the player's position, velocity, and direction.
     *
     * @param pl        The firing player.
     * @param pools     Object pool — shot is allocated from [ObjectPools.shots].
     * @param sessionId The session id stored in shot.id for kill credit.
     */
    fun tryFireShot(
        pl: Player,
        pools: ObjectPools,
        sessionId: Int,
    ) {
        if (!pl.isAlive()) return
        if (!pl.lastKeyv[Key.KEY_FIRE_SHOT.ordinal]) return
        if (pl.shots >= GameConst.NUM_SHOTS) return
        if (pl.shotTime > 0.0) return

        val shot = pools.shots.allocate() ?: return // pool exhausted

        shot.pos = pl.pos
        val shotVx = (pl.floatDirCos * GameConst.SHOT_SPEED + pl.vel.x).toFloat()
        val shotVy = (pl.floatDirSin * GameConst.SHOT_SPEED + pl.vel.y).toFloat()
        shot.vel = Vector(shotVx, shotVy)
        shot.life = GameConst.SHOT_LIFE
        shot.type = ObjType.SHOT.code.toUByte()
        shot.id = sessionId.toShort()
        shot.team = pl.team

        pl.shots++
        pl.shotTime = GameConst.SHOT_SPEED_FACTOR
    }

    // -----------------------------------------------------------------------
    // Shot tick — movement, wall collision, ship collision
    // -----------------------------------------------------------------------

    /**
     * Advance all live shots by one tick: move, wrap, check wall/ship collisions.
     *
     * Returns a list of [ShotKillEvent] for each player killed this tick.
     *
     * @param pools    Object pools holding live shots.
     * @param world    The game world geometry.
     * @param players  All active players keyed by session id.
     */
    fun tickShots(
        pools: ObjectPools,
        world: World,
        players: Map<Int, Player>,
    ): List<ShotKillEvent> {
        // Avoid allocating a list when the pool is empty — common case.
        if (pools.shots.count == 0) return emptyList()

        var kills: MutableList<ShotKillEvent>? = null
        val hitRadius = (GameConst.SHOT_RADIUS + GameConst.SHIP_SZ).toDouble()

        pools.shots.forEachAlive { shot ->
            // 1. Move
            val newCx = shot.pos.cx + (shot.vel.x * ClickConst.CLICK).toInt()
            val newCy = shot.pos.cy + (shot.vel.y * ClickConst.CLICK).toInt()

            // 2. Wrap
            val wrappedCx = world.wrapXClick(newCx)
            val wrappedCy = world.wrapYClick(newCy)

            // 3. Wall collision → free (return true)
            val bx = (wrappedCx / ClickConst.BLOCK_CLICKS).coerceIn(0, world.x - 1)
            val by = (wrappedCy / ClickConst.BLOCK_CLICKS).coerceIn(0, world.y - 1)
            if (world.isSolid(bx, by)) return@forEachAlive true

            shot.pos = ClPos(wrappedCx, wrappedCy)

            // 4. Lifetime
            if (shot.tickLife()) return@forEachAlive true

            // 5. Ship collision
            val shotPxX =
                shot.pos.cx
                    .toPixel()
                    .toDouble()
            val shotPxY =
                shot.pos.cy
                    .toPixel()
                    .toDouble()
            val ownerSessionId = shot.id.toInt()

            for ((sessionId, pl) in players) {
                if (!pl.isAlive()) continue
                if (sessionId == ownerSessionId) continue
                // Team immunity: shots do not damage members of the same team
                // (applies to both player shots and cannon shots which carry shot.team).
                // team == 0 means free-for-all / no team — no immunity.
                if (shot.team != 0u.toUShort() && shot.team == pl.team) continue

                val dx =
                    shotPxX -
                        pl.pos.cx
                            .toPixel()
                            .toDouble()
                val dy =
                    shotPxY -
                        pl.pos.cy
                            .toPixel()
                            .toDouble()
                if (dx * dx + dy * dy <= hitRadius * hitRadius) {
                    // Shield absorbs the shot — consume shot but no kill
                    if ((pl.used and PlayerAbility.SHIELD) != 0L) {
                        return@forEachAlive true // shot consumed, player survives
                    }
                    if (kills == null) kills = mutableListOf()
                    kills.add(ShotKillEvent(victimSessionId = sessionId, killerSessionId = ownerSessionId))
                    pl.plState = PlayerState.KILLED
                    pl.recoveryCount = GameConst.RECOVERY_DELAY.toDouble()
                    return@forEachAlive true // shot consumed
                }
            }

            false // shot still alive
        }

        return kills ?: emptyList()
    }

    // -----------------------------------------------------------------------
    // Item pickup
    // -----------------------------------------------------------------------

    /**
     * Check all ALIVE players for proximity to floating [ItemObject]s.
     * Any item within pickup range is collected, added to the player's
     * inventory (clamped to the world item limit), and freed from the pool.
     *
     * Pickup radius: `(SHIP_SZ + ITEM_SIZE/2) * CLICK` clicks
     * (mirrors C collision.c `radius = (SHIP_SZ + obj->pl_radius) * CLICK`
     * where `pl_radius` defaults to `ITEM_SIZE/2 = 8`).
     *
     * @param players  All active players.
     * @param pools    Object pools (items pool is scanned).
     * @param world    The game world (for item limits in [World.items]).
     */
    fun tickItemPickup(
        players: Map<Int, Player>,
        pools: ObjectPools,
        world: World,
    ) {
        // Pickup radius in click-space
        val pickupRadiusClick = (GameConst.SHIP_SZ + org.lambertland.kxpilot.common.Item.ITEM_SIZE / 2) * ClickConst.CLICK
        val radiusSqClick = pickupRadiusClick.toLong() * pickupRadiusClick.toLong()

        pools.items.forEachAlive { item ->
            var consumed = false
            for ((_, pl) in players) {
                if (!pl.isAlive()) continue
                val dx = (pl.pos.cx - item.pos.cx).toLong()
                val dy = (pl.pos.cy - item.pos.cy).toLong()
                if (dx * dx + dy * dy > radiusSqClick) continue

                // Collect the item
                val idx = item.itemType
                if (idx >= 0 && idx < Item.NUM_ITEMS) {
                    val limit = world.items[idx]?.limit ?: Int.MAX_VALUE
                    pl.item[idx] = (pl.item[idx] + item.itemCount).coerceAtMost(limit)

                    // Set have-bits for items that require them
                    when (Item.fromId(idx)) {
                        Item.ARMOR -> {
                            if (pl.item[idx] > 0) pl.have = pl.have or PlayerAbility.ARMOR
                        }

                        Item.MIRROR -> {
                            if (pl.item[idx] > 0) pl.have = pl.have or PlayerAbility.MIRROR
                        }

                        Item.DEFLECTOR -> {
                            if (pl.item[idx] > 0) pl.have = pl.have or PlayerAbility.DEFLECTOR
                        }

                        else -> { /* no have-bit required */ }
                    }
                }
                consumed = true
                break // one player picks up each item
            }
            consumed // return true to free from pool
        }
    }

    // -----------------------------------------------------------------------
    // Player-player ship collision
    // -----------------------------------------------------------------------

    /**
     * Check every pair of ALIVE players for a ship-to-ship collision.
     *
     * Matches C server `collision.c` player-player block with
     * `CRASH_WITH_PLAYER` mode (no bounce, no limited lives):
     *   - Collision radius: `(2 * SHIP_SZ - 6)` pixels (≈ 26 px).
     *   - A player survives if it has an active shield; otherwise it is killed.
     *   - Team-immune pairs are skipped (same team, team > 0).
     *
     * Returns a list of [ShipCollisionEvent] for each player killed this tick.
     *
     * @param players All active players keyed by session id.
     */
    fun tickPlayerCollisions(players: Map<Int, Player>): List<ShipCollisionEvent> {
        if (players.size < 2) return emptyList()

        // Collision radius in pixels: (2*SHIP_SZ - 6)
        val range = (2.0 * GameConst.SHIP_SZ - 6.0)
        val rangeSq = range * range

        // Materialise to a list once for O(n²) indexed access — but filter to
        // alive+non-phasing immediately so the inner loop is shorter.
        val alive =
            players.entries.filterTo(ArrayList(players.size)) { (_, pl) ->
                pl.isAlive() && !pl.isPhasing()
            }
        if (alive.size < 2) return emptyList()

        var kills: MutableList<ShipCollisionEvent>? = null

        for (i in alive.indices) {
            val (idA, plA) = alive[i]

            for (j in i + 1 until alive.size) {
                val (idB, plB) = alive[j]

                // Skip team-immune pairs (same non-zero team)
                if (plA.team.toInt() != 0 && plA.team == plB.team) continue

                val dx =
                    plA.pos.cx
                        .toPixel()
                        .toDouble() -
                        plB.pos.cx
                            .toPixel()
                            .toDouble()
                val dy =
                    plA.pos.cy
                        .toPixel()
                        .toDouble() -
                        plB.pos.cy
                            .toPixel()
                            .toDouble()
                if (dx * dx + dy * dy > rangeSq) continue

                // Collision — kill whichever player has no shield
                val aShielded = (plA.used and PlayerAbility.SHIELD) != 0L
                val bShielded = (plB.used and PlayerAbility.SHIELD) != 0L

                if (!aShielded) {
                    plA.plState = PlayerState.KILLED
                    plA.recoveryCount = GameConst.RECOVERY_DELAY.toDouble()
                    // Track who shoved A so wall-kill credit is attributed correctly.
                    plA.physics.lastWallAttacker = idB
                    if (kills == null) kills = mutableListOf()
                    kills.add(ShipCollisionEvent(victimSessionId = idA, killerSessionId = idB))
                }
                if (!bShielded) {
                    plB.plState = PlayerState.KILLED
                    plB.recoveryCount = GameConst.RECOVERY_DELAY.toDouble()
                    // Track who shoved B so wall-kill credit is attributed correctly.
                    plB.physics.lastWallAttacker = idA
                    if (kills == null) kills = mutableListOf()
                    kills.add(ShipCollisionEvent(victimSessionId = idB, killerSessionId = idA))
                }
            }
        }
        return kills ?: emptyList()
    }

    // -----------------------------------------------------------------------
    // Recovery / respawn tick
    // -----------------------------------------------------------------------

    /**
     * Advance the death-and-recovery state machine for [pl] by one tick.
     *
     * Mirrors the C server logic in server/update.c (recovery_count block)
     * and server/player.c (`Kill_player` / `Player_set_state PL_STATE_APPEARING`):
     *
     *   KILLED   → zero velocity/acc, set recovery_count = RECOVERY_DELAY,
     *              transition to APPEARING.
     *   APPEARING → decrement recovery_count each tick.
     *              When it reaches 0 → call [world.respawn] at the home base
     *              and transition to ALIVE.
     *
     * @param pl    The player to advance.
     * @param world The game world (for base positions).
     */
    fun tickRecovery(
        pl: Player,
        world: ServerGameWorld,
    ) {
        when (pl.plState) {
            PlayerState.KILLED -> {
                // Zero out motion so the wreck doesn't keep drifting
                pl.vel = Vector.ZERO
                pl.acc = Vector.ZERO
                pl.recoveryCount = GameConst.RECOVERY_DELAY.toDouble()
                pl.plState = PlayerState.APPEARING
            }

            PlayerState.APPEARING -> {
                pl.recoveryCount -= 1.0
                if (pl.recoveryCount <= 0.0) {
                    pl.recoveryCount = 0.0
                    val base = world.world.findSpawnBase(pl.team.toInt())
                    val spawnPos =
                        base?.pos
                            ?: org.lambertland.kxpilot.common.ClPos(
                                world.world.cwidth / 2,
                                world.world.cheight / 2,
                            )
                    val spawnDir = if (base != null) ServerGameWorld.dirToRadians(base.dir) else 0.0
                    world.respawn(pl, spawnPos, spawnDir)
                    // plState is set to ALIVE inside world.respawn()
                }
            }

            else -> { /* ALIVE, DEAD, PAUSED, WAITING, UNDEFINED — no action */ }
        }
    }

    // -----------------------------------------------------------------------
    // Shot cooldown tick
    // -----------------------------------------------------------------------

    /**
     * Decrement the per-player shot cooldown and sync the in-flight shot count.
     * Call once per tick per player after [tickShots].
     *
     * @param pl           The player to update.
     * @param liveShotCount Current number of live shots owned by this player.
     */
    fun tickShotCooldown(
        pl: Player,
        liveShotCount: Int,
    ) {
        pl.shots = liveShotCount
        if (pl.shotTime > 0.0) pl.shotTime -= 1.0
    }

    // -----------------------------------------------------------------------
    // Cannon tick — AI fire + dead-tick recovery
    // -----------------------------------------------------------------------

    /**
     * Advance all map cannons by one tick:
     *   1. Decrement [Cannon.deadTicks] for destroyed cannons; skip if > 0.
     *   2. Decrement [Cannon.fireTimer].
     *   3. If any ALIVE, non-team-immune player is within [CannonConst.CANNON_DISTANCE]
     *      pixels, fire a shot aimed at the target (smartness 0 = fixed dir;
     *      smartness ≥ 1 = predictive intercept aim).
     *
     * Smartness levels (mirroring C `cannon.c`):
     *   0 — fire in the cannon's fixed [Cannon.dir] direction.
     *   1 — fire toward the player's current position (direct aim).
     *   2 — fire toward the player's current position (direct aim, closest target).
     *   3 — fire toward the predicted intercept position (linear extrapolation
     *       using `t = dist / shotSpeed`).
     *
     * Cannon shots are fired using id = [CANNON_SHOT_ID] (negative sentinel so
     * [tickShots] cannot attribute a kill to a player session).
     *
     * @param cannons  The world's cannon list.
     * @param players  All active players.
     * @param pools    Object pools; shots are allocated from [ObjectPools.shots].
     */
    fun tickCannons(
        cannons: List<Cannon>,
        players: Map<Int, Player>,
        pools: ObjectPools,
    ) {
        for (cannon in cannons) {
            // Dead cannons wait before coming back online
            if (cannon.deadTicks > 0.0) {
                cannon.deadTicks -= 1.0
                continue
            }

            // Decrement fire cooldown
            if (cannon.fireTimer > 0.0) {
                cannon.fireTimer -= 1.0
                continue
            }

            // Check if any non-team-immune player is in range
            val cannonPxX =
                cannon.pos.cx
                    .toPixel()
                    .toDouble()
            val cannonPxY =
                cannon.pos.cy
                    .toPixel()
                    .toDouble()
            val rangeSq = CannonConst.DISTANCE * CannonConst.DISTANCE

            var targetFound = false
            for ((_, pl) in players) {
                if (!pl.isAlive()) continue
                if (pl.isPhasing()) continue
                // Team-immune: same non-zero team as cannon
                if (cannon.team != 0 && cannon.team == pl.team.toInt()) continue

                val dx =
                    pl.pos.cx
                        .toPixel()
                        .toDouble() - cannonPxX
                val dy =
                    pl.pos.cy
                        .toPixel()
                        .toDouble() - cannonPxY
                if (dx * dx + dy * dy <= rangeSq) {
                    targetFound = true
                    break
                }
            }
            if (!targetFound) continue

            // Determine fire direction based on smartness
            val cannonSpeed = if (cannon.shotSpeed > 0f) cannon.shotSpeed.toDouble() else GameConst.SHOT_SPEED
            val smartness = cannon.smartness.toInt()

            val fireDir: Int =
                if (smartness <= 0) {
                    // Smartness 0: fire in the cannon's fixed direction
                    cannon.dir
                } else {
                    // Smartness ≥ 1: aim at the nearest in-range player
                    // Find the closest in-range, non-immune, alive player
                    var closestDist = Double.MAX_VALUE
                    var closestPl: Player? = null
                    for ((_, pl) in players) {
                        if (!pl.isAlive()) continue
                        if (pl.isPhasing()) continue
                        if (cannon.team != 0 && cannon.team == pl.team.toInt()) continue
                        val dx =
                            pl.pos.cx
                                .toPixel()
                                .toDouble() - cannonPxX
                        val dy =
                            pl.pos.cy
                                .toPixel()
                                .toDouble() - cannonPxY
                        val dist = sqrt(dx * dx + dy * dy)
                        if (dist <= CannonConst.DISTANCE && dist < closestDist) {
                            closestDist = dist
                            closestPl = pl
                        }
                    }
                    if (closestPl == null) {
                        cannon.dir // fallback
                    } else {
                        val pl = closestPl
                        val dx =
                            pl.pos.cx
                                .toPixel()
                                .toDouble() - cannonPxX
                        val dy =
                            pl.pos.cy
                                .toPixel()
                                .toDouble() - cannonPxY

                        if (smartness >= 3) {
                            // Smartness 3: predictive intercept using linear extrapolation.
                            // Estimate time-of-flight as t = dist / shotSpeed (in pixels),
                            // then predict where the player will be at time t.
                            // vel is in pixels/tick, dx/dy are already in pixels — no CLICK
                            // factor here.  The C code works in click-space but we are
                            // already in pixel-space, so the factor must NOT be applied.
                            val t = closestDist / cannonSpeed
                            val predictedDx = dx + pl.vel.x.toDouble() * t
                            val predictedDy = dy + pl.vel.y.toDouble() * t
                            computeDir(predictedDx, predictedDy)
                        } else {
                            // Smartness 1 or 2: direct aim at current position
                            computeDir(dx, dy)
                        }
                    }
                }

            // Fire a shot in the computed direction
            val shot = pools.shots.allocate() ?: continue
            shot.pos = cannon.pos
            val fireRad = ServerGameWorld.dirToRadians(fireDir)
            shot.vel =
                Vector(
                    (cos(fireRad) * cannonSpeed).toFloat(),
                    (sin(fireRad) * cannonSpeed).toFloat(),
                )
            shot.life = GameConst.SHOT_LIFE
            shot.type = ObjType.SHOT.code.toUByte()
            shot.id = CANNON_SHOT_ID // not owned by any player session
            shot.team = cannon.team.toUShort()

            // Reset fire cooldown — same as SHOT_SPEED_FACTOR for simplicity
            cannon.fireTimer = GameConst.SHOT_SPEED_FACTOR
        }
    }

    /**
     * Convert a (dx, dy) offset vector to a direction integer in 0..RES-1.
     * Returns 0 if the vector is zero-length.
     */
    private fun computeDir(
        dx: Double,
        dy: Double,
    ): Int {
        if (dx == 0.0 && dy == 0.0) return 0
        val angle = kotlin.math.atan2(dy, dx)
        return ServerGameWorld.radToDir(angle).toInt()
    }

    /** Sentinel shot id used for cannon-fired shots (no player gets kill credit). */
    const val CANNON_SHOT_ID: Short = -1
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

// ---------------------------------------------------------------------------
// ShotKillEvent — carries kill credit info out of tickShots
// ---------------------------------------------------------------------------

/**
 * Emitted by [ServerPhysics.tickShots] when a shot kills a player.
 *
 * @param victimSessionId  Session id of the killed player.
 * @param killerSessionId  Session id of the player who fired the shot.
 */
data class ShotKillEvent(
    val victimSessionId: Int,
    val killerSessionId: Int,
)

// ---------------------------------------------------------------------------
// ShipCollisionEvent — carries kill credit info out of tickPlayerCollisions
// ---------------------------------------------------------------------------

/**
 * Emitted by [ServerPhysics.tickPlayerCollisions] when a ship-to-ship
 * collision kills a player.
 *
 * @param victimSessionId  Session id of the killed player.
 * @param killerSessionId  Session id of the other player in the collision.
 */
data class ShipCollisionEvent(
    val victimSessionId: Int,
    val killerSessionId: Int,
)
