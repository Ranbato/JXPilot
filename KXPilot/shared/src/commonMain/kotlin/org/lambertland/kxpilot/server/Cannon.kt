package org.lambertland.kxpilot.server

import org.lambertland.kxpilot.common.ClPos
import org.lambertland.kxpilot.common.GameConst
import org.lambertland.kxpilot.common.Item

/**
 * Weapon types a cannon can fire.  Maps to `CW_*` defines in `server/cannon.h`.
 */
enum class CannonWeapon(
    val code: Int,
) {
    /** Plain bullet(s); always available. */
    SHOT(0),

    /** Dropped or thrown mine; uses one mine. */
    MINE(1),

    /** Torpedo, heatseeker, or smart missile; uses one missile. */
    MISSILE(2),

    /** Blinding, stun, or normal laser; needs a laser item. */
    LASER(3),

    /** ECM pulse; uses one ECM item. */
    ECM(4),

    /** Tractor or pressor beam; needs a tractorbeam item. */
    TRACTORBEAM(5),

    /** Transporter beam; uses one transporter item. */
    TRANSPORTER(6),

    /** Stream of exhaust particles; needs an afterburner and uses one fuel pack. */
    GASJET(7),
    ;

    companion object {
        private val byCode = entries.associateBy { it.code }

        fun fromCode(code: Int): CannonWeapon? = byCode[code]
    }
}

/**
 * Defense types a cannon can activate.  Maps to `CD_*` defines in `server/cannon.h`.
 */
enum class CannonDefense(
    val code: Int,
) {
    /** Emergency shield: absorbs any shot for ~4 seconds; uses one emergency shield. */
    EM_SHIELD(0),

    /** Phasing device: lets any shot pass through for ~4 seconds; uses one phasing device. */
    PHASING(1),
    ;

    companion object {
        private val byCode = entries.associateBy { it.code }

        fun fromCode(code: Int): CannonDefense? = byCode[code]
    }
}

// ---------------------------------------------------------------------------
// CannonConst — numeric constants from server/cannon.h
// ---------------------------------------------------------------------------

/**
 * Behavioural constants for cannon AI and physics.
 *
 * Runtime-dependent macros from `cannon.h` are intentionally omitted:
 *  - `CANNON_SHOT_LIFE` / `CANNON_SHOT_LIFE_MAX` depend on `randomMT()` at
 *    fire time and are computed in firing logic, not stored here.
 *  - `CANNON_USE_ITEM` is an `extern long` option controlled by the server
 *    configuration system.
 */
object CannonConst {
    /**
     * Maximum range at which a cannon detects and fires at a target (pixels).
     * C: `CANNON_DISTANCE = VISIBILITY_DISTANCE * 0.5`
     * `GameConst.VISIBILITY_DISTANCE` = 1000.0 px → 500 px.
     * (Note: the C comment says "modified by sensors"; this is the base value.)
     */
    const val DISTANCE: Double = GameConst.VISIBILITY_DISTANCE * 0.5

    /**
     * Probability that a cannon throws an item on death, multiplied by the
     * server option `dropItemOnKillProb`.
     * C: `CANNON_DROP_ITEM_PROB = 0.7`
     */
    const val DROP_ITEM_PROB: Double = 0.7

    /**
     * Mass of a mine deployed by a cannon, as a fraction of `MINE_MASS`.
     * C: `CANNON_MINE_MASS = MINE_MASS * 0.6`
     * The factor is stored here; multiply by the mine mass at use site.
     */
    const val MINE_MASS_FACTOR: Double = 0.6

    /**
     * Mass of each shot fired by a cannon.
     * C: `CANNON_SHOT_MASS = 0.4`
     */
    const val SHOT_MASS: Double = 0.4

    /**
     * Number of laser pulses used when computing pulse lifetime for a cannon.
     * C: `CANNON_PULSES = 1`
     */
    const val PULSES: Int = 1

    /**
     * Half-arc (in RES heading-units) within which the cannon can fire.
     * C: `CANNON_SPREAD = RES / 3` ≈ 42 heading-units (≈ 120°).
     */
    const val SPREAD: Int = GameConst.RES / 3

    /**
     * Maximum smartness level of a cannon's AI (inclusive range 0–[SMARTNESS_MAX]).
     * C: `CANNON_SMARTNESS_MAX = 3`
     */
    const val SMARTNESS_MAX: Int = 3
}

// ---------------------------------------------------------------------------
// Cannon entity — map tile with AI
// (moved here from Map.kt for co-location with CannonConst/CannonWeapon/CannonDefense)
// ---------------------------------------------------------------------------

/**
 * A map cannon.  Maps to C `cannon_t`.
 *
 * Mutable class (not data class) — most runtime fields change every
 * frame (cooldown, damage, tractor state, etc.).  `data class` buys
 * nothing here and its `copy()` semantics are misleading for a hot-path
 * mutable entity.  IntArray fields also break structural equality anyway.
 */
class Cannon(
    val pos: ClPos,
    var dir: Int,
    val connMask: UInt,
    var lastChange: Long = 0L,
    /** Mutable item counts per type.  Content is mutated during play; the array itself is fixed. */
    val itemInventory: IntArray = IntArray(Item.NUM_ITEMS),
    var tractorTargetId: Int = 0,
    var tractorIsPressor: Boolean = false,
    val team: Int = 0,
    var used: Long = 0L,
    var deadTicks: Double = 0.0,
    var damaged: Double = 0.0,
    var tractorCount: Double = 0.0,
    var emergencyShieldLeft: Double = 0.0,
    var phasingLeft: Double = 0.0,
    val group: Int = 0,
    var score: Double = 0.0,
    val id: Short = 0,
    var smartness: Short = 0,
    var shotSpeed: Float = 0f,
    val initialItemInventory: IntArray = IntArray(Item.NUM_ITEMS),
    /**
     * Ticks remaining before this cannon can fire again.
     *
     * Defaults to [GameConst.SHOT_SPEED_FACTOR] so cannons do not all fire
     * simultaneously on the first game tick.  The C server randomises the
     * initial delay; using a full cooldown is a safe conservative default.
     */
    var fireTimer: Double = GameConst.SHOT_SPEED_FACTOR,
    /**
     * Weapon type this cannon fires.  C: cannon_t.weapon field (cannon.h).
     * Defaults to [CannonWeapon.SHOT] (plain bullet) if not set by the map loader.
     */
    var weapon: CannonWeapon = CannonWeapon.SHOT,
)
