package org.lambertland.kxpilot.engine

import org.lambertland.kxpilot.common.GameConst
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sign
import kotlin.math.sin

// ---------------------------------------------------------------------------
// NPC AI — constants
// ---------------------------------------------------------------------------

/**
 * Tuning constants for the NPC combat AI.
 *
 * Values are calibrated to match C XPilot-NG robot behaviour at 60 Hz.
 * The C server runs at 12.5 Hz; per-tick values are scaled by 60/12.5 = 4.8.
 * The C robot used configurable per-robot parameters; here we use the
 * default / max-capability values from robotdef.c.
 */
object NpcAiConst {
    /**
     * Distance at which an NPC notices the player and transitions to CHASE.
     * C: Visibility_distance is dynamic (256–1000 px, robotdef.c:2213-2219).
     * Using 1000 px (VISIBILITY_DISTANCE) as the upper bound default.
     */
    const val DETECT_RANGE_PX: Float = 1000f

    /**
     * Distance at which an NPC transitions from CHASE to ATTACK and starts firing.
     * C: robots fire when target is within ~500 px (robotdef.c:1071 uses half visibility).
     * Deliberately less than DETECT_RANGE_PX so CHASE is a real intermediate state.
     */
    const val ATTACK_RANGE_PX: Float = 500f

    /**
     * Ticks between NPC shots.
     * C: robots fire every 2 C-ticks (robotdef.c:1070: robot_count % 2).
     * Scaled to 60 Hz: 2 * (60 / 12.5) ≈ 10 ticks.
     */
    const val SHOT_COOLDOWN_TICKS: Int = 10

    /**
     * Angular acceleration per tick when the NPC is steering (rad/tick²).
     * C: turnacc = MAX_PLAYER_TURNSPEED = 64 heading-units/C-tick² (robotdef.c:944).
     * In rad/C-tick²: MAX_PLAYER_TURNSPEED * TURN_RATE_RAD ≈ 3.14 rad/C-tick².
     * Scaled to 60 Hz: ÷ HZ_RATIO ≈ 0.654 rad/tick².
     */
    const val TURN_ACC: Double = GameConst.MAX_PLAYER_TURNSPEED * EngineConst.TURN_RATE_RAD / EngineConst.HZ_RATIO

    /**
     * Angular damping keep-fraction per tick — fraction of turnVel *retained* each tick.
     * C autopilot: pl->turnresistance = 0.2 (update.c:261), meaning 80% of turnVel
     * is discarded per tick — snappy, responsive turns.
     * Named TURN_KEEP_FRACTION to reflect its semantic: 0.2 = keep 20%.
     */
    const val TURN_KEEP_FRACTION: Double = 0.2

    /** Patrol/idle drift speed (px/tick). */
    const val CRUISE_SPEED: Float = 0.8f

    /** Speed when closing on a detected player (px/tick). */
    const val CHASE_SPEED: Float = 2.2f

    /** Speed during combat manoeuvres (px/tick). */
    const val ATTACK_SPEED: Float = 1.6f

    /** Speed when fleeing (px/tick). */
    const val EVADE_SPEED: Float = 3.5f

    /** Duration of an evasion burst in ticks (≈ 1.5 s). */
    const val EVADE_DURATION_TICKS: Int = 90

    /**
     * Fuel threshold below which the NPC switches to EVADE.
     * C: robots evade when fuel.sum < fuel_l3 = 150.0 (robotdef.c:280,1125).
     * As fraction of MAX_PLAYER_FUEL=2600: 150/2600 * 2600 = 150 (same units — fuel).
     */
    const val EVADE_HP_THRESHOLD: Float = 150f

    /**
     * Predictive aim lead factor: 0 = shoot at current position, 1 = full lead.
     * C robot uses smart shot homing; we approximate with partial linear prediction.
     */
    const val AIM_LEAD_FACTOR: Float = 0.45f

    /**
     * How fast the NPC shot travels (px/tick) — used to compute lead time.
     * Matches EngineConst.SHOT_SPEED = GameConst.SHOT_SPEED = 21.0 (C default, cmdline.c:182).
     */
    const val NPC_SHOT_SPEED: Double = EngineConst.SHOT_SPEED

    /**
     * Ticks between NPC missile launches.
     * C: last_fired_missile guard ≈ 5 C-ticks (robotdef.c:1041).
     * Scaled to 60 Hz: 5 × HZ_RATIO ≈ 24 ticks.
     */
    const val MISSILE_COOLDOWN_TICKS: Int = 24

    /**
     * Ticks between NPC mine drops.
     * C: last_dropped_mine + 10 C-tick guard (robotdef.c:1091).
     * Scaled to 60 Hz: 10 × HZ_RATIO = 48 ticks.
     */
    const val MINE_DROP_COOLDOWN_TICKS: Int = 48

    /**
     * Maximum distance at which an NPC will drop a mine (pixels).
     * C: robot drops mine when player is close enough to be threatened.
     * Using 3× MINE_TRIGGER_RADIUS = 300 px as a practical threshold.
     */
    const val MINE_DROP_RANGE_PX: Float = 300f
}

// ---------------------------------------------------------------------------
// NPC AI — behavior states
// ---------------------------------------------------------------------------

/** Immutable snapshot of a potential target for one AI tick. */
data class TargetSnapshot(
    val id: Int,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val isAlive: Boolean,
)

/**
 * Behavior state for one NPC ship, matching a subset of the C robot modes:
 *   PATROL  ≈ RM_ROBOT_IDLE    — drifting, scanning for the player
 *   CHASE   ≈ RM_NAVIGATE      — closing distance to player
 *   ATTACK  ≈ RM_ATTACK        — in weapon range, shooting
 *   EVADE   ≈ RM_EVADE_LEFT/RIGHT — fleeing when low HP
 */
enum class NpcBehavior { PATROL, CHASE, ATTACK, EVADE }

// ---------------------------------------------------------------------------
// NPC AI — per-ship state
// ---------------------------------------------------------------------------

/**
 * Mutable per-NPC AI state updated every tick.
 *
 * Lifecycle: created once per [DemoShip] by [NpcAiManager] at map load;
 * reset when the NPC is re-spawned; discarded when the NPC dies.
 */
class NpcAiState(
    val npcId: Int,
    /** Initial patrol heading in radians. */
    patrolAngleRad: Double = 0.0,
) {
    var behavior: NpcBehavior = NpcBehavior.PATROL

    /** Ticks until the next shot is allowed. */
    var shotCooldown: Int = 0

    /**
     * Ticks until the next missile launch is allowed.
     * C: robots use last_fired_missile guard (robotdef.c:1041).
     * Scaled to 60 Hz: ~5 C-ticks × HZ_RATIO ≈ 24 ticks.
     */
    var missileCooldown: Int = 0

    /**
     * Ticks until the next mine drop is allowed.
     * C: last_dropped_mine + 10 C-tick guard (robotdef.c:1091).
     * Scaled to 60 Hz: 10 × HZ_RATIO = 48 ticks.
     */
    var mineDropCooldown: Int = 0

    /** Ticks remaining in the current EVADE burst. */
    var evadeTimer: Int = 0

    /** Id of the target selected on the last tick (-1 if none). */
    var targetId: Int = -1

    /** Slowly rotating patrol heading (radians). */
    var patrolAngleRad: Double = patrolAngleRad

    /** Current angular velocity (rad/tick) for turn inertia. */
    var turnVelRad: Double = 0.0
}

// ---------------------------------------------------------------------------
// NPC weapon events
// ---------------------------------------------------------------------------

/**
 * A weapon event produced by the AI each tick.
 * Consumed by [GameEngine] to spawn the appropriate entity in the world.
 */
sealed class NpcWeaponEvent {
    abstract val npcId: Int
    abstract val x: Float
    abstract val y: Float
    abstract val headingRad: Double
    abstract val npcVx: Float
    abstract val npcVy: Float

    /**
     * NPC fires a plain shot.
     * Consumed by [GameEngine.spawnNpcShot].
     */
    data class Shot(
        override val npcId: Int,
        override val x: Float,
        override val y: Float,
        override val headingRad: Double,
        override val npcVx: Float,
        override val npcVy: Float,
    ) : NpcWeaponEvent()

    /**
     * NPC fires a smart/heat/torpedo missile.
     * Consumed by [GameEngine.spawnNpcMissile].
     */
    data class Missile(
        override val npcId: Int,
        override val x: Float,
        override val y: Float,
        override val headingRad: Double,
        override val npcVx: Float,
        override val npcVy: Float,
    ) : NpcWeaponEvent()

    /**
     * NPC drops a proximity mine at its current position.
     * Consumed by [GameEngine.spawnNpcMine].
     */
    data class Mine(
        override val npcId: Int,
        override val x: Float,
        override val y: Float,
        override val headingRad: Double,
        override val npcVx: Float,
        override val npcVy: Float,
    ) : NpcWeaponEvent()

    /**
     * NPC shield state change.
     * Consumed by [GameEngine] to set [DemoShip.shield].
     */
    data class ShieldChange(
        override val npcId: Int,
        override val x: Float,
        override val y: Float,
        override val headingRad: Double,
        override val npcVx: Float,
        override val npcVy: Float,
        val active: Boolean,
    ) : NpcWeaponEvent()
}

/**
 * Backwards-compatible type alias — existing callers that collect [NpcShotEvent]
 * can be migrated to [NpcWeaponEvent.Shot] incrementally.
 */
typealias NpcShotEvent = NpcWeaponEvent.Shot

// ---------------------------------------------------------------------------
// NPC AI — manager (owns all NpcAiState instances for a game session)
// ---------------------------------------------------------------------------

/**
 * Manages per-NPC AI states for the demo/local game mode.
 *
 * Call [tickAll] once per game tick, passing the current NPC list and player
 * snapshot.  Collect the returned [NpcShotEvent] list and pass each event to
 * [GameEngine.spawnNpcShot] to fire NPC shots into the world.
 *
 * @param worldW  World width in pixels (for toroidal wrap).
 * @param worldH  World height in pixels.
 */
class NpcAiManager(
    private val worldW: Float,
    private val worldH: Float,
) {
    private val states: MutableMap<Int, NpcAiState> = mutableMapOf()

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /**
     * Register AI state for a newly spawned NPC.
     * Calling this for an already-known id replaces the existing state.
     */
    fun register(npc: DemoShip) {
        val patrolAngle = (npc.heading / 128f) * 2.0 * PI
        states[npc.id] = NpcAiState(npc.id, patrolAngleRad = patrolAngle)
    }

    /** Remove AI state for a dead/removed NPC. */
    fun remove(npcId: Int) {
        states.remove(npcId)
    }

    /** Remove all AI states (e.g. on map change). */
    fun clear() = states.clear()

    /**
     * Returns the current behavior of the NPC with [npcId], or null if not registered.
     * Exposed for testing; avoid using in production game-loop code.
     */
    fun getBehavior(npcId: Int): NpcBehavior? = states[npcId]?.behavior

    /**
     * Returns the current evade timer of the NPC with [npcId], or null if not registered.
     * Exposed for testing.
     */
    fun getEvadeTimer(npcId: Int): Int? = states[npcId]?.evadeTimer

    /** Returns the id of the target selected for [npcId] on the last tick, or -1 if none. */
    fun getTargetId(npcId: Int): Int = states[npcId]?.targetId ?: -1

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Toroidal signed delta from [fromX], [fromY] to [toX], [toY] in a world of
     * [worldW] × [worldH].  Returns the shortest-path (dx, dy) pair.
     */
    private fun toroidalDelta(
        fromX: Float,
        fromY: Float,
        toX: Float,
        toY: Float,
    ): Pair<Double, Double> {
        var dx = (toX - fromX).toDouble()
        var dy = (toY - fromY).toDouble()
        val halfW = worldW / 2.0
        val halfH = worldH / 2.0
        if (dx > halfW) dx -= worldW
        if (dx < -halfW) dx += worldW
        if (dy > halfH) dy -= worldH
        if (dy < -halfH) dy += worldH
        return Pair(dx, dy)
    }

    // -----------------------------------------------------------------------
    // Per-tick update
    // -----------------------------------------------------------------------

    /**
     * Advance AI for every registered NPC.  Returns a list of shot events for
     * NPCs that fire this tick.  The caller must pass these to
     * [GameEngine.spawnNpcShot].
     *
     * @param npcs          Live NPC ship list (only ships still alive).
     * @param playerX       Player world-pixel X.
     * @param playerY       Player world-pixel Y.
     * @param playerVx      Player velocity X (px/tick).
     * @param playerVy      Player velocity Y (px/tick).
     * @param playerAlive   True if the player is currently alive.
     * @param treasureGoals Static goal zones from the map; used by ball-carrying NPCs
     *                      to navigate toward the opposing team's goal.  Empty list = no CTF.
     */
    fun tickAll(
        npcs: List<DemoShip>,
        playerX: Float,
        playerY: Float,
        playerVx: Float,
        playerVy: Float,
        playerAlive: Boolean,
        treasureGoals: List<TreasureGoal> = emptyList(),
    ): List<NpcWeaponEvent> {
        // Build combined target pool: human player (id=1) + all live NPCs
        val allTargets: List<TargetSnapshot> =
            buildList {
                add(TargetSnapshot(id = 1, x = playerX, y = playerY, vx = playerVx, vy = playerVy, isAlive = playerAlive))
                for (npc in npcs) {
                    if (npc.hp > 0f) add(TargetSnapshot(id = npc.id, x = npc.x, y = npc.y, vx = npc.vx, vy = npc.vy, isAlive = true))
                }
            }
        val events = mutableListOf<NpcWeaponEvent>()
        for (npc in npcs) {
            val state = states[npc.id] ?: continue
            events += tickOne(state, npc, allTargets, treasureGoals)
        }
        return events
    }

    // -----------------------------------------------------------------------
    // Single-NPC AI tick
    // -----------------------------------------------------------------------

    private fun tickOne(
        state: NpcAiState,
        npc: DemoShip,
        allTargets: List<TargetSnapshot>,
        treasureGoals: List<TreasureGoal> = emptyList(),
    ): List<NpcWeaponEvent> {
        // Select nearest living target that is not self
        val target: TargetSnapshot? =
            allTargets
                .filter { it.id != npc.id && it.isAlive }
                .minByOrNull { t ->
                    val (dx, dy) = toroidalDelta(npc.x, npc.y, t.x, t.y)
                    dx * dx + dy * dy
                }
        val targetAlive = target != null
        state.targetId = target?.id ?: -1

        // --- Toroidal delta to chosen target ---
        val (dx, dy) = if (target != null) toroidalDelta(npc.x, npc.y, target.x, target.y) else Pair(0.0, 0.0)
        val dist = hypot(dx, dy).toFloat()

        // --- Behavior transitions ---
        if (npc.hp <= NpcAiConst.EVADE_HP_THRESHOLD && state.behavior != NpcBehavior.EVADE) {
            state.behavior = NpcBehavior.EVADE
            state.evadeTimer = NpcAiConst.EVADE_DURATION_TICKS
        }
        if (state.behavior != NpcBehavior.EVADE) {
            state.behavior =
                when (state.behavior) {
                    NpcBehavior.PATROL -> {
                        if (!targetAlive || dist > NpcAiConst.DETECT_RANGE_PX) NpcBehavior.PATROL else NpcBehavior.CHASE
                    }

                    NpcBehavior.CHASE -> {
                        when {
                            !targetAlive || dist > NpcAiConst.DETECT_RANGE_PX -> NpcBehavior.PATROL
                            dist <= NpcAiConst.ATTACK_RANGE_PX -> NpcBehavior.ATTACK
                            else -> NpcBehavior.CHASE
                        }
                    }

                    NpcBehavior.ATTACK -> {
                        when {
                            !targetAlive -> NpcBehavior.PATROL
                            dist > NpcAiConst.ATTACK_RANGE_PX -> NpcBehavior.CHASE
                            else -> NpcBehavior.ATTACK
                        }
                    }

                    NpcBehavior.EVADE -> {
                        NpcBehavior.EVADE
                    } // handled above
                }
        }

        // --- Evade timer countdown ---
        if (state.behavior == NpcBehavior.EVADE) {
            state.evadeTimer--
            if (state.evadeTimer <= 0) {
                // Only exit EVADE if HP has recovered above the threshold.
                // If HP is still low we stay in EVADE (reset timer) to avoid the
                // oscillation bug: EVADE→PATROL→EVADE every EVADE_DURATION_TICKS+1 ticks.
                if (npc.hp > NpcAiConst.EVADE_HP_THRESHOLD) {
                    state.behavior = NpcBehavior.PATROL
                } else {
                    state.evadeTimer = NpcAiConst.EVADE_DURATION_TICKS
                }
            }
        }

        // --- Desired heading and speed ---
        // If this NPC is carrying a ball, navigate toward the opposing team's goal
        // rather than the player.  C: robots navigate to the treasure goal (robotdef.c).
        val ballGoalOverride: Pair<Double, Float>? =
            if (npc.carryingBallId != BallData.NO_PLAYER && treasureGoals.isNotEmpty()) {
                val npcTeam = 1 // NPCs are always team 1 in the local engine
                val goal = treasureGoals.firstOrNull { it.team != npcTeam }
                if (goal != null) {
                    val (gdx, gdy) = toroidalDelta(npc.x, npc.y, goal.x, goal.y)
                    Pair(atan2(gdy, gdx), NpcAiConst.CHASE_SPEED)
                } else {
                    null
                }
            } else {
                null
            }

        val (desiredAngleRad, desiredSpeed) =
            ballGoalOverride
                ?: when (state.behavior) {
                    NpcBehavior.PATROL -> {
                        // Slowly rotate patrol direction
                        state.patrolAngleRad += 0.004
                        Pair(state.patrolAngleRad, NpcAiConst.CRUISE_SPEED)
                    }

                    NpcBehavior.CHASE -> {
                        Pair(atan2(dy, dx), NpcAiConst.CHASE_SPEED)
                    }

                    NpcBehavior.ATTACK -> {
                        // Predictive aim: lead the target by AIM_LEAD_FACTOR * travel time
                        val travelTicks = dist / NpcAiConst.NPC_SHOT_SPEED
                        val leadX = dx + (target?.vx ?: 0f) * travelTicks * NpcAiConst.AIM_LEAD_FACTOR
                        val leadY = dy + (target?.vy ?: 0f) * travelTicks * NpcAiConst.AIM_LEAD_FACTOR
                        Pair(atan2(leadY, leadX), NpcAiConst.ATTACK_SPEED)
                    }

                    NpcBehavior.EVADE -> {
                        // Flee directly away from player
                        Pair(atan2(-dy, -dx), NpcAiConst.EVADE_SPEED)
                    }
                }

        // --- Turn inertia toward desired heading ---
        val currentAngleRad = (npc.heading / 128.0) * 2.0 * PI
        // Shortest angular delta [-π, π]
        var angleDiff = desiredAngleRad - currentAngleRad
        while (angleDiff > PI) angleDiff -= 2.0 * PI
        while (angleDiff < -PI) angleDiff += 2.0 * PI
        // Correct integration order: accelerate first, then damp.
        // This matches C update.c physics: turnvel += acc; turnvel *= (1 - resistance).
        state.turnVelRad += angleDiff.sign * NpcAiConst.TURN_ACC
        state.turnVelRad *= NpcAiConst.TURN_KEEP_FRACTION
        var newHeadingRad = currentAngleRad + state.turnVelRad
        // Wrap heading to [0, 2π) — also guards against floating-point boundary values
        newHeadingRad = ((newHeadingRad % (2.0 * PI)) + 2.0 * PI) % (2.0 * PI)
        npc.heading = (newHeadingRad / (2.0 * PI) * 128.0).toFloat() % 128.0f

        // --- Write desired velocity (owned by AI; physics blends in DemoGameState.tick) ---
        // Do NOT write npc.vx/vy directly — that would discard tractor/pressor beam
        // effects applied by engine.tick() earlier in the same frame.
        npc.desiredVx = (cos(newHeadingRad) * desiredSpeed).toFloat()
        npc.desiredVy = (sin(newHeadingRad) * desiredSpeed).toFloat()

        // --- Weapon fire priority (ATTACK state only) ---
        // Matches C robotdef.c Robot_default_play weapon dispatch order:
        //   1. Missile (if held and cooled down) — highest damage, limited supply
        //   2. Mine drop (if held and cooled down, and near the player)
        //   3. Plain shot (default — no items required)
        // Shield activates reactively when HP is low (any state).
        val events = mutableListOf<NpcWeaponEvent>()

        // Advance cooldowns
        if (state.shotCooldown > 0) state.shotCooldown--
        if (state.missileCooldown > 0) state.missileCooldown--
        if (state.mineDropCooldown > 0) state.mineDropCooldown--

        val inAttack = targetAlive && state.behavior == NpcBehavior.ATTACK && dist <= NpcAiConst.ATTACK_RANGE_PX
        val weaponBase =
            NpcWeaponEvent.Shot( // reused as template for coordinates/heading
                npcId = npc.id,
                x = npc.x,
                y = npc.y,
                headingRad = newHeadingRad,
                npcVx = npc.desiredVx,
                npcVy = npc.desiredVy,
            )

        // Shield: activate while low HP (C: HAS_EMERGENCY_SHIELD or low fuel → raise shield)
        if (npc.hasShieldItem && npc.hp <= NpcAiConst.EVADE_HP_THRESHOLD) {
            if (!npc.shield) {
                npc.shield = true
                events +=
                    NpcWeaponEvent.ShieldChange(
                        npcId = npc.id,
                        x = npc.x,
                        y = npc.y,
                        headingRad = newHeadingRad,
                        npcVx = npc.desiredVx,
                        npcVy = npc.desiredVy,
                        active = true,
                    )
            }
        } else if (npc.shield && npc.hasShieldItem) {
            npc.shield = false
            events +=
                NpcWeaponEvent.ShieldChange(
                    npcId = npc.id,
                    x = npc.x,
                    y = npc.y,
                    headingRad = newHeadingRad,
                    npcVx = npc.desiredVx,
                    npcVy = npc.desiredVy,
                    active = false,
                )
        }

        if (inAttack) {
            when {
                // 1. Missile — fires when missile items held and cooled
                // C: robot_count % 2 == 0 && item[ITEM_MISSILE] > 0 (robotdef.c:1073–1082)
                // Cooldown 24 ticks ≈ 5 C-ticks × HZ_RATIO
                npc.missileCount > 0 && state.missileCooldown == 0 -> {
                    state.missileCooldown = NpcAiConst.MISSILE_COOLDOWN_TICKS
                    events +=
                        NpcWeaponEvent.Missile(
                            npcId = npc.id,
                            x = npc.x,
                            y = npc.y,
                            headingRad = newHeadingRad,
                            npcVx = npc.desiredVx,
                            npcVy = npc.desiredVy,
                        )
                }

                // 2. Mine — drops when mine items held, cooled, and player nearby
                // C: robot_count % 32 < item[ITEM_MINE] (robotdef.c:1087–1096)
                // Cooldown 48 ticks ≈ 10 C-ticks × HZ_RATIO; only within 3× trigger radius
                npc.mineCount > 0 && state.mineDropCooldown == 0 &&
                    dist < NpcAiConst.MINE_DROP_RANGE_PX -> {
                    state.mineDropCooldown = NpcAiConst.MINE_DROP_COOLDOWN_TICKS
                    events +=
                        NpcWeaponEvent.Mine(
                            npcId = npc.id,
                            x = npc.x,
                            y = npc.y,
                            headingRad = newHeadingRad,
                            npcVx = npc.desiredVx,
                            npcVy = npc.desiredVy,
                        )
                }

                // 3. Plain shot (default)
                state.shotCooldown == 0 -> {
                    state.shotCooldown = NpcAiConst.SHOT_COOLDOWN_TICKS
                    events += weaponBase
                }
            }
        }

        return events
    }
}
