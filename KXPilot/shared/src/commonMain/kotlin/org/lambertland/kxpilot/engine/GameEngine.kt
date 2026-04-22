@file:OptIn(ExperimentalUnsignedTypes::class)

package org.lambertland.kxpilot.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.lambertland.kxpilot.client.KeyState
import org.lambertland.kxpilot.common.ClPos
import org.lambertland.kxpilot.common.ClickConst
import org.lambertland.kxpilot.common.GameConst
import org.lambertland.kxpilot.common.Key
import org.lambertland.kxpilot.common.Vector
import org.lambertland.kxpilot.common.pixelToClick
import org.lambertland.kxpilot.common.toPixel
import org.lambertland.kxpilot.server.CellType
import org.lambertland.kxpilot.server.ObjStatus
import org.lambertland.kxpilot.server.Player
import org.lambertland.kxpilot.server.PlayerState
import org.lambertland.kxpilot.server.World
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.time.TimeSource

// ---------------------------------------------------------------------------
// Game constants for local simulation
// ---------------------------------------------------------------------------

private object EngineConst {
    /** Angular step per tick (radians). One heading-unit of 128 per full circle. */
    const val TURN_RATE_RAD: Double = 2.0 * PI / GameConst.RES // ≈ 0.04909 rad/tick

    /** Thrust acceleration impulse magnitude in pixels/tick². */
    const val THRUST_POWER: Double = 0.5

    /** Shot initial speed (pixels/tick, relative to player velocity). */
    const val SHOT_SPEED: Double = 12.0

    /** Shot collision radius in pixels (for shot–ship tests). */
    const val SHOT_RADIUS: Double = 2.0

    /** Shot lifetime in ticks. */
    const val SHOT_LIFE: Float = 60f

    /**
     * Velocity decay factor applied every tick (0 = no friction, approaching 1 = instant stop).
     * Kept at 0 to match XPilot default (frictionless space).
     */
    const val FRICTION: Double = 0.0

    /**
     * Maximum sub-step size for swept collision (pixels).
     * Velocity is walked in steps no larger than this to prevent tunneling through walls.
     * Must be ≤ BLOCK_SZ (35) to guarantee at least one sample per block.
     */
    const val SWEEP_STEP: Double = GameConst.BLOCK_SZ.toDouble() - 1.0 // 34 px

    // --- Missiles ---

    /** Missile base speed (pixels/tick). */
    const val MISSILE_SPEED: Double = 10.0

    /** Homing turn rate (radians/tick toward locked target). */
    const val MISSILE_TURN_RATE: Double = 0.08

    /** Missile lifetime (ticks). */
    const val MISSILE_LIFE: Float = 120f

    /** Missile blast radius for NPC hit (pixels). */
    const val MISSILE_RADIUS: Double = 8.0

    // --- Mines ---

    /** Mine arming delay (ticks before proximity is checked). */
    const val MINE_ARM_TICKS: Int = 30

    /** Mine proximity trigger radius (pixels). */
    const val MINE_TRIGGER_RADIUS: Double = 30.0

    /** Mine lifetime (ticks). */
    const val MINE_LIFE: Float = 600f

    // --- Shields ---

    /** Fuel cost per tick when shield is active. */
    const val SHIELD_FUEL_COST: Double = 2.0

    /** Starting player fuel amount. */
    const val INITIAL_FUEL: Double = 3000.0

    // --- Tractor beam ---

    /** Tractor pull/push force in pixels/tick² applied to locked NPC. */
    const val TRACTOR_FORCE: Float = 0.8f

    /** Max tractor effective range (pixels). */
    const val TRACTOR_RANGE: Double = 400.0

    // --- Ball / treasure ---

    /** Ball collision radius (pixels). From const.h: BALL_RADIUS = 10. */
    const val BALL_RADIUS: Double = 10.0

    /** Ball mass (arbitrary units). From cmdline.c default: ballMass = 50. */
    const val BALL_MASS: Double = 50.0

    /** Player ship mass used for momentum exchange (approximate). */
    const val PLAYER_MASS: Double = 60.0

    /** Natural length of the connector spring (pixels). */
    const val CONNECTOR_LENGTH: Double = 120.0

    /** Connector spring constant (force/px). From cmdline.c default. */
    const val CONNECTOR_SPRING: Double = 1650.0

    /** Connector damping coefficient. From cmdline.c default. */
    const val CONNECTOR_DAMPING: Double = 2.0

    /**
     * Maximum stretch/compression ratio before connector breaks.
     * If |1 - dist/naturalLen| > this, the connector snaps.
     */
    const val CONNECTOR_BREAK_RATIO: Double = 0.30

    /** Treasure goal hit-box half-size (pixels). Ball must be within this to score. */
    const val TREASURE_GOAL_RADIUS: Double = 20.0

    /**
     * Dimensionless scale applied to raw spring/damping forces before dividing
     * by mass to obtain pixel/tick² acceleration.
     *
     * Derivation: the C server operates in "click" units (64 clicks = 1 pixel)
     * and its spring constant is expressed per-click².  Translating to pixel
     * space requires dividing by CLICK² = 64² = 4096.  We use a slightly
     * rounded value (1/4096 ≈ 0.000244) and absorb the remainder into the
     * empirically-matched CONNECTOR_SPRING constant.  In practice this keeps
     * peak connector accelerations in the ~0.1–2 px/tick² range matching the
     * original game feel.
     */
    const val CONNECTOR_FORCE_SCALE: Double = 0.0001
}

// ---------------------------------------------------------------------------
// ShotData — lightweight shot record
// ---------------------------------------------------------------------------

/**
 * Represents a single active shot.
 *
 * Plain class (not data class) — fields are mutated every tick; generated
 * equals/hashCode on mutable state produces incorrect results after mutation.
 *
 * @param ownerId  The [Player.id] of the firing player.  Shots skip collision
 *                 against their owner while [freshTick] is true.
 */
class ShotData(
    var pos: ClPos,
    var vel: Vector,
    var life: Float,
    val ownerId: Short,
    /** True on the tick the shot is created; cleared after first position advance. */
    var freshTick: Boolean = true,
)

// ---------------------------------------------------------------------------
// MissileData — heat-seeking missile
// ---------------------------------------------------------------------------

/**
 * A homing missile.  Each tick it steers toward [targetNpcId] (if present and
 * reachable) by rotating its heading up to [EngineConst.MISSILE_TURN_RATE] rad.
 *
 * Plain class (not data class) — [pos] and [headingRad] mutate every tick.
 */
class MissileData(
    var pos: ClPos,
    /** Current heading in radians (Y-up convention). */
    var headingRad: Double,
    var life: Float,
    /** NPC id of the lock target at spawn time; -1 = unguided. */
    val targetNpcId: Int,
    val ownerId: Short,
)

// ---------------------------------------------------------------------------
// MineData — proximity mine
// ---------------------------------------------------------------------------

/**
 * A proximity mine.  Stationary after being dropped; detonates when any ship
 * enters [EngineConst.MINE_TRIGGER_RADIUS] after the arming delay expires.
 *
 * Plain class (not data class) — [armTicks] and [life] mutate every tick.
 * [pos] is set once at drop and never updated; the constructor receives a
 * defensive copy of [ClPos] (value type) so the mine cannot follow the player.
 */
class MineData(
    val pos: ClPos,
    var armTicks: Int = EngineConst.MINE_ARM_TICKS,
    var life: Float = EngineConst.MINE_LIFE,
    val ownerId: Short,
)

// ---------------------------------------------------------------------------
// TreasurePlacement — typed input for spawnBallsFromTreasures
// ---------------------------------------------------------------------------

/**
 * Describes one treasure tile's position and team.
 *
 * Used instead of an anonymous [Triple] so that call sites are self-documenting.
 * Populated from [org.lambertland.kxpilot.resources.MapTreasure] by the factory
 * layer, keeping the engine free of a direct resource dependency.
 */
data class TreasurePlacement(
    /** Block-column index (X) of the treasure tile. */
    val blockX: Int,
    /** Block-row index (Y) of the treasure tile. */
    val blockY: Int,
    /** Team that owns this treasure tile. */
    val team: Int,
)

// ---------------------------------------------------------------------------
// TreasureGoal — static goal zone (immutable after map load)
// ---------------------------------------------------------------------------

/**
 * A fixed-position goal zone derived from a treasure tile.
 *
 * Stored separately from [BallData] so that goal detection always uses the
 * authoritative tile centre, even after a ball has been dragged away from its
 * spawn position.  Populated once by [GameEngine.spawnBallsFromTreasures].
 */
data class TreasureGoal(
    /** World-pixel X of the tile centre. */
    val x: Float,
    /** World-pixel Y of the tile centre. */
    val y: Float,
    /** Team that owns this goal.  A ball touched by the opposing team scores here. */
    val team: Int,
)

// ---------------------------------------------------------------------------
// BallData — treasure ball
// ---------------------------------------------------------------------------

/**
 * A treasure ball that bounces around the world.
 *
 * Plain class (not data class) — all fields mutate every tick.
 *
 * @param homeX  World-pixel X of the treasure tile (spawn/goal location).
 * @param homeY  World-pixel Y of the treasure tile.
 * @param team   Team this treasure belongs to (from the map).
 */
class BallData(
    var pos: ClPos,
    var vel: Vector,
    /** Team that last touched this ball (for scoring). -1 = neutral. */
    var touchTeam: Int = -1,
    /**
     * Id of the player currently holding a connector to this ball.
     * [NO_PLAYER] (-1) means unattached.
     *
     * Int (not Short) to avoid sign-extension ambiguity: Short.MAX_VALUE is
     * 32767, so a naive sentinel of -1.toShort() == 65535.toShort() which could
     * collide with a real id.  Int gives a safe, unambiguous sentinel.
     */
    var connectedPlayerId: Int = NO_PLAYER,
    /** Home treasure tile centre X (pixels). Ball respawns here on score. */
    val homeX: Float,
    /** Home treasure tile centre Y (pixels). */
    val homeY: Float,
    /** Team that owns the treasure tile (opposing team scores by delivering here). */
    val homeTeam: Int,
) {
    companion object {
        /** Sentinel value: ball has no connected player. Aliases [GameConst.NO_ID]. */
        const val NO_PLAYER: Int = GameConst.NO_ID
    }
}

// ---------------------------------------------------------------------------
// GameEngine
// ---------------------------------------------------------------------------

/**
 * Local physics engine owning a [World], one player ship, and all active shots.
 *
 * Coordinate conventions:
 *  - Positions: [ClPos] (click-space; 64 clicks = 1 pixel).
 *  - Velocities: [Vector] in pixels/tick.
 *  - Angles: radians, Y-up convention (cos(0)=right, sin(π/2)=up).
 *  - World is toroidal; [World.wrapXClick] / [World.wrapYClick] enforce wrap.
 *
 * Call [tick] once per frame, then [KeyState.advanceTick].
 */
class GameEngine(
    val world: World,
) {
    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    /** The locally-controlled player ship. */
    val player: Player =
        Player().apply {
            // Use id=1 so shot owner-immunity comparisons are never accidentally
            // true for zero-initialised NPC/enemy ids.
            id = 1
            plState = PlayerState.ALIVE
            val cx = (world.width / 2).pixelToClick()
            val cy = (world.height / 2).pixelToClick()
            pos = ClPos(cx, cy)
            vel = Vector(0f, 0f)
            acc = Vector(0f, 0f)
            setFloatDir(0.0) // heading = right (+X)
            dir = 0
        }

    /** Active shots in the world. */
    val shots: MutableList<ShotData> = mutableListOf()

    /** Active homing missiles. */
    val missiles: MutableList<MissileData> = mutableListOf()

    /** Deployed proximity mines. */
    val mines: MutableList<MineData> = mutableListOf()

    /** Active treasure balls. */
    val balls: MutableList<BallData> = mutableListOf()

    /**
     * Static goal zones loaded from the map.  Populated once by
     * [spawnBallsFromTreasures]; never modified during play.
     * Each entry corresponds to one treasure tile and is the authoritative
     * target position for goal detection — independent of where any ball
     * currently sits.
     */
    val treasureGoals: MutableList<TreasureGoal> = mutableListOf()

    /**
     * All non-shot game objects (enemy ships, debris, etc.).
     * Shots are tracked separately in [shots].
     */
    val objects: MutableList<Any> = mutableListOf()

    // -----------------------------------------------------------------------
    // Lock target state
    // -----------------------------------------------------------------------

    /**
     * NPC id of the currently locked target, or -1 for no lock.
     * Cycling is done via [lockNext] / [lockPrev].
     */
    var lockedNpcId: Int = -1
        private set

    /**
     * Last known world-pixel direction (radians) to the locked target,
     * updated every tick.  Used by [drawHud] to position the lock dot.
     */
    var lockDirRad: Double = Double.NaN
        private set

    /**
     * Distance to locked target in pixels (last known), or 0 if no lock.
     */
    var lockDistPx: Double = 0.0
        private set

    // -----------------------------------------------------------------------
    // Player fuel — local simulation
    // -----------------------------------------------------------------------

    /** Current fuel level. Starts full; depleted by shield/thrust/weapons. */
    var fuel: Double = EngineConst.INITIAL_FUEL
        private set

    /** Max fuel capacity. */
    val fuelMax: Double = EngineConst.INITIAL_FUEL

    /**
     * True when the shield was active during the most recent [tick].
     * Reflects both the key-down state AND fuel availability — the authoritative
     * source for HUD rendering.  Read-only outside the engine.
     */
    var shieldActive: Boolean = false
        private set

    // -----------------------------------------------------------------------
    // Convenience accessors
    // -----------------------------------------------------------------------

    /** Player world-pixel X (Y-up space, for rendering). */
    val playerPixelX: Float get() =
        player.pos.cx
            .toPixel()
            .toFloat()

    /** Player world-pixel Y (Y-up space, for rendering). */
    val playerPixelY: Float get() =
        player.pos.cy
            .toPixel()
            .toFloat()

    // -----------------------------------------------------------------------
    // Tick
    // -----------------------------------------------------------------------

    /**
     * Advance simulation by one frame.
     *
     * Steps:
     *  1. Rotation — integrate [floatDir] continuously; derive [dir] for protocol.
     *  2. Thrust — apply acceleration impulse in heading direction.
     *  3. Shield — drain fuel; prevent shot damage when active.
     *  4. Gravity — add world gravity at player's current block.
     *  5. Velocity integration — vel += acc.
     *  6. Speed clamp — ‖vel‖ ≤ [GameConst.SPEED_LIMIT].
     *  7. Friction — optional velocity decay (zero by default).
     *  8. Swept position integration — walk velocity in sub-steps ≤ SWEEP_STEP.
     *  9. Fire shot / missile / drop mine.
     * 10. Update lock state (direction + distance to locked target).
     * 11. Tractor/pressor beam.
     * 12. Update shots, missiles, mines.
     *
     * @param keys  Key state for this tick.  Call [KeyState.advanceTick] after.
     * @param npcShips  NPC ship list from [DemoGameState] used for lock / missile homing.
     */
    fun tick(
        keys: KeyState,
        npcShips: List<EngineTarget> = emptyList(),
    ) {
        if (!player.isAlive()) return

        // --- 1. Rotation ---
        if (keys.isDown(Key.KEY_TURN_LEFT)) {
            player.setFloatDir(player.floatDir + EngineConst.TURN_RATE_RAD)
        }
        if (keys.isDown(Key.KEY_TURN_RIGHT)) {
            player.setFloatDir(player.floatDir - EngineConst.TURN_RATE_RAD)
        }
        player.setFloatDir(wrapAngle(player.floatDir))
        player.dir = floatDirToIntHeading(player.floatDir)

        // --- 2. Thrust ---
        if (keys.isDown(Key.KEY_THRUST)) {
            val ax = player.floatDirCos * EngineConst.THRUST_POWER
            val ay = player.floatDirSin * EngineConst.THRUST_POWER
            player.acc = Vector(ax.toFloat(), ay.toFloat())
            player.objStatus = (player.objStatus.toInt() or ObjStatus.THRUSTING).toUShort()
        } else {
            player.acc = Vector(0f, 0f)
            player.objStatus = (player.objStatus.toInt() and ObjStatus.THRUSTING.inv()).toUShort()
        }

        // --- 3. Shield --- drain fuel; shots that hit a shielded player are deflected
        shieldActive = keys.isDown(Key.KEY_SHIELD) && fuel > 0.0
        if (shieldActive) {
            fuel = (fuel - EngineConst.SHIELD_FUEL_COST).coerceAtLeast(0.0)
        }

        // --- 4. Gravity ---
        if (world.x > 0 && world.y > 0) {
            val bx = toroidalBlock(playerPixelX, GameConst.BLOCK_SZ.toFloat(), world.x)
            val by = toroidalBlock(playerPixelY, GameConst.BLOCK_SZ.toFloat(), world.y)
            val g = world.gravity[bx][by]
            player.vel = Vector(player.vel.x + g.x, player.vel.y + g.y)
        }

        // --- 5. Integrate velocity ---
        player.vel =
            Vector(
                player.vel.x + player.acc.x,
                player.vel.y + player.acc.y,
            )

        // --- 6. Speed clamp ---
        val speed = hypot(player.vel.x.toDouble(), player.vel.y.toDouble())
        if (speed > GameConst.SPEED_LIMIT) {
            val scale = (GameConst.SPEED_LIMIT / speed).toFloat()
            player.vel = Vector(player.vel.x * scale, player.vel.y * scale)
        }

        // --- 7. Friction ---
        if (EngineConst.FRICTION > 0.0) {
            val decay = (1.0 - EngineConst.FRICTION).toFloat()
            player.vel = Vector(player.vel.x * decay, player.vel.y * decay)
        }

        // --- 8. Swept position integration + wall collision ---
        val (newPos, reflVel) =
            sweepMove(
                pos = player.pos,
                velX = player.vel.x.toDouble(),
                velY = player.vel.y.toDouble(),
                radius = GameConst.SHIP_SZ.toDouble(),
            )
        player.pos = newPos
        if (reflVel != null) {
            player.vel = Vector(reflVel.first.toFloat(), reflVel.second.toFloat())
        }

        // --- 9. Weapons ---
        if (keys.justPressed(Key.KEY_FIRE_SHOT)) {
            spawnShot()
        }
        if (keys.justPressed(Key.KEY_FIRE_MISSILE)) {
            spawnMissile()
        }
        if (keys.justPressed(Key.KEY_DROP_MINE)) {
            dropMine()
        }

        // --- 9b. Lock cycling ---
        if (keys.justPressed(Key.KEY_LOCK_NEXT)) lockNext(npcShips)
        if (keys.justPressed(Key.KEY_LOCK_PREV)) lockPrev(npcShips)

        // --- 10. Lock target tracking ---
        updateLockState(npcShips)

        // --- 11. Tractor / pressor beam ---
        if (keys.isDown(Key.KEY_TRACTOR_BEAM)) {
            applyTractorBeam(npcShips, pressor = false)
        }

        // --- 12. Update shots ---
        val shieldRadius = GameConst.SHIP_SZ + RenderConst.SHIP_RADIUS.toDouble()
        val shotIter = shots.iterator()
        while (shotIter.hasNext()) {
            val shot = shotIter.next()

            val (sp, hitWall) = sweepMoveShot(shot.pos, shot.vel.x.toDouble(), shot.vel.y.toDouble())
            if (hitWall) {
                shotIter.remove()
                continue
            }
            shot.pos = sp

            shot.life -= 1f
            if (shot.life <= 0f) {
                shotIter.remove()
                continue
            }

            if (shot.ownerId == player.id) {
                shot.freshTick = false
                continue
            }
            shot.freshTick = false

            // Shield blocks the shot (reflect it away)
            if (shieldActive && checkCollision(shot.pos, player.pos, shieldRadius)) {
                shotIter.remove()
                continue
            }

            if (checkCollision(shot.pos, player.pos, EngineConst.SHOT_RADIUS + GameConst.SHIP_SZ)) {
                player.plState = PlayerState.KILLED
                shotIter.remove()
            }
        }

        // --- 13. Update missiles ---
        val missIter = missiles.iterator()
        while (missIter.hasNext()) {
            val m = missIter.next()
            m.life -= 1f
            if (m.life <= 0f) {
                missIter.remove()
                continue
            }

            // Homing: steer toward locked NPC if this missile has a target
            val targetNpc = if (m.targetNpcId >= 0) npcShips.firstOrNull { it.id == m.targetNpcId } else null
            val headingRad =
                if (targetNpc != null) {
                    val tx = targetNpc.x.toDouble()
                    val ty = targetNpc.y.toDouble()
                    val mx =
                        m.pos.cx
                            .toPixel()
                            .toDouble()
                    val my =
                        m.pos.cy
                            .toPixel()
                            .toDouble()
                    val desired = atan2(ty - my, tx - mx)
                    val diff = wrapAngle(desired - m.headingRad + PI) - PI
                    m.headingRad = wrapAngle(m.headingRad + diff.coerceIn(-EngineConst.MISSILE_TURN_RATE, EngineConst.MISSILE_TURN_RATE))
                    m.headingRad
                } else {
                    m.headingRad
                }

            val mvx = cos(headingRad) * EngineConst.MISSILE_SPEED
            val mvy = sin(headingRad) * EngineConst.MISSILE_SPEED
            val (mp, mHitWall) = sweepMoveShot(m.pos, mvx, mvy)
            if (mHitWall) {
                missIter.remove()
                continue
            }
            m.pos = mp

            // Hit NPC ships
            var hitNpc = false
            for (npc in npcShips) {
                val npx = npc.x.toDouble().pixelToClickInt()
                val npy = npc.y.toDouble().pixelToClickInt()
                val npcClick = ClPos(world.wrapXClick(npx), world.wrapYClick(npy))
                if (checkCollision(m.pos, npcClick, EngineConst.MISSILE_RADIUS + GameConst.SHIP_SZ)) {
                    npc.shield = false // mark as hit (visual feedback — no HP yet)
                    hitNpc = true
                    break
                }
            }
            if (hitNpc) {
                missIter.remove()
                continue
            }

            // Shield blocks missile
            if (shieldActive && checkCollision(m.pos, player.pos, shieldRadius)) {
                missIter.remove()
                continue
            }
            // Own missiles never harm the player
            if (m.ownerId != player.id) {
                if (checkCollision(m.pos, player.pos, EngineConst.MISSILE_RADIUS + GameConst.SHIP_SZ)) {
                    player.plState = PlayerState.KILLED
                    missIter.remove()
                }
            }
        }

        // --- 14. Update mines ---
        val mineIter = mines.iterator()
        while (mineIter.hasNext()) {
            val mine = mineIter.next()
            mine.life -= 1f
            if (mine.life <= 0f) {
                mineIter.remove()
                continue
            }
            if (mine.armTicks > 0) {
                mine.armTicks--
                continue
            }

            // Check proximity of NPC ships
            var detonated = false
            for (npc in npcShips) {
                val npx = npc.x.toDouble().pixelToClickInt()
                val npy = npc.y.toDouble().pixelToClickInt()
                val npcClick = ClPos(world.wrapXClick(npx), world.wrapYClick(npy))
                if (checkCollision(mine.pos, npcClick, EngineConst.MINE_TRIGGER_RADIUS)) {
                    detonated = true
                    break
                }
            }
            // Own mines don't trigger on player; enemy mines do
            if (!detonated && mine.ownerId != player.id) {
                if (checkCollision(mine.pos, player.pos, EngineConst.MINE_TRIGGER_RADIUS)) {
                    if (!shieldActive) {
                        player.plState = PlayerState.KILLED
                    }
                    detonated = true
                }
            }
            if (detonated) mineIter.remove()
        }

        // --- 15. Update balls ---
        updateBalls(keys)
    }

    // -----------------------------------------------------------------------
    // Ball / treasure physics
    // -----------------------------------------------------------------------

    /**
     * Advance ball physics for one tick.
     *
     * Per tick for each ball:
     *  1. Apply gravity at ball's block.
     *  2. If connected to player: compute spring force (connector model from
     *     shot.c Update_connector_force), apply to ball and player; break if
     *     over-extended.
     *  3. Walk ball using [sweepMove] (same wall bounce as the player).
     *  4. Detect ball–player proximity: elastic momentum exchange + attach.
     *  5. Check if ball entered an opposing team's treasure: score + respawn.
     *
     * **Mutation safety:** [checkBallGoal] may call [respawnBallAtHome], which
     * mutates the ball in-place but never adds or removes entries from [balls].
     * The loop is therefore safe against ConcurrentModificationException.
     * If future changes require adding/removing balls during a tick, defer those
     * operations to a `pendingBallRespawns: MutableList<BallData>` processed
     * after this loop completes.
     */
    private fun updateBalls(keys: KeyState) {
        if (balls.isEmpty()) return

        val playerAlive = player.isAlive()
        val connectorHeld = playerAlive && keys.isDown(Key.KEY_CONNECTOR)

        for (ball in balls) {
            // --- gravity ---
            if (world.x > 0 && world.y > 0) {
                val bpx = (ball.pos.cx.toPixel()).toFloat()
                val bpy = (ball.pos.cy.toPixel()).toFloat()
                val gbx = toroidalBlock(bpx, GameConst.BLOCK_SZ.toFloat(), world.x)
                val gby = toroidalBlock(bpy, GameConst.BLOCK_SZ.toFloat(), world.y)
                val g = world.gravity[gbx][gby]
                ball.vel = Vector(ball.vel.x + g.x, ball.vel.y + g.y)
            }

            // --- connector spring force (if attached to player) ---
            if (ball.connectedPlayerId == player.id.toInt()) {
                if (!playerAlive) {
                    ball.connectedPlayerId = BallData.NO_PLAYER
                } else if (!connectorHeld) {
                    // Player released KEY_CONNECTOR — snap the tow cable.
                    // Matches C server behaviour: connector only persists while
                    // the key is held (shot.c Update_connector_force only runs
                    // when OBJ_CONNECTOR status bit is set, which is gated on
                    // the key).
                    ball.connectedPlayerId = BallData.NO_PLAYER
                } else {
                    val bpx =
                        ball.pos.cx
                            .toPixel()
                            .toDouble()
                    val bpy =
                        ball.pos.cy
                            .toPixel()
                            .toDouble()
                    val ppx = playerPixelX.toDouble()
                    val ppy = playerPixelY.toDouble()
                    var dx = bpx - ppx
                    var dy = bpy - ppy
                    // Toroidal shortest path
                    val halfW = world.width / 2.0
                    val halfH = world.height / 2.0
                    if (dx > halfW) dx -= world.width
                    if (dx < -halfW) dx += world.width
                    if (dy > halfH) dy -= world.height
                    if (dy < -halfH) dy += world.height

                    val dist = hypot(dx, dy)
                    val natural = EngineConst.CONNECTOR_LENGTH
                    val ratio = if (natural > 0.0) (1.0 - dist / natural) else 0.0

                    // Break if over-compressed / over-extended
                    if (kotlin.math.abs(ratio) > EngineConst.CONNECTOR_BREAK_RATIO) {
                        ball.connectedPlayerId = BallData.NO_PLAYER
                    } else if (dist > 0.1) {
                        // Spring + damping along connector axis
                        val nx = dx / dist
                        val ny = dy / dist

                        // Relative velocity of ball toward player (projection onto axis)
                        val relVx = ball.vel.x.toDouble() - player.vel.x.toDouble()
                        val relVy = ball.vel.y.toDouble() - player.vel.y.toDouble()
                        val relV = relVx * nx + relVy * ny // positive = ball moving away

                        // Force on ball toward player (spring pulls when stretched)
                        // F = -k*(dist - natural) - damping*relV
                        val springF = -EngineConst.CONNECTOR_SPRING * (dist - natural)
                        val dampF = -EngineConst.CONNECTOR_DAMPING * relV
                        val totalF = (springF + dampF)

                        // acceleration = F/mass  (pixel/tick² — see EngineConst.CONNECTOR_FORCE_SCALE)
                        val ballAx = (totalF * nx * EngineConst.CONNECTOR_FORCE_SCALE / EngineConst.BALL_MASS).toFloat()
                        val ballAy = (totalF * ny * EngineConst.CONNECTOR_FORCE_SCALE / EngineConst.BALL_MASS).toFloat()
                        val playerAx = (-totalF * nx * EngineConst.CONNECTOR_FORCE_SCALE / EngineConst.PLAYER_MASS).toFloat()
                        val playerAy = (-totalF * ny * EngineConst.CONNECTOR_FORCE_SCALE / EngineConst.PLAYER_MASS).toFloat()

                        ball.vel = Vector(ball.vel.x + ballAx, ball.vel.y + ballAy)
                        player.vel = Vector(player.vel.x + playerAx, player.vel.y + playerAy)
                    }
                }
            }

            // --- ball movement with wall bounce ---
            val (newPos, reflVel) =
                sweepMove(
                    pos = ball.pos,
                    velX = ball.vel.x.toDouble(),
                    velY = ball.vel.y.toDouble(),
                    radius = EngineConst.BALL_RADIUS,
                )
            ball.pos = newPos
            if (reflVel != null) {
                ball.vel = Vector(reflVel.first.toFloat(), reflVel.second.toFloat())
            }

            // --- ball–player collision ---
            if (playerAlive && checkCollision(ball.pos, player.pos, EngineConst.BALL_RADIUS + GameConst.SHIP_SZ)) {
                if (ball.connectedPlayerId != player.id.toInt()) {
                    // Elastic momentum exchange (Delta_mv_partly_elastic approximation)
                    val m1 = EngineConst.PLAYER_MASS
                    val m2 = EngineConst.BALL_MASS
                    val total = m1 + m2
                    val pvx = player.vel.x.toDouble()
                    val pvy = player.vel.y.toDouble()
                    val bvx = ball.vel.x.toDouble()
                    val bvy = ball.vel.y.toDouble()
                    val newPvx = ((m1 - m2) * pvx + 2.0 * m2 * bvx) / total
                    val newPvy = ((m1 - m2) * pvy + 2.0 * m2 * bvy) / total
                    val newBvx = ((m2 - m1) * bvx + 2.0 * m1 * pvx) / total
                    val newBvy = ((m2 - m1) * bvy + 2.0 * m1 * pvy) / total
                    player.vel = Vector(newPvx.toFloat(), newPvy.toFloat())
                    ball.vel = Vector(newBvx.toFloat(), newBvy.toFloat())

                    // Tag with player's team
                    ball.touchTeam = player.team.toInt()

                    // Attach connector if KEY_CONNECTOR held — initial grab
                    if (connectorHeld) {
                        ball.connectedPlayerId = player.id.toInt()
                    }
                }
            }
            // Detach on key-release is handled in the connector-spring block above
            // (when connectedPlayerId == player.id && !connectorHeld).

            // --- scoring: ball enters opposing team's home treasure ---
            checkBallGoal(ball)
        }
    }

    /**
     * Check whether [ball] has entered any **opposing team's** static treasure
     * goal zone.  If so: increment score and respawn the ball at its home.
     *
     * Goal zones are [TreasureGoal] records loaded from the map — fixed tile
     * centres that never move regardless of ball positions.  A team scores by
     * delivering a ball touched by their own team into a goal owned by the
     * opposing team.
     *
     * Note: respawning the ball in-place (via `ball.pos` mutation) is safe here
     * because [updateBalls] iterates by value index and does not add/remove
     * entries.  If that changes, this should be deferred via a pending-respawn
     * list processed after the loop.
     */
    private fun checkBallGoal(ball: BallData) {
        if (ball.touchTeam < 0) return // neutral ball — nobody has touched it
        val bpx =
            ball.pos.cx
                .toPixel()
                .toDouble()
        val bpy =
            ball.pos.cy
                .toPixel()
                .toDouble()
        for (goal in treasureGoals) {
            if (goal.team == ball.touchTeam) continue // same team — cannot score here
            val dx = bpx - goal.x
            val dy = bpy - goal.y
            if (hypot(dx, dy) < EngineConst.TREASURE_GOAL_RADIUS) {
                // Ball delivered to opposing goal — award score to the local player
                // if it was their team (or team 0 in a free-for-all map).
                if (ball.touchTeam == player.team.toInt() || player.team.toInt() == 0) {
                    // player.score is Double (matches C player_t floating-point score
                    // field; whole-number increments are correct for CTF ball delivery).
                    player.score += 1.0
                }
                respawnBallAtHome(ball)
                return
            }
        }
    }

    /** Teleport ball back to its home treasure location with zero velocity.
     *
     * Private: only the engine should respawn balls.  Callers outside the engine
     * that need to reset all balls should use [spawnBallsFromTreasures].
     */
    private fun respawnBallAtHome(ball: BallData) {
        val click = ClickConst.CLICK.toDouble()
        ball.pos =
            ClPos(
                world.wrapXClick((ball.homeX * click).toInt()),
                world.wrapYClick((ball.homeY * click).toInt()),
            )
        ball.vel = Vector(0f, 0f)
        ball.touchTeam = -1
        ball.connectedPlayerId = BallData.NO_PLAYER
    }

    /**
     * Spawn one [BallData] per entry in [treasures], placed at the centre of
     * the corresponding map tile.  Also records the tile centres as static
     * [treasureGoals] so that goal detection does not depend on ball positions.
     * Call once after loading a map.
     */
    fun spawnBallsFromTreasures(treasures: List<TreasurePlacement>) {
        balls.clear()
        treasureGoals.clear()
        val bs = GameConst.BLOCK_SZ.toDouble()
        val click = ClickConst.CLICK.toDouble()
        for (t in treasures) {
            val homeX = (t.blockX * bs + bs / 2.0).toFloat()
            val homeY = (t.blockY * bs + bs / 2.0).toFloat()
            treasureGoals +=
                TreasureGoal(
                    x = homeX,
                    y = homeY,
                    team = t.team,
                )
            balls +=
                BallData(
                    pos =
                        ClPos(
                            world.wrapXClick((homeX * click).toInt()),
                            world.wrapYClick((homeY * click).toInt()),
                        ),
                    vel = Vector(0f, 0f),
                    homeX = homeX,
                    homeY = homeY,
                    homeTeam = t.team,
                )
        }
    }

    // -----------------------------------------------------------------------
    // Respawn
    // -----------------------------------------------------------------------

    /**
     * Reset the player to the world centre and mark them alive.
     * Clears velocity, heading, and all active shots.
     */
    fun respawn() {
        val cx = (world.width / 2).pixelToClick()
        val cy = (world.height / 2).pixelToClick()
        player.pos = ClPos(cx, cy)
        player.vel = Vector(0f, 0f)
        player.acc = Vector(0f, 0f)
        player.setFloatDir(0.0)
        player.dir = 0
        player.plState = PlayerState.ALIVE
        shots.clear()
        missiles.clear()
        mines.clear()
        fuel = fuelMax
        shieldActive = false
        lockedNpcId = -1
        lockDirRad = Double.NaN
        lockDistPx = 0.0
    }

    /**
     * Teleport the player to a spawn base and mark them alive.
     *
     * If the world has at least one base the player is placed at [baseIndex]
     * (clamped to the available count) and oriented according to the base's
     * direction.  If no bases exist, falls back to [respawn] (world centre).
     *
     * @param baseIndex  Preferred base index (0 = first base).  Wraps around
     *                   if the index exceeds the number of available bases.
     *
     * @throws IllegalArgumentException if [baseIndex] is negative.
     */
    fun spawnAtBase(baseIndex: Int = 0) {
        require(baseIndex >= 0) { "baseIndex must be non-negative, was $baseIndex" }
        val base =
            world.bases
                .takeIf { it.isNotEmpty() }
                ?.let { it[baseIndex % it.size] }
        if (base == null) {
            respawn()
            return
        }
        player.pos = base.pos
        player.vel = Vector(0f, 0f)
        player.acc = Vector(0f, 0f)
        // Convert heading units to radians (heading 0 = right = 0 rad)
        val angleRad = base.dir.toDouble() / GameConst.RES.toDouble() * 2.0 * kotlin.math.PI
        player.setFloatDir(angleRad)
        player.dir = base.dir.toShort()
        player.plState = PlayerState.ALIVE
        player.homeBase = base
        shots.clear()
        missiles.clear()
        mines.clear()
        fuel = fuelMax
        shieldActive = false
        lockedNpcId = -1
        lockDirRad = Double.NaN
        lockDistPx = 0.0
    }

    // -----------------------------------------------------------------------
    // Lock target cycling
    // -----------------------------------------------------------------------

    /**
     * Cycle the lock target forward through the NPC ship list.
     * If no target is currently locked, locks the first NPC.
     * If the list is empty, clears the lock.
     */
    fun lockNext(npcShips: List<EngineTarget>) {
        if (npcShips.isEmpty()) {
            lockedNpcId = -1
            return
        }
        val idx = npcShips.indexOfFirst { it.id == lockedNpcId }
        lockedNpcId = npcShips[(idx + 1) % npcShips.size].id
    }

    /**
     * Cycle the lock target backward through the NPC ship list.
     */
    fun lockPrev(npcShips: List<EngineTarget>) {
        if (npcShips.isEmpty()) {
            lockedNpcId = -1
            return
        }
        val idx = npcShips.indexOfFirst { it.id == lockedNpcId }
        lockedNpcId = npcShips[((idx - 1) + npcShips.size) % npcShips.size].id
    }

    /**
     * Update [lockDirRad] and [lockDistPx] toward the currently locked NPC.
     * Called from [tick].
     */
    private fun updateLockState(npcShips: List<EngineTarget>) {
        val target = if (lockedNpcId >= 0) npcShips.firstOrNull { it.id == lockedNpcId } else null
        if (target == null) {
            lockDistPx = 0.0
            lockDirRad = Double.NaN // no target — HUD checks isNaN to suppress dot
            return
        }
        val dx = target.x.toDouble() - playerPixelX.toDouble()
        val dy = target.y.toDouble() - playerPixelY.toDouble()
        lockDistPx = hypot(dx, dy)
        lockDirRad = atan2(dy, dx)
    }

    // -----------------------------------------------------------------------
    // Swept movement — prevents tunneling
    // -----------------------------------------------------------------------

    /**
     * Walk [pos] along ([velX], [velY]) in sub-steps ≤ [EngineConst.SWEEP_STEP] px.
     *
     * On each sub-step, sample the four AABB corners (half-size [radius]) against
     * the block map.  On the first wall contact:
     *  - The sub-step direction is reflected about the wall normal.
     *  - Remaining sub-steps continue with the reflected direction so the ship
     *    bounces away rather than sticking.
     *
     * Returns the final wrapped position and, if a wall was hit, the reflected
     * velocity for the next tick (null = no contact this tick).
     *
     * Reflection rules (matching the C server's polygon normal approach):
     *  - FILLED block face (axis-aligned): negate the velocity component
     *    perpendicular to the face (determined by minimum penetration depth).
     *  - Diagonal (45°) block: reflect about the wall direction using the C server
     *    formula `(fx,fy) = (vx*c + vy*s, vx*s - vy*c)` where c=(dx²-dy²)/l²,
     *    s=2*dx*dy/l² and (dx,dy) is the wall direction vector.
     *    SW↗NE diagonal (REC_LU, REC_RD): swap vx↔vy.
     *    NW↘SE diagonal (REC_RU, REC_LD): negate-and-swap.
     *    Only applied when velocity is directed INTO the solid (mirrors C SIDE()<0).
     */
    private fun sweepMove(
        pos: ClPos,
        velX: Double,
        velY: Double,
        radius: Double,
    ): Pair<ClPos, Pair<Double, Double>?> {
        if (world.x == 0 || world.y == 0) return Pair(pos, null)
        val totalDist = hypot(velX, velY)
        if (totalDist == 0.0) return Pair(pos, null)

        val steps = max(1, ceil(totalDist / EngineConst.SWEEP_STEP).toInt())
        var dx = velX / steps
        var dy = velY / steps

        var cx = pos.cx.toDouble()
        var cy = pos.cy.toDouble()
        val clicksPerPx = ClickConst.CLICK.toDouble()

        // Track the reflected velocity for the next tick (set on first contact).
        var reflectedVel: Pair<Double, Double>? = null

        for (step in 0 until steps) {
            val nextCx = cx + dx * clicksPerPx
            val nextCy = cy + dy * clicksPerPx

            val nextPxX = nextCx / clicksPerPx
            val nextPxY = nextCy / clicksPerPx

            val hit = sampleCorners(nextPxX, nextPxY, radius, dx, dy)

            if (hit != null) {
                val (rdx, rdy) = hit // reflected sub-step direction
                if (reflectedVel == null) {
                    // Scale the sub-step reflection back to full-tick velocity.
                    // dx/dy are vel/steps, so reflected vel = rdx/rdy * steps.
                    reflectedVel = Pair(rdx * steps, rdy * steps)
                }
                dx = rdx
                dy = rdy
                // Don't advance into the wall this sub-step; push the ship to
                // the surface by advancing one sub-step in the reflected direction
                // (clamped so it cannot go negative or exceed the block boundary).
                // This prevents the ship from becoming embedded at wall corners
                // where repeated reflections would freeze the position.
                val pushCx = cx + rdx * clicksPerPx
                val pushCy = cy + rdy * clicksPerPx
                val pushHit = sampleCorners(pushCx / clicksPerPx, pushCy / clicksPerPx, radius, rdx, rdy)
                if (pushHit == null) {
                    cx = pushCx
                    cy = pushCy
                }
                continue
            }

            cx = nextCx
            cy = nextCy
        }

        return Pair(
            ClPos(world.wrapXClick(cx.toInt()), world.wrapYClick(cy.toInt())),
            reflectedVel,
        )
    }

    /**
     * Public façade for NPC/demo ships.  Accepts and returns pixel-space floats
     * (same coordinate system as [DemoShip.x]/[DemoShip.y]).  Internally converts
     * to click coordinates, runs [sweepMove], and converts back.
     *
     * Returns the new (x, y) position and the post-reflection (vx, vy) velocity.
     * If no wall was hit the returned velocity equals the input velocity.
     */
    fun sweepMovePixels(
        x: Float,
        y: Float,
        vx: Float,
        vy: Float,
        radius: Float = RenderConst.SHIP_RADIUS,
    ): Triple<Float, Float, Pair<Float, Float>> {
        val click = ClickConst.CLICK.toDouble()
        val pos = ClPos((x * click).toInt(), (y * click).toInt())
        val (newPos, reflectedVel) = sweepMove(pos, vx.toDouble(), vy.toDouble(), radius.toDouble())
        val newX = newPos.cx.toFloat() / click.toFloat()
        val newY = newPos.cy.toFloat() / click.toFloat()
        val newVx = reflectedVel?.first?.toFloat() ?: vx
        val newVy = reflectedVel?.second?.toFloat() ?: vy
        return Triple(newX, newY, Pair(newVx, newVy))
    }

    /**
     * Variant of [sweepMove] for shots: returns the new position and a flag
     * indicating whether a wall was hit.  Shots are destroyed on contact.
     */
    private fun sweepMoveShot(
        pos: ClPos,
        velX: Double,
        velY: Double,
    ): Pair<ClPos, Boolean> {
        if (world.x == 0 || world.y == 0) {
            return Pair(
                ClPos(
                    world.wrapXClick(pos.cx + velX.pixelToClickInt()),
                    world.wrapYClick(pos.cy + velY.pixelToClickInt()),
                ),
                false,
            )
        }
        val totalDist = hypot(velX, velY)
        if (totalDist == 0.0) return Pair(pos, false)

        val steps = max(1, ceil(totalDist / EngineConst.SWEEP_STEP).toInt())
        val dx = velX / steps
        val dy = velY / steps
        val clicksPerPx = ClickConst.CLICK.toDouble()

        var cx = pos.cx.toDouble()
        var cy = pos.cy.toDouble()

        for (step in 0 until steps) {
            val nextCx = cx + dx * clicksPerPx
            val nextCy = cy + dy * clicksPerPx
            val bx = toroidalBlock((nextCx / clicksPerPx).toFloat(), GameConst.BLOCK_SZ.toFloat(), world.x)
            val by = toroidalBlock((nextCy / clicksPerPx).toFloat(), GameConst.BLOCK_SZ.toFloat(), world.y)
            val cell = world.getBlock(bx, by)
            if (cell == CellType.FILLED) {
                return Pair(ClPos(world.wrapXClick(cx.toInt()), world.wrapYClick(cy.toInt())), true)
            }
            // Diagonal blocks also destroy shots when the centre enters the solid triangle.
            if (cell == CellType.REC_LU || cell == CellType.REC_LD ||
                cell == CellType.REC_RU || cell == CellType.REC_RD
            ) {
                val nx = nextCx / clicksPerPx
                val ny = nextCy / clicksPerPx
                if (cornerInsideDiagonal(cell, nx, ny, bx, by)) {
                    return Pair(ClPos(world.wrapXClick(cx.toInt()), world.wrapYClick(cy.toInt())), true)
                }
            }
            cx = nextCx
            cy = nextCy
        }

        return Pair(ClPos(world.wrapXClick(cx.toInt()), world.wrapYClick(cy.toInt())), false)
    }

    /**
     * Sample four AABB corners at ([pxX], [pxY]) with half-size [radius] against
     * the block map.  Returns the reflected sub-step velocity `(rdx, rdy)` if
     * any corner is inside a solid cell, or `null` if the position is clear.
     *
     * **FILLED blocks** — axis-aligned reflection:
     * The face hit is identified by the minimum penetration depth on each axis
     * (smallest depth = shallowest entry = that face was crossed).  Velocity
     * is negated on the axis perpendicular to the hit face.  The velocity
     * direction is used as a tiebreaker when depths are close (avoids
     * reflecting the wrong axis when grazing a corner at a shallow angle).
     *
     * **Diagonal blocks** — normal reflection about the 45° hypotenuse:
     * Matching the C server polygon normals and tile geometry (xpmap.c):
     *  - `REC_LU` / `REC_RD` (SW↗NE diagonal, direction (1,1)/√2): swap vx↔vy.
     *  - `REC_RU` / `REC_LD` (NW↘SE diagonal, direction (1,-1)/√2): negate-and-swap.
     * Only applied when the corner is on the solid side AND velocity points into
     * the wall (mirrors C server SIDE(vel,line) < 0 guard).
     */
    private fun sampleCorners(
        pxX: Double,
        pxY: Double,
        radius: Double,
        velX: Double,
        velY: Double,
    ): Pair<Double, Double>? {
        val r = radius
        val corners =
            arrayOf(
                Pair(pxX + r, pxY + r),
                Pair(pxX - r, pxY + r),
                Pair(pxX + r, pxY - r),
                Pair(pxX - r, pxY - r),
            )

        var maxPenX = 0.0
        var maxPenY = 0.0
        var anyFilled = false
        // For diagonals: accumulate reflected velocity components (average if
        // multiple corners hit different diagonals).
        var diagVelX = velX
        var diagVelY = velY
        var anyDiag = false

        for ((cx, cy) in corners) {
            val bx = toroidalBlock(cx.toFloat(), GameConst.BLOCK_SZ.toFloat(), world.x)
            val by = toroidalBlock(cy.toFloat(), GameConst.BLOCK_SZ.toFloat(), world.y)
            val cell = world.getBlock(bx, by)

            when (cell) {
                CellType.FILLED -> {
                    anyFilled = true
                    val bs = GameConst.BLOCK_SZ.toDouble()
                    val blockLeft = bx * bs
                    val blockRight = (bx + 1) * bs
                    val blockBottom = by * bs
                    val blockTop = (by + 1) * bs
                    // Penetration depth on each axis: smaller depth = entry face.
                    val penX = minOf(cx - blockLeft, blockRight - cx)
                    val penY = minOf(cy - blockBottom, blockTop - cy)
                    maxPenX = max(maxPenX, penX)
                    maxPenY = max(maxPenY, penY)
                }

                CellType.REC_LU, CellType.REC_LD, CellType.REC_RU, CellType.REC_RD -> {
                    // Only collide if the corner is on the solid side AND velocity
                    // is moving into the wall (mirrors C server SIDE() < 0 guard).
                    if (cornerInsideDiagonal(cell, cx, cy, bx, by) &&
                        velocityIntoWall(cell, velX, velY)
                    ) {
                        // Reflect velocity about the diagonal's wall normal.
                        val (rvx, rvy) = reflectAboutDiagonal(cell, diagVelX, diagVelY)
                        diagVelX = rvx
                        diagVelY = rvy
                        anyDiag = true
                    }
                }

                else -> {} // non-solid cell types: no collision
            }
        }

        if (anyFilled) {
            // Use penetration depth to identify the hit face; velocity direction
            // as tiebreaker to avoid spurious reflections when grazing.
            val eps = 0.5
            val reflX =
                when {
                    velX == 0.0 -> false

                    velY == 0.0 -> true

                    maxPenX < maxPenY - eps -> true

                    // shallower X entry → X face
                    maxPenY < maxPenX - eps -> false

                    // shallower Y entry → Y face
                    else -> true // equal: corner hit, reflect both
                }
            val reflY =
                when {
                    velY == 0.0 -> false
                    velX == 0.0 -> true
                    maxPenY < maxPenX - eps -> true
                    maxPenX < maxPenY - eps -> false
                    else -> true
                }
            val rvx = if (reflX) -velX else velX
            val rvy = if (reflY) -velY else velY
            return Pair(rvx, rvy)
        }

        if (anyDiag) {
            return Pair(diagVelX, diagVelY)
        }

        return null
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Convert a pixel coordinate to a block index with toroidal wrap,
     * consistent with [World.wrapXClick] / [World.wrapYClick].
     */
    internal fun toroidalBlock(
        pixelCoord: Float,
        blockSize: Float,
        worldBlocks: Int,
    ): Int {
        val raw = (pixelCoord / blockSize).toInt()
        return ((raw % worldBlocks) + worldBlocks) % worldBlocks
    }

    /**
     * Returns true if ([cx], [cy]) is on the **solid** side of the diagonal
     * in the block at ([bx], [by]).
     *
     * Block-local normalised coordinates: `fx = cx/BS - bx`, `fy = cy/BS - by`,
     * both in [0, 1].  From xpmap.c (comment at line 1438):
     *   REC_LU = wall triangle pointing left and up   (solid at top-left)
     *   REC_RD = wall triangle pointing right and down (solid at bottom-right)
     *   REC_RU = wall triangle pointing right and up  (solid at top-right)
     *   REC_LD = wall triangle pointing left and down  (solid at bottom-left)
     *
     * The hypotenuse for each pair:
     *   SW↗NE diagonal (direction (1,1)/√2): REC_LU, REC_RD
     *     - REC_LU: solid = upper-left  → fy > fx
     *     - REC_RD: solid = lower-right → fy < fx
     *   NW↘SE diagonal (direction (1,-1)/√2): REC_RU, REC_LD
     *     - REC_RU: solid = upper-right → fy > 1 − fx
     *     - REC_LD: solid = lower-left  → fy < 1 − fx
     */
    private fun cornerInsideDiagonal(
        cell: CellType,
        cx: Double,
        cy: Double,
        bx: Int,
        by: Int,
    ): Boolean {
        val bs = GameConst.BLOCK_SZ.toDouble()
        val fx = cx / bs - bx
        val fy = cy / bs - by
        return when (cell) {
            CellType.REC_LU -> fy > fx
            CellType.REC_RD -> fy < fx
            CellType.REC_RU -> fy > (1.0 - fx)
            CellType.REC_LD -> fy < (1.0 - fx)
            else -> false
        }
    }

    /**
     * Returns true if the velocity ([vx], [vy]) is directed **into** the solid
     * side of the given diagonal [cell].  Matching the C server's
     * `SIDE(vel, line) < 0` guard — only bounce when moving into the wall,
     * never when already moving away from it.
     *
     * Wall outward normals (pointing away from solid):
     *   SW↗NE diagonal:
     *     - REC_LU (solid upper-left):  outward = ( 1,−1)/√2 → bounce when vx − vy < 0
     *     - REC_RD (solid lower-right): outward = (−1, 1)/√2 → bounce when vy − vx < 0
     *   NW↘SE diagonal:
     *     - REC_RU (solid upper-right): outward = ( 1, 1)/√2 → bounce when vx + vy < 0
     *     - REC_LD (solid lower-left):  outward = (−1,−1)/√2 → bounce when vx + vy > 0
     */
    private fun velocityIntoWall(
        cell: CellType,
        vx: Double,
        vy: Double,
    ): Boolean =
        when (cell) {
            CellType.REC_LU -> vx - vy < 0

            // moving toward upper-left solid
            CellType.REC_RD -> vx - vy > 0

            // moving toward lower-right solid
            CellType.REC_RU -> vx + vy > 0

            // moving toward upper-right solid
            CellType.REC_LD -> vx + vy < 0

            // moving toward lower-left solid
            else -> false
        }

    /**
     * Reflect velocity ([vx], [vy]) about the wall normal of a diagonal cell.
     *
     * Using C server formula: `fx = vx*c + vy*s`, `fy = vx*s - vy*c`
     * where c = (dx²−dy²)/l², s = 2·dx·dy/l² and (dx,dy) is wall direction.
     *
     *  - `REC_LU` / `REC_RD`: SW↗NE diagonal, direction (1,1)/√2 → c=0, s=1.
     *    Result: `vx' = vy, vy' = vx`  (swap).
     *  - `REC_RU` / `REC_LD`: NW↘SE diagonal, direction (1,−1)/√2 → c=0, s=−1.
     *    Result: `vx' = −vy, vy' = −vx`  (negate-swap).
     */
    private fun reflectAboutDiagonal(
        cell: CellType,
        vx: Double,
        vy: Double,
    ): Pair<Double, Double> =
        when (cell) {
            CellType.REC_LU, CellType.REC_RD -> Pair(vy, vx)

            // swap
            CellType.REC_LD, CellType.REC_RU -> Pair(-vy, -vx)

            // negate-swap
            else -> Pair(vx, vy)
        }

    /**
     * Returns true if positions [a] and [b] are within [radiusPixels] of each
     * other, using toroidal shortest-path distance.
     */
    private fun checkCollision(
        a: ClPos,
        b: ClPos,
        radiusPixels: Double,
    ): Boolean {
        var dx = (a.cx - b.cx).toPixel().toDouble()
        var dy = (a.cy - b.cy).toPixel().toDouble()
        val halfW = world.width / 2.0
        val halfH = world.height / 2.0
        if (dx > halfW) dx -= world.width
        if (dx < -halfW) dx += world.width
        if (dy > halfH) dy -= world.height
        if (dy < -halfH) dy += world.height
        return hypot(dx, dy) < radiusPixels
    }

    /** Derive integer heading 0..RES-1 from a continuous angle in radians. */
    private fun floatDirToIntHeading(angle: Double): Short {
        val h = (angle / (2.0 * PI) * GameConst.RES).toInt()
        val max = GameConst.RES
        return (((h % max) + max) % max).toShort()
    }

    /** Normalise angle to [0, 2π). */
    private fun wrapAngle(a: Double): Double {
        val twoPi = 2.0 * PI
        var r = a % twoPi
        if (r < 0.0) r += twoPi
        return r
    }

    // -----------------------------------------------------------------------
    // Shot / missile / mine spawning
    // -----------------------------------------------------------------------

    private fun spawnShot() {
        // Spawn one tick ahead of the player to avoid zero-distance self-collision.
        val shotVx = (player.vel.x + player.floatDirCos * EngineConst.SHOT_SPEED).toFloat()
        val shotVy = (player.vel.y + player.floatDirSin * EngineConst.SHOT_SPEED).toFloat()
        val startCx = world.wrapXClick(player.pos.cx + shotVx.toDouble().pixelToClickInt())
        val startCy = world.wrapYClick(player.pos.cy + shotVy.toDouble().pixelToClickInt())
        shots +=
            ShotData(
                pos = ClPos(startCx, startCy),
                vel = Vector(shotVx, shotVy),
                life = EngineConst.SHOT_LIFE,
                ownerId = player.id,
                freshTick = true,
            )
    }

    private fun spawnMissile() {
        missiles +=
            MissileData(
                pos = player.pos,
                headingRad = player.floatDir,
                life = EngineConst.MISSILE_LIFE,
                targetNpcId = lockedNpcId,
                ownerId = player.id,
            )
    }

    private fun dropMine() {
        // Mine is placed at player position and stays stationary
        mines +=
            MineData(
                pos = player.pos,
                armTicks = EngineConst.MINE_ARM_TICKS,
                life = EngineConst.MINE_LIFE,
                ownerId = player.id,
            )
    }

    // -----------------------------------------------------------------------
    // Tractor / pressor beam
    // -----------------------------------------------------------------------

    /**
     * Apply a tractor (attract) or pressor (repel) force to the currently locked
     * NPC ship.  Force magnitude decreases linearly with distance.
     *
     * @param pressor  true = push away (pressor), false = pull toward (tractor).
     */
    private fun applyTractorBeam(
        npcShips: List<EngineTarget>,
        pressor: Boolean,
    ) {
        if (lockedNpcId < 0) return
        val target = npcShips.firstOrNull { it.id == lockedNpcId } ?: return
        val dx = target.x.toDouble() - playerPixelX.toDouble()
        val dy = target.y.toDouble() - playerPixelY.toDouble()
        val dist = hypot(dx, dy)
        if (dist < 1.0 || dist > EngineConst.TRACTOR_RANGE) return
        val scale = (1.0 - dist / EngineConst.TRACTOR_RANGE).toFloat() * EngineConst.TRACTOR_FORCE
        val nx = (dx / dist * scale).toFloat()
        val ny = (dy / dist * scale).toFloat()
        if (pressor) {
            target.vx += nx
            target.vy += ny
        } else {
            target.vx -= nx
            target.vy -= ny
        }
    }

    // -----------------------------------------------------------------------
    // Factory
    // -----------------------------------------------------------------------

    companion object {
        /**
         * Build a [GameEngine] with an empty [widthBlocks] × [heightBlocks] world
         * (all cells [CellType.SPACE]).  Useful for offline / demo play.
         */
        fun forEmptyWorld(
            widthBlocks: Int,
            heightBlocks: Int,
        ): GameEngine {
            val world =
                World().apply {
                    x = widthBlocks
                    y = heightBlocks
                    bwidthFloor = widthBlocks
                    bheightFloor = heightBlocks
                    width = widthBlocks * GameConst.BLOCK_SZ
                    height = heightBlocks * GameConst.BLOCK_SZ
                    cwidth = width * ClickConst.CLICK
                    cheight = height * ClickConst.CLICK
                    block = Array(widthBlocks) { Array(heightBlocks) { CellType.SPACE } }
                    gravity = Array(widthBlocks) { Array(heightBlocks) { Vector(0f, 0f) } }
                }
            return GameEngine(world)
        }
    }
}

// ---------------------------------------------------------------------------
// Internal extension: Double pixels → clicks
// ---------------------------------------------------------------------------

internal fun Double.pixelToClickInt(): Int = (this * ClickConst.CLICK).toInt()

// ---------------------------------------------------------------------------
// Coroutine-based game loop
// ---------------------------------------------------------------------------

/**
 * Launch a fixed-timestep game loop as a child coroutine of [scope].
 *
 * The loop is cancelled automatically when [scope] is cancelled, so the
 * caller controls lifetime (e.g. a Compose `rememberCoroutineScope()` or
 * an injected application scope).
 *
 * [onTick] is called once per tick **synchronously on the coroutine's
 * dispatcher**.  It should call [GameEngine.tick] and advance keys.
 * It is not a `suspend` function; all state mutation happens synchronously
 * to avoid re-entrancy.
 *
 * Time is measured with [TimeSource.Monotonic] (KMP-safe; no JVM APIs).
 *
 * @param scope           The coroutine scope whose lifecycle owns this loop.
 * @param tickIntervalMs  Fixed tick interval in milliseconds (default 16 ≈ 60 Hz).
 * @param onTick          Called once per tick; receiver is this [GameEngine].
 */
fun GameEngine.startLoop(
    scope: CoroutineScope,
    tickIntervalMs: Long = 16L,
    onTick: GameEngine.() -> Unit,
) {
    scope.launch {
        val clock = TimeSource.Monotonic
        var lastMark = clock.markNow()
        var accumMs = 0L
        while (isActive) {
            delay(tickIntervalMs)
            val now = clock.markNow()
            val elapsed = (now - lastMark).inWholeMilliseconds.coerceAtMost(100L)
            lastMark = now
            accumMs += elapsed
            while (accumMs >= tickIntervalMs) {
                onTick()
                accumMs -= tickIntervalMs
            }
        }
    }
}
