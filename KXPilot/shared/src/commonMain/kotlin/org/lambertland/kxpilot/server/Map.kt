@file:OptIn(ExperimentalUnsignedTypes::class)

package org.lambertland.kxpilot.server

import org.lambertland.kxpilot.common.ClPos
import org.lambertland.kxpilot.common.GameConst
import org.lambertland.kxpilot.common.Item
import org.lambertland.kxpilot.common.Rules

// ---------------------------------------------------------------------------
// Ported from: server/map.h
// ---------------------------------------------------------------------------

// ------------------------------------------------------------------
// Map cell-type constants (server-side, differ from SETUP_* codes)
// ------------------------------------------------------------------

/**
 * Internal server map cell types.
 * Maps to the `SPACE`, `BASE`, `FILLED`, … constants in server/map.h.
 *
 * Each entry carries a [code] matching the original integer constant so that
 * raw wire/file values can be round-tripped via [fromRaw].
 */
enum class CellType(
    val code: Int,
) {
    SPACE(0),
    BASE(1),
    FILLED(2),
    REC_LU(3),
    REC_LD(4),
    REC_RU(5),
    REC_RD(6),
    FUEL(7),
    CANNON(8),
    CHECK(9),
    POS_GRAV(10),
    NEG_GRAV(11),
    CWISE_GRAV(12),
    ACWISE_GRAV(13),
    WORMHOLE(14),
    TREASURE(15),
    TARGET(16),
    ITEM_CONCENTRATOR(17),
    DECOR_FILLED(18),
    DECOR_LU(19),
    DECOR_LD(20),
    DECOR_RU(21),
    DECOR_RD(22),
    UP_GRAV(23),
    DOWN_GRAV(24),
    RIGHT_GRAV(25),
    LEFT_GRAV(26),
    FRICTION(27),
    ASTEROID_CONCENTRATOR(28),
    BASE_ATTRACTOR(127),
    ;

    companion object {
        private val BY_CODE: Map<Int, CellType> = entries.associateBy { it.code }

        /**
         * Return the [CellType] whose [code] equals [raw], or [SPACE] for
         * unrecognised values.
         */
        fun fromRaw(raw: Int): CellType = BY_CODE[raw] ?: SPACE
    }
}

/**
 * Cardinal direction codes in RES units.
 * Maps to `DIR_RIGHT`, `DIR_UP`, `DIR_LEFT`, `DIR_DOWN` in server/map.h.
 *
 * All four are `const val` — `GameConst.RES` is `const val Int`, so
 * `Int / Int` expressions are legal compile-time constants.
 */
object Direction {
    const val RIGHT: Int = 0
    const val UP: Int = GameConst.RES / 4
    const val LEFT: Int = GameConst.RES / 2
    const val DOWN: Int = 3 * GameConst.RES / 4
}

// ------------------------------------------------------------------
// Map entity data classes
// ------------------------------------------------------------------

/**
 * A fuel station on the map.  Maps to C `fuel_t`.
 *
 * Mutable class (not data class) — [fuel] and [lastChange] are updated
 * every frame as players refuel.
 */
class Fuel(
    val pos: ClPos,
    var fuel: Double,
    val connMask: UInt,
    var lastChange: Long,
    val team: Int,
)

/** A gravity source.  Maps to C `grav_t`. */
data class Grav(
    val pos: ClPos,
    val force: Double,
    val type: CellType,
)

/**
 * A player spawn base.  Maps to C `base_t`.
 *
 * Plain class (not data class) — [initialItems] is an IntArray, which makes
 * `data class` `copy()` semantically incorrect (shallow copy, shared array).
 * Explicit [equals]/[hashCode] are provided for structural comparison when needed.
 */
class Base(
    val pos: ClPos,
    val dir: Int,
    val ind: Int,
    val team: Int,
    val order: Int,
    val initialItems: IntArray = IntArray(Item.NUM_ITEMS),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Base) return false
        return pos == other.pos && dir == other.dir && ind == other.ind &&
            team == other.team && order == other.order &&
            initialItems.contentEquals(other.initialItems)
    }

    override fun hashCode(): Int = arrayOf(pos, dir, ind, team, order).contentHashCode() * 31 + initialItems.contentHashCode()
}

/** A timing checkpoint.  Maps to C `check_t`. */
data class Check(
    val pos: ClPos,
)

/**
 * Per-item-type world configuration.
 * Maps to C `item_t` in server/map.h.
 * Renamed to [ItemConfig] to avoid clash with the [org.lambertland.kxpilot.common.Item] enum.
 */
data class ItemConfig(
    val prob: Double,
    val max: Int,
    val num: Int,
    val chance: Int,
    val cannonProb: Double,
    val minPerPack: Int,
    val maxPerPack: Int,
    val initial: Int,
    val cannonInitial: Int,
    val limit: Int,
)

/**
 * Asteroid world configuration.
 * Maps to C `asteroid_t` (the server map struct, not the game object).
 * Renamed to [AsteroidConfig] to avoid clash with server game-object types.
 */
data class AsteroidConfig(
    val prob: Double,
    val max: Int,
    val num: Int,
    val chance: Int,
)

/**
 * Wormhole behaviour type.  Maps to C `wormtype_t` enum.
 *
 * - [NORMAL]  — bidirectional teleporter; randomly selects a destination.
 * - [IN]      — entrance only; ships enter here but cannot exit here.
 * - [OUT]     — exit only; ships emerge here but cannot enter.
 * - [FIXED]   — always teleports to the same paired wormhole (fixed routing).
 */
enum class WormType { NORMAL, IN, OUT, FIXED }

/**
 * A wormhole tile.  Maps to C `wormhole_t`.
 *
 * Mutable class (not data class) — [countdown] ticks down every frame and
 * [lastDest]/[lastId] change on each teleportation.  `data class` `copy()`
 * would be needed on every mutation, causing per-frame allocation pressure.
 */
class Wormhole(
    val pos: ClPos,
    var lastDest: Int = -1,
    var countdown: Double = 0.0,
    var type: WormType = WormType.NORMAL,
    var lastId: Int = 0,
    var lastBlock: Int = 0,
    var group: Int = 0,
)

/**
 * A ball treasure chest.  Maps to C `treasure_t`.
 *
 * Mutable class (not data class) — [have], [empty], and [destroyed] change
 * at runtime as balls are taken and returned.
 */
class Treasure(
    val pos: ClPos,
    var have: Boolean = false,
    var team: Int = 0,
    var destroyed: Int = 0,
    var empty: Boolean = true,
    var ballStyle: Int = 0,
)

/**
 * A destructible target tile.  Maps to C `target_t` (server version).
 *
 * **Immutable-update style** (unlike [Wormhole] / [Treasure] which are mutable
 * classes): `Target` is a `data class` whose "mutable" fields ([deadTicks],
 * [damage], [lastChange]) are changed by replacing the instance in the
 * [World.targets] list via `copy()`.  This is safe because:
 *  - Targets are hit infrequently (not every frame), so per-hit allocation is
 *    not a hot-path concern.
 *  - The extension functions [applyHit] and [destroy] return a *new* `Target`,
 *    making the mutation sites explicit and easy to audit.
 *  - `data class` `equals`/`hashCode`/`toString` are consistent with
 *    structural identity, which is useful in tests.
 *
 * Contrast with [Wormhole] (countdown ticks every frame) and [Treasure]
 * (mutated every frame when a ball is held) where per-frame allocation would
 * be unacceptable.
 */
data class Target(
    val pos: ClPos,
    val team: Int = 0,
    val deadTicks: Double = 0.0,
    val damage: Double = 0.0,
    val connMask: UInt = 0u,
    val updateMask: UInt = 0u,
    val lastChange: Long = 0L,
    val group: Int = 0,
) {
    /** True while the target is alive (not destroyed). */
    val isAlive: Boolean get() = deadTicks <= 0.0
}

/**
 * Apply [dmg] damage to this target.
 *
 * Returns a new [Target] with updated [damage].  The caller should check
 * `newTarget.damage >= targetMaxDamage` and call [destroy] if the target
 * was destroyed.
 *
 * Maps to the damage accumulation part of C `Target_hit`.
 */
fun Target.applyHit(dmg: Double): Target = copy(damage = damage + dmg)

/**
 * Mark this target as destroyed at server time [now].
 *
 * Returns a new [Target] with [deadTicks] set to [deadTicksOnDestroy] and
 * [lastChange] set to [now].  Call after verifying `damage >= targetMaxDamage`.
 *
 * Maps to the destruction path of C `Target_hit`.
 */
fun Target.destroy(
    now: Long,
    deadTicksOnDestroy: Double,
): Target = copy(deadTicks = deadTicksOnDestroy, lastChange = now)

/** Team statistics for a team slot.  Maps to C `team_t`. */
data class Team(
    val numMembers: Int = 0,
    val numRobots: Int = 0,
    val numBases: Int = 0,
    val numTreasures: Int = 0,
    val numEmptyTreasures: Int = 0,
    val treasuresDestroyed: Int = 0,
    val treasuresLeft: Int = 0,
    val swapperId: Int = 0,
)

/** An item-concentrator tile.  Maps to C `item_concentrator_t`. */
data class ItemConcentrator(
    val pos: ClPos,
)

/** An asteroid-concentrator tile.  Maps to C `asteroid_concentrator_t`. */
data class AsteroidConcentrator(
    val pos: ClPos,
)

/** A friction-area tile.  Maps to C `friction_area_t`. */
data class FrictionArea(
    val pos: ClPos,
    val frictionSetting: Double,
    val friction: Double,
    val group: Int,
)

/**
 * An active ECM pulse in the world (server side).
 * Maps to C anonymous `ecm_t` struct in server/map.h.
 */
data class Ecm(
    val size: Double,
    val pos: ClPos,
    val id: Int,
)

/** An active transporter beam.  Maps to C `transporter_t`. */
data class Transporter(
    val pos: ClPos,
    val victimId: Int,
    val id: Int,
    val count: Double,
)

// ------------------------------------------------------------------
// World container
// ------------------------------------------------------------------

/**
 * The complete server world state.
 * Maps to C `struct world` in server/map.h.
 *
 * This is intentionally a mutable class (not a data class) because
 * it holds references to mutable collections that change every frame.
 */
class World {
    /** Size of world in blocks, rounded up. */
    var x: Int = 0
    var y: Int = 0
    var bwidthFloor: Int = 0
    var bheightFloor: Int = 0
    var diagonal: Double = 0.0

    /** Size in pixels. */
    var width: Int = 0
    var height: Int = 0

    /** Size in clicks. */
    var cwidth: Int = 0
    var cheight: Int = 0

    var hypotenuse: Double = 0.0

    var rules: Rules? = null
    var name: String = ""
    var author: String = ""
    var dataUrl: String = ""

    /** Map cell type: block[bx][by] */
    var block: Array<Array<CellType>> = emptyArray()

    /** Per-block gravity vectors. */
    var gravity: Array<Array<org.lambertland.kxpilot.common.Vector>> = emptyArray()

    val items: Array<ItemConfig?> = arrayOfNulls(Item.NUM_ITEMS)
    var asteroids: AsteroidConfig? = null
    val teams: Array<Team> = Array(GameConst.MAX_TEAMS) { Team() }

    var numTeamBases: Int = 0

    val asteroidConcs: MutableList<AsteroidConcentrator> = mutableListOf()
    val bases: MutableList<Base> = mutableListOf()
    val cannons: MutableList<Cannon> = mutableListOf()
    val ecms: MutableList<Ecm> = mutableListOf()
    val fuels: MutableList<Fuel> = mutableListOf()
    val frictionAreas: MutableList<FrictionArea> = mutableListOf()
    val gravs: MutableList<Grav> = mutableListOf()
    val itemConcs: MutableList<ItemConcentrator> = mutableListOf()
    val targets: MutableList<Target> = mutableListOf()
    val transporters: MutableList<Transporter> = mutableListOf()
    val treasures: MutableList<Treasure> = mutableListOf()
    val wormholes: MutableList<Wormhole> = mutableListOf()

    val checks: MutableList<Check> = mutableListOf()

    var haveOptions: Boolean = false

    // ------------------------------------------------------------------
    // Accessors / helpers
    // ------------------------------------------------------------------

    fun getBlock(
        bx: Int,
        by: Int,
    ): CellType {
        require(block.isNotEmpty()) { "World.getBlock: block array not initialised" }
        require(bx in 0 until x && by in 0 until y) {
            "World.getBlock: ($bx,$by) out of bounds for $x×$y world"
        }
        return block[bx][by]
    }

    /** Unchecked block read.  Only call after verifying bounds are valid. */
    internal fun getBlockRaw(
        bx: Int,
        by: Int,
    ): CellType = block[bx][by]

    fun setBlock(
        bx: Int,
        by: Int,
        type: CellType,
    ) {
        require(block.isNotEmpty()) { "World.setBlock: block array not initialised" }
        require(bx in 0 until x && by in 0 until y) {
            "World.setBlock: ($bx,$by) out of bounds for $x×$y world"
        }
        block[bx][by] = type
    }

    fun containsClPos(
        cx: Int,
        cy: Int,
    ): Boolean = cx in 0 until cwidth && cy in 0 until cheight

    fun wrapXClick(cx: Int): Int = ((cx % cwidth) + cwidth) % cwidth

    fun wrapYClick(cy: Int): Int = ((cy % cheight) + cheight) % cheight
}

// ---------------------------------------------------------------------------
// World extension helpers
// ---------------------------------------------------------------------------

// Note: block↔click conversions are in common/Click.kt:
//   BlkPos.toCenterClPos()  — block coord to click-space centre of that block
//   Click.toBlock()         — click coord to the block it falls in

/**
 * Atomically initialise all geometry fields of this [World] from raw block dimensions.
 *
 * ## Motivation
 * Before this helper existed, grid initialisation was spread across
 * `MapToWorld.kt`, `GameEngine.forEmptyWorld`, and test helpers — each
 * setting a different subset of the nine inter-dependent fields in different
 * orders.  A missed assignment produces a silently corrupt world (zero
 * `cwidth`, un-allocated `block`, etc.).
 *
 * ## Contract
 * All nine geometry fields (`x`, `y`, `bwidthFloor`, `bheightFloor`,
 * `width`, `height`, `cwidth`, `cheight`, `block`, `gravity`) are set
 * together here.  Callers **must not** set any of these fields individually
 * after construction — use this function instead.
 *
 * @param cols   World width in blocks (≥ 1).
 * @param rows   World height in blocks (≥ 1).
 * @param fill   Initial cell type for every block (default [CellType.SPACE]).
 */
fun World.initGrid(
    cols: Int,
    rows: Int,
    fill: CellType = CellType.SPACE,
) {
    require(cols >= 1) { "World.initGrid: cols=$cols must be ≥ 1" }
    require(rows >= 1) { "World.initGrid: rows=$rows must be ≥ 1" }

    x = cols
    y = rows
    bwidthFloor = cols
    bheightFloor = rows
    width = cols * GameConst.BLOCK_SZ
    height = rows * GameConst.BLOCK_SZ
    cwidth = width * org.lambertland.kxpilot.common.ClickConst.CLICK
    cheight = height * org.lambertland.kxpilot.common.ClickConst.CLICK
    block = Array(cols) { Array(rows) { fill } }
    gravity = Array(cols) { Array(rows) { org.lambertland.kxpilot.common.Vector.ZERO } }
}

/**
 * Wrap both click axes and return the resulting [ClPos].
 * The world is toroidal, so coordinates outside `[0, cwidth)` / `[0, cheight)` wrap.
 */
fun World.wrapClick(
    cx: Int,
    cy: Int,
): ClPos = ClPos(wrapXClick(cx), wrapYClick(cy))

/**
 * Return true if block `(bx, by)` is solid (impassable by objects).
 *
 * Solid types: FILLED, all four REC_* diagonals, and DECOR_FILLED /
 * DECOR_* decorative variants.  Matches the C collision predicates in
 * `walls.c`.
 */
fun World.isSolid(
    bx: Int,
    by: Int,
): Boolean {
    if (bx !in 0 until x || by !in 0 until y) return true // out-of-bounds is solid
    return when (getBlockRaw(bx, by)) {
        CellType.FILLED,
        CellType.REC_LU, CellType.REC_LD, CellType.REC_RU, CellType.REC_RD,
        CellType.DECOR_FILLED,
        CellType.DECOR_LU, CellType.DECOR_LD, CellType.DECOR_RU, CellType.DECOR_RD,
        -> true

        else -> false
    }
}

/**
 * Find the nearest fuel station to click position `(cx, cy)`.
 *
 * Returns `null` if there are no fuel stations in the world.
 * Distance is squared-Euclidean to avoid a sqrt on every call.
 */
fun World.nearestFuel(
    cx: Int,
    cy: Int,
): Fuel? {
    var best: Fuel? = null
    var bestDist2 = Long.MAX_VALUE
    for (f in fuels) {
        val dx = (f.pos.cx - cx).toLong()
        val dy = (f.pos.cy - cy).toLong()
        val d2 = dx * dx + dy * dy
        if (d2 < bestDist2) {
            bestDist2 = d2
            best = f
        }
    }
    return best
}
