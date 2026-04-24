@file:OptIn(ExperimentalUnsignedTypes::class)

package org.lambertland.kxpilot.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.lambertland.kxpilot.client.KeyState
import org.lambertland.kxpilot.common.ClPos
import org.lambertland.kxpilot.common.ClickConst
import org.lambertland.kxpilot.common.EnergyDrain
import org.lambertland.kxpilot.common.GameConst
import org.lambertland.kxpilot.common.Item
import org.lambertland.kxpilot.common.Key
import org.lambertland.kxpilot.common.Vector
import org.lambertland.kxpilot.common.pixelToClick
import org.lambertland.kxpilot.common.toBlock
import org.lambertland.kxpilot.common.toPixel
import org.lambertland.kxpilot.server.CannonConst
import org.lambertland.kxpilot.server.CannonWeapon
import org.lambertland.kxpilot.server.CellType
import org.lambertland.kxpilot.server.ObjStatus
import org.lambertland.kxpilot.server.Player
import org.lambertland.kxpilot.server.PlayerState
import org.lambertland.kxpilot.server.World
import org.lambertland.kxpilot.server.WormType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.TimeSource

// ---------------------------------------------------------------------------
// Game constants for local simulation
// ---------------------------------------------------------------------------

internal object EngineConst {
    /**
     * Scale factor from C-server ticks (12.5 Hz) to KXPilot ticks (60 Hz).
     * All C per-tick rates are multiplied/divided by this to convert:
     * - angular velocities/rates:   ÷ HZ_RATIO
     * - fuel costs per tick:        × HZ_RATIO
     * - lifetimes in ticks:         × HZ_RATIO
     * - spatial speeds (px/tick):   no scaling (a pixel is a pixel)
     */
    const val HZ_RATIO: Double = 60.0 / 12.5 // 4.8

    /** Angular step per tick (radians). One heading-unit of 128 per full circle.
     *  C: `pl->dir += (turn_left ? turnspeed : -turnspeed)` each tick, where
     *  `turnspeed = 1` heading-unit.  Heading changes are instantaneous — no angular
     *  momentum or inertia in the C server.
     */
    const val TURN_RATE_RAD: Double = 2.0 * PI / GameConst.RES // ≈ 0.04909 rad/tick

    /**
     * Thrust acceleration impulse magnitude in pixels/tick².
     * C: acc = MAX_PLAYER_POWER / shipMass = 55.0 / 20.0 = 2.75 px/C-tick² at 12.5 Hz.
     * Scaled to 60 Hz: 2.75 / HZ_RATIO ≈ 0.573 px/tick².
     */
    const val THRUST_POWER: Double = GameConst.MAX_PLAYER_POWER / GameConst.PLAYER_MASS / HZ_RATIO

    // --- Thrust fuel (#8) ---

    /**
     * Fuel cost per tick while thrusting at full power.
     * C formula: f = power * 0.0008 per C-tick at 12.5 Hz (update.c:972).
     * At default power=55: 55.0 * 0.0008 = 0.044 fuel/C-tick.
     * Scaled to 60 Hz: 0.044 * HZ_RATIO = 0.211 fuel/tick.
     */
    const val THRUST_FUEL_COST: Double = GameConst.MAX_PLAYER_POWER * 0.0008 * HZ_RATIO

    /**
     * At zero fuel, thrust still works but at this fraction of THRUST_POWER (fumes mode).
     * C: fumes power = MIN_PLAYER_POWER * 0.6 = 5.0 * 0.6 = 3.0 (update.c:984).
     * As fraction of MAX_PLAYER_POWER=55: MIN_PLAYER_POWER * 0.6 / MAX_PLAYER_POWER ≈ 0.055.
     */
    const val FUMES_THRUST_FRACTION: Double = GameConst.MIN_PLAYER_POWER * 0.6 / GameConst.MAX_PLAYER_POWER

    /**
     * Shot initial speed (pixels/tick, relative to player velocity).
     * C default: options.shotSpeed = 21.0 px/C-tick (cmdline.c:182).
     * This is a spatial speed (px per game tick), so no Hz scaling needed.
     */
    const val SHOT_SPEED: Double = GameConst.SHOT_SPEED

    /** Shot collision radius in pixels (for shot–ship tests). */
    const val SHOT_RADIUS: Double = 2.0

    /**
     * Shot lifetime in ticks.
     * C default: options.shotLife = 60.0 C-ticks at 12.5 Hz (cmdline.c:192).
     * Scaled to 60 Hz: 60 * HZ_RATIO = 288 ticks.
     */
    const val SHOT_LIFE: Float = 288f // = (60.0 * HZ_RATIO).toFloat()

    // --- Shields ---

    /**
     * Fuel cost per tick when shield is active.
     * C value: ED_SHIELD = -0.20 per C-tick at 12.5 Hz (serverconst.h:117).
     * Scaled to 60 Hz: abs(ED_SHIELD) * HZ_RATIO = 0.96 fuel/tick.
     */
    const val SHIELD_FUEL_COST: Double = -EnergyDrain.SHIELD * HZ_RATIO

    /** C: ED_CLOAKING_DEVICE = -0.07 per C-tick. Scaled: × HZ_RATIO. */
    const val CLOAK_FUEL_COST: Double = -EnergyDrain.CLOAKING_DEVICE * HZ_RATIO // = 0.07 * 4.8 = 0.336

    // --- Mines ---

    /**
     * Mine arming delay (ticks before proximity detection activates).
     * KXPilot-specific design constant — C default: options.mineFuseTicks = 0 (cmdline.c).
     * 30 ticks (~0.5 s) is an intentional KXPilot design choice for a brief safety window.
     * Stored as [MineData.armTicksRemaining]; decremented each tick; proximity checks
     * are skipped while > 0.
     */
    const val MINE_ARM_TICKS: Int = 30

    /**
     * Maximum sub-step size for swept collision (pixels).
     * Velocity is walked in steps no larger than this to prevent tunneling through walls.
     * Must be ≤ BLOCK_SZ (35) to guarantee at least one sample per block.
     */
    const val SWEEP_STEP: Double = GameConst.BLOCK_SZ.toDouble() - 1.0 // 34 px

    /**
     * Velocity decay factor applied every tick (0 = no friction, approaching 1 = instant stop).
     * Kept at 0 to match XPilot default (frictionless space).
     */
    const val FRICTION: Double = 0.0

    // --- Missiles ---

    /**
     * Missile base speed (pixels/tick).
     * C: SMART_SHOT_MAX_SPEED = 22.0 px/C-tick (serverconst.h:174). Spatial speed, no Hz scaling.
     */
    const val MISSILE_SPEED: Double = 22.0

    /**
     * Homing turn rate (radians/tick toward locked target).
     * C: SMART_TURNSPEED = 2.6 heading-units/C-tick (serverconst.h:173).
     * In rad/C-tick: 2.6 * TURN_RATE_RAD ≈ 0.1277.
     * Scaled to 60 Hz (angular rate): ÷ HZ_RATIO ≈ 0.0266 rad/tick.
     */
    const val MISSILE_TURN_RATE: Double = 2.6 * TURN_RATE_RAD / HZ_RATIO

    /**
     * Missile lifetime (ticks).
     * C default: options.missileLife = 2400.0 C-ticks (cmdline.c:2279).
     * Scaled to 60 Hz: 2400 * HZ_RATIO = 11520 ticks.
     */
    const val MISSILE_LIFE: Float = 11520f // = (2400.0 * HZ_RATIO).toFloat()

    /**
     * Missile blast collision radius for NPC hit (pixels).
     * C: MISSILE_RANGE = 4 px (serverconst.h:169).
     */
    const val MISSILE_RADIUS: Double = 4.0

    // --- Mines ---

    /**
     * Mine proximity trigger radius (pixels) — used for both the trigger and the kill zone.
     * C: mine->pl_range = MINE_RANGE = VISIBILITY_DISTANCE * 0.1 = 100 px (serverconst.h:162).
     * C uses a single radius for both proximity detection and player/NPC collision.
     */
    const val MINE_TRIGGER_RADIUS: Double = GameConst.VISIBILITY_DISTANCE * 0.1

    /**
     * Mine lifetime (ticks).
     * C default: options.mineLife = 7200.0 C-ticks (cmdline.c:2259).
     * Scaled to 60 Hz: 7200 * HZ_RATIO = 34560 ticks.
     */
    const val MINE_LIFE: Float = 34560f // = (7200.0 * HZ_RATIO).toFloat()

    /**
     * Starting player fuel amount.
     * C: MAX_PLAYER_FUEL = 2600.0 (serverconst.h:128). Using max as the starting value
     * so the player begins at full fuel.
     */
    const val INITIAL_FUEL: Double = GameConst.MAX_PLAYER_FUEL

    // --- Tractor beam ---

    /**
     * Base tractor/pressor beam range with 0 tractor items (pixels).
     * C: TRACTOR_MAX_RANGE(0) = 200 px (serverconst.h:209).
     */
    const val TRACTOR_RANGE_BASE: Double = 200.0

    /**
     * Additional tractor range per tractor item held (pixels/item).
     * C: TRACTOR_MAX_RANGE(n) = 200 + n * 50 (serverconst.h:209).
     */
    const val TRACTOR_RANGE_PER_ITEM: Double = 50.0

    /**
     * Base tractor pull/push force with 0 tractor items (force-units on mass-20 ship).
     * C: TRACTOR_MAX_FORCE(0) = 40 (serverconst.h:210).
     * Converted to px/C-tick² = 40/20 = 2.0; scaled to 60 Hz: ÷ HZ_RATIO ≈ 0.417 px/tick².
     */
    const val TRACTOR_FORCE_BASE: Double = 40.0

    /**
     * Additional tractor force per tractor item held (same force-unit scale as TRACTOR_FORCE_BASE).
     * C: TRACTOR_MAX_FORCE(n) = 40 + n * 20 (serverconst.h:210).
     */
    const val TRACTOR_FORCE_PER_ITEM: Double = 20.0

    /**
     * Tractor beam distance-based force falloff factor.
     * C: TRACTOR_PERCENT(dist, maxdist) = 1.0 - TRACTOR_FALLOFF * dist/maxdist (serverconst.h:211).
     * At max range the beam applies (1 - TRACTOR_FALLOFF) = 50% of max force.
     */
    const val TRACTOR_FALLOFF: Double = 0.5

    /**
     * Reference fuel cost for the tractor/pressor beam at mid-range (percent ≈ 0.75).
     * C: TRACTOR_COST(percent) = -1.5 * percent per C-tick (serverconst.h:213).
     * Scaled to 60 Hz: 1.5 * percent * HZ_RATIO.
     * The live engine uses the dynamic formula (percent-scaled); this constant is
     * retained for documentation and tests only.
     */
    const val TRACTOR_FUEL_COST: Double = 5.4 // = 1.5 * 0.75 * HZ_RATIO (mid-range reference)

    // --- NPC combat ---

    /**
     * Fuel each NPC ship starts with — fuel is used as health (no separate HP concept in C).
     * C: MAX_PLAYER_FUEL = 2600.0 (serverconst.h:128).
     */
    const val NPC_INITIAL_HP: Float = 2600f // = GameConst.MAX_PLAYER_FUEL.toFloat()

    /**
     * Fuel damage dealt to an NPC ship by one player shot.
     * C: ED_SHOT_HIT = -25.0 fuel (serverconst.h:121).
     * At 2600 starting fuel: 104 shots to kill — matches C.
     */
    const val NPC_SHOT_HP_DAMAGE: Float = 25f // = (-EnergyDrain.SHOT_HIT).toFloat()

    /**
     * Fuel damage dealt to an NPC ship by a missile direct hit.
     * C: ED_SMART_SHOT_HIT = -120.0 fuel (serverconst.h:122).
     * At 2600 starting fuel: ~22 direct missile hits to kill.
     */
    const val NPC_MISSILE_HP_DAMAGE: Float = 120f // = (-EnergyDrain.SMART_SHOT_HIT).toFloat()

    // --- Mine debris (C: Make_debris / Player_collides_with_debris) ---

    /**
     * Mass of each debris fragment (arbitrary units).
     * C: #define DEBRIS_MASS 4.5  (serverconst.h:234)
     */
    const val DEBRIS_MASS: Double = 4.5

    /**
     * Expected number of debris fragments a mine explosion spawns (documentation only).
     * The actual count is random each detonation:
     * C: num_debris = (int)(intensity * num_modv * (0.20 + 0.10*rfrac()))
     *   = (int)(512 * 1 * (0.20 + 0.10*rfrac())) → range [102, 153], expected value ≈ 128.
     * This constant is not used in the spawn loop — it exists only for tests and documentation.
     * (shot.c:1248)
     */
    const val MINE_DEBRIS_COUNT: Int = 128 // expected value; actual count is random [102, 153]

    /**
     * Minimum debris speed (pixels/tick).
     * C: min_speed = 20 * speed_modv, speed_modv=1.  (shot.c:1275)
     */
    const val MINE_DEBRIS_MIN_SPEED: Double = 20.0

    /**
     * Maximum debris speed (pixels/tick).
     * C: max_speed = (intensity >> 2) * speed_modv = 512 >> 2 = 128.  (shot.c:1275)
     */
    const val MINE_DEBRIS_MAX_SPEED: Double = 128.0 // intensity >> 2 = 512 >> 2

    /**
     * Minimum debris lifetime (C-ticks at 12.5 Hz).
     * C: min_life = 8 * life_modv, life_modv=1.  (shot.c:1249)
     * Scaled to 60 Hz: 8 * HZ_RATIO = 38.4 ticks.
     */
    const val MINE_DEBRIS_MIN_LIFE: Float = (8.0 * HZ_RATIO).toFloat() // ≈ 38

    /**
     * Maximum debris lifetime (C-ticks at 12.5 Hz).
     * C: max_life = (intensity >> 1) * life_modv = 256.  (shot.c:1250)
     * Scaled to 60 Hz: 256 * HZ_RATIO = 1228.8 ticks.
     */
    const val MINE_DEBRIS_MAX_LIFE: Float = ((512 shr 1).toDouble() * HZ_RATIO).toFloat() // ≈ 1229

    /**
     * Debris-to-ship collision radius (pixels).
     * C: range = (SHIP_SZ + obj->pl_range) * CLICK / CLICK = SHIP_SZ + pl_radius
     * Debris pl_radius (pl_range) = 6 (passed as the `radius` arg in Make_debris call, shot.c:1272).
     * So collision happens when distance < SHIP_SZ + 6 = 16 + 6 = 22 px.  (collision.c:484)
     */
    const val MINE_DEBRIS_HIT_RADIUS: Double = GameConst.SHIP_SZ + 6.0 // 22 px

    // --- Respawn delay (#19) ---

    /**
     * Ticks after death before the player can respawn.
     * C value: RECOVERY_DELAY = 36 ticks at 12 FPS = 3 seconds.
     * Scaled to 60 Hz: 3 * 60 = 180 ticks.
     */
    const val RESPAWN_DELAY_TICKS: Int = 180

    /**
     * Ticks after death before an NPC ship respawns at its home base.
     * Matches the player respawn delay (3 s at 60 Hz = 180 ticks).
     */
    const val NPC_RESPAWN_DELAY_TICKS: Int = 180

    // --- Ball / treasure ---

    /** Ball collision radius (pixels). From const.h: BALL_RADIUS = 10. */
    const val BALL_RADIUS: Double = 10.0

    /** Ball mass (arbitrary units). From cmdline.c default: ballMass = 50. */
    const val BALL_MASS: Double = 50.0

    /** Player ship mass used for momentum exchange. C default: options.shipMass = 20.0 (cmdline.c:142). */
    const val PLAYER_MASS: Double = GameConst.PLAYER_MASS

    /** Natural length of the connector spring (pixels).
     *  C: options.ballConnectorLength default = 120.0 px (cmdline.c:1601).
     */
    const val CONNECTOR_LENGTH: Double = 120.0

    /**
     * Connector spring constant (force/px).
     * C: options.ballConnectorSpring = 1650.0 (cmdline.c default).
     * Empirically matches C's per-click² spring when translated to pixel units.
     */
    const val CONNECTOR_SPRING: Double = 1650.0

    /**
     * Connector damping coefficient.
     * C: options.ballConnectorDamping = 2.0 (cmdline.c default).
     */
    const val CONNECTOR_DAMPING: Double = 2.0

    /**
     * Maximum stretch/compression ratio before connector breaks.
     * If |1 - dist/naturalLen| > this, the connector snaps.
     * C: breakRatio = 0.30 derived from break threshold in collision.c / connector.c.
     */
    const val CONNECTOR_BREAK_RATIO: Double = 0.30

    /**
     * Treasure goal hit-box half-size (pixels). Ball must be within this to score.
     * C: ballGoalSize = 20 px (cmdline.c / map.c default — half of a block edge).
     */
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

    // --- Homing missile (smart shot) acceleration ---

    /**
     * Homing missile acceleration ramp (pixels/tick²).
     * C: SMART_SHOT_ACC = 0.6 px/C-tick² (serverconst.h:175).
     * No Hz scaling — spatial acceleration, not an angular rate.
     */
    const val SMART_SHOT_ACC: Double = 0.6

    /**
     * Speed divisor applied when the missile overshoots (speed reduction factor).
     * C: SMART_SHOT_DECFACT = 3 (serverconst.h:176).
     */
    const val SMART_SHOT_DECFACT: Int = 3

    /**
     * Minimum speed floor for homing missiles (pixels/tick).
     * C: SMART_SHOT_MIN_SPEED = SMART_SHOT_ACC * 8 = 4.8 px/C-tick (serverconst.h:177).
     */
    const val SMART_SHOT_MIN_SPEED: Double = SMART_SHOT_ACC * 8.0 // 4.8 px/tick

    /**
     * Look-ahead window used by homing missile guidance (C-ticks).
     * C: SMART_SHOT_LOOK_AH = 4 C-ticks (serverconst.h:178).
     * Scaled to 60 Hz: 4 * HZ_RATIO ticks.
     */
    const val SMART_SHOT_LOOK_AH: Double = 4.0 * HZ_RATIO // ≈ 19.2 ticks

    // --- Cannon ---

    /**
     * Cannon pulse (shot) lifetime (ticks).
     * C: CANNON_PULSE_LIFE = 4.75 C-ticks (serverconst.h:194).
     * Scaled to 60 Hz: 4.75 * HZ_RATIO ≈ 22.8 ticks.
     */
    const val CANNON_PULSE_LIFE: Float = (4.75 * HZ_RATIO).toFloat() // ≈ 22.8 ticks

    // --- Laser pulse (BL-01) ---

    /**
     * Speed of a laser pulse (pixels/tick).
     * C: options.pulseSpeed default = 30 px/C-tick (serverconst.h); spatial quantity — no Hz scaling.
     */
    const val LASER_PULSE_SPEED: Double = 30.0

    /**
     * Lifetime of a player-fired laser pulse.
     * C: CANNON_PULSE_LIFE = 4.75 C-ticks for cannon; player uses same constant.
     * Scaled: 4.75 * HZ_RATIO ticks.
     */
    const val LASER_PULSE_LIFE: Float = CANNON_PULSE_LIFE

    /**
     * Minimum ticks between successive laser pulses.
     * C: laserRepeatRate = 2 C-ticks (options default); scaled to 60 Hz: 2 * HZ_RATIO ≈ 9.6 → 10.
     */
    const val LASER_REPEAT_TICKS: Double = 2.0 * HZ_RATIO // ≈ 9.6 ticks

    // --- Wormhole (BL-02) ---

    /**
     * Wormhole capture radius (pixels).
     * C: WORMHOLE_RADIUS = (BLOCK_CLICKS / 2) - 1 in click units → (BLOCK_SZ / 2) px.
     */
    const val WORMHOLE_RADIUS_PX: Double = GameConst.BLOCK_SZ / 2.0

    /**
     * Ticks the wormhole destination stays fixed after a teleport.
     * C: options.wormholeStableTicks default = 250 C-ticks; scaled to 60 Hz.
     */
    const val WORMHOLE_STABLE_TICKS: Int = (250.0 * HZ_RATIO).toInt() // ≈ 1200

    // --- Checkpoint (BL-05) ---

    /**
     * Radius within which the player passes a checkpoint (pixels).
     * C: checkpointRadius = 0.5 → 0.5 * BLOCK_SZ px.
     */
    const val CHECKPOINT_RADIUS_PX: Double = GameConst.BLOCK_SZ * 0.5

    // --- Shot bounce (BL-03) ---

    /**
     * Factor by which shot life is multiplied each time it bounces off a wall.
     * C: options.objectWallBounceLifeFactor default = 0.9.
     */
    const val BOUNCE_LIFE_FACTOR: Float = 0.9f

    /**
     * Braking factor applied to shot speed on wall bounce (1.0 = no energy loss).
     * C: options.objectWallBounceBrakeFactor default = 1.0.
     */
    const val BOUNCE_BRAKE_FACTOR: Double = 1.0

    // --- World items (BL-07) ---

    /**
     * Minimum lifetime for a spawned world item (ticks).
     * C: 1500 C-ticks × HZ_RATIO.
     */
    const val WORLD_ITEM_MIN_LIFE: Float = (1500.0 * HZ_RATIO).toFloat()

    /**
     * Extra lifetime range for a spawned world item (ticks).
     * C: rfrac() * 512 C-ticks × HZ_RATIO.
     */
    const val WORLD_ITEM_EXTRA_LIFE: Float = (512.0 * HZ_RATIO).toFloat()

    /**
     * Pickup radius for world items (pixels).
     * C: SHIP_SZ + ITEM_SIZE / 2 = 16 + 8 = 24 px.
     */
    const val WORLD_ITEM_PICKUP_RADIUS: Double = GameConst.SHIP_SZ + GameConst.ITEM_SIZE / 2.0

    /**
     * Ticks between item spawn attempts.
     * C: once per second (1 C-tick at 12.5 Hz → HZ_RATIO = 4.8 ticks at 60 Hz; but "once per second" = HZ_RATIO).
     */
    const val ITEM_SPAWN_INTERVAL: Double = HZ_RATIO

    // --- NPC spawning ---

    /**
     * Minimum interval between NPC ship spawns (ticks).
     * C: NPC_SPAWN_DELAY = 24 C-ticks (serverconst.h:230 or equivalent).
     * Scaled to 60 Hz: 24 * HZ_RATIO ≈ 115.2 ticks.
     */
    const val NPC_SPAWN_DELAY: Float = (24.0 * HZ_RATIO).toFloat() // ≈ 115 ticks

    /**
     * Maximum number of NPC ship slots active at once.
     * C: MAX_SHIPS = 16 (serverconst.h:14 / game limit).
     */
    const val MAX_NPC_SHIPS: Int = 16

    // --- HUD warning ---

    /**
     * Duration of HUD low-fuel / low-shield warning flash (ticks).
     * C: WARN_TICKS = 24 C-ticks (serverconst.h:245).
     * Scaled to 60 Hz: 24 * HZ_RATIO ≈ 115.2 ticks.
     */
    const val WARN_TICKS: Float = (24.0 * HZ_RATIO).toFloat() // ≈ 115 ticks

    // --- Deflector (BL-12) ---

    /**
     * Base deflector range (at 1 item). C: (1*0.5+1)*BLOCK_CLICKS = 1.5*2240.
     * This is the per-item multiplier portion; add DEFLECTOR_RANGE_BASE for total.
     */
    const val DEFLECTOR_RANGE_PER_ITEM: Double = 0.5 * ClickConst.BLOCK_CLICKS

    /** Fixed base range contribution regardless of item count. C: 1 * BLOCK_CLICKS. */
    const val DEFLECTOR_RANGE_BASE: Double = 1.0 * ClickConst.BLOCK_CLICKS

    /** Max force per deflector item. C: item_count * 0.2. */
    const val DEFLECTOR_FORCE_PER_ITEM: Double = 0.2

    // --- Pulsar gravity ---

    /**
     * Acceleration applied by a map gravity pulsar per tick (pixels/tick²).
     * C: PULSAR_POWER = 2.7 px/C-tick² (serverconst.h:219).
     * Scaled to 60 Hz: 2.7 / HZ_RATIO px/tick².
     */
    const val PULSAR_POWER: Double = 2.7 / HZ_RATIO // ≈ 0.5625 px/tick²

    /** Per-charge phase duration. C: PHASING_TIME = 48 C-ticks. Scaled to 60 Hz. */
    const val PHASING_TIME_TICKS: Double = 48.0 * HZ_RATIO // ≈ 230.4 ticks

    /** Emergency shield duration per charge. C: EMERGENCY_SHIELD_TIME = 48 C-ticks. Scaled to 60 Hz. */
    const val EMERGENCY_SHIELD_TIME_TICKS: Double = 48.0 * HZ_RATIO // ≈ 230.4 ticks

    /** Emergency thrust duration per charge. C: EMERGENCY_THRUST_TIME = 48 C-ticks. Scaled to 60 Hz. */
    const val EMERGENCY_THRUST_TIME_TICKS: Double = 48.0 * HZ_RATIO // ≈ 230.4 ticks
}

// ---------------------------------------------------------------------------
// WeaponConst — forward declarations for unimplemented weapon systems
// ---------------------------------------------------------------------------

/**
 * Constants for weapon and item systems that are defined in XPilot-NG but not yet
 * implemented in KXPilot.  Grouped here to keep unimplemented features clearly
 * segregated from the live [EngineConst] block.
 *
 * All values are taken verbatim from serverconst.h / common/const.h unless noted.
 * Hz scaling uses [EngineConst.HZ_RATIO] = 60 / 12.5 = 4.8.
 * VISIBILITY_DISTANCE = 1000 px, shipMass = 20, shotMass = 0.1 (cmdline defaults).
 */
internal object WeaponConst {
    private const val HZ_RATIO = EngineConst.HZ_RATIO
    private const val VIS = 1000.0 // VISIBILITY_DISTANCE
    private const val SHIP_MASS = 20.0
    private const val SHOT_MASS = 0.1

    // --- Heat-seeker ---

    /** Heat-seeker acquisition range (px). C: HEAT_RANGE = VIS/2 = 500 px. */
    const val HEAT_RANGE: Double = VIS / 2.0 // 500 px

    /** Heat-seeker speed multiplier relative to normal shot speed. C: HEAT_SPEED_FACT = 1.7. */
    const val HEAT_SPEED_FACT: Double = 1.7

    /** Close-phase timeout (C-ticks). C: HEAT_CLOSE_TIMEOUT = 2*12 = 24. Scaled: *HZ_RATIO. */
    const val HEAT_CLOSE_TIMEOUT: Float = (2.0 * 12.0 * HZ_RATIO).toFloat() // ≈ 115 ticks

    /** Close-phase range (px). C: HEAT_CLOSE_RANGE = HEAT_RANGE = 500 px. */
    const val HEAT_CLOSE_RANGE: Double = HEAT_RANGE

    /** Close-phase guidance error (heading units). C: HEAT_CLOSE_ERROR = 0. */
    const val HEAT_CLOSE_ERROR: Int = 0

    /** Mid-phase timeout (C-ticks). C: HEAT_MID_TIMEOUT = 4*12 = 48. Scaled: *HZ_RATIO. */
    const val HEAT_MID_TIMEOUT: Float = (4.0 * 12.0 * HZ_RATIO).toFloat() // ≈ 230 ticks

    /** Mid-phase range (px). C: HEAT_MID_RANGE = 2 * HEAT_RANGE = 1000 px. */
    const val HEAT_MID_RANGE: Double = 2.0 * HEAT_RANGE // 1000 px

    /** Mid-phase guidance error (heading units). C: HEAT_MID_ERROR = 8. */
    const val HEAT_MID_ERROR: Int = 8

    /** Wide-phase timeout (C-ticks). C: HEAT_WIDE_TIMEOUT = 8*12 = 96. Scaled: *HZ_RATIO. */
    const val HEAT_WIDE_TIMEOUT: Float = (8.0 * 12.0 * HZ_RATIO).toFloat() // ≈ 461 ticks

    /** Wide-phase guidance error (heading units). C: HEAT_WIDE_ERROR = 16. */
    const val HEAT_WIDE_ERROR: Int = 16

    // --- Torpedo ---

    /**
     * Torpedo acceleration ramp (px/tick²).
     * C: TORPEDO_ACC = (18 * SMART_SHOT_MAX_SPEED) / (12 * TORPEDO_SPEED_TIME)
     *    = (18 * 22) / (12 * 24) = 396 / 288 = 1.375 px/C-tick².
     * No Hz scaling — spatial acceleration.
     */
    const val TORPEDO_ACC: Double = (18.0 * 22.0) / (12.0 * (2.0 * 12.0)) // 1.375 px/tick²

    /**
     * Torpedo acceleration duration (C-ticks). C: TORPEDO_SPEED_TIME = 2*12 = 24. Scaled.
     */
    const val TORPEDO_SPEED_TIME: Float = (2.0 * 12.0 * HZ_RATIO).toFloat() // ≈ 115 ticks

    /**
     * Torpedo sensing radius (px). C: TORPEDO_RANGE = MINE_RANGE * 0.45 = 100 * 0.45 = 45 px.
     */
    const val TORPEDO_RANGE: Double = VIS * 0.1 * 0.45 // 45 px

    // --- Nuke ---

    /**
     * Nuke acceleration (px/tick²). C: NUKE_ACC = 5 * TORPEDO_ACC = 5 * 1.375 = 6.875.
     */
    const val NUKE_ACC: Double = 5.0 * TORPEDO_ACC // 6.875 px/tick²

    /**
     * Nuke acceleration duration (C-ticks). C: NUKE_SPEED_TIME = 2*12 = 24. Scaled.
     */
    const val NUKE_SPEED_TIME: Float = (2.0 * 12.0 * HZ_RATIO).toFloat() // ≈ 115 ticks

    /**
     * Nuke kill radius (px). C: NUKE_RANGE = MINE_RANGE * 1.5 = 100 * 1.5 = 150 px.
     */
    const val NUKE_RANGE: Double = VIS * 0.1 * 1.5 // 150 px

    /**
     * Mine-explosion multiplier for nuke blast. C: NUKE_MINE_EXPL_MULT = 3.
     */
    const val NUKE_MINE_EXPL_MULT: Int = 3

    /**
     * Smart-shot-explosion multiplier for nuke blast. C: NUKE_SMART_EXPL_MULT = 4.
     */
    const val NUKE_SMART_EXPL_MULT: Int = 4

    // --- Cluster ---

    /**
     * Default shot mass used to compute cluster shot count (arbitrary units).
     * C: options.shotMass default = 0.1 (cmdline.c).
     * CLUSTER_MASS_SHOTS(mass) = mass * 0.9 / shotMass.
     */
    const val CLUSTER_SHOT_MASS: Double = SHOT_MASS // 0.1

    /**
     * Cluster fragment count formula factor. C: CLUSTER_MASS_SHOTS(m) = m*0.9/shotMass.
     * This constant encodes the 0.9 factor; combine with CLUSTER_SHOT_MASS at use site.
     */
    const val CLUSTER_MASS_FACTOR: Double = 0.9

    // --- Mine sensing ---

    /**
     * Base mine sensor range (px). C: MINE_SENSE_BASE_RANGE = MINE_RANGE * 1.3 = 130 px.
     */
    const val MINE_SENSE_BASE_RANGE: Double = VIS * 0.1 * 1.3 // 130 px

    /**
     * Per-item mine sensor range bonus (px). C: MINE_SENSE_RANGE_FACTOR = MINE_RANGE * 0.3 = 30 px.
     */
    const val MINE_SENSE_RANGE_FACTOR: Double = VIS * 0.1 * 0.3 // 30 px

    // --- ECM / Transporter ---

    /**
     * ECM effective radius (px). C: ECM_DISTANCE = VIS * 0.4 = 400 px.
     */
    const val ECM_DISTANCE: Double = VIS * 0.4 // 400 px

    /**
     * Fuel drained per ECM activation. C: ED_ECM = -60.0 fuel (serverconst.h:114).
     */
    const val ECM_FUEL_COST: Double = 60.0

    /**
     * Multiplier on [EngineConst.VIS] for energy/fuel-effect interaction range.
     * C: ENERGY_RANGE_FACTOR = 2.5 (serverconst.h:236). Multi-player only — used to
     * compute the radius within which one player's energy state affects another.
     * Not applicable in single-player KXPilot; defined here for completeness.
     */
    const val ENERGY_RANGE_FACTOR: Double = 2.5

    /**
     * Duration (in 60 Hz ticks) for which a missile is blinded by ECM.
     * C: CONFUSED_TIME = 3 C-ticks (serverconst.h:176). Scaled to 60 Hz: × HZ_RATIO.
     */
    const val CONFUSED_TIME: Float = (3.0 * HZ_RATIO).toFloat() // ≈ 14.4 ticks

    /**
     * Transporter effective radius (px). C: TRANSPORTER_DISTANCE = VIS * 0.2 = 200 px.
     */
    const val TRANSPORTER_DISTANCE: Double = VIS * 0.2 // 200 px

    // --- Fuel station / pack ---

    /**
     * Fuel mass scaling factor (mass per fuel unit). C: FUEL_MASS(f) = f * 0.005.
     */
    const val FUEL_MASS_FACTOR: Double = 0.005

    /**
     * Fuel station refill rate (fuel units/C-tick). C: REFUEL_RATE = 5.0. Scaled to 60 Hz.
     */
    const val REFUEL_RATE: Double = 5.0 / HZ_RATIO // ≈ 1.042 fuel/tick

    /**
     * Per-tank refill threshold below which a tank accepts fuel. C: TANK_REFILL_LIMIT = 350/8 = 43.75.
     */
    const val TANK_REFILL_LIMIT: Double = 350.0 / 8.0 // 43.75

    // --- Energy packs ---

    /**
     * Minimum fuel contained in a dropped energy pack (inclusive).
     * C: ENERGY_PACK_FUEL = 500.0 + rfrac() * 511.0 (serverconst.h:129).
     */
    const val ENERGY_PACK_FUEL_MIN: Double = 500.0

    /**
     * Maximum fuel contained in a dropped energy pack (inclusive).
     * C upper bound: 500 + 511 = 1011 (serverconst.h:129).
     */
    const val ENERGY_PACK_FUEL_MAX: Double = 1011.0

    /**
     * Minimum lifetime of a stationary energy pack (60 Hz ticks).
     * C: item.c:317 — `item->life = 1500 + rfrac() * 512` C-ticks.
     * Minimum: 1500 C-ticks × HZ_RATIO = 7200 ticks.
     */
    const val ENERGY_PACK_LIFE_MIN: Float = (1500.0 * HZ_RATIO).toFloat() // 7200 ticks

    /**
     * Maximum lifetime of a stationary energy pack (60 Hz ticks).
     * C: item.c:317 — upper bound is 1500 + 512 = 2012 C-ticks.
     * Maximum: 2012 C-ticks × HZ_RATIO = 9657.6 ≈ 9658 ticks.
     */
    const val ENERGY_PACK_LIFE_MAX: Float = ((1500.0 + 512.0) * HZ_RATIO).toFloat() // ≈ 9658 ticks

    /**
     * Pickup radius for energy packs (pixels). Player's centre must be within this.
     * C: collision.c:484 — `(SHIP_SZ + item->pl_range)` where `pl_range = ITEM_SIZE/2 = 8`.
     * Effective radius: 16 + 8 = 24 px. C source: item.h:78 (ITEM_SIZE=16), const.h:155 (SHIP_SZ=16).
     */
    const val ENERGY_PACK_PICKUP_RADIUS: Double = GameConst.SHIP_SZ + GameConst.ITEM_SIZE / 2.0 // 24 px

    // --- Targets ---

    /**
     * Target hit point pool. C: TARGET_DAMAGE = 250.0 (common/const.h:147).
     */
    const val TARGET_DAMAGE: Double = 250.0

    /**
     * Target repair rate per C-tick while a fuel station services it.
     * C: TARGET_FUEL_REPAIR_PER_FRAME = TARGET_DAMAGE / (12 * 10). Scaled.
     */
    const val TARGET_FUEL_REPAIR_PER_FRAME: Double = TARGET_DAMAGE / (12.0 * 10.0) / HZ_RATIO

    /**
     * Slow background repair rate per 60 Hz tick (no fuel station required).
     * C: TARGET_REPAIR_PER_FRAME = TARGET_DAMAGE / (12 * 600) (serverconst.h:228). Scaled.
     */
    const val TARGET_REPAIR_PER_FRAME: Double = TARGET_DAMAGE / (12.0 * 600.0) / HZ_RATIO

    // --- Armor ---

    /**
     * Mass added to ship per armor item held (arbitrary units).
     * C: ARMOR_MASS = shipMass / 14 ≈ 1.43 (serverconst.h:145).
     */
    const val ARMOR_MASS: Double = SHIP_MASS / 14.0 // ≈ 1.429

    // --- Tanks ---

    /**
     * Maximum number of fuel tanks a player can carry. C: MAX_TANKS = 8.
     */
    const val MAX_TANKS: Int = 8

    /**
     * Mass per fuel tank item (arbitrary units). C: TANK_MASS = shipMass / 10 = 2.0.
     */
    const val TANK_MASS: Double = SHIP_MASS / 10.0 // 2.0

    /**
     * Main-tank fuel capacity (fuel units). C: TANK_CAP(0) = MAX_PLAYER_FUEL = 2600.0.
     */
    const val TANK_CAP_MAIN: Double = GameConst.MAX_PLAYER_FUEL // 2600.0

    /**
     * Auxiliary-tank fuel capacity (fuel units). C: TANK_CAP(n>0) = MAX_PLAYER_FUEL/3 ≈ 866.7.
     */
    const val TANK_CAP_AUX: Double = GameConst.MAX_PLAYER_FUEL / 3.0 // ≈ 866.7

    /**
     * Thrust-speed reduction factor when carrying tanks. C: TANK_THRUST_FACT = 0.7.
     */
    const val TANK_THRUST_FACT: Double = 0.7

    /**
     * Duration a tank suppresses thrust after firing (C-ticks). C: TANK_NOTHRUST_TIME = HEAT_CLOSE_TIMEOUT/2+2 = 14. Scaled.
     */
    const val TANK_NOTHRUST_TIME: Float = ((2.0 * 12.0 / 2.0 + 2.0) * HZ_RATIO).toFloat() // ≈ 67 ticks

    /**
     * Duration thrust is active between tank no-thrust windows (C-ticks). C: TANK_THRUST_TIME = TANK_NOTHRUST_TIME/2+1 = 8. Scaled.
     */
    const val TANK_THRUST_TIME: Float = (((2.0 * 12.0 / 2.0 + 2.0) / 2.0 + 1.0) * HZ_RATIO).toFloat() // ≈ 38 ticks

    // --- Afterburner ---

    /**
     * Number of afterburner levels (log₂). C: LG2_MAX_AFTERBURNER = 4.
     */
    const val LG2_MAX_AFTERBURNER: Int = 4

    /**
     * Maximum afterburner item level. C: MAX_AFTERBURNER = (1 << LG2_MAX_AFTERBURNER) - 1 = 15.
     * Used in the level-scaled power and fuel formulas.
     */
    const val MAX_AFTERBURNER: Int = (1 shl LG2_MAX_AFTERBURNER) - 1 // 15

    /**
     * Afterburner fuel multiplier at max level vs. normal thrust. C: ALT_FUEL_FACT = 3.
     * Used in AFTER_BURN_FUEL: fuel_cost * ((MAX_AFTERBURNER+1) + level*(ALT_FUEL_FACT-1)) /
     * (MAX_AFTERBURNER+1).
     */
    const val ALT_FUEL_FACT: Int = 3

    /**
     * Mass of normal (non-afterburner) exhaust sparks.
     * C: options.thrustMass default = 0.0435 (cmdline.c). Purely cosmetic (OBJ_SPARK has no
     * server-side collision damage); used to scale spark visual size in the renderer.
     */
    const val THRUST_MASS: Double = 0.0435

    /**
     * Mass multiplier applied to afterburner exhaust sparks vs. normal sparks.
     * C: ALT_SPARK_MASS_FACT = 4.2 (serverconst.h:132).
     * Afterburner sparks: mass = THRUST_MASS × ALT_SPARK_MASS_FACT ≈ 0.183.
     * Purely cosmetic — the heavier sparks render larger/brighter (blue in C).
     */
    const val ALT_SPARK_MASS_FACT: Double = 4.2

    /**
     * Expected total thrust sparks per C-tick at full power.
     * C: tot_sparks = (MAX_PLAYER_POWER * 0.15 + 2.5) * timeStep ≈ 10.75 (ship.c:56).
     * Scaled to 60 Hz: ÷ HZ_RATIO ≈ 2.24 sparks/tick.
     */
    const val SPARK_COUNT_PER_TICK: Double = (GameConst.MAX_PLAYER_POWER * 0.15 + 2.5) / HZ_RATIO // ≈ 2.24

    /**
     * Maximum spark speed (pixels/tick, relative to ship).
     * C: max_speed = (1 + power * 0.14) * sparkSpeed = (1 + 55 * 0.14) * 30 = 261 clicks/C-tick.
     * Convert clicks→px (÷64) and scale to 60 Hz (÷HZ_RATIO): ≈ 0.85 px/tick.
     */
    const val SPARK_MAX_SPEED: Double = (1.0 + GameConst.MAX_PLAYER_POWER * 0.14) * 30.0 / 64.0 / HZ_RATIO // ≈ 0.85

    /**
     * Maximum spark lifetime (60 Hz ticks).
     * C: max_life = 3 + power * 0.35 ≈ 22.25 C-ticks. Scaled to 60 Hz: × HZ_RATIO ≈ 106.8.
     */
    const val SPARK_MAX_LIFE: Float = ((3.0 + GameConst.MAX_PLAYER_POWER * 0.35) * HZ_RATIO).toFloat() // ≈ 107

    /**
     * Default thrust nozzle width (options.thrustWidth).
     * C: cmdline.c:3784 — default value 0.25.
     */
    const val THRUST_WIDTH: Double = 0.25

    /**
     * Half-angle of the thrust exhaust cone (radians).
     * C: ship.c:57–58 — half-spread = (RES*0.2+1) * thrustWidth heading-units,
     * converted to radians: × (2π/RES).
     * = (128*0.2+1) * 0.25 * (2π/128) ≈ 0.326 rad.
     */
    val SPARK_SPREAD_RAD: Double = (GameConst.RES * 0.2 + 1.0) * THRUST_WIDTH * (2.0 * PI / GameConst.RES)

    /**
     * Minimum spark speed (pixels/tick, relative to ship).
     * C: Make_debris called with min_speed = 1.0 clicks/C-tick (ship.c).
     * Convert: 1.0 / PIXEL_CLICKS / HZ_RATIO ≈ 0.00326 px/tick.
     */
    const val SPARK_MIN_SPEED: Double = 1.0 / ClickConst.PIXEL_CLICKS / HZ_RATIO

    // --- Misc ---

    /**
     * Fuel drain per transporter use. C: ED_TRANSPORTER = -60.0 (serverconst.h:115).
     */
    const val TRANSPORTER_FUEL_COST: Double = 60.0
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
 * reachable) by rotating its heading up to [EngineConst.MISSILE_TURN_RATE] rad,
 * and accelerates toward [EngineConst.MISSILE_SPEED] using the C smart-shot
 * ramp: speed increases by [EngineConst.SMART_SHOT_ACC] each tick; on overshoot
 * it is divided by [EngineConst.SMART_SHOT_DECFACT]; floor is
 * [EngineConst.SMART_SHOT_MIN_SPEED].
 *
 * Plain class (not data class) — [pos], [headingRad], and [speed] mutate every tick.
 */
class MissileData(
    var pos: ClPos,
    /** Current heading in radians (Y-up convention). */
    var headingRad: Double,
    var life: Float,
    /** NPC id of the lock target at spawn time; -1 = unguided. */
    val targetNpcId: Int,
    val ownerId: Short,
    /** Current speed in pixels/tick.  Starts at SMART_SHOT_MIN_SPEED and ramps up. */
    var speed: Double = EngineConst.SMART_SHOT_MIN_SPEED,
    /**
     * Remaining ticks of ECM blindness.  While > 0 the missile ignores its target and
     * coasts unguided.  C: smart_count = CONFUSED_TIME after ECM pulse (item.c).
     */
    var confusedTicks: Float = 0f,
)

// ---------------------------------------------------------------------------
// MineData — proximity mine
// ---------------------------------------------------------------------------

/**
 * A proximity mine.  Stationary after being dropped; detonates when any ship
 * enters [EngineConst.MINE_TRIGGER_RADIUS].
 *
 * C behaviour (mineFuseTicks = 0 default): the owner is permanently immune —
 * their own mine never triggers against them.  There is no arming delay.
 * [ownerImmune] models C's `fuse = -1` (permanent owner immunity).
 *
 * KXPilot-specific: [armTicksRemaining] counts down from [EngineConst.MINE_ARM_TICKS]
 * (30 ticks ≈ 0.5 s) after the mine is dropped.  Proximity detection is suppressed
 * until the counter reaches 0.  This differs from C default (instant arming) but
 * provides a safety window matching the intent documented in [EngineConst.MINE_ARM_TICKS].
 *
 * Plain class (not data class) — [life] and [armTicksRemaining] mutate every tick.
 * [pos] is set once at drop and never updated; the constructor receives a
 * defensive copy of [ClPos] (value type) so the mine cannot follow the player.
 */
class MineData(
    val pos: ClPos,
    /** When true, the mine's owner can never trigger it (C default: permanent). */
    val ownerImmune: Boolean = true,
    var life: Float = EngineConst.MINE_LIFE,
    val ownerId: Short,
    /**
     * Ticks remaining before proximity detection activates.
     * Counts down from [EngineConst.MINE_ARM_TICKS] to 0 each tick.
     * While > 0 the mine is unarmed and cannot detonate.
     */
    var armTicksRemaining: Int = EngineConst.MINE_ARM_TICKS,
    /**
     * True when the player (or any NPC) is within the sensing radius this tick.
     * C: `MINE_SENSE_BASE_RANGE + sensor * MINE_SENSE_RANGE_FACTOR` determines
     * whether the mine's ownership information is sent to the client (frame.c:733).
     * Here we expose it as a flag for HUD "mine nearby" warnings.
     */
    var sensed: Boolean = false,
)

// ---------------------------------------------------------------------------
// DebrisData — a single debris fragment from a mine/shot explosion
// ---------------------------------------------------------------------------

/**
 * A single debris fragment ejected by a mine explosion.
 *
 * Mirrors the C `OBJ_DEBRIS` object created by [Make_debris] (ship.c).
 *
 * Physics:
 *  - Moves at constant velocity [vel] each tick (no gravity by default for OBJ_DEBRIS without GRAVITY bit — but mine debris DO have GRAVITY set via `status = GRAVITY`, shot.c:1186).
 *  - Wall collision: bounces (same [sweepMoveShot] logic as shots — debris are destroyed on wall contact in C).
 *  - Lifetime [life] counts down to 0; removed when exhausted.
 *  - Collision radius [plRadius] = 6 px (the `radius=6` arg in Make_debris, shot.c:1272).
 *    Hit detection uses distance < (SHIP_SZ + plRadius) = 22 px.
 *
 * Damage on collision: `collision_cost(mass, speed) = mass * speed / 128.0` fuel drain
 * per fragment hit (collision.c:451). Normal HAS_SHIELD does NOT protect — only
 * HAS_EMERGENCY_SHIELD (Player_collides_with_debris, collision.c:974).
 */
class DebrisData(
    var pos: ClPos,
    val vel: Vector,
    var life: Float,
    /** Fragment mass. C: DEBRIS_MASS = 4.5. */
    val mass: Double = EngineConst.DEBRIS_MASS,
    /** Collision radius in pixels. C: pl_radius = pl_range = 6. */
    val plRadius: Double = 6.0,
    /** Owner id — used so owner-immunity logic can be extended in the future. */
    val ownerId: Short,
)

// ---------------------------------------------------------------------------
// EnergyPackData — stationary fuel collectible
// ---------------------------------------------------------------------------

/**
 * A stationary energy pack on the map.  When the player flies over it the pack
 * is consumed and a random amount of fuel in
 * [[WeaponConst.ENERGY_PACK_FUEL_MIN]..[WeaponConst.ENERGY_PACK_FUEL_MAX]] is
 * added to the player's fuel pool.
 *
 * C: `ITEM_FUEL` world item; picked up by `do_get_item` in server/item.c.
 * `amount = ENERGY_PACK_FUEL = 500 + rfrac() * 511`.
 *
 * Plain class — [life] mutates each tick.
 */
class EnergyPackData(
    val pos: ClPos,
    /** Ticks remaining before the pack expires without being collected. */
    var life: Float = WeaponConst.ENERGY_PACK_LIFE_MIN,
)

// ---------------------------------------------------------------------------
// SparkData — thrust exhaust spark (cosmetic)
// ---------------------------------------------------------------------------

/**
 * A single thrust exhaust spark.  Sparks are purely cosmetic — they have no
 * server-side collision or damage in C (OBJ_SPARK without damage bit).
 *
 * Two varieties are produced each thrust tick:
 * - Normal sparks: [isAfterburner] = false, mass = [WeaponConst.THRUST_MASS].
 * - Afterburner sparks: [isAfterburner] = true, mass = [WeaponConst.THRUST_MASS] × [WeaponConst.ALT_SPARK_MASS_FACT].
 *   Only spawned when [PlayerItems.afterburner] > 0.
 *
 * C: [Make_debris] with OBJ_SPARK in server/ship.c.
 */
class SparkData(
    var pos: ClPos,
    /** Velocity (pixels/tick, Y-up). */
    val vel: Vector,
    var life: Float,
    /**
     * Visual mass — passed to the renderer to scale spark size/brightness.
     * Normal: [WeaponConst.THRUST_MASS]; afterburner: × [WeaponConst.ALT_SPARK_MASS_FACT].
     */
    val mass: Double,
    /** True for the heavier blue afterburner sparks; false for normal red/orange sparks. */
    val isAfterburner: Boolean,
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
// TorpedoData — accelerating torpedo
// ---------------------------------------------------------------------------

/**
 * A torpedo.  Ballistic after launch; accelerates from 0 toward [EngineConst.MISSILE_SPEED]
 * at [WeaponConst.TORPEDO_ACC] px/tick² for [WeaponConst.TORPEDO_SPEED_TIME] ticks,
 * then coasts.  Detonates (proximity blast) on any target within [WeaponConst.TORPEDO_RANGE].
 *
 * C: torpedo behaviour from server/serverconst.h lines 177-180.
 */
class TorpedoData(
    var pos: ClPos,
    var headingRad: Double,
    var life: Float,
    val ownerId: Short,
    /** Current speed (px/tick). Starts at 0 and ramps up. */
    var speed: Double = 0.0,
    /** Ticks of acceleration remaining. */
    var accelTicksLeft: Float = WeaponConst.TORPEDO_SPEED_TIME,
)

// ---------------------------------------------------------------------------
// HeatSeekerData — 3-phase heat-seeking missile
// ---------------------------------------------------------------------------

/**
 * A heat-seeking missile with 3 guidance phases (close, mid, wide).
 *
 * Each phase has a timeout, range threshold, and heading error (jitter).
 * C: HEAT_CLOSE/MID/WIDE constants (server/serverconst.h:191-198).
 *
 * Plain class — [pos], [headingRad], [speed], [phaseTicks] mutate every tick.
 */
class HeatSeekerData(
    var pos: ClPos,
    var headingRad: Double,
    var life: Float,
    val ownerId: Short,
    /** NPC id of lock target (-1 = unguided). */
    val targetNpcId: Int,
    /** Current speed (px/tick). Uses HEAT_SPEED_FACT * MISSILE_SPEED at launch. */
    var speed: Double = WeaponConst.HEAT_SPEED_FACT * EngineConst.MISSILE_SPEED,
    /** Ticks remaining in the current guidance phase. */
    var phaseTicks: Float = WeaponConst.HEAT_CLOSE_TIMEOUT,
    /** Current guidance phase (0=close, 1=mid, 2=wide, 3=unguided). */
    var phase: Int = 0,
    /**
     * Remaining ticks of ECM blindness.  While > 0 guidance is suppressed.
     * C: smart_count = CONFUSED_TIME after ECM pulse (item.c).
     */
    var confusedTicks: Float = 0f,
)

// ---------------------------------------------------------------------------
// ClusterMineData — proximity mine that fragments into shots on detonation
// ---------------------------------------------------------------------------

/**
 * A cluster mine.  Stationary like a normal mine.  On detonation it spawns
 * [fragmentCount] shots in a full-circle burst instead of debris.
 *
 * Fragment count = mass * [WeaponConst.CLUSTER_MASS_FACTOR] / [WeaponConst.CLUSTER_SHOT_MASS].
 * Using default mass = [GameConst.MINE_MASS] ≈ 270 shots.
 *
 * C: cluster.c (serverconst.h:200-201).
 */
class ClusterMineData(
    var pos: ClPos,
    val ownerImmune: Boolean,
    var life: Float = EngineConst.MINE_LIFE,
    val ownerId: Short,
    val mass: Double = GameConst.MINE_MASS,
)

// ---------------------------------------------------------------------------
// NukeData — accelerating nuke torpedo with blast chain-explosion
// ---------------------------------------------------------------------------

/**
 * A nuclear torpedo.  Like a torpedo but with a larger blast radius
 * ([WeaponConst.NUKE_RANGE]) and chain-explosion multipliers on detonation.
 *
 * C: server/serverconst.h lines 182-187.
 */
class NukeData(
    var pos: ClPos,
    var headingRad: Double,
    var life: Float,
    val ownerId: Short,
    /** Current speed (px/tick). Ramps at NUKE_ACC for NUKE_SPEED_TIME ticks. */
    var speed: Double = 0.0,
    /** Ticks of acceleration remaining. */
    var accelTicksLeft: Float = WeaponConst.NUKE_SPEED_TIME,
    /** True when the TOGGLE_NUCLEAR key is active at launch (always true for NukeData). */
    @Suppress("unused") val isNuclear: Boolean = true,
)

// ---------------------------------------------------------------------------
// WorldItem — a collectible item lying on the map (BL-07)
// ---------------------------------------------------------------------------

/**
 * A collectible item spawned on the map.
 *
 * C: `item_t` placed by `Place_item` (server/item.c).
 * Lifetime: `1500 + rfrac() * 512` C-ticks × HZ_RATIO.
 * Pickup radius: [GameConst.SHIP_SZ] + [GameConst.ITEM_SIZE] / 2 = 24 px.
 *
 * @param pos       Click-space position.
 * @param itemType  Which [Item] this pack grants.
 * @param life      Remaining ticks.
 */
class WorldItem(
    var pos: ClPos,
    val itemType: org.lambertland.kxpilot.common.Item,
    var life: Float,
)

// ---------------------------------------------------------------------------
// PlayerItems — per-player item inventory
// ---------------------------------------------------------------------------

/**
 * Counts of each equippable item currently held by the player.
 *
 * All counts are zero by default (no items at game start).
 * Item-system features in [GameEngine] read from this inventory to scale
 * their behaviour (e.g. tractor range, tank capacity, ECM radius).
 *
 * C analogue: the `items[]` array on the `player` struct (server/player.h).
 *
 * @param tractorBeam   Number of tractor-beam items held.
 * @param sensor        Number of sensor items held (mine sensing range).
 * @param armor         Number of armor items held (mass bonus + damage reduction).
 * @param tanks         Number of extra fuel tanks held (0 = main tank only).
 * @param afterburner   Afterburner level (0 = none, max = [GameConst.MAX_AFTERBURNER]).
 * @param ecm           Number of ECM items held.
 * @param transporter   Number of transporter items held.
 */
data class PlayerItems(
    val tractorBeam: Int = 0,
    val sensor: Int = 0,
    val armor: Int = 0,
    val tanks: Int = 0,
    val afterburner: Int = 0,
    val ecm: Int = 0,
    val transporter: Int = 0,
    /** Number of hyperjump charges held. C: ITEM_HYPERJUMP (player.h). */
    val hyperjump: Int = 0,
    /** Number of wide-angle (spread) items held. C: ITEM_WIDEANGLE. */
    val wideangle: Int = 0,
    /** Number of laser items held. C: ITEM_LASER. */
    val laser: Int = 0,
    /** Number of deflector items held. C: ITEM_DEFLECTOR (BL-12). */
    val deflector: Int = 0,
    /** Number of cloaking device items held. C: ITEM_CLOAK (BL-13). */
    val cloak: Int = 0,
    /** Number of phasing device charges held. C: ITEM_PHASING (BL-14). */
    val phasing: Int = 0,
    /** Number of emergency shield charges held. C: ITEM_EMERGENCY_SHIELD (BL-15). */
    val emergencyShield: Int = 0,
    /** Number of emergency thrust charges held. C: ITEM_EMERGENCY_THRUST (BL-16). */
    val emergencyThrust: Int = 0,
) {
    companion object {
        /** Convenience instance with every item count at zero. */
        val NONE = PlayerItems()
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
    private val rng: Random = Random.Default,
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

    /** Active debris fragments (from mine/explosion effects). */
    val debris: MutableList<DebrisData> = mutableListOf()

    /** Active treasure balls. */
    val balls: MutableList<BallData> = mutableListOf()

    /** Active torpedoes. */
    val torpedoes: MutableList<TorpedoData> = mutableListOf()

    /** Active heat-seeking missiles. */
    val heatSeekers: MutableList<HeatSeekerData> = mutableListOf()

    /** Deployed cluster mines. */
    val clusterMines: MutableList<ClusterMineData> = mutableListOf()

    /** Active nuclear torpedoes. */
    val nukes: MutableList<NukeData> = mutableListOf()

    /**
     * Active laser pulses.  C: OBJ_PULSE fired by KEY_FIRE_LASER (BL-01).
     * Shares [ShotData] type — laser pulses are fast shots with short life.
     */
    val laserPulses: MutableList<ShotData> = mutableListOf()

    /**
     * Stationary energy packs waiting to be collected.
     * C: `ITEM_FUEL` world items placed by [Place_item] (server/item.c).
     * Populated by [spawnEnergyPack]; consumed by player overlap in [tick].
     */
    val energyPacks: MutableList<EnergyPackData> = mutableListOf()

    /**
     * Active world items available for the player to pick up (BL-07).
     * C: items placed by [Place_item] in server/item.c.
     */
    val worldItems: MutableList<WorldItem> = mutableListOf()

    /**
     * Ticks until the next item spawn attempt (BL-07).
     * C: once per second (12.5 C-ticks → 60 KXPilot ticks).
     */
    private var itemSpawnTimer: Double = EngineConst.HZ_RATIO

    /**
     * Active thrust exhaust sparks.  Purely cosmetic — exposed for the UI renderer.
     * C: OBJ_SPARK objects produced by [Make_debris] in server/ship.c each thrust tick.
     */
    val sparks: MutableList<SparkData> = mutableListOf()

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
        internal set

    /** Max fuel capacity — extends with additional fuel tanks ([PlayerItems.tanks]). */
    val fuelMax: Double
        get() = EngineConst.INITIAL_FUEL + WeaponConst.TANK_CAP_AUX * playerItems.tanks

    /**
     * Effective ship mass including armor and fuel tanks.
     *
     * C: mass = shipMass + ARMOR_MASS*numArmor + TANK_MASS*numTanks
     * Higher mass reduces thrust acceleration proportionally.
     */
    val effectiveMass: Double
        get() =
            EngineConst.PLAYER_MASS +
                WeaponConst.ARMOR_MASS * playerItems.armor +
                WeaponConst.TANK_MASS * playerItems.tanks

    /**
     * True when the shield was active during the most recent [tick].
     * Reflects both the key-down state AND fuel availability — the authoritative
     * source for HUD rendering.  Read-only outside the engine.
     */
    var shieldActive: Boolean = false
        private set

    /**
     * True when the deflector is active (BL-12).
     * Toggled by KEY_DEFLECTOR; requires at least one deflector item and fuel > 0.
     * C: `deflector_count` / `used & HAS_DEFLECTOR` in server/update.c.
     */
    var deflectorActive: Boolean = false

    /**
     * True when the cloaking device is active (BL-13).
     * Toggled by KEY_CLOAK; requires at least one cloak item and fuel > 0.
     * C: `USES_CLOAKING_DEVICE` bit in server/update.c.
     */
    var cloakActive: Boolean = false

    /**
     * True when the phasing device is active (BL-14).
     * Toggled by KEY_PHASING; requires at least one phasing charge.
     * C: `USES_PHASING_DEVICE` bit in server/update.c.
     */
    var phasingActive: Boolean = false

    /**
     * Remaining ticks of the current phasing charge.
     * Counts down from [EngineConst.PHASING_TIME_TICKS]; auto-consumes next charge on expiry.
     */
    var phasingTicksLeft: Double = 0.0

    /**
     * True when the emergency shield is active (BL-15).
     * Activated by KEY_EMERGENCY_SHIELD or auto on first pickup when no shield is active.
     * C: `HAS_EMERGENCY_SHIELD` / `USES_EMERGENCY_SHIELD` in server/update.c + collision.c.
     */
    var emergencyShieldActive: Boolean = false

    /**
     * Remaining ticks of the current emergency shield charge.
     * Counts down only when a collision is blocked this tick; auto-consumes next charge on expiry.
     */
    var emergencyShieldTicksLeft: Double = 0.0

    /** Per-tick flag: set true when the emergency shield blocked a hit this tick. */
    private var emergencyShieldBlockedHit: Boolean = false

    // --- Emergency thrust (BL-16) ---

    /** True when the emergency thrust is active (BL-16). */
    var emergencyThrustActive: Boolean = false

    /** Remaining ticks of the current emergency thrust charge. Counts down only while thrusting with fuel. */
    var emergencyThrustTicksLeft: Double = 0.0

    // --- Item inventory ---

    /**
     * Items currently held by the player.  Defaults to [PlayerItems.NONE] (no items).
     * Set this before (or during) a game session to enable item-scaled features:
     * tractor range, tank capacity, afterburner boost, ECM radius, etc.
     */
    var playerItems: PlayerItems = PlayerItems.NONE

    // --- Self-destruct (BL-08) ---

    /**
     * Remaining ticks until the player self-destructs, or 0 when inactive.
     * First KEY_SELF_DESTRUCT press starts the countdown; second cancels it.
     * C: `selfdestructcountdown` in server/update.c.
     */
    var selfDestructTicks: Double = 0.0
        private set

    // --- Shot spread (BL-04) ---

    /**
     * Current spread level (0–3).  Cycled by KEY_TOGGLE_SPREAD.
     * 0 = tightest (3-shot fan at ±1 heading-unit gap if wideangle held);
     * 3 = widest (3-shot fan at ±(MODS_SPREAD_MAX - 0) = ±3 heading-unit gap).
     * C: pl->mods ModsSpread packed bitfield.
     */
    var spreadLevel: Int = 0
        private set

    // --- Checkpoint tracking (BL-05) ---

    /**
     * Index of the next checkpoint the player must pass.
     * Wraps around to 0 after reaching `world.checks.size - 1`.
     * C: pl->check_id.
     */
    var checkIndex: Int = 0
        internal set

    /**
     * Number of completed laps (increments each time checkIndex wraps to 0).
     * C: pl->round.
     */
    var laps: Int = 0
        internal set

    // --- Laser fire timer (BL-01) ---

    /**
     * Ticks remaining before the laser can fire again.
     * C: laser_time + laserRepeatRate > frame_time gating.
     */
    private var laserFireTimer: Double = 0.0

    // --- HUD warning state ---

    /**
     * True when fuel is critically low (below 20 % of [fuelMax]).
     * Updated every [tick]; safe to poll from the renderer.
     */
    var warnFuel: Boolean = false
        private set

    /**
     * True when the shield has just been depleted (fuel hit 0 while shield was active).
     * Stays true for [EngineConst.WARN_TICKS] ticks, then clears.
     */
    var warnShield: Boolean = false
        private set

    /** Countdown in ticks for the shield-depleted warning display. */
    private var warnShieldTicks: Float = 0f

    /**
     * True when at least one live mine is within the player's sensing radius
     * (`MINE_SENSE_BASE_RANGE + sensor * MINE_SENSE_RANGE_FACTOR` px).
     *
     * Updated each tick by the mine loop.  Safe to poll from the HUD renderer.
     * Maps to the C client-side mine proximity indicator.
     */
    val mineSensed: Boolean
        get() = mines.any { it.sensed }

    // --- Respawn delay (#19) ---

    /**
     * Ticks remaining before the player can respawn after death.
     * Counted down by [tick] when the player is dead; [spawnAtBase]/[respawn]
     * are blocked until this reaches 0 (or they reset it to 0 themselves).
     */
    var deathTicksRemaining: Int = 0
        private set

    // --- NPC respawn state (Phase 5) ---

    /**
     * Factory functions for each known NPC, keyed by NPC id.
     * Registered the first time an NPC is seen in [tick]; used to recreate the
     * ship at its home position when its respawn timer expires.
     *
     * The factory must return a fresh [EngineTarget] with full HP and home
     * position — the same state the NPC started in.  [buildNpcShipsFromBases]
     * populates this via [registerNpcFactory] before the first game tick.
     */
    private val npcFactories: MutableMap<Int, () -> EngineTarget> = mutableMapOf()

    /**
     * Respawn timers for dead NPCs, keyed by NPC id.
     * Decremented each tick; when an entry reaches 0 the NPC is respawned
     * and the entry is removed.
     */
    private val npcRespawnTimers: MutableMap<Int, Int> = mutableMapOf()

    /**
     * Register a factory for an NPC ship.  Must be called before the first
     * [tick] that includes this NPC.
     *
     * @param npcId   The [EngineTarget.id] of the NPC.
     * @param factory A zero-arg lambda that returns a fully-initialised
     *                [EngineTarget] at its home position with full HP.
     */
    fun registerNpcFactory(
        npcId: Int,
        factory: () -> EngineTarget,
    ) {
        npcFactories[npcId] = factory
    }

    /**
     * Remove the factory and any pending respawn timer for an NPC.
     * Call when an NPC is permanently removed (e.g. on map change).
     */
    fun unregisterNpc(npcId: Int) {
        npcFactories.remove(npcId)
        npcRespawnTimers.remove(npcId)
    }

    /** Remove all NPC registrations (e.g. on map change or session reset). */
    fun clearNpcs() {
        npcFactories.clear()
        npcRespawnTimers.clear()
    }

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
     * @param npcShips  Mutable NPC ship list.  Dead NPCs are removed by this method
     *                  and re-added when their respawn timer elapses, provided a
     *                  factory was registered with [registerNpcFactory].
     */
    fun tick(
        keys: KeyState,
        npcShips: MutableList<EngineTarget> = mutableListOf(),
    ) {
        // --- NPC respawn countdown (runs even while player is dead) ---
        tickNpcRespawn(npcShips)

        if (!player.isAlive()) {
            // #19 respawn delay countdown
            if (deathTicksRemaining > 0) deathTicksRemaining--
            return
        }

        // --- 1. Rotation --- (C: direct heading step, no inertia)
        if (keys.isDown(Key.KEY_TURN_LEFT)) {
            player.setFloatDir(wrapAngle(player.floatDir + EngineConst.TURN_RATE_RAD))
        }
        if (keys.isDown(Key.KEY_TURN_RIGHT)) {
            player.setFloatDir(wrapAngle(player.floatDir - EngineConst.TURN_RATE_RAD))
        }
        player.dir = floatDirToIntHeading(player.floatDir)

        // --- 2. Thrust --- (#8 fuel drain)
        if (keys.isDown(Key.KEY_THRUST)) {
            val ab = playerItems.afterburner // 0..MAX_AFTERBURNER (0..15)
            // Emergency thrust acts as if afterburner = MAX_AFTERBURNER
            val effectiveAb = if (emergencyThrustActive) WeaponConst.MAX_AFTERBURNER else ab
            val afterburnerActive = effectiveAb > 0 && fuel > 0.0
            // C: AFTER_BURN_POWER_FACTOR(n) = afterburnerPowerMult
            //    * (1 + n * (ALT_SPARK_MASS_FACT-1) / (MAX_AFTERBURNER+1))
            //    afterburnerPowerMult defaults to 1.0 (cmdline.c).
            // Level 0 → factor 1.0 (no change); level 15 → factor ≈ 4.0.
            val thrustMultiplier: Double =
                if (afterburnerActive) {
                    1.0 + effectiveAb * (WeaponConst.ALT_SPARK_MASS_FACT - 1.0) /
                        (WeaponConst.MAX_AFTERBURNER + 1.0)
                } else {
                    1.0
                }
            // C: AFTER_BURN_FUEL(f,n) = f * ((MAX_AFTERBURNER+1) + n*(ALT_FUEL_FACT-1))
            //    / (MAX_AFTERBURNER+1).
            // Level 0 → ×1.0; level 15 → ×3.0.
            val fuelMultiplier: Double =
                if (afterburnerActive) {
                    (
                        (WeaponConst.MAX_AFTERBURNER + 1) +
                            effectiveAb * (WeaponConst.ALT_FUEL_FACT - 1.0)
                    ) /
                        (WeaponConst.MAX_AFTERBURNER + 1.0)
                } else {
                    1.0
                }
            // Scale thrust by base mass / effective mass to account for armor + tank weight
            val massScale = EngineConst.PLAYER_MASS / effectiveMass
            val power =
                if (fuel > 0.0) {
                    EngineConst.THRUST_POWER * thrustMultiplier * massScale
                } else {
                    EngineConst.THRUST_POWER * EngineConst.FUMES_THRUST_FRACTION * massScale
                }
            val ax = player.floatDirCos * power
            val ay = player.floatDirSin * power
            player.acc = Vector(ax.toFloat(), ay.toFloat())
            player.objStatus = (player.objStatus.toInt() or ObjStatus.THRUSTING).toUShort()
            fuel = (fuel - EngineConst.THRUST_FUEL_COST * fuelMultiplier).coerceAtLeast(0.0)
            spawnThrustSparks(afterburnerActive)
            // BL-16: emergency thrust timer — only drains while thrusting with fuel
            if (emergencyThrustActive && fuel > 0.0) {
                emergencyThrustTicksLeft -= 1.0
                if (emergencyThrustTicksLeft <= 0.0) {
                    if (playerItems.emergencyThrust > 0) {
                        emergencyThrustActive = true
                        emergencyThrustTicksLeft = EngineConst.EMERGENCY_THRUST_TIME_TICKS
                        playerItems = playerItems.copy(emergencyThrust = playerItems.emergencyThrust - 1)
                    } else {
                        emergencyThrustActive = false
                    }
                }
            }
        } else {
            player.acc = Vector(0f, 0f)
            player.objStatus = (player.objStatus.toInt() and ObjStatus.THRUSTING.inv()).toUShort()
        }

        // --- 3. Shield --- drain fuel; shots that hit a shielded player are deflected
        val prevShieldActive = shieldActive
        shieldActive = keys.isDown(Key.KEY_SHIELD) && fuel > 0.0
        if (shieldActive) {
            fuel = (fuel - EngineConst.SHIELD_FUEL_COST).coerceAtLeast(0.0)
            // If fuel just hit 0, shield dropped — trigger warning
            if (fuel <= 0.0) {
                warnShieldTicks = EngineConst.WARN_TICKS
            }
        }
        // Advance shield-depleted warning countdown
        if (warnShieldTicks > 0f) {
            warnShieldTicks -= 1f
        }
        warnShield = warnShieldTicks > 0f

        // BL-12: deflector fuel drain
        if (deflectorActive) {
            fuel = (fuel + EnergyDrain.DEFLECTOR * EngineConst.HZ_RATIO).coerceAtLeast(0.0)
            if (fuel <= 0.0) deflectorActive = false
        }

        // BL-13: cloaking device fuel drain
        if (cloakActive) {
            fuel = (fuel + EnergyDrain.CLOAKING_DEVICE * EngineConst.HZ_RATIO).coerceAtLeast(0.0)
            if (fuel <= 0.0) cloakActive = false
        }

        // BL-14: phasing device fuel drain and timer
        if (phasingActive) {
            fuel = (fuel + EnergyDrain.PHASING_DEVICE * EngineConst.HZ_RATIO).coerceAtLeast(0.0)
            if (fuel <= 0.0) {
                deactivatePhasing()
            } else {
                phasingTicksLeft -= 1.0
                if (phasingTicksLeft <= 0.0) {
                    if (playerItems.phasing > 0) {
                        // Auto-consume next charge
                        playerItems = playerItems.copy(phasing = playerItems.phasing - 1)
                        phasingTicksLeft = EngineConst.PHASING_TIME_TICKS
                    } else {
                        deactivatePhasing()
                    }
                }
            }
        }

        // BL-15: emergency shield timer — only drains when a collision was blocked this tick
        // Reset hit flag at start of collision processing (done just before collision checks below)
        emergencyShieldBlockedHit = false

        // Update fuel-low warning: warn when below 20% of max
        warnFuel = fuel < fuelMax * 0.20

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
        if (phasingActive) {
            // BL-14: phased player passes through walls — skip sweep collision,
            // advance position directly by velocity.
            val newCx =
                world.wrapXClick(
                    player.pos.cx +
                        player.vel.x
                            .toDouble()
                            .pixelToClickInt(),
                )
            val newCy =
                world.wrapYClick(
                    player.pos.cy +
                        player.vel.y
                            .toDouble()
                            .pixelToClickInt(),
                )
            player.pos = ClPos(newCx, newCy)
        } else {
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
        }

        // --- 9. Weapons --- (#1 shield/weapon lockout: can't fire with shield up)
        if (!shieldActive) {
            if (keys.justPressed(Key.KEY_FIRE_SHOT)) {
                spawnSpreadShots()
            }
            if (keys.justPressed(Key.KEY_FIRE_MISSILE)) {
                spawnMissile()
            }
            if (keys.justPressed(Key.KEY_DROP_MINE)) {
                dropMine()
            }
            if (keys.justPressed(Key.KEY_FIRE_TORPEDO)) {
                spawnTorpedo(nuclear = keys.isDown(Key.KEY_TOGGLE_NUCLEAR))
            }
            if (keys.justPressed(Key.KEY_FIRE_HEAT)) {
                spawnHeatSeeker()
            }
            if (keys.justPressed(Key.KEY_DROP_MINE) && keys.isDown(Key.KEY_TOGGLE_CLUSTER)) {
                // Cluster mine: KEY_DROP_MINE while KEY_TOGGLE_CLUSTER is held
                dropClusterMine()
            }
            if (keys.justPressed(Key.KEY_ECM)) {
                activateEcm(npcShips)
            }
            if (keys.justPressed(Key.KEY_TRANSPORTER)) {
                activateTransporter(npcShips)
            }
            // BL-01: laser fire — held key, gated by laserFireTimer
            if (keys.isDown(Key.KEY_FIRE_LASER) && playerItems.laser > 0) {
                if (laserFireTimer <= 0.0) {
                    fireLaserPulse()
                    laserFireTimer = EngineConst.LASER_REPEAT_TICKS
                }
            }
        }
        if (laserFireTimer > 0.0) laserFireTimer -= 1.0

        // --- 9b. Lock cycling ---
        if (keys.justPressed(Key.KEY_LOCK_NEXT)) lockNext(npcShips)
        if (keys.justPressed(Key.KEY_LOCK_PREV)) lockPrev(npcShips)

        // --- 9c. Repair tool ---
        // C: Do_repair in update.c — drain REFUEL_RATE fuel and fast-repair the nearest target.
        // For now, repair is applied to all alive damaged targets while KEY_REPAIR is held.
        if (keys.isDown(Key.KEY_REPAIR)) {
            for (i in world.targets.indices) {
                playerActivatesRepair(i)
            }
        }

        // --- 9d. Toggle-once keys (not weapon-locked) ---
        // BL-04: cycle spread level
        if (keys.justPressed(Key.KEY_TOGGLE_SPREAD)) {
            spreadLevel = (spreadLevel + 1) % (GameConst.MODS_SPREAD_MAX + 1)
        }
        // BL-08: self-destruct toggle
        if (keys.justPressed(Key.KEY_SELF_DESTRUCT)) {
            if (selfDestructTicks > 0.0) {
                // Second press cancels
                selfDestructTicks = 0.0
            } else {
                // First press starts countdown
                selfDestructTicks = GameConst.SELF_DESTRUCT_DELAY * EngineConst.HZ_RATIO
            }
        }
        // BL-09: hyperjump
        if (keys.justPressed(Key.KEY_HYPERJUMP) && playerItems.hyperjump > 0) {
            playerItems = playerItems.copy(hyperjump = playerItems.hyperjump - 1)
            doHyperjump()
        }
        // BL-10: detonate all owned mines
        if (keys.justPressed(Key.KEY_DETONATE_MINES)) {
            detonateAllMines()
        }
        // BL-12: deflector toggle
        if (keys.justPressed(Key.KEY_DEFLECTOR)) {
            if (playerItems.deflector > 0 && fuel > 0.0) {
                deflectorActive = !deflectorActive
            }
        }
        // BL-13: cloak toggle
        if (keys.justPressed(Key.KEY_CLOAK)) {
            if (playerItems.cloak > 0 && fuel > 0.0) {
                cloakActive = !cloakActive
            }
        }
        // BL-14: phasing device toggle
        if (keys.justPressed(Key.KEY_PHASING)) {
            if (playerItems.phasing > 0) {
                if (!phasingActive) {
                    phasingActive = true
                    phasingTicksLeft = EngineConst.PHASING_TIME_TICKS
                    playerItems = playerItems.copy(phasing = playerItems.phasing - 1)
                } else {
                    deactivatePhasing()
                }
            }
        }
        // BL-15: emergency shield activation
        if (keys.justPressed(Key.KEY_EMERGENCY_SHIELD)) {
            if (playerItems.emergencyShield > 0) {
                activateEmergencyShield()
            }
        }

        if (keys.justPressed(Key.KEY_EMERGENCY_THRUST)) {
            if (playerItems.emergencyThrust > 0) {
                emergencyThrustActive = true
                emergencyThrustTicksLeft = EngineConst.EMERGENCY_THRUST_TIME_TICKS
                playerItems = playerItems.copy(emergencyThrust = playerItems.emergencyThrust - 1)
            }
        }

        // --- 10. Lock target tracking ---
        updateLockState(npcShips)

        // --- 11. Tractor / pressor beam ---
        if (keys.isDown(Key.KEY_TRACTOR_BEAM)) {
            applyTractorBeam(npcShips, pressor = false)
        }
        if (keys.isDown(Key.KEY_PRESSOR_BEAM)) {
            applyTractorBeam(npcShips, pressor = true)
        }

        // --- 12. Update shots ---
        val shieldRadius = GameConst.SHIP_SZ + RenderConst.SHIP_RADIUS.toDouble()
        val shotIter = shots.iterator()
        while (shotIter.hasNext()) {
            val shot = shotIter.next()

            // BL-03: shots bounce off walls
            val (sp, bounced, newVx, newVy) = sweepMoveShotBounce(shot.pos, shot.vel.x.toDouble(), shot.vel.y.toDouble())
            if (bounced) {
                shot.life *= EngineConst.BOUNCE_LIFE_FACTOR
                shot.vel =
                    Vector(
                        (newVx * EngineConst.BOUNCE_BRAKE_FACTOR).toFloat(),
                        (newVy * EngineConst.BOUNCE_BRAKE_FACTOR).toFloat(),
                    )
            }
            shot.pos = sp

            shot.life -= 1f
            if (shot.life <= 0f) {
                shotIter.remove()
                continue
            }

            if (shot.ownerId == player.id) {
                shot.freshTick = false
                // Player-fired shots: check NPC ship collision
                var hitNpc = false
                for (npc in npcShips) {
                    val npx = npc.x.toDouble().pixelToClickInt()
                    val npy = npc.y.toDouble().pixelToClickInt()
                    val npcClick = ClPos(world.wrapXClick(npx), world.wrapYClick(npy))
                    if (checkCollision(shot.pos, npcClick, EngineConst.SHOT_RADIUS + GameConst.SHIP_SZ)) {
                        if (npc.hp > 0f) { // #C guard: only score once per NPC death
                            npc.hp -= EngineConst.NPC_SHOT_HP_DAMAGE
                            if (npc.hp <= 0f) {
                                player.score += 1.0
                            }
                        }
                        npc.shield = false // hit visual feedback
                        hitNpc = true
                        break
                    }
                }
                if (hitNpc) {
                    shotIter.remove()
                }
                continue
            }
            shot.freshTick = false

            // Shield blocks the shot — C: when shielded, apply ED_SHOT_HIT drain directly;
            // if fuel hits 0 the shield drops but the player survives (no instant death).
            // (collision.c: Player_add_fuel(pl, drain); if (pl->fuel.sum <= 0) CLR_BIT(HAS_SHIELD))
            val effectiveShieldForShot = shieldActive || emergencyShieldActive
            if (effectiveShieldForShot && checkCollision(shot.pos, player.pos, shieldRadius)) {
                fuel = (fuel + EnergyDrain.SHOT_HIT).coerceAtLeast(0.0)
                if (fuel <= 0.0) shieldActive = false
                if (emergencyShieldActive) emergencyShieldBlockedHit = true
                shotIter.remove()
                continue
            }

            if (!phasingActive && checkCollision(shot.pos, player.pos, EngineConst.SHOT_RADIUS + GameConst.SHIP_SZ)) {
                killPlayer()
                shotIter.remove()
            }
        }

        // --- 13. Update missiles ---
        val missIter = missiles.iterator()
        while (missIter.hasNext()) {
            val m = missIter.next()
            m.life -= 1f
            // Decrement confusion countdown alongside life, so re-acquisition happens
            // on the tick AFTER the countdown reaches zero (matches C: shot.c:1618–1624).
            if (m.confusedTicks > 0f) m.confusedTicks -= 1f
            if (m.life <= 0f) {
                missIter.remove()
                continue
            }

            // Homing guidance: steer toward LOOK_AH-tick predicted position of target.
            // C: smart shot uses look-ahead of SMART_SHOT_LOOK_AH ticks (serverconst.h:178).
            // Suppress guidance while ECM-confused (C: smart_count > 0 skips lock-on).
            // SP-SIMPLIFICATION: C re-locks missile to a random player each
            // CONFUSED_UPDATE_GRANULARITY frames while confused, then probabilistically
            // re-locks to smart_relock_id on expiry (shot.c:1618). Single-player has one
            // NPC pool so the original targetNpcId is retained instead.
            // NPC missiles use targetNpcId == player.id to home on the player (BL-20).
            val npcTargetingPlayer = m.ownerId != player.id && m.targetNpcId == player.id.toInt()
            // BL-13: if player is cloaked and missile targets the player, apply visibility roll
            if (npcTargetingPlayer && cloakActive && m.confusedTicks <= 0f) {
                if (!canSeePlayerWhenCloaked()) {
                    m.confusedTicks = WeaponConst.CONFUSED_TIME
                }
            }
            val targetNpc =
                if (m.confusedTicks <= 0f && m.targetNpcId >= 0 &&
                    !npcTargetingPlayer
                ) {
                    npcShips.firstOrNull { it.id == m.targetNpcId }
                } else {
                    null
                }
            val homingTargetX: Double?
            val homingTargetY: Double?
            if (npcTargetingPlayer && m.confusedTicks <= 0f && player.isAlive()) {
                // NPC missile homes on the player
                homingTargetX = playerPixelX.toDouble() + player.vel.x * EngineConst.SMART_SHOT_LOOK_AH
                homingTargetY = playerPixelY.toDouble() + player.vel.y * EngineConst.SMART_SHOT_LOOK_AH
            } else if (targetNpc != null) {
                homingTargetX = targetNpc.x.toDouble() + targetNpc.vx * EngineConst.SMART_SHOT_LOOK_AH
                homingTargetY = targetNpc.y.toDouble() + targetNpc.vy * EngineConst.SMART_SHOT_LOOK_AH
            } else {
                homingTargetX = null
                homingTargetY = null
            }
            if (homingTargetX != null && homingTargetY != null) {
                val mx =
                    m.pos.cx
                        .toPixel()
                        .toDouble()
                val my =
                    m.pos.cy
                        .toPixel()
                        .toDouble()
                // Predicted target position (look-ahead)
                val desired = atan2(homingTargetY - my, homingTargetX - mx)
                val diff = wrapAngle(desired - m.headingRad + PI) - PI
                val prevHeading = m.headingRad
                m.headingRad = wrapAngle(m.headingRad + diff.coerceIn(-EngineConst.MISSILE_TURN_RATE, EngineConst.MISSILE_TURN_RATE))
                // Speed ramp: C SMART_SHOT_ACC ramp toward MISSILE_SPEED.
                // On overshoot (turned more than diff), divide speed by SMART_SHOT_DECFACT.
                val turned = wrapAngle(m.headingRad - prevHeading + PI) - PI
                val overshot = kotlin.math.abs(turned) < kotlin.math.abs(diff) * 0.5
                m.speed =
                    if (overshot) {
                        (m.speed / EngineConst.SMART_SHOT_DECFACT).coerceAtLeast(EngineConst.SMART_SHOT_MIN_SPEED)
                    } else {
                        (m.speed + EngineConst.SMART_SHOT_ACC).coerceAtMost(EngineConst.MISSILE_SPEED)
                    }
            } else {
                // Unguided: still ramp speed up to max
                m.speed = (m.speed + EngineConst.SMART_SHOT_ACC).coerceAtMost(EngineConst.MISSILE_SPEED)
            }

            val mvx = cos(m.headingRad) * m.speed
            val mvy = sin(m.headingRad) * m.speed
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
                    if (npc.hp > 0f) { // #C guard: only score once per NPC death
                        npc.hp -= EngineConst.NPC_MISSILE_HP_DAMAGE
                        if (npc.hp <= 0f) {
                            player.score += 1.0
                        }
                    }
                    npc.shield = false
                    hitNpc = true
                    break
                }
            }
            if (hitNpc) {
                missIter.remove()
                continue
            }

            // Shield blocks missile — C: apply Missile_hit_drain (= ED_SMART_SHOT_HIT for default
            // mods) directly; if fuel hits 0 the shield drops but the player survives.
            // (collision.c: drain = Missile_hit_drain(missile); Player_add_fuel(pl, drain);
            //  if (pl->fuel.sum <= 0) CLR_BIT(HAS_SHIELD))
            val effectiveShieldForMissile = shieldActive || emergencyShieldActive
            if (effectiveShieldForMissile && checkCollision(m.pos, player.pos, shieldRadius)) {
                fuel = (fuel + EnergyDrain.SMART_SHOT_HIT).coerceAtLeast(0.0)
                if (fuel <= 0.0) shieldActive = false
                if (emergencyShieldActive) emergencyShieldBlockedHit = true
                missIter.remove()
                continue
            }
            // Own missiles never harm the player
            if (m.ownerId != player.id) {
                if (!phasingActive && checkCollision(m.pos, player.pos, EngineConst.MISSILE_RADIUS + GameConst.SHIP_SZ)) {
                    killPlayer()
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

            // Arm countdown: suppress proximity detection until the mine is armed.
            // KXPilot-specific: MINE_ARM_TICKS grace window after drop.
            if (mine.armTicksRemaining > 0) {
                mine.armTicksRemaining--
                continue
            }

            // C: mine->pl_range = MINE_RANGE. A single radius is used for both proximity
            // detection and the collision that kills the player.  No separate blast radius.
            var detonated = false

            // --- Sensing check (Phase 2e) ---
            // Outer sensing range: MINE_SENSE_BASE_RANGE + sensor * MINE_SENSE_RANGE_FACTOR.
            // When the player enters this radius the mine is "sensed" (HUD can warn).
            // C: frame.c:733 — used to decide whether to send ownership info to client.
            val senseRadius =
                WeaponConst.MINE_SENSE_BASE_RANGE +
                    WeaponConst.MINE_SENSE_RANGE_FACTOR * playerItems.sensor
            mine.sensed = checkCollision(mine.pos, player.pos, senseRadius)

            // Check NPC ships (owner-immune logic not applied to NPCs — they are never owners)
            for (npc in npcShips) {
                if (npc.hp <= 0f) continue // dead NPCs cannot trigger a mine
                val npx = npc.x.toDouble().pixelToClickInt()
                val npy = npc.y.toDouble().pixelToClickInt()
                val npcClick = ClPos(world.wrapXClick(npx), world.wrapYClick(npy))
                if (checkCollision(mine.pos, npcClick, EngineConst.MINE_TRIGGER_RADIUS)) {
                    detonated = true
                    break
                }
            }

            // Check player — owner-immune: own mines never trigger on player (C default fuse=-1)
            if (!detonated && !phasingActive && !(mine.ownerImmune && mine.ownerId == player.id)) {
                if (checkCollision(mine.pos, player.pos, EngineConst.MINE_TRIGGER_RADIUS)) {
                    detonated = true
                }
            }

            if (detonated) {
                // Spawn C-authentic debris with a random count each detonation.
                // C: num_debris = (int)(intensity * num_modv * (0.20 + 0.10*rfrac()))
                //   = (int)(512 * 1 * (0.20 + 0.10*rfrac())) → range [102, 153]  (shot.c:1248)
                // C: Make_debris(..., type=OBJ_DEBRIS, mass=DEBRIS_MASS, color=RED, radius=6,
                //     num_debris, min_dir=0, max_dir=RES-1,
                //     min_speed=20, max_speed=128, min_life=8, max_life=256)  (shot.c:1264-1276)
                // Each fragment: dir = uniform in [0, RES-1]; speed = uniform [20, 128] px/tick.
                // (ship.c:573-610)
                val intensity = 512
                val numDebris = (intensity * (0.20 + 0.10 * rng.nextDouble())).toInt()

                val mineVx = 0f // mines are stationary
                val mineVy = 0f
                repeat(numDebris) {
                    val dir = rng.nextDouble(0.0, 2.0 * PI)
                    val speed =
                        EngineConst.MINE_DEBRIS_MIN_SPEED +
                            rng.nextDouble() *
                            (EngineConst.MINE_DEBRIS_MAX_SPEED - EngineConst.MINE_DEBRIS_MIN_SPEED)
                    val dvx = (mineVx + cos(dir) * speed).toFloat()
                    val dvy = (mineVy + sin(dir) * speed).toFloat()
                    val life =
                        EngineConst.MINE_DEBRIS_MIN_LIFE +
                            rng.nextFloat() *
                            (EngineConst.MINE_DEBRIS_MAX_LIFE - EngineConst.MINE_DEBRIS_MIN_LIFE)
                    debris +=
                        DebrisData(
                            pos = mine.pos,
                            vel = Vector(dvx, dvy),
                            life = life,
                            ownerId = mine.ownerId,
                        )
                }
                mineIter.remove()
            }
        }

        // --- 15. Update debris ---
        // C: Player_collides_with_debris — collision_cost(mass, speed) = mass*speed/128.0 fuel drain.
        // Normal HAS_SHIELD does NOT protect from debris (only HAS_EMERGENCY_SHIELD does).
        val debrisIter = debris.iterator()
        while (debrisIter.hasNext()) {
            val d = debrisIter.next()
            d.life -= 1f
            if (d.life <= 0f) {
                debrisIter.remove()
                continue
            }

            // Move debris — destroyed on wall contact (same as shots)
            val (dp, hitWall) = sweepMoveShot(d.pos, d.vel.x.toDouble(), d.vel.y.toDouble())
            if (hitWall) {
                debrisIter.remove()
                continue
            }
            d.pos = dp

            val debrisSpeed = hypot(d.vel.x.toDouble(), d.vel.y.toDouble())
            val cost = d.mass * debrisSpeed / 128.0 // collision_cost(mass, speed)

            // Hit NPC ships — one fragment hits at most one target, then is destroyed
            var hitSomething = false
            for (npc in npcShips) {
                if (npc.hp <= 0f) continue
                val npx = npc.x.toDouble().pixelToClickInt()
                val npy = npc.y.toDouble().pixelToClickInt()
                val npcClick = ClPos(world.wrapXClick(npx), world.wrapYClick(npy))
                if (checkCollision(d.pos, npcClick, EngineConst.MINE_DEBRIS_HIT_RADIUS)) {
                    npc.hp -= cost.toFloat()
                    if (npc.hp <= 0f) player.score += 1.0
                    hitSomething = true
                    break
                }
            }
            if (hitSomething) {
                debrisIter.remove()
                continue
            }

            // Hit player — shield does NOT protect from debris (C: only HAS_EMERGENCY_SHIELD bypasses)
            if (player.isAlive() && checkCollision(d.pos, player.pos, EngineConst.MINE_DEBRIS_HIT_RADIUS)) {
                if (emergencyShieldActive) {
                    // Emergency shield blocks debris (C: HAS_EMERGENCY_SHIELD in collision.c:974)
                    emergencyShieldBlockedHit = true
                    debrisIter.remove()
                } else {
                    fuel = (fuel - cost).coerceAtLeast(0.0)
                    if (fuel <= 0.0) killPlayer()
                    debrisIter.remove()
                }
            }
        }

        // --- 16. Update balls ---
        // BL-15: process emergency shield timer after all collision checks
        if (emergencyShieldActive) {
            if (emergencyShieldBlockedHit) {
                emergencyShieldTicksLeft -= 1.0
                if (emergencyShieldTicksLeft <= 0.0) {
                    if (playerItems.emergencyShield > 0) {
                        activateEmergencyShield()
                    } else {
                        emergencyShieldActive = false
                    }
                }
            }
            emergencyShieldBlockedHit = false // reset for next tick
        }

        updateBalls(keys, npcShips)

        // --- 17. Update torpedoes ---
        tickTorpedoes(npcShips)

        // --- 18. Update heat-seekers ---
        tickHeatSeekers(npcShips)

        // --- 19. Update cluster mines ---
        tickClusterMines(npcShips)

        // --- 20. Update nukes ---
        tickNukes(npcShips)

        // --- 21. Map cannons fire ---
        tickCannons()

        // --- 22. Fuel station refueling ---
        tickFuelStations()

        // --- 23. Destructible targets repair ---
        tickTargets()

        // --- 24. Energy pack pickup and expiry ---
        tickEnergyPacks()

        // --- 24b. World items spawn, expire, pickup (BL-07) ---
        tickWorldItems()

        // --- 25. Advance and expire thrust sparks ---
        tickSparks()

        // --- 26. Laser pulses (BL-01) ---
        tickLaserPulses(npcShips)

        // --- 27. Self-destruct countdown (BL-08) ---
        if (selfDestructTicks > 0.0) {
            selfDestructTicks -= 1.0
            if (selfDestructTicks <= 0.0) {
                selfDestructTicks = 0.0
                killPlayer()
            }
        }

        // --- 28. Wormhole teleportation (BL-02) ---
        tickWormholes()

        // --- 29. Checkpoint detection (BL-05) ---
        tickCheckpoints()

        // --- 30. Per-tile friction areas (BL-06) ---
        tickFrictionAreas()

        // --- 30b. Deflector — push approaching shots/NPCs outward (BL-12) ---
        tickDeflector(shots, laserPulses, npcShips)

        // --- 31. NPC death detection: queue dead NPCs for respawn ---
        val deadIter = npcShips.iterator()
        while (deadIter.hasNext()) {
            val npc = deadIter.next()
            if (npc.hp <= 0f && !npcRespawnTimers.containsKey(npc.id)) {
                if (npcFactories.containsKey(npc.id)) {
                    npcRespawnTimers[npc.id] = EngineConst.NPC_RESPAWN_DELAY_TICKS
                }
                deadIter.remove()
            }
        }
    }

    /**
     * Convenience overload accepting a plain (immutable) [List].
     *
     * The list is copied into a [MutableList] internally so NPC lifecycle
     * management (death removal, respawn re-insertion) still runs; changes are
     * not reflected back to the caller's list.  Pass a [MutableList] directly
     * when you need the respawned NPC to appear in your own collection.
     */
    @JvmName("tickImmutable")
    fun tick(
        keys: KeyState,
        npcShips: List<EngineTarget>,
    ) = tick(keys, npcShips.toMutableList())

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
    private fun updateBalls(
        keys: KeyState,
        npcShips: List<EngineTarget>,
    ) {
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

            // --- ball–NPC collision / carry ---
            // C: robots can pick up and carry the treasure ball (robotdef.c navigate/attack).
            // When an NPC enters BALL_RADIUS + SHIP_SZ of an unowned (or opponent-touched) ball
            // it picks it up, tags the ball with the NPC's team, and starts carrying it.
            if (ball.connectedPlayerId == BallData.NO_PLAYER) {
                for (npc in npcShips) {
                    val npcClick =
                        ClPos(
                            world.wrapXClick((npc.x.toDouble() * ClickConst.CLICK).toInt()),
                            world.wrapYClick((npc.y.toDouble() * ClickConst.CLICK).toInt()),
                        )
                    if (checkCollision(ball.pos, npcClick, EngineConst.BALL_RADIUS + GameConst.SHIP_SZ)) {
                        // Elastic momentum exchange
                        val m1 = EngineConst.PLAYER_MASS
                        val m2 = EngineConst.BALL_MASS
                        val total = m1 + m2
                        val nvx = npc.vx.toDouble()
                        val nvy = npc.vy.toDouble()
                        val bvx = ball.vel.x.toDouble()
                        val bvy = ball.vel.y.toDouble()
                        val newNvx = ((m1 - m2) * nvx + 2.0 * m2 * bvx) / total
                        val newNvy = ((m1 - m2) * nvy + 2.0 * m2 * bvy) / total
                        val newBvx = ((m2 - m1) * bvx + 2.0 * m1 * nvx) / total
                        val newBvy = ((m2 - m1) * bvy + 2.0 * m1 * nvy) / total
                        npc.vx = newNvx.toFloat()
                        npc.vy = newNvy.toFloat()
                        ball.vel = Vector(newBvx.toFloat(), newBvy.toFloat())
                        // Determine NPC team from DemoShip if available; default team 1
                        val npcTeam = (npc as? DemoShip)?.let { 1 } ?: 1
                        ball.touchTeam = npcTeam
                        ball.connectedPlayerId = npc.id
                        (npc as? DemoShip)?.carryingBallId = npc.id
                        break
                    }
                }
            }

            // --- scoring: ball enters opposing team's home treasure ---
            checkBallGoal(ball, npcShips)
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
    private fun checkBallGoal(
        ball: BallData,
        npcShips: List<EngineTarget>,
    ) {
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
                // Ball delivered to opposing goal — award score to whichever entity last touched it.
                if (ball.connectedPlayerId == player.id.toInt() ||
                    (
                        ball.connectedPlayerId == BallData.NO_PLAYER &&
                            (ball.touchTeam == player.team.toInt() || player.team.toInt() == 0)
                    )
                ) {
                    player.score += 1.0
                } else {
                    // Award score to the carrying NPC (or the last NPC that touched it)
                    val carrier = npcShips.firstOrNull { it.id == ball.connectedPlayerId }
                    (carrier as? DemoShip)?.score?.plus(1.0).also {
                        (carrier as? DemoShip)?.score = it ?: 0.0
                    }
                    // Clear carry state on the NPC
                    (carrier as? DemoShip)?.carryingBallId = BallData.NO_PLAYER
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
     *
     * Returns false (and does nothing) if [deathTicksRemaining] > 0 — the
     * player must wait for the respawn delay to elapse first (mirrors C server
     * RECOVERY_DELAY).  Returns true when the respawn succeeded.
     */
    fun respawn(): Boolean {
        if (deathTicksRemaining > 0) return false
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
        debris.clear()
        // Disconnect the player from any ball they were holding when they died.
        // Without this, the spring-force code would apply one phantom tick of
        // connector physics at world-centre before the key-release path fires.
        balls.forEach { if (it.connectedPlayerId == player.id.toInt()) it.connectedPlayerId = BallData.NO_PLAYER }
        fuel = fuelMax
        shieldActive = false
        deflectorActive = false
        cloakActive = false
        phasingActive = false
        phasingTicksLeft = 0.0
        emergencyShieldActive = false
        emergencyShieldTicksLeft = 0.0
        emergencyShieldBlockedHit = false
        emergencyThrustActive = false
        emergencyThrustTicksLeft = 0.0
        lockedNpcId = -1
        lockDirRad = Double.NaN
        lockDistPx = 0.0
        deathTicksRemaining = 0
        selfDestructTicks = 0.0
        laserFireTimer = 0.0
        return true
    }

    /**
     * Spawn the player at the [baseIndex]-th base in the world.
     * Falls back to [respawn] if there are no bases.
     * Returns false (and does nothing) if [deathTicksRemaining] > 0.
     */
    fun spawnAtBase(baseIndex: Int = 0): Boolean {
        if (deathTicksRemaining > 0) return false
        val bases = world.bases
        if (bases.isEmpty()) return respawn()
        val base = bases[baseIndex.coerceIn(0, bases.lastIndex)]
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
        debris.clear()
        balls.forEach { if (it.connectedPlayerId == player.id.toInt()) it.connectedPlayerId = BallData.NO_PLAYER }
        fuel = fuelMax
        shieldActive = false
        lockedNpcId = -1
        lockDirRad = Double.NaN
        lockDistPx = 0.0
        deathTicksRemaining = 0
        return true
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
        // If the locked NPC was removed from the list (stale lock), clear rather than
        // silently jumping to index 0.  The caller should call clearLock() when an NPC dies,
        // but this guard makes the invariant robust against missed calls.
        if (idx == -1 && lockedNpcId >= 0) {
            clearLock()
            return
        }
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
        if (idx == -1 && lockedNpcId >= 0) {
            clearLock()
            return
        }
        lockedNpcId = npcShips[((idx - 1) + npcShips.size) % npcShips.size].id
    }

    /**
     * Clear the current lock target (e.g. when the locked NPC is removed from the world).
     */
    fun clearLock() {
        lockedNpcId = -1
        lockDirRad = Double.NaN
        lockDistPx = 0.0
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

    /** Kill the player: set state, start respawn timer, clear own mines (#19, #20). */
    private fun killPlayer() {
        player.plState = PlayerState.KILLED
        deathTicksRemaining = EngineConst.RESPAWN_DELAY_TICKS
        // #20: own mines cleared on death
        mines.removeAll { it.ownerId == player.id }
    }

    /** BL-14: deactivate the phasing device; kill player if inside a wall. */
    private fun deactivatePhasing() {
        phasingActive = false
        phasingTicksLeft = 0.0
        // C: if player is inside a wall after de-phasing, kill them
        if (isInsideWall(player.pos)) {
            killPlayer()
        }
    }

    /** BL-15: activate emergency shield, consuming one charge and resetting the timer. */
    private fun activateEmergencyShield() {
        playerItems = playerItems.copy(emergencyShield = playerItems.emergencyShield - 1)
        emergencyShieldActive = true
        emergencyShieldTicksLeft = EngineConst.EMERGENCY_SHIELD_TIME_TICKS
    }

    /** BL-14: returns true if [pos] is inside a non-space (solid) map cell. */
    private fun isInsideWall(pos: ClPos): Boolean {
        val bx = pos.cx / ClickConst.BLOCK_CLICKS
        val by = pos.cy / ClickConst.BLOCK_CLICKS
        val cell = world.getBlock(bx, by)
        return cell == CellType.FILLED
    }

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

    /**
     * Spawn a shot fired by an NPC ship (from [NpcShotEvent]).
     *
     * NPC shots use the NPC's id as [ShotData.ownerId] so the existing shot loop
     * treats them as enemy shots — they will collide with the player (unless the
     * shield is active) without needing any additional changes to the shot loop.
     *
     * Called from the InGameScreen game loop after [NpcAiManager.tickAll].
     */
    fun spawnNpcShot(event: NpcShotEvent) {
        val shotVx = (event.npcVx + cos(event.headingRad) * NpcAiConst.NPC_SHOT_SPEED).toFloat()
        val shotVy = (event.npcVy + sin(event.headingRad) * NpcAiConst.NPC_SHOT_SPEED).toFloat()
        val startCx = world.wrapXClick((event.x * ClickConst.CLICK).toInt() + shotVx.toDouble().pixelToClickInt())
        val startCy = world.wrapYClick((event.y * ClickConst.CLICK).toInt() + shotVy.toDouble().pixelToClickInt())
        // NPC ids use NPC_ID_BASE offset; they fit in Short range (100–32767).
        // Require here so a misconfigured id fails loudly rather than silently
        // wrapping and masquerading as the player's own shots.
        require(event.npcId in 2..Short.MAX_VALUE) {
            "NPC id ${event.npcId} is out of valid Short range [2, ${Short.MAX_VALUE}]"
        }
        shots +=
            ShotData(
                pos = ClPos(startCx, startCy),
                vel = Vector(shotVx, shotVy),
                life = EngineConst.SHOT_LIFE,
                ownerId = event.npcId.toShort(), // enemy ownerId — hits the player
                freshTick = true,
            )
    }

    /**
     * Spawn a missile fired by an NPC ship (from [NpcWeaponEvent.Missile]).
     *
     * Uses the same [MissileData] path as a player missile but with the NPC's id
     * as ownerId.  The missile will home on the player (lockedNpcId is from the
     * player's perspective; NPC missiles target the player via targetNpcId = -1
     * which is the sentinel for "track nearest enemy" — the player in this context).
     * C: Fire_shot(pl, OBJ_SMART_SHOT, ...) (robotdef.c:1065).
     */
    fun spawnNpcMissile(event: NpcWeaponEvent.Missile) {
        require(event.npcId in 2..Short.MAX_VALUE) {
            "NPC id ${event.npcId} is out of valid Short range [2, ${Short.MAX_VALUE}]"
        }
        val inheritedVx = event.npcVx.toDouble()
        val inheritedVy = event.npcVy.toDouble()
        val startCx = world.wrapXClick((event.x * ClickConst.CLICK).toInt() + inheritedVx.pixelToClickInt())
        val startCy = world.wrapYClick((event.y * ClickConst.CLICK).toInt() + inheritedVy.pixelToClickInt())
        missiles +=
            MissileData(
                pos = ClPos(startCx, startCy),
                headingRad = event.headingRad,
                life = EngineConst.MISSILE_LIFE,
                // targetNpcId = player.id means "home on the player" — detected in the
                // homing loop (NPC missile targeting branch).
                targetNpcId = player.id.toInt(),
                ownerId = event.npcId.toShort(),
            )
    }

    /**
     * Drop a mine at an NPC's position (from [NpcWeaponEvent.Mine]).
     *
     * The mine is placed stationary; ownerImmune=false so it can be triggered by
     * any ship including the owner (C: NPC mines threaten all ships — NPC has no
     * permanent immunity).
     * C: Place_mine(pl) (robotdef.c:1093).
     */
    fun spawnNpcMine(event: NpcWeaponEvent.Mine) {
        require(event.npcId in 2..Short.MAX_VALUE) {
            "NPC id ${event.npcId} is out of valid Short range [2, ${Short.MAX_VALUE}]"
        }
        mines +=
            MineData(
                pos =
                    ClPos(
                        world.wrapXClick((event.x * ClickConst.CLICK).toInt()),
                        world.wrapYClick((event.y * ClickConst.CLICK).toInt()),
                    ),
                ownerImmune = false, // C: NPC mines are live for all ships
                life = EngineConst.MINE_LIFE,
                ownerId = event.npcId.toShort(),
                armTicksRemaining = EngineConst.MINE_ARM_TICKS,
            )
    }

    /**
     * Dispatch all weapon events returned by [NpcAiManager.tickAll] for one tick.
     *
     * Convenience method — the InGameScreen game loop may call this instead of
     * manually switching on event types.
     */
    fun dispatchNpcWeaponEvents(
        events: List<NpcWeaponEvent>,
        npcShips: List<DemoShip>,
    ) {
        for (event in events) {
            when (event) {
                is NpcWeaponEvent.Shot -> {
                    spawnNpcShot(event)
                }

                is NpcWeaponEvent.Missile -> {
                    spawnNpcMissile(event)
                }

                is NpcWeaponEvent.Mine -> {
                    spawnNpcMine(event)
                }

                is NpcWeaponEvent.ShieldChange -> {
                    npcShips.firstOrNull { it.id == event.npcId }?.shield = event.active
                }
            }
        }
    }

    private fun spawnMissile() {
        // #15 Missile inherits player velocity by applying it as initial position offset
        // (equivalent to the C server adding player vel to missile vel at spawn)
        val inheritedVx = player.vel.x.toDouble()
        val inheritedVy = player.vel.y.toDouble()
        val startCx = world.wrapXClick(player.pos.cx + inheritedVx.pixelToClickInt())
        val startCy = world.wrapYClick(player.pos.cy + inheritedVy.pixelToClickInt())
        missiles +=
            MissileData(
                pos = ClPos(startCx, startCy),
                headingRad = player.floatDir,
                life = EngineConst.MISSILE_LIFE,
                targetNpcId = lockedNpcId,
                ownerId = player.id,
            )
    }

    private fun dropMine() {
        // Mine is placed at player position and stays stationary.
        // ownerImmune = true: C default (mineFuseTicks=0 → fuse=-1, permanent owner immunity).
        mines +=
            MineData(
                pos = player.pos,
                ownerImmune = true,
                life = EngineConst.MINE_LIFE,
                ownerId = player.id,
            )
    }

    private fun spawnTorpedo(nuclear: Boolean) {
        // Torpedo: starts at player position, heading = player heading.
        // C: TORPEDO_SPEED_TIME ticks of acceleration at TORPEDO_ACC px/tick².
        // Inherits player velocity as spawn offset (same pattern as missiles).
        val inheritedVx = player.vel.x.toDouble()
        val inheritedVy = player.vel.y.toDouble()
        val startCx = world.wrapXClick(player.pos.cx + inheritedVx.pixelToClickInt())
        val startCy = world.wrapYClick(player.pos.cy + inheritedVy.pixelToClickInt())
        if (nuclear) {
            nukes +=
                NukeData(
                    pos = ClPos(startCx, startCy),
                    headingRad = player.floatDir,
                    life = EngineConst.MISSILE_LIFE,
                    ownerId = player.id,
                )
        } else {
            torpedoes +=
                TorpedoData(
                    pos = ClPos(startCx, startCy),
                    headingRad = player.floatDir,
                    life = EngineConst.MISSILE_LIFE,
                    ownerId = player.id,
                )
        }
    }

    private fun spawnHeatSeeker() {
        // Heat-seeker: targets nearest NPC (or locked NPC if set), 3-phase guidance.
        val inheritedVx = player.vel.x.toDouble()
        val inheritedVy = player.vel.y.toDouble()
        val startCx = world.wrapXClick(player.pos.cx + inheritedVx.pixelToClickInt())
        val startCy = world.wrapYClick(player.pos.cy + inheritedVy.pixelToClickInt())
        heatSeekers +=
            HeatSeekerData(
                pos = ClPos(startCx, startCy),
                headingRad = player.floatDir,
                life = EngineConst.MISSILE_LIFE,
                ownerId = player.id,
                targetNpcId = lockedNpcId,
            )
    }

    private fun dropClusterMine() {
        clusterMines +=
            ClusterMineData(
                pos = player.pos,
                ownerImmune = true,
                ownerId = player.id,
            )
    }

    /**
     * Activate ECM pulse: disrupts missiles and heat-seekers within [WeaponConst.ECM_DISTANCE].
     * C: ECM pulse sets smart_count = CONFUSED_TIME on each missile within range (item.c).
     * While confused, missiles coast unguided for [WeaponConst.CONFUSED_TIME] ticks.
     */
    private fun activateEcm(npcShips: List<EngineTarget>) {
        fuel = (fuel + EnergyDrain.ECM).coerceAtLeast(0.0)
        val px = playerPixelX.toDouble()
        val py = playerPixelY.toDouble()
        val ecmRange = WeaponConst.ECM_DISTANCE
        for (m in missiles) {
            val mx =
                m.pos.cx
                    .toPixel()
                    .toDouble()
            val my =
                m.pos.cy
                    .toPixel()
                    .toDouble()
            if (hypot(mx - px, my - py) <= ecmRange) {
                // Blind the missile: set confusedTicks so guidance is suppressed.
                // C: smart_count = CONFUSED_TIME in item.c; tick loop skips targeting while > 0.
                // SP-SIMPLIFICATION: C re-locks missile to a random player each
                // CONFUSED_UPDATE_GRANULARITY frames while confused, then probabilistically
                // re-locks to smart_relock_id on expiry (shot.c:1618). Single-player has one
                // NPC pool so the original targetNpcId is retained instead.
                m.confusedTicks = WeaponConst.CONFUSED_TIME
            }
        }
        for (hs in heatSeekers) {
            val hx =
                hs.pos.cx
                    .toPixel()
                    .toDouble()
            val hy =
                hs.pos.cy
                    .toPixel()
                    .toDouble()
            if (hypot(hx - px, hy - py) <= ecmRange) {
                // SP-SIMPLIFICATION: see missile comment above (same simplification applies).
                hs.confusedTicks = WeaponConst.CONFUSED_TIME
            }
        }
    }

    // -----------------------------------------------------------------------
    // Transporter
    // -----------------------------------------------------------------------

    /**
     * Activate the transporter beam.
     *
     * Finds the nearest NPC within [EngineConst.TRANSPORTER_DISTANCE] pixels.
     * If one is found, consumes one transporter item and [EngineConst.TRANSPORTER_FUEL_COST]
     * fuel, then awards the player one sensor item (single-player approximation of
     * the C `Transporter_transfer` which steals a random item from another player).
     *
     * Maps to C `Transporter_activate` in server/transporter.c.
     */
    private fun activateTransporter(npcShips: List<EngineTarget>) {
        if (playerItems.transporter <= 0) return
        if (fuel < WeaponConst.TRANSPORTER_FUEL_COST) return

        // Find nearest NPC within range
        var nearest: EngineTarget? = null
        var nearestDist = Double.MAX_VALUE
        for (npc in npcShips) {
            val npx = npc.x.toDouble().pixelToClickInt()
            val npy = npc.y.toDouble().pixelToClickInt()
            if (checkCollision(player.pos, ClPos(world.wrapXClick(npx), world.wrapYClick(npy)), WeaponConst.TRANSPORTER_DISTANCE)) {
                var dx = npc.x.toDouble() - playerPixelX.toDouble()
                var dy = npc.y.toDouble() - playerPixelY.toDouble()
                val halfW = world.width / 2.0
                val halfH = world.height / 2.0
                if (dx > halfW) dx -= world.width
                if (dx < -halfW) dx += world.width
                if (dy > halfH) dy -= world.height
                if (dy < -halfH) dy += world.height
                val dist = hypot(dx, dy)
                if (dist < nearestDist) {
                    nearestDist = dist
                    nearest = npc
                }
            }
        }

        if (nearest != null) {
            // Consume transporter item and fuel; award player one sensor
            playerItems =
                playerItems.copy(
                    transporter = playerItems.transporter - 1,
                    sensor = playerItems.sensor + 1,
                )
            fuel = (fuel - WeaponConst.TRANSPORTER_FUEL_COST).coerceAtLeast(0.0)
        }
    }

    // -----------------------------------------------------------------------
    // Torpedo tick
    // -----------------------------------------------------------------------

    private fun tickTorpedoes(npcShips: List<EngineTarget>) {
        val iter = torpedoes.iterator()
        while (iter.hasNext()) {
            val t = iter.next()
            t.life -= 1f
            if (t.life <= 0f) {
                iter.remove()
                continue
            }

            // Acceleration ramp
            if (t.accelTicksLeft > 0f) {
                t.speed = (t.speed + WeaponConst.TORPEDO_ACC).coerceAtMost(EngineConst.MISSILE_SPEED)
                t.accelTicksLeft -= 1f
            }
            val tvx = cos(t.headingRad) * t.speed
            val tvy = sin(t.headingRad) * t.speed
            val (tp, hitWall) = sweepMoveShot(t.pos, tvx, tvy)
            if (hitWall) {
                iter.remove()
                continue
            }
            t.pos = tp

            // Proximity blast
            var detonated = false
            for (npc in npcShips) {
                val nx = npc.x.toDouble().pixelToClickInt()
                val ny = npc.y.toDouble().pixelToClickInt()
                if (checkCollision(
                        t.pos,
                        ClPos(world.wrapXClick(nx), world.wrapYClick(ny)),
                        WeaponConst.TORPEDO_RANGE + GameConst.SHIP_SZ,
                    )
                ) {
                    if (npc.hp > 0f) {
                        npc.hp -= EngineConst.NPC_MISSILE_HP_DAMAGE
                        if (npc.hp <= 0f) player.score += 1.0
                    }
                    detonated = true
                    break
                }
            }
            if (!detonated) {
                val effectiveShieldForTorpedo = shieldActive || emergencyShieldActive
                if (effectiveShieldForTorpedo && checkCollision(t.pos, player.pos, WeaponConst.TORPEDO_RANGE + GameConst.SHIP_SZ)) {
                    fuel = (fuel + EnergyDrain.SMART_SHOT_HIT).coerceAtLeast(0.0)
                    if (emergencyShieldActive) emergencyShieldBlockedHit = true
                    detonated = true
                } else if (t.ownerId != player.id && checkCollision(t.pos, player.pos, WeaponConst.TORPEDO_RANGE + GameConst.SHIP_SZ)) {
                    killPlayer()
                    detonated = true
                }
            }
            if (detonated) iter.remove()
        }
    }

    // -----------------------------------------------------------------------
    // Heat-seeker tick
    // -----------------------------------------------------------------------

    private fun tickHeatSeekers(npcShips: List<EngineTarget>) {
        val iter = heatSeekers.iterator()
        while (iter.hasNext()) {
            val hs = iter.next()
            hs.life -= 1f
            // Decrement confusion countdown alongside life, so re-acquisition happens
            // on the tick AFTER the countdown reaches zero (matches C: shot.c:1618–1624).
            if (hs.confusedTicks > 0f) hs.confusedTicks -= 1f
            if (hs.life <= 0f) {
                iter.remove()
                continue
            }

            // Phase transition
            hs.phaseTicks -= 1f
            if (hs.phaseTicks <= 0f) {
                hs.phase = (hs.phase + 1).coerceAtMost(3)
                hs.phaseTicks =
                    when (hs.phase) {
                        1 -> WeaponConst.HEAT_MID_TIMEOUT
                        2 -> WeaponConst.HEAT_WIDE_TIMEOUT
                        else -> Float.MAX_VALUE // unguided coast
                    }
            }

            // Guidance: steer toward target with phase-appropriate error
            // Suppress guidance while ECM-confused (C: smart_count > 0 skips lock-on).
            // SP-SIMPLIFICATION: C re-locks heat-seeker to a random player each
            // CONFUSED_UPDATE_GRANULARITY frames while confused, then probabilistically
            // re-locks to smart_relock_id on expiry (shot.c:1618). Single-player has one
            // NPC pool so the original targetNpcId is retained instead.
            val targetNpc = if (hs.confusedTicks <= 0f && hs.targetNpcId >= 0) npcShips.firstOrNull { it.id == hs.targetNpcId } else null
            if (targetNpc != null && hs.phase < 3) {
                val mx =
                    hs.pos.cx
                        .toPixel()
                        .toDouble()
                val my =
                    hs.pos.cy
                        .toPixel()
                        .toDouble()
                val tx = targetNpc.x.toDouble()
                val ty = targetNpc.y.toDouble()
                val dist = hypot(tx - mx, ty - my)
                // Check phase range gate
                val inRange =
                    when (hs.phase) {
                        0 -> dist <= WeaponConst.HEAT_CLOSE_RANGE
                        1 -> dist <= WeaponConst.HEAT_MID_RANGE
                        else -> true
                    }
                if (inRange) {
                    val desired = atan2(ty - my, tx - mx)
                    // Add phase-appropriate heading jitter
                    val errorHU =
                        when (hs.phase) {
                            0 -> WeaponConst.HEAT_CLOSE_ERROR
                            1 -> WeaponConst.HEAT_MID_ERROR
                            else -> WeaponConst.HEAT_WIDE_ERROR
                        }
                    val errorRad =
                        errorHU * EngineConst.TURN_RATE_RAD *
                            (rng.nextDouble() * 2.0 - 1.0)
                    val diff = wrapAngle(desired + errorRad - hs.headingRad + PI) - PI
                    hs.headingRad = wrapAngle(hs.headingRad + diff.coerceIn(-EngineConst.MISSILE_TURN_RATE, EngineConst.MISSILE_TURN_RATE))
                }
            }

            val hvx = cos(hs.headingRad) * hs.speed
            val hvy = sin(hs.headingRad) * hs.speed
            val (hp, hitWall) = sweepMoveShot(hs.pos, hvx, hvy)
            if (hitWall) {
                iter.remove()
                continue
            }
            hs.pos = hp

            // Hit detection (same as missiles)
            var hit = false
            for (npc in npcShips) {
                val nx = npc.x.toDouble().pixelToClickInt()
                val ny = npc.y.toDouble().pixelToClickInt()
                if (checkCollision(
                        hs.pos,
                        ClPos(world.wrapXClick(nx), world.wrapYClick(ny)),
                        EngineConst.MISSILE_RADIUS + GameConst.SHIP_SZ,
                    )
                ) {
                    if (npc.hp > 0f) {
                        npc.hp -= EngineConst.NPC_MISSILE_HP_DAMAGE
                        if (npc.hp <= 0f) player.score += 1.0
                    }
                    hit = true
                    break
                }
            }
            if (!hit) {
                val effectiveShieldForHs = shieldActive || emergencyShieldActive
                if (effectiveShieldForHs &&
                    checkCollision(hs.pos, player.pos, (EngineConst.MISSILE_RADIUS + GameConst.SHIP_SZ).toDouble() + 8)
                ) {
                    fuel = (fuel + EnergyDrain.SMART_SHOT_HIT).coerceAtLeast(0.0)
                    if (emergencyShieldActive) emergencyShieldBlockedHit = true
                    hit = true
                } else if (hs.ownerId != player.id && checkCollision(hs.pos, player.pos, EngineConst.MISSILE_RADIUS + GameConst.SHIP_SZ)) {
                    killPlayer()
                    hit = true
                }
            }
            if (hit) iter.remove()
        }
    }

    // -----------------------------------------------------------------------
    // Cluster mine tick
    // -----------------------------------------------------------------------

    private fun tickClusterMines(npcShips: List<EngineTarget>) {
        val iter = clusterMines.iterator()
        while (iter.hasNext()) {
            val cm = iter.next()
            cm.life -= 1f
            if (cm.life <= 0f) {
                iter.remove()
                continue
            }

            var detonated = false
            for (npc in npcShips) {
                if (npc.hp <= 0f) continue
                val nx = npc.x.toDouble().pixelToClickInt()
                val ny = npc.y.toDouble().pixelToClickInt()
                if (checkCollision(cm.pos, ClPos(world.wrapXClick(nx), world.wrapYClick(ny)), EngineConst.MINE_TRIGGER_RADIUS)) {
                    detonated = true
                    break
                }
            }
            if (!detonated && !(cm.ownerImmune && cm.ownerId == player.id)) {
                if (checkCollision(cm.pos, player.pos, EngineConst.MINE_TRIGGER_RADIUS)) detonated = true
            }

            if (detonated) {
                // Spawn shots in a full circle — count = mass * 0.9 / shotMass
                val count = (cm.mass * WeaponConst.CLUSTER_MASS_FACTOR / WeaponConst.CLUSTER_SHOT_MASS).toInt().coerceAtLeast(1)
                val angleStep = 2.0 * PI / count
                repeat(count) { i ->
                    val dir = i * angleStep
                    val svx = (cos(dir) * GameConst.SHOT_SPEED).toFloat()
                    val svy = (sin(dir) * GameConst.SHOT_SPEED).toFloat()
                    shots +=
                        ShotData(
                            pos = cm.pos,
                            vel = Vector(svx, svy),
                            life = EngineConst.SHOT_LIFE,
                            ownerId = cm.ownerId,
                        )
                }
                // Fuel drain on owner: CLUSTER_MASS_DRAIN = count * |ED_SHOT|
                if (cm.ownerId == player.id) {
                    fuel = (fuel + EnergyDrain.SHOT * count).coerceAtLeast(0.0)
                }
                iter.remove()
            }
        }
    }

    // -----------------------------------------------------------------------
    // Nuke tick
    // -----------------------------------------------------------------------

    private fun tickNukes(npcShips: List<EngineTarget>) {
        val iter = nukes.iterator()
        while (iter.hasNext()) {
            val n = iter.next()
            n.life -= 1f
            if (n.life <= 0f) {
                iter.remove()
                continue
            }

            // Acceleration ramp
            if (n.accelTicksLeft > 0f) {
                n.speed = (n.speed + WeaponConst.NUKE_ACC).coerceAtMost(EngineConst.MISSILE_SPEED)
                n.accelTicksLeft -= 1f
            }
            val nvx = cos(n.headingRad) * n.speed
            val nvy = sin(n.headingRad) * n.speed
            val (np, hitWall) = sweepMoveShot(n.pos, nvx, nvy)
            if (hitWall) {
                iter.remove()
                continue
            }
            n.pos = np

            // Proximity blast — larger radius than torpedo
            var detonated = false
            for (npc in npcShips) {
                val nx2 = npc.x.toDouble().pixelToClickInt()
                val ny2 = npc.y.toDouble().pixelToClickInt()
                if (checkCollision(
                        n.pos,
                        ClPos(world.wrapXClick(nx2), world.wrapYClick(ny2)),
                        WeaponConst.NUKE_RANGE + GameConst.SHIP_SZ,
                    )
                ) {
                    if (npc.hp > 0f) {
                        // Nuke deals NUKE_SMART_EXPL_MULT × missile damage
                        npc.hp -= EngineConst.NPC_MISSILE_HP_DAMAGE * WeaponConst.NUKE_SMART_EXPL_MULT
                        if (npc.hp <= 0f) player.score += 1.0
                    }
                    detonated = true
                    break
                }
            }
            if (!detonated) {
                val effectiveShieldForNuke = shieldActive || emergencyShieldActive
                if (effectiveShieldForNuke && checkCollision(n.pos, player.pos, WeaponConst.NUKE_RANGE + GameConst.SHIP_SZ)) {
                    fuel = (fuel + EnergyDrain.SMART_SHOT_HIT * WeaponConst.NUKE_SMART_EXPL_MULT).coerceAtLeast(0.0)
                    if (emergencyShieldActive) emergencyShieldBlockedHit = true
                    detonated = true
                } else if (n.ownerId != player.id && checkCollision(n.pos, player.pos, WeaponConst.NUKE_RANGE + GameConst.SHIP_SZ)) {
                    killPlayer()
                    detonated = true
                }
            }
            if (detonated) {
                // Chain-explosion: spawn NUKE_MINE_EXPL_MULT debris bursts
                repeat(WeaponConst.NUKE_MINE_EXPL_MULT) {
                    val numDebris = (512 * (0.20 + 0.10 * rng.nextDouble())).toInt()
                    repeat(numDebris) {
                        val dir2 = rng.nextDouble(0.0, 2.0 * PI)
                        val spd =
                            EngineConst.MINE_DEBRIS_MIN_SPEED +
                                rng.nextDouble() * (EngineConst.MINE_DEBRIS_MAX_SPEED - EngineConst.MINE_DEBRIS_MIN_SPEED)
                        debris +=
                            DebrisData(
                                pos = n.pos,
                                vel = Vector(cos(dir2).toFloat() * spd.toFloat(), sin(dir2).toFloat() * spd.toFloat()),
                                life =
                                    EngineConst.MINE_DEBRIS_MIN_LIFE + rng.nextFloat() *
                                        (EngineConst.MINE_DEBRIS_MAX_LIFE - EngineConst.MINE_DEBRIS_MIN_LIFE),
                                ownerId = n.ownerId,
                            )
                    }
                }
                iter.remove()
            }
        }
    }

    // -----------------------------------------------------------------------
    // NPC respawn tick
    // -----------------------------------------------------------------------

    /**
     * Decrement all pending NPC respawn timers.  When a timer reaches zero,
     * calls the registered factory to create a fresh [EngineTarget] and adds
     * it back to [npcShips].
     *
     * Called at the *start* of [tick] so respawned NPCs are immediately visible
     * to the rest of the tick's weapon loops (consistent with C server order).
     *
     * Maps to the respawn path of C `Robot_start` in server/robot.c.
     */
    private fun tickNpcRespawn(npcShips: MutableList<EngineTarget>) {
        val respawnedIds = mutableListOf<Int>()
        for ((id, ticks) in npcRespawnTimers) {
            val remaining = ticks - 1
            if (remaining <= 0) {
                val factory = npcFactories[id]
                if (factory != null) {
                    npcShips += factory()
                }
                respawnedIds += id
            } else {
                npcRespawnTimers[id] = remaining
            }
        }
        for (id in respawnedIds) npcRespawnTimers.remove(id)
    }

    // -----------------------------------------------------------------------
    // Cannon tick
    // -----------------------------------------------------------------------

    /**
     * Advance all map cannons by one tick.
     *
     * Each cannon decrements its [fireTimer].  When it reaches zero the cannon
     * checks whether the player is within [CannonConst.DISTANCE] pixels.  If so
     * it fires a shot aimed at the player and resets the timer to
     * [GameConst.SHOT_SPEED_FACTOR].  Dead cannons (deadTicks > 0) are skipped.
     *
     * Maps to C `Cannon_update` / `Cannon_check_fire` in server/cannon.c.
     */
    private fun tickCannons() {
        for (cannon in world.cannons) {
            if (cannon.deadTicks > 0.0) continue
            cannon.fireTimer -= 1.0
            if (cannon.fireTimer > 0.0) continue

            // Check distance to player (cannon.pos is in click units)
            val playerAlive = player.isAlive()
            if (playerAlive && checkCollision(cannon.pos, player.pos, CannonConst.DISTANCE)) {
                // Aim toward player
                val dx = (player.pos.cx - cannon.pos.cx).toPixel().toDouble()
                val dy = (player.pos.cy - cannon.pos.cy).toPixel().toDouble()
                val angle = atan2(dy, dx)

                // BL-11: dispatch on cannon weapon type
                when (cannon.weapon) {
                    CannonWeapon.SHOT -> {
                        val shotVx = (cos(angle) * EngineConst.SHOT_SPEED).toFloat()
                        val shotVy = (sin(angle) * EngineConst.SHOT_SPEED).toFloat()
                        shots +=
                            ShotData(
                                pos =
                                    ClPos(
                                        world.wrapXClick(cannon.pos.cx + shotVx.toDouble().pixelToClickInt()),
                                        world.wrapYClick(cannon.pos.cy + shotVy.toDouble().pixelToClickInt()),
                                    ),
                                vel = Vector(shotVx, shotVy),
                                life = EngineConst.CANNON_PULSE_LIFE,
                                ownerId = -2,
                                freshTick = true,
                            )
                    }

                    CannonWeapon.LASER -> {
                        // BL-11 + BL-01: cannon fires a laser pulse
                        val vx = (cos(angle) * EngineConst.LASER_PULSE_SPEED).toFloat()
                        val vy = (sin(angle) * EngineConst.LASER_PULSE_SPEED).toFloat()
                        laserPulses +=
                            ShotData(
                                pos =
                                    ClPos(
                                        world.wrapXClick(cannon.pos.cx + vx.toDouble().pixelToClickInt()),
                                        world.wrapYClick(cannon.pos.cy + vy.toDouble().pixelToClickInt()),
                                    ),
                                vel = Vector(vx, vy),
                                life = EngineConst.CANNON_PULSE_LIFE,
                                ownerId = -2,
                                freshTick = true,
                            )
                    }

                    CannonWeapon.MISSILE -> {
                        // BL-11: cannon fires a missile toward the player
                        val inheritedVx = 0.0
                        val inheritedVy = 0.0
                        missiles +=
                            MissileData(
                                pos =
                                    ClPos(
                                        world.wrapXClick(cannon.pos.cx),
                                        world.wrapYClick(cannon.pos.cy),
                                    ),
                                headingRad = angle,
                                life = EngineConst.MISSILE_LIFE,
                                targetNpcId = player.id.toInt(),
                                ownerId = -2,
                            )
                    }

                    else -> {
                        // MINE, ECM, TRACTORBEAM, TRANSPORTER, GASJET — stub: fire plain shot
                        val shotVx = (cos(angle) * EngineConst.SHOT_SPEED).toFloat()
                        val shotVy = (sin(angle) * EngineConst.SHOT_SPEED).toFloat()
                        shots +=
                            ShotData(
                                pos =
                                    ClPos(
                                        world.wrapXClick(cannon.pos.cx + shotVx.toDouble().pixelToClickInt()),
                                        world.wrapYClick(cannon.pos.cy + shotVy.toDouble().pixelToClickInt()),
                                    ),
                                vel = Vector(shotVx, shotVy),
                                life = EngineConst.CANNON_PULSE_LIFE,
                                ownerId = -2,
                                freshTick = true,
                            )
                    }
                }
            }
            cannon.fireTimer = GameConst.SHOT_SPEED_FACTOR
        }
    }

    // -----------------------------------------------------------------------
    // Fuel station tick
    // -----------------------------------------------------------------------

    /**
     * Refuel the player from nearby fuel stations each tick.
     *
     * If the player is within one block ([GameConst.BLOCK_SZ] px) of a fuel
     * station and the station has fuel remaining, withdraw up to
     * [GameConst.REFUEL_RATE] units per tick and add them to the player's fuel
     * up to [fuelMax].
     *
     * Maps to C `Fuel_update_player` in server/fuel.c.
     */
    private fun tickFuelStations() {
        if (!player.isAlive()) return
        for (station in world.fuels) {
            if (checkCollision(station.pos, player.pos, GameConst.BLOCK_SZ.toDouble())) {
                val amount = station.withdraw(GameConst.REFUEL_RATE)
                fuel = (fuel + amount).coerceAtMost(fuelMax)
            }
        }
    }

    // -----------------------------------------------------------------------
    // Target tick
    // -----------------------------------------------------------------------

    /**
     * Advance all map targets by one tick.
     *
     * Each alive target:
     * - slowly repairs by [WeaponConst.TARGET_REPAIR_PER_FRAME] per tick (C: passive background
     *   repair, serverconst.h TARGET_REPAIR_PER_FRAME = TARGET_DAMAGE / (12 * 600)).
     * - also repairs faster by [WeaponConst.TARGET_FUEL_REPAIR_PER_FRAME] when the player
     *   actively uses the repair tool ([playerActivatesRepair]) — NOT applied here.
     *
     * Shot→target collision is handled inside the shot tick loop (step 9) where
     * shots already check map tiles; this function only handles the passive repair.
     *
     * Maps to the repair path of C `Target_update` in server/target.c.
     */
    private fun tickTargets() {
        for (i in world.targets.indices) {
            val t = world.targets[i]
            if (t.isAlive && t.damage > 0.0) {
                world.targets[i] =
                    t.copy(
                        // Passive background repair only (no fuel station required).
                        // C: TARGET_REPAIR_PER_FRAME = TARGET_DAMAGE / (12 * 600) per C-tick.
                        // The fast fuel-driven rate (TARGET_FUEL_REPAIR_PER_FRAME) is only applied
                        // when the player activates the repair tool — see playerActivatesRepair().
                        damage = (t.damage - WeaponConst.TARGET_REPAIR_PER_FRAME).coerceAtLeast(0.0),
                    )
            }
        }
    }

    /**
     * Player-activated repair action for a specific target.
     *
     * Call once per tick while the player holds KEY_REPAIR and a target is in range.
     * Drains [WeaponConst.REFUEL_RATE] fuel; if insufficient fuel, does nothing.
     *
     * C: `Do_repair` in server/update.c (lines 770–800). The fast repair rate
     * [WeaponConst.TARGET_FUEL_REPAIR_PER_FRAME] is consumed here, not in passive tick.
     *
     * @param targetIndex  Index into [world.targets] of the target being repaired.
     */
    fun playerActivatesRepair(targetIndex: Int) {
        if (targetIndex !in world.targets.indices) return
        val t = world.targets[targetIndex]
        if (!t.isAlive || t.damage <= 0.0) return
        if (fuel < WeaponConst.REFUEL_RATE) return
        fuel -= WeaponConst.REFUEL_RATE
        world.targets[targetIndex] =
            t.copy(damage = (t.damage - WeaponConst.TARGET_FUEL_REPAIR_PER_FRAME).coerceAtLeast(0.0))
    }

    // -----------------------------------------------------------------------
    // Energy packs
    // -----------------------------------------------------------------------

    /**
     * Tick all energy packs: decay lifetime and check for player pickup.
     *
     * On overlap (centre-to-centre ≤ [WeaponConst.ENERGY_PACK_PICKUP_RADIUS]), the pack
     * is consumed and a random amount of fuel in
     * [[WeaponConst.ENERGY_PACK_FUEL_MIN]..[WeaponConst.ENERGY_PACK_FUEL_MAX]] is
     * added to [fuel], clamped to [fuelMax].
     *
     * C: `do_get_item(ITEM_FUEL)` → `Player_add_fuel(pl, ENERGY_PACK_FUEL)` (item.c).
     */
    private fun tickEnergyPacks() {
        val px = playerPixelX.toDouble().pixelToClickInt()
        val py = playerPixelY.toDouble().pixelToClickInt()
        val playerClick = ClPos(world.wrapXClick(px), world.wrapYClick(py))
        val iter = energyPacks.iterator()
        while (iter.hasNext()) {
            val pack = iter.next()
            pack.life -= 1f
            if (pack.life <= 0f) {
                iter.remove()
                continue
            }
            if (checkCollision(pack.pos, playerClick, WeaponConst.ENERGY_PACK_PICKUP_RADIUS)) {
                val amount =
                    WeaponConst.ENERGY_PACK_FUEL_MIN +
                        rng.nextDouble() * (WeaponConst.ENERGY_PACK_FUEL_MAX - WeaponConst.ENERGY_PACK_FUEL_MIN)
                fuel = (fuel + amount).coerceAtMost(fuelMax)
                iter.remove()
            }
        }
    }

    /**
     * Place a new energy pack at [pos].
     *
     * C equivalent: `Place_item(null, ITEM_FUEL)` which spawns an `OBJ_ITEM` at a random
     * map location.  In single-player KXPilot the map/demo layer decides where to place packs;
     * this method provides the engine-side spawn point.
     */
    fun spawnEnergyPack(pos: ClPos) {
        val life =
            WeaponConst.ENERGY_PACK_LIFE_MIN +
                (rng.nextFloat() * (WeaponConst.ENERGY_PACK_LIFE_MAX - WeaponConst.ENERGY_PACK_LIFE_MIN))
        energyPacks.add(EnergyPackData(pos, life))
    }

    // -----------------------------------------------------------------------
    // Thrust sparks
    // -----------------------------------------------------------------------

    /**
     * Spawn exhaust sparks for one thrust tick.
     *
     * C: `Ship_thrust` in server/ship.c.  Two [Make_debris] batches:
     * 1. Normal sparks: count = tot_sparks − alt_sparks; mass = [WeaponConst.THRUST_MASS].
     * 2. Afterburner sparks (if [afterburnerActive]): count = alt_sparks;
     *    mass = [WeaponConst.THRUST_MASS] × [WeaponConst.ALT_SPARK_MASS_FACT].
     *
     * Sparks launch from the engine position (behind the ship) in a cone centred on
     * the reverse heading (heading + π), spread by ±[WeaponConst.SPARK_SPREAD_RAD].
     * Their velocity is ship velocity + a random component in
     * [[WeaponConst.SPARK_MIN_SPEED], [WeaponConst.SPARK_MAX_SPEED]].
     */
    private fun spawnThrustSparks(afterburnerActive: Boolean) {
        val baseCount = WeaponConst.SPARK_COUNT_PER_TICK
        val altFrac =
            if (afterburnerActive) {
                playerItems.afterburner.toDouble() / (GameConst.MAX_AFTERBURNER + 1.0)
            } else {
                0.0
            }
        val altCount = baseCount * altFrac
        val normalCount = baseCount - altCount

        // Reverse heading (exhaust shoots backward)
        val exhaustDir = wrapAngle(player.floatDir + PI)
        // C: ±(RES*0.2+1)*thrustWidth heading-units → SPARK_SPREAD_RAD (ship.c:57–58).
        val spread = WeaponConst.SPARK_SPREAD_RAD
        val px = playerPixelX.toDouble().pixelToClickInt()
        val py = playerPixelY.toDouble().pixelToClickInt()
        val origin = ClPos(world.wrapXClick(px), world.wrapYClick(py))
        val shipVx = player.vel.x
        val shipVy = player.vel.y

        fun spawnBatch(
            count: Double,
            mass: Double,
            isAlt: Boolean,
        ) {
            // Stochastic rounding: floor(count + rfrac()) so expectation = count
            val n = (count + rng.nextDouble()).toInt()
            repeat(n) {
                val angle = exhaustDir + (rng.nextDouble() * 2.0 - 1.0) * spread
                val speed = WeaponConst.SPARK_MIN_SPEED + rng.nextDouble() * (WeaponConst.SPARK_MAX_SPEED - WeaponConst.SPARK_MIN_SPEED)
                val vx = (shipVx + cos(angle) * speed).toFloat()
                val vy = (shipVy + sin(angle) * speed).toFloat()
                val life = (rng.nextDouble() * WeaponConst.SPARK_MAX_LIFE).toFloat().coerceAtLeast(1f)
                sparks.add(SparkData(origin, Vector(vx, vy), life, mass, isAlt))
            }
        }

        spawnBatch(normalCount, WeaponConst.THRUST_MASS, false)
        if (altCount > 0.0) {
            spawnBatch(altCount, WeaponConst.THRUST_MASS * WeaponConst.ALT_SPARK_MASS_FACT, true)
        }
    }

    /**
     * Advance all sparks by their velocity (no wall collision — C sparks have GRAVITY
     * bit but OBJ_SPARK is not blocked by walls in the client), decay life, remove expired.
     *
     * Note: C spark positions are sent to the client; the client renders them.
     * KXPilot keeps them in the engine for the renderer.
     */
    private fun tickSparks() {
        val iter = sparks.iterator()
        while (iter.hasNext()) {
            val s = iter.next()
            s.life -= 1f
            if (s.life <= 0f) {
                iter.remove()
                continue
            }
            val nx = (s.pos.cx.toDouble() + s.vel.x * ClickConst.PIXEL_CLICKS).toInt()
            val ny = (s.pos.cy.toDouble() + s.vel.y * ClickConst.PIXEL_CLICKS).toInt()
            s.pos = ClPos(world.wrapXClick(nx), world.wrapYClick(ny))
        }
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
        // #D Toroidal shortest-path correction (matches C server Wrap() macro)
        var dx = target.x.toDouble() - playerPixelX.toDouble()
        var dy = target.y.toDouble() - playerPixelY.toDouble()
        val halfW = world.width / 2.0
        val halfH = world.height / 2.0
        if (dx > halfW) dx -= world.width
        if (dx < -halfW) dx += world.width
        if (dy > halfH) dy -= world.height
        if (dy < -halfH) dy += world.height
        val dist = hypot(dx, dy)
        val tractorItems = playerItems.tractorBeam
        val maxRange = EngineConst.TRACTOR_RANGE_BASE + tractorItems * EngineConst.TRACTOR_RANGE_PER_ITEM
        if (dist < 1.0 || dist > maxRange) return
        val maxForceUnits = EngineConst.TRACTOR_FORCE_BASE + tractorItems * EngineConst.TRACTOR_FORCE_PER_ITEM
        val maxForcePxTick2 = (maxForceUnits / GameConst.PLAYER_MASS / EngineConst.HZ_RATIO).toFloat()
        val percent = (1.0 - EngineConst.TRACTOR_FALLOFF * (dist / maxRange))
        val scale = (maxForcePxTick2 * percent).toFloat()
        val nx = (dx / dist * scale).toFloat()
        val ny = (dy / dist * scale).toFloat()
        if (pressor) {
            target.vx += nx
            target.vy += ny
        } else {
            target.vx -= nx
            target.vy -= ny
        }
        // Newton's 3rd: operator receives equal-and-opposite reaction.
        // C: General_tractor_beam applies force/pl->mass back to the beam user
        // in the direction toward the target (tractor) or away from it (pressor).
        // tractor: player is pulled toward target (+nx, +ny toward target direction)
        // pressor: player is pushed away from target (-nx, -ny)
        if (pressor) {
            player.vel = Vector(player.vel.x - nx, player.vel.y - ny)
        } else {
            player.vel = Vector(player.vel.x + nx, player.vel.y + ny)
        }
        // C: TRACTOR_COST(percent) = -1.5 * percent per C-tick.
        // Scaled to 60 Hz: 1.5 * percent * HZ_RATIO.
        val fuelCost = 1.5 * percent * EngineConst.HZ_RATIO
        fuel = (fuel - fuelCost).coerceAtLeast(0.0)
    }

    // -----------------------------------------------------------------------
    // BL-01: Laser pulse fire + tick
    // -----------------------------------------------------------------------

    /**
     * Fire one laser pulse in the player's heading direction.
     * C: Fire_general_laser → spawns OBJ_PULSE.  Fuel cost: ED_LASER per fire event.
     */
    private fun fireLaserPulse() {
        fuel = (fuel + EnergyDrain.LASER * EngineConst.HZ_RATIO).coerceAtLeast(0.0)
        val speed = EngineConst.LASER_PULSE_SPEED
        val vx = (player.vel.x + player.floatDirCos * speed).toFloat()
        val vy = (player.vel.y + player.floatDirSin * speed).toFloat()
        val cx = world.wrapXClick(player.pos.cx + vx.toDouble().pixelToClickInt())
        val cy = world.wrapYClick(player.pos.cy + vy.toDouble().pixelToClickInt())
        laserPulses +=
            ShotData(
                pos = ClPos(cx, cy),
                vel = Vector(vx, vy),
                life = EngineConst.LASER_PULSE_LIFE,
                ownerId = player.id,
                freshTick = true,
            )
    }

    /**
     * Advance all laser pulses one tick.
     * Laser pulses bounce off walls (BL-03 bounce path) or are removed.
     * On player/NPC hit: [EnergyDrain.LASER_HIT] fuel damage (effectively kills).
     */
    private fun tickLaserPulses(npcShips: MutableList<EngineTarget>) {
        val iter = laserPulses.iterator()
        while (iter.hasNext()) {
            val pulse = iter.next()
            pulse.life -= 1f
            if (pulse.life <= 0f) {
                iter.remove()
                continue
            }

            // Move with bounce (BL-03 shared path)
            val (newPos, bounced, newVx, newVy) =
                sweepMoveShotBounce(
                    pulse.pos,
                    pulse.vel.x.toDouble(),
                    pulse.vel.y.toDouble(),
                )
            if (bounced) {
                pulse.life *= EngineConst.BOUNCE_LIFE_FACTOR
                pulse.vel =
                    Vector(
                        (newVx * EngineConst.BOUNCE_BRAKE_FACTOR).toFloat(),
                        (newVy * EngineConst.BOUNCE_BRAKE_FACTOR).toFloat(),
                    )
                pulse.pos = newPos
            } else {
                pulse.pos = newPos
            }
            if (pulse.life <= 0f) {
                iter.remove()
                continue
            }

            // Skip fresh tick (avoid self-hit)
            if (pulse.freshTick) {
                pulse.freshTick = false
                continue
            }

            // Hit NPC ships (ownerId is player — so pulse hits enemies)
            var hit = false
            for (npc in npcShips) {
                if (npc.hp <= 0f) continue
                val npx = npc.x.toDouble().pixelToClickInt()
                val npy = npc.y.toDouble().pixelToClickInt()
                val npcClick = ClPos(world.wrapXClick(npx), world.wrapYClick(npy))
                if (checkCollision(pulse.pos, npcClick, GameConst.SHIP_SZ.toDouble())) {
                    npc.hp = 0f
                    player.score += 1.0
                    hit = true
                    break
                }
            }
            if (hit) {
                iter.remove()
                continue
            }

            // Hit player (enemy laser pulses, ownerId != player.id)
            if (pulse.ownerId != player.id && player.isAlive() && !shieldActive) {
                if (checkCollision(pulse.pos, player.pos, GameConst.SHIP_SZ.toDouble())) {
                    fuel = (fuel + EnergyDrain.LASER_HIT).coerceAtLeast(0.0)
                    if (fuel <= 0.0) killPlayer()
                    iter.remove()
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // BL-03: Bounce-aware sweep move for shots
    // -----------------------------------------------------------------------

    /**
     * Result of [sweepMoveShotBounce]: new position, whether a bounce occurred,
     * and the new velocity components after reflection.
     */
    private data class SweepBounceResult(
        val pos: ClPos,
        val bounced: Boolean,
        val velX: Double,
        val velY: Double,
    )

    /**
     * Sweep-move a shot/pulse by ([velX], [velY]) pixels, reflecting off axis-aligned
     * FILLED walls instead of stopping.  Returns the final position and (possibly
     * reflected) velocity.
     *
     * C: Bounce_object in walls.c — reflect velocity on the penetrated wall axis.
     * Life reduction is applied by the caller ([BOUNCE_LIFE_FACTOR]).
     */
    private fun sweepMoveShotBounce(
        pos: ClPos,
        velX: Double,
        velY: Double,
    ): SweepBounceResult {
        if (world.x == 0 || world.y == 0) {
            return SweepBounceResult(
                ClPos(
                    world.wrapXClick(pos.cx + velX.pixelToClickInt()),
                    world.wrapYClick(pos.cy + velY.pixelToClickInt()),
                ),
                false,
                velX,
                velY,
            )
        }
        val totalDist = hypot(velX, velY)
        if (totalDist == 0.0) return SweepBounceResult(pos, false, velX, velY)

        val steps = max(1, ceil(totalDist / EngineConst.SWEEP_STEP).toInt())
        val dx = velX / steps
        val dy = velY / steps
        val clicksPerPx = ClickConst.CLICK.toDouble()

        var cx = pos.cx.toDouble()
        var cy = pos.cy.toDouble()
        var rvx = velX
        var rvy = velY
        var bounced = false

        for (step in 0 until steps) {
            val nextCx = cx + (rvx / steps) * clicksPerPx
            val nextCy = cy + (rvy / steps) * clicksPerPx
            val bx = toroidalBlock((nextCx / clicksPerPx).toFloat(), GameConst.BLOCK_SZ.toFloat(), world.x)
            val by = toroidalBlock((nextCy / clicksPerPx).toFloat(), GameConst.BLOCK_SZ.toFloat(), world.y)
            val cell = world.getBlock(bx, by)
            if (cell == CellType.FILLED) {
                // Determine axis of reflection by minimum penetration
                val blockLeft = bx * GameConst.BLOCK_SZ.toDouble() * clicksPerPx
                val blockRight = blockLeft + GameConst.BLOCK_SZ.toDouble() * clicksPerPx
                val blockBottom = by * GameConst.BLOCK_SZ.toDouble() * clicksPerPx
                val blockTop = blockBottom + GameConst.BLOCK_SZ.toDouble() * clicksPerPx
                val penX = if (rvx > 0) nextCx - blockLeft else blockRight - nextCx
                val penY = if (rvy > 0) nextCy - blockBottom else blockTop - nextCy
                if (penX < penY || (penX == penY && abs(rvx) >= abs(rvy))) {
                    rvx = -rvx
                } else {
                    rvy = -rvy
                }
                bounced = true
                break
            }
            cx = nextCx
            cy = nextCy
        }
        return SweepBounceResult(
            ClPos(world.wrapXClick(cx.toInt()), world.wrapYClick(cy.toInt())),
            bounced,
            rvx,
            rvy,
        )
    }

    // -----------------------------------------------------------------------
    // BL-04: Shot spread helper
    // -----------------------------------------------------------------------

    /**
     * Spawn shots with spread fan.
     * If [PlayerItems.wideangle] > 0, fires three shots (centre + two side shots
     * at ± heading gap determined by [spreadLevel]).  Otherwise fires one shot.
     *
     * C: Fire_normal_shots in event.c — fan at MODS_SPREAD_MAX - spreadLevel heading-unit gap.
     */
    private fun spawnSpreadShots() {
        if (playerItems.wideangle <= 0) {
            spawnShot()
            return
        }
        // Angle gap in radians: heading units are RES per full circle
        val gapUnits = (GameConst.MODS_SPREAD_MAX - spreadLevel).coerceAtLeast(1)
        val gapRad = gapUnits.toDouble() / GameConst.RES * 2.0 * PI
        val baseAngle = player.floatDir
        for (offset in listOf(-gapRad, 0.0, gapRad)) {
            val angle = baseAngle + offset
            val vx = (player.vel.x + cos(angle) * EngineConst.SHOT_SPEED).toFloat()
            val vy = (player.vel.y + sin(angle) * EngineConst.SHOT_SPEED).toFloat()
            val cx = world.wrapXClick(player.pos.cx + vx.toDouble().pixelToClickInt())
            val cy = world.wrapYClick(player.pos.cy + vy.toDouble().pixelToClickInt())
            shots +=
                ShotData(
                    pos = ClPos(cx, cy),
                    vel = Vector(vx, vy),
                    life = EngineConst.SHOT_LIFE,
                    ownerId = player.id,
                    freshTick = true,
                )
        }
    }

    // -----------------------------------------------------------------------
    // BL-08: Self-destruct (state in selfDestructTicks; countdown in tick)
    // -----------------------------------------------------------------------
    // (No separate helper needed — countdown and kill are inlined in tick)

    // -----------------------------------------------------------------------
    // BL-09: Hyperjump
    // -----------------------------------------------------------------------

    /**
     * Teleport the player to a random safe (SPACE) position.
     * Up to 8 attempts; if none found, the hyperjump fails silently.
     * C: Do_hyperjump in update.c.
     */
    private fun doHyperjump() {
        if (world.x == 0 || world.y == 0) return
        repeat(8) {
            val bx = rng.nextInt(world.x / GameConst.BLOCK_SZ)
            val by = rng.nextInt(world.y / GameConst.BLOCK_SZ)
            if (world.getBlock(bx, by) == CellType.SPACE) {
                // Place player at centre of chosen block
                val cx = bx * ClickConst.BLOCK_CLICKS + ClickConst.BLOCK_CLICKS / 2
                val cy = by * ClickConst.BLOCK_CLICKS + ClickConst.BLOCK_CLICKS / 2
                player.pos = ClPos(cx, cy)
                player.vel = Vector(0f, 0f)
                return
            }
        }
        // All attempts failed — silently refund charge
        playerItems = playerItems.copy(hyperjump = playerItems.hyperjump + 1)
    }

    // -----------------------------------------------------------------------
    // BL-10: Detonate all owned mines
    // -----------------------------------------------------------------------

    /**
     * Immediately detonate all proximity mines owned by the player.
     * C: KEY_DETONATE_MINES → iterates pl->mines, calls Detonate_mine.
     * Reuses the same debris spawning as the proximity trigger path.
     */
    private fun detonateAllMines() {
        val iter = mines.iterator()
        while (iter.hasNext()) {
            val mine = iter.next()
            if (mine.ownerId != player.id) continue
            // Same debris spawn as proximity trigger
            val intensity = 512
            val numDebris = (intensity * (0.20 + 0.10 * rng.nextDouble())).toInt()
            repeat(numDebris) {
                val dir = rng.nextDouble(0.0, 2.0 * PI)
                val speed =
                    EngineConst.MINE_DEBRIS_MIN_SPEED +
                        rng.nextDouble() *
                        (EngineConst.MINE_DEBRIS_MAX_SPEED - EngineConst.MINE_DEBRIS_MIN_SPEED)
                debris +=
                    DebrisData(
                        pos = mine.pos,
                        vel = Vector((cos(dir) * speed).toFloat(), (sin(dir) * speed).toFloat()),
                        life =
                            EngineConst.MINE_DEBRIS_MIN_LIFE +
                                rng.nextFloat() *
                                (EngineConst.MINE_DEBRIS_MAX_LIFE - EngineConst.MINE_DEBRIS_MIN_LIFE),
                        ownerId = mine.ownerId,
                    )
            }
            iter.remove()
        }
    }

    // -----------------------------------------------------------------------
    // BL-02: Wormhole teleportation
    // -----------------------------------------------------------------------

    /**
     * Check if the player overlaps a wormhole and teleport if so.
     * C: Do_wormholes in wormhole.c.
     *
     * - WORM_OUT type: exit-only, cannot be entered.
     * - WORM_FIXED type: always uses the same destination.
     * - Stable countdown: if countdown > 0 reuse lastDest, else pick new random dest.
     */
    private fun tickWormholes() {
        if (!player.isAlive()) return
        val wormholes = world.wormholes
        if (wormholes.isEmpty()) return
        for (wh in wormholes) {
            if (wh.type == WormType.OUT) continue // exit-only
            if (!checkCollision(wh.pos, player.pos, EngineConst.WORMHOLE_RADIUS_PX)) continue

            // Decrement all wormhole countdowns each tick (C: all wormholes tick)
            // (done outside the match so we only decrement once per tick per wormhole)
            // Destination selection
            val destIndex: Int =
                when {
                    wh.type == WormType.FIXED -> {
                        wh.lastDest
                    }

                    wh.countdown > 0 -> {
                        wh.lastDest
                    }

                    else -> {
                        // Pick random non-IN, non-FIXED wormhole
                        val candidates =
                            wormholes.indices.filter { i ->
                                val w = wormholes[i]
                                w !== wh && w.type != WormType.IN && w.type != WormType.FIXED
                            }
                        if (candidates.isEmpty()) -1 else candidates[rng.nextInt(candidates.size)]
                    }
                }
            if (destIndex < 0 || destIndex >= wormholes.size) continue
            val dest = wormholes[destIndex]
            wh.lastDest = destIndex
            wh.countdown = EngineConst.WORMHOLE_STABLE_TICKS.toDouble()

            // Teleport player
            player.pos = dest.pos
            break
        }
        // Tick all wormhole countdowns
        for (wh in wormholes) {
            if (wh.countdown > 0) wh.countdown--
        }
    }

    // -----------------------------------------------------------------------
    // BL-05: Checkpoint detection
    // -----------------------------------------------------------------------

    /**
     * Check if the player has passed the next checkpoint.
     * C: Race_player_pass_checkpoint in race.c.
     */
    private fun tickCheckpoints() {
        if (!player.isAlive()) return
        val checks = world.checks
        if (checks.isEmpty()) return
        val idx = checkIndex % checks.size
        val cp = checks[idx]
        if (checkCollision(cp.pos, player.pos, EngineConst.CHECKPOINT_RADIUS_PX)) {
            checkIndex++
            if (checkIndex >= checks.size) {
                checkIndex = 0
                laps++
            }
        }
    }

    // -----------------------------------------------------------------------
    // BL-06: Per-tile friction areas
    // -----------------------------------------------------------------------

    /**
     * Apply per-tile friction from [FrictionArea] blocks to the player's velocity.
     * C: applied in Ship_update after global friction step.
     *
     * Each [FrictionArea] stores `friction` = precomputed per-tick decay factor.
     * We multiply velocity by `(1 - friction)` if the player's block matches.
     */
    private fun tickFrictionAreas() {
        if (!player.isAlive()) return
        if (world.frictionAreas.isEmpty()) return
        val px = player.pos.cx.toPixel()
        val py = player.pos.cy.toPixel()
        for (fa in world.frictionAreas) {
            val faBx = fa.pos.cx.toBlock()
            val faBy = fa.pos.cy.toBlock()
            val playerBx = (px / GameConst.BLOCK_SZ)
            val playerBy = (py / GameConst.BLOCK_SZ)
            if (faBx == playerBx && faBy == playerBy) {
                val decay = (1.0 - fa.friction).toFloat()
                player.vel = Vector(player.vel.x * decay, player.vel.y * decay)
                break // one friction area per block is sufficient
            }
        }
    }

    // -----------------------------------------------------------------------
    // BL-12: Deflector — push approaching shots and NPCs outward
    // -----------------------------------------------------------------------

    /**
     * Probabilistic visibility check used when the player is cloaked (BL-13).
     * C: `rfrac()*(sensor+1) > rfrac()*(cloak+1)` in server/update.c.
     * NPCs have no sensor items in current impl (sensorCount = 0).
     * Returns true if the NPC can see the player despite cloaking.
     */
    private fun canSeePlayerWhenCloaked(): Boolean {
        val sensorCount = 0 // NPCs have no sensor items in current implementation
        val cloakCount = playerItems.cloak
        return rng.nextDouble() * (sensorCount + 1) > rng.nextDouble() * (cloakCount + 1)
    }

    /**
     * For each shot/laser/NPC within range that is approaching the player,
     * apply a repulsive velocity impulse proportional to proximity and angular
     * alignment.
     *
     * C: `Do_deflector` in server/item.c.
     *
     * Range (clicks): items * DEFLECTOR_RANGE_PER_ITEM + DEFLECTOR_RANGE_BASE.
     * Max force:      items * DEFLECTOR_FORCE_PER_ITEM.
     * Force formula:  ((range-dist)/range)² * maxForce * angleFactor / shotMass.
     */
    private fun tickDeflector(
        shots: MutableList<ShotData>,
        laserPulses: MutableList<ShotData>,
        npcShips: List<EngineTarget>,
    ) {
        if (!deflectorActive) return
        val items = playerItems.deflector
        val range = items * EngineConst.DEFLECTOR_RANGE_PER_ITEM + EngineConst.DEFLECTOR_RANGE_BASE
        val maxForce = items * EngineConst.DEFLECTOR_FORCE_PER_ITEM
        val px =
            player.pos.cx
                .toPixel()
                .toDouble()
        val py =
            player.pos.cy
                .toPixel()
                .toDouble()
        val halfW = world.width / 2.0
        val halfH = world.height / 2.0

        fun deflectShot(shot: ShotData) {
            var dx =
                shot.pos.cx
                    .toPixel()
                    .toDouble() - px
            var dy =
                shot.pos.cy
                    .toPixel()
                    .toDouble() - py
            if (dx > halfW) dx -= world.width
            if (dx < -halfW) dx += world.width
            if (dy > halfH) dy -= world.height
            if (dy < -halfH) dy += world.height
            // Convert pixel distance to clicks; subtract ship radius
            val distClicks = hypot(dx, dy) * ClickConst.CLICK - GameConst.SHIP_SZ * ClickConst.CLICK
            if (distClicks <= 0 || distClicks >= range) return
            val dirToShot = atan2(dy, dx)
            val shotVelDir = atan2(shot.vel.y.toDouble(), shot.vel.x.toDouble())
            // idir: angle between dirToShot and shot velocity direction.
            // idir ≈ 0 means shot moves AWAY from player; idir ≈ π means shot moves TOWARD player.
            // C: idir in (π/2, 3π/2) → shot has component toward player → deflect.
            var idir = shotVelDir - dirToShot
            while (idir < 0) idir += 2 * PI
            while (idir >= 2 * PI) idir -= 2 * PI
            // Only deflect if shot is approaching: idir in (π/2, 3π/2)
            if (idir <= PI / 2 || idir >= 3 * PI / 2) return
            val rangeFactor = (range - distClicks) / range
            val angleFactor = (PI / 4 - abs(idir - PI)) / (PI / 4)
            val force = rangeFactor * rangeFactor * maxForce * angleFactor
            // C uses obj->mass ≈ 0.1 for shots; dv = force / mass
            val shotMass = WeaponConst.CLUSTER_SHOT_MASS // 0.1
            val dv = force / shotMass
            val dist2d = hypot(dx, dy)
            if (dist2d < 0.001) return
            // Push radially outward (away from player)
            shot.vel =
                Vector(
                    (shot.vel.x + (dx / dist2d * dv).toFloat()),
                    (shot.vel.y + (dy / dist2d * dv).toFloat()),
                )
        }

        shots.forEach { deflectShot(it) }
        laserPulses.forEach { deflectShot(it) }

        // NPC ships: apply same formula (ships have higher effective mass, skip dv scaling)
        for (npc in npcShips) {
            var dx = npc.x.toDouble() - px
            var dy = npc.y.toDouble() - py
            if (dx > halfW) dx -= world.width
            if (dx < -halfW) dx += world.width
            if (dy > halfH) dy -= world.height
            if (dy < -halfH) dy += world.height
            val distClicks = hypot(dx, dy) * ClickConst.CLICK - GameConst.SHIP_SZ * ClickConst.CLICK
            if (distClicks <= 0 || distClicks >= range) continue
            val dirToNpc = atan2(dy, dx)
            val npcVelDir = atan2(npc.vy.toDouble(), npc.vx.toDouble())
            var idir = npcVelDir - dirToNpc
            while (idir < 0) idir += 2 * PI
            while (idir >= 2 * PI) idir -= 2 * PI
            if (idir <= PI / 2 || idir >= 3 * PI / 2) continue
            val rangeFactor = (range - distClicks) / range
            val angleFactor = (PI / 4 - abs(idir - PI)) / (PI / 4)
            val force = rangeFactor * rangeFactor * maxForce * angleFactor
            val dist2d = hypot(dx, dy)
            if (dist2d < 0.001) continue
            npc.vx += (dx / dist2d * force).toFloat()
            npc.vy += (dy / dist2d * force).toFloat()
        }
    }

    // -----------------------------------------------------------------------
    // BL-07: World item spawning, expiry, and pickup
    // -----------------------------------------------------------------------

    /**
     * Advance world items: decrement life, remove expired, spawn new items from
     * [World.items] config, and detect player pickup.
     *
     * C: `Place_item` (item.c) + player pickup collision in `Do_items` (update.c).
     * Spawn rate: once per [EngineConst.ITEM_SPAWN_INTERVAL] ticks.
     * Pickup radius: [EngineConst.WORLD_ITEM_PICKUP_RADIUS] px.
     */
    private fun tickWorldItems() {
        // --- Expire and pickup ---
        val iter = worldItems.iterator()
        while (iter.hasNext()) {
            val wi = iter.next()
            wi.life -= 1f
            if (wi.life <= 0f) {
                iter.remove()
                continue
            }

            // Pickup: player overlaps item
            if (player.isAlive() &&
                checkCollision(wi.pos, player.pos, EngineConst.WORLD_ITEM_PICKUP_RADIUS)
            ) {
                applyItemPickup(wi.itemType)
                iter.remove()
            }
        }

        // --- Spawn ---
        itemSpawnTimer -= 1.0
        if (itemSpawnTimer > 0.0) return
        itemSpawnTimer = EngineConst.ITEM_SPAWN_INTERVAL

        if (world.x == 0 || world.y == 0) return

        for (i in world.items.indices) {
            val cfg = world.items[i] ?: continue
            val currentCount = worldItems.count { it.itemType.id == i }
            if (currentCount >= cfg.max) continue
            if (cfg.chance <= 0) continue
            // C: rfrac() * chance < 1.0 → probability 1/chance per check
            if (rng.nextDouble() * cfg.chance >= 1.0) continue

            // Place at random SPACE block
            repeat(8) attempt@{
                val bx = rng.nextInt(world.x)
                val by = rng.nextInt(world.y)
                if (world.getBlock(bx, by) != CellType.SPACE) return@attempt
                val cx = bx * ClickConst.BLOCK_CLICKS + ClickConst.BLOCK_CLICKS / 2
                val cy = by * ClickConst.BLOCK_CLICKS + ClickConst.BLOCK_CLICKS / 2
                val life = EngineConst.WORLD_ITEM_MIN_LIFE + rng.nextFloat() * EngineConst.WORLD_ITEM_EXTRA_LIFE
                worldItems.add(
                    WorldItem(
                        pos = ClPos(cx, cy),
                        itemType = Item.fromId(i),
                        life = life,
                    ),
                )
                return@attempt
            }
        }
    }

    /**
     * Apply the effect of picking up one [item] to [playerItems].
     * C: `Do_item` in server/item.c.
     * Internal visibility for test helpers.
     */
    internal fun applyItemPickup(item: Item) {
        playerItems =
            when (item) {
                Item.FUEL -> {
                    fuel = (fuel + WeaponConst.TANK_CAP_AUX).coerceAtMost(fuelMax)
                    playerItems
                }

                Item.WIDEANGLE -> {
                    playerItems.copy(wideangle = playerItems.wideangle + 1)
                }

                Item.AFTERBURNER -> {
                    playerItems.copy(afterburner = (playerItems.afterburner + 1).coerceAtMost(GameConst.MAX_AFTERBURNER))
                }

                Item.SENSOR -> {
                    playerItems.copy(sensor = playerItems.sensor + 1)
                }

                Item.TRANSPORTER -> {
                    playerItems.copy(transporter = playerItems.transporter + 1)
                }

                Item.TANK -> {
                    playerItems.copy(tanks = playerItems.tanks + 1)
                }

                Item.ECM -> {
                    playerItems.copy(ecm = playerItems.ecm + 1)
                }

                Item.LASER -> {
                    playerItems.copy(laser = playerItems.laser + 1)
                }

                Item.TRACTOR_BEAM -> {
                    playerItems.copy(tractorBeam = playerItems.tractorBeam + 1)
                }

                Item.ARMOR -> {
                    playerItems.copy(armor = playerItems.armor + 1)
                }

                Item.HYPERJUMP -> {
                    playerItems.copy(hyperjump = playerItems.hyperjump + 1)
                }

                Item.DEFLECTOR -> {
                    playerItems.copy(deflector = playerItems.deflector + 1)
                }

                Item.CLOAK -> {
                    playerItems.copy(cloak = playerItems.cloak + 1)
                }

                Item.PHASING -> {
                    playerItems.copy(phasing = playerItems.phasing + 1)
                }

                Item.EMERGENCY_SHIELD -> {
                    val newCount = playerItems.emergencyShield + 1
                    val newItems = playerItems.copy(emergencyShield = newCount)
                    // Auto-activate on first pickup if no shield is currently active
                    if (newCount == 1 && !shieldActive && !emergencyShieldActive) {
                        // Activate immediately: sets timer, decrements count back to 0
                        emergencyShieldActive = true
                        emergencyShieldTicksLeft = EngineConst.EMERGENCY_SHIELD_TIME_TICKS
                        newItems.copy(emergencyShield = 0)
                    } else {
                        newItems
                    }
                }

                Item.EMERGENCY_THRUST -> {
                    playerItems.copy(emergencyThrust = playerItems.emergencyThrust + 1)
                }

                else -> {
                    playerItems
                } // unhandled items: no effect for now
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
