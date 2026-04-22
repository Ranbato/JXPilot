package org.lambertland.kxpilot.common

// ---------------------------------------------------------------------------
// Ported from: common/const.h  +  server/serverconst.h
// ---------------------------------------------------------------------------

/**
 * Global game constants.
 * Maps to the named `#define` constants in common/const.h and server/serverconst.h.
 */
object GameConst {
    // ------------------------------------------------------------------
    // Resolution / grid
    // ------------------------------------------------------------------
    /** Number of discrete rotation directions (trigonometric table size). */
    const val RES: Int = 128

    /** Map block size in pixels. */
    const val BLOCK_SZ: Int = 35

    // ------------------------------------------------------------------
    // String length limits
    // ------------------------------------------------------------------

    /** Maximum characters in a player name / short string. */
    const val MAX_CHARS: Int = 80

    /** Maximum length of a message (chat / error). */
    const val MSG_LEN: Int = 256

    // ------------------------------------------------------------------
    // Modifier banks
    // ------------------------------------------------------------------

    /** Number of modifier bank slots per player. */
    const val NUM_MODBANKS: Int = 4

    // ------------------------------------------------------------------
    // Physics limits
    // ------------------------------------------------------------------
    const val SPEED_LIMIT: Double = 65.0
    const val MAX_PLAYER_TURNSPEED: Double = 64.0
    const val MIN_PLAYER_TURNSPEED: Double = 0.0
    const val MAX_PLAYER_POWER: Double = 55.0
    const val MIN_PLAYER_POWER: Double = 5.0
    const val MAX_PLAYER_TURNRESISTANCE: Double = 1.0
    const val MIN_PLAYER_TURNRESISTANCE: Double = 0.0

    // ------------------------------------------------------------------
    // Fuel
    // ------------------------------------------------------------------
    const val MAX_STATION_FUEL: Double = 500.0

    /**
     * Maximum fuel a player ship can carry across all tanks.
     * From server/serverconst.h: `#define MAX_PLAYER_FUEL 2600.0`
     */
    const val MAX_PLAYER_FUEL: Double = 2600.0

    /**
     * Fuel transferred per tick at a refuel station.
     * From server/serverconst.h: `#define REFUEL_RATE 5.0`
     */
    const val REFUEL_RATE: Double = 5.0

    /**
     * Extra mass contributed per unit of carried fuel: `f * FUEL_MASS_COEFF`.
     * From server/serverconst.h: `#define FUEL_MASS(f) ((f) * 0.005)`
     */
    const val FUEL_MASS_COEFF: Double = 0.005

    /** Helper: returns the mass contribution of [fuel] units of fuel. */
    fun fuelMass(fuel: Double): Double = fuel * FUEL_MASS_COEFF

    /**
     * Extra mass added per thrust item.
     * From server/serverconst.h: `#define THRUST_MASS 0.7`
     */
    const val THRUST_MASS: Double = 0.7

    // ------------------------------------------------------------------
    // Combat / damage
    // ------------------------------------------------------------------
    const val TARGET_DAMAGE: Double = 250.0
    const val SELF_DESTRUCT_DELAY: Double = 150.0

    /** Collision-hit radius in pixels. */
    const val SHIP_SZ: Int = 16
    const val VISIBILITY_DISTANCE: Double = 1000.0
    const val BALL_RADIUS: Int = 10
    const val MISSILE_LEN: Int = 15

    /**
     * Mass of a mine object.
     * From server/serverconst.h: `#define MINE_MASS 30.0`
     */
    const val MINE_MASS: Double = 30.0

    /**
     * Collision radius of a mine in pixels.
     * From server/serverconst.h: `#define MINE_RADIUS 8`
     */
    const val MINE_RADIUS: Int = 8

    /**
     * Speed multiplier for mine-spawned shots.
     * From server/serverconst.h: `#define MINE_SPEED_FACT 1.3`
     */
    const val MINE_SPEED_FACT: Double = 1.3

    /**
     * Mass of a missile object.
     * From server/serverconst.h: `#define MISSILE_MASS 5.0`
     */
    const val MISSILE_MASS: Double = 5.0

    // ------------------------------------------------------------------
    // Teams
    // ------------------------------------------------------------------
    const val MAX_TEAMS: Int = 10
    const val OLD_MAX_CHECKS: Int = 26
    const val TEAM_NOT_SET: Int = 0xffff
    const val EXPIRED_MINE_ID: Int = 4096

    // ------------------------------------------------------------------
    // Fuel tanks  (server/serverconst.h)
    // ------------------------------------------------------------------

    /**
     * Maximum number of extra fuel tanks a player can carry.
     * From server/serverconst.h: `#define MAX_TANKS 8`
     */
    const val MAX_TANKS: Int = 8

    // ------------------------------------------------------------------
    // IDs  (server/serverconst.h)
    // ------------------------------------------------------------------

    /**
     * Sentinel value meaning "no player / object".
     * From server/serverconst.h: `#define NO_ID (-1)`
     */
    const val NO_ID: Int = -1

    /**
     * Total number of distinct player IDs (0..NUM_IDS-1).
     * From server/serverconst.h: `#define NUM_IDS 256`
     */
    const val NUM_IDS: Int = 256

    /**
     * Number of cannon IDs in the cannon-ID range.
     * From server/serverconst.h: `#define NUM_CANNON_IDS 10000`
     */
    const val NUM_CANNON_IDS: Int = 10000

    /**
     * First cannon object ID (exclusive lower bound above EXPIRED_MINE_ID).
     * From server/serverconst.h: `#define MIN_CANNON_ID (EXPIRED_MINE_ID + 1)`
     */
    const val MIN_CANNON_ID: Int = EXPIRED_MINE_ID + 1 // 4097

    /**
     * Last cannon object ID (inclusive).
     * From server/serverconst.h: `#define MAX_CANNON_ID (EXPIRED_MINE_ID + NUM_CANNON_IDS)`
     */
    const val MAX_CANNON_ID: Int = EXPIRED_MINE_ID + NUM_CANNON_IDS // 14096

    /**
     * Maximum total in-flight shot objects.
     * From server/serverconst.h: `#define MAX_TOTAL_SHOTS 16384`
     */
    const val MAX_TOTAL_SHOTS: Int = 16384

    /**
     * Sentinel for an unset player alliance.
     * From server/serverconst.h: `#define ALLIANCE_NOT_SET (-1)`
     */
    const val ALLIANCE_NOT_SET: Int = -1

    // ------------------------------------------------------------------
    // Afterburner levels  (server/serverconst.h)
    // ------------------------------------------------------------------

    /**
     * log₂ of the maximum afterburner level count.
     * From server/serverconst.h: `#define LG2_MAX_AFTERBURNER 4`
     */
    const val LG2_MAX_AFTERBURNER: Int = 4

    /**
     * Maximum afterburner level (15 = `(1<<4)-1`).
     * From server/serverconst.h: `#define MAX_AFTERBURNER ((1<<LG2_MAX_AFTERBURNER)-1)`
     */
    const val MAX_AFTERBURNER: Int = (1 shl LG2_MAX_AFTERBURNER) - 1 // 15

    // ------------------------------------------------------------------
    // Server timing  (server/serverconst.h)
    // ------------------------------------------------------------------

    /**
     * Maximum server frames per second.
     * From server/serverconst.h: `#define MAX_SERVER_FPS 255`
     */
    const val MAX_SERVER_FPS: Int = 255

    /**
     * Ticks between player death and respawn.
     * From server/serverconst.h: `#define RECOVERY_DELAY (12 * 3)` = 36
     */
    const val RECOVERY_DELAY: Int = 12 * 3 // 36

    // ------------------------------------------------------------------
    // Item / ability timer durations  (server/serverconst.h)
    // ------------------------------------------------------------------

    /**
     * Duration of a standard shield in ticks.
     * From server/serverconst.h: `#define SHIELD_TIME (2 * 12)` = 24
     */
    const val SHIELD_TIME: Int = 2 * 12 // 24

    /**
     * Duration of an emergency shield in ticks.
     * From server/serverconst.h: `#define EMERGENCY_SHIELD_TIME (4 * 12)` = 48
     */
    const val EMERGENCY_SHIELD_TIME: Int = 4 * 12 // 48

    /**
     * Duration of a phasing device activation in ticks.
     * From server/serverconst.h: `#define PHASING_TIME (4 * 12)` = 48
     */
    const val PHASING_TIME: Int = 4 * 12 // 48

    /**
     * Duration of an emergency thrust boost in ticks.
     * From server/serverconst.h: `#define EMERGENCY_THRUST_TIME (4 * 12)` = 48
     */
    const val EMERGENCY_THRUST_TIME: Int = 4 * 12 // 48

    // ------------------------------------------------------------------
    // View
    // ------------------------------------------------------------------
    const val MIN_VIEW_SIZE: Int = 384
    const val MAX_VIEW_SIZE: Int = 1024
    const val DEF_VIEW_SIZE: Int = 768

    // ------------------------------------------------------------------
    // Spark display
    // ------------------------------------------------------------------
    const val MIN_SPARK_RAND: Int = 0x00
    const val MAX_SPARK_RAND: Int = 0x80
    const val DEF_SPARK_RAND: Int = 0x55

    // ------------------------------------------------------------------
    // Debris rendering
    // ------------------------------------------------------------------
    const val DEBRIS_TYPES: Int = 8 * 4 * 4

    // ------------------------------------------------------------------
    // Score rate display
    // ------------------------------------------------------------------

    /** Number of samples in the score-rate sliding window. */
    const val SCORE_RATE_SIZE: Int = 20

    /** Score-rate display range (ticks). */
    const val SCORE_RATE_RANGE: Int = 1024

    // ------------------------------------------------------------------
    // Math
    // ------------------------------------------------------------------
    const val PI_VALUE: Double = kotlin.math.PI
}

// ---------------------------------------------------------------------------
// Energy drain constants  (server/serverconst.h  ED_*)
// ---------------------------------------------------------------------------

/**
 * Per-use fuel cost (negative = drain) for each weapon / device action.
 * Maps to `ED_*` constants in server/serverconst.h.
 *
 * Note: [PL_CRASH] and [BALL_HIT] are option-dependent at runtime; the
 * values here are the default option values used at server startup.
 * Use `options.playerCollisionFuelDrain` / `options.ballCollisionFuelDrain`
 * for live option-aware code.
 */
object EnergyDrain {
    /** Fuel cost per shot fired. */
    const val SHOT: Double = -0.2

    /** Fuel cost per smart-shot fired. */
    const val SMART_SHOT: Double = -30.0

    /** Fuel cost per mine placed. */
    const val MINE: Double = -60.0

    /** Fuel cost per ECM pulse. */
    const val ECM: Double = -60.0

    /** Fuel cost per transporter use. */
    const val TRANSPORTER: Double = -60.0

    /** Fuel cost per hyperjump. */
    const val HYPERJUMP: Double = -60.0

    /** Fuel cost per tick while shield is active.
     * Note: value equals [SHOT] (-0.20) — this identity is intentional per C source serverconst.h. */
    const val SHIELD: Double = -0.20

    /** Fuel cost per tick while phasing device is active. */
    const val PHASING_DEVICE: Double = -0.40

    /** Fuel cost per tick while cloaking device is active. */
    const val CLOAKING_DEVICE: Double = -0.07

    /** Fuel cost per tick while deflector is active. */
    const val DEFLECTOR: Double = -0.15

    /** Fuel cost when a plain shot hits this player. */
    const val SHOT_HIT: Double = -25.0

    /** Fuel cost when a smart shot hits this player. */
    const val SMART_SHOT_HIT: Double = -120.0

    /** Fuel cost per laser shot. */
    const val LASER: Double = -10.0

    /** Fuel cost when a laser hits this player. */
    const val LASER_HIT: Double = -100.0
}

// ---------------------------------------------------------------------------
// Colour constants  (common/const.h  BLACK / WHITE / BLUE / RED)
// ---------------------------------------------------------------------------

/**
 * The four server-supported display colours.
 * Maps to the `BLACK`, `WHITE`, `BLUE`, `RED` `#define` constants.
 */
enum class GameColor(
    val index: Int,
) {
    BLACK(0),
    WHITE(1),
    BLUE(2),
    RED(3),
    ;

    companion object {
        const val NUM_COLORS: Int = 4

        fun fromIndex(i: Int): GameColor = entries.firstOrNull { it.index == i } ?: BLACK
    }
}

// ---------------------------------------------------------------------------
// Polygon style flags  (common/const.h  STYLE_*)
// ---------------------------------------------------------------------------

/**
 * Bit-flag constants controlling how a polygon is rendered.
 * Maps to `STYLE_FILLED`, `STYLE_TEXTURED`, `STYLE_INVISIBLE`,
 * `STYLE_INVISIBLE_RADAR` in common/const.h.
 *
 * Stored as `const val Int` (same pattern as [ObjStatus]) so the values are
 * compile-time constants with no heap allocation.  Cast to `.toUInt()` when
 * testing against a `UInt` field.
 */
object PolygonStyleFlags {
    const val FILLED: Int = 1 shl 0
    const val TEXTURED: Int = 1 shl 1
    const val INVISIBLE: Int = 1 shl 2
    const val INVISIBLE_RADAR: Int = 1 shl 3
}
