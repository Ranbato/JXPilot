package org.lambertland.kxpilot.server

import org.lambertland.kxpilot.common.ClPos
import org.lambertland.kxpilot.common.GameConst
import org.lambertland.kxpilot.common.Item
import org.lambertland.kxpilot.common.ShipShape
import org.lambertland.kxpilot.common.Vector

// ---------------------------------------------------------------------------
// Ported from: server/player.h
// ---------------------------------------------------------------------------

// ------------------------------------------------------------------
// Player type and state enums
// ------------------------------------------------------------------

/**
 * Extended player type (human, robot, tank).
 * Maps to `PL_TYPE_*` constants in server/player.h.
 */
enum class PlayerType(
    val code: Int,
) {
    HUMAN(0),
    ROBOT(1),
    TANK(2),
    ;

    companion object {
        fun fromCode(code: Int): PlayerType = entries.firstOrNull { it.code == code } ?: HUMAN
    }
}

/**
 * Player lifecycle state machine states.
 * Maps to `PL_STATE_*` constants in server/player.h.
 */
enum class PlayerState(
    val code: Int,
) {
    UNDEFINED(0),
    WAITING(1),
    APPEARING(2),
    ALIVE(3),
    KILLED(4),
    DEAD(5),
    PAUSED(6),
    ;

    companion object {
        fun fromCode(code: Int): PlayerState = entries.firstOrNull { it.code == code } ?: UNDEFINED
    }
}

/**
 * Bit-flags for items a player has or uses (the `have` / `used` Long fields).
 * Maps to `HAS_*` / `USES_*` constants in server/player.h.
 * HAS_* and USES_* share the same bit values.
 *
 * **Intentional bit gaps:** bits 5–14 are unassigned in the C source.
 * The sparse allocation matches the original `server/player.h` layout and
 * is preserved here for wire-format compatibility.  Do not fill the gaps.
 */
object PlayerAbility {
    const val EMERGENCY_THRUST: Long = 1L shl 30
    const val AUTOPILOT: Long = 1L shl 29
    const val TRACTOR_BEAM: Long = 1L shl 28
    const val LASER: Long = 1L shl 27
    const val CLOAKING_DEVICE: Long = 1L shl 26
    const val SHIELD: Long = 1L shl 25
    const val REFUEL: Long = 1L shl 24
    const val REPAIR: Long = 1L shl 23
    const val COMPASS: Long = 1L shl 22
    const val AFTERBURNER: Long = 1L shl 21
    const val CONNECTOR: Long = 1L shl 20
    const val EMERGENCY_SHIELD: Long = 1L shl 19
    const val DEFLECTOR: Long = 1L shl 18
    const val PHASING_DEVICE: Long = 1L shl 17
    const val MIRROR: Long = 1L shl 16
    const val ARMOR: Long = 1L shl 15
    const val SHOT: Long = 1L shl 4
    const val BALL: Long = 1L shl 3
}

/**
 * Bit-flags for the player `pl_status` UShort field (server-only bits).
 * Maps to `HOVERPAUSE`, `REPROGRAM`, `FINISH`, `RACE_OVER` in server/player.h.
 *
 * Stored as `const val Int` (same pattern as [ObjStatus]) so the values are
 * compile-time constants with no heap allocation.  Cast to `.toUInt()` when
 * testing against a `UInt` field.
 */
object PlayerStatus {
    const val HOVERPAUSE: Int = 1 shl 9
    const val REPROGRAM: Int = 1 shl 10
    const val FINISH: Int = 1 shl 11
    const val RACE_OVER: Int = 1 shl 12
}

// ------------------------------------------------------------------
// Supporting data classes
// ------------------------------------------------------------------

/**
 * Player fuel tanks.  Maps to C `pl_fuel_t` in server/player.h.
 *
 * Mutable class (not data class) — fields are updated every frame as the
 * player consumes or collects fuel.  `data class` `copy()` would shallow-copy
 * the [tank] DoubleArray, making the copy share the same backing array.
 *
 * [tank] has indices 0 (main fixed tank) through [numTanks] inclusive.
 */
class PlayerFuel(
    var sum: Double = 0.0,
    var max: Double = 0.0,
    var current: Int = 0,
    numTanks: Int = 0,
    /** tank[0] is the main fixed tank; tank[1..numTanks] are extra tanks. */
    val tank: DoubleArray = DoubleArray(1 + MAX_TANKS),
) {
    /**
     * Number of extra fuel tanks (0..MAX_TANKS).
     *
     * The setter enforces the same invariant as the `init` block so that
     * post-construction assignments are also validated — not just the initial
     * value.
     */
    var numTanks: Int = numTanks
        set(value) {
            require(value in 0..MAX_TANKS) {
                "PlayerFuel: numTanks=$value exceeds MAX_TANKS=$MAX_TANKS"
            }
            field = value
        }

    init {
        require(numTanks in 0..MAX_TANKS) {
            "PlayerFuel: numTanks=$numTanks exceeds MAX_TANKS=$MAX_TANKS"
        }
    }

    companion object {
        /** Alias to [GameConst.MAX_TANKS] — kept here for local reference within PlayerFuel. */
        const val MAX_TANKS: Int = GameConst.MAX_TANKS
    }

    /** Zero all tanks and reset sum/max/current. */
    fun reset() {
        sum = 0.0
        max = 0.0
        current = 0
        numTanks = 0
        tank.fill(0.0)
    }
}

/** Visibility status of one player to another.  Maps to C `visibility_t`. */
data class Visibility(
    val canSee: Boolean = false,
    val lastChange: Long = 0L,
)

/** Record of a single shove event.  Maps to C `shove_t`. */
data class Shove(
    val pusherId: Int = 0,
    val time: Int = 0,
)

/**
 * Inline lock-target data embedded in [Player].
 *
 * Mutable class (not data class) — [distance] is updated every tick as the
 * locked target moves.  `data class` `copy()` would allocate a new instance
 * on every lock-state refresh, adding unnecessary pressure on the hot path.
 */
class LockInfo {
    var tagged: Int = 0
    var plId: Int = 0
    var distance: Double = 0.0

    /** Reset all fields to their zero/default state. */
    fun reset() {
        tagged = 0
        plId = 0
        distance = 0.0
    }
}

// ------------------------------------------------------------------
// Player
// ------------------------------------------------------------------

/**
 * A connected player (human, robot, or tank).
 * Maps to C `player_t` in server/player.h.
 *
 * Extends [GameObjectBase] (OBJECT_BASE fields) so the physics engine can
 * treat players and objects uniformly.
 *
 * This is intentionally a mutable class — the server mutates player state
 * every frame.  [data class] would be wrong here.
 */
class Player : GameObjectBase() {
    // Identity
    var plType: PlayerType = PlayerType.HUMAN
    var plTypeMyChar: Char = ' '
    var plOldStatus: UByte = 0u

    // State
    var plStatus: UShort = 0u
    var plState: PlayerState = PlayerState.UNDEFINED
    var plLife: Int = 0
    var plDeathsSinceJoin: Int = 0
    var plPrevTeam: UShort = 0u

    // Physics / movement
    var turnspeed: Double = 0.0
    var velocity: Double = 0.0

    /**
     * Current heading angle **in radians** (0 ≤ floatDir < 2π).
     *
     * **UNIT DIVERGENCE FROM C:** The C field `float_dir` is stored in RES-units
     * (0 ≤ float_dir < 128), and `Player_set_float_dir` computes trig as
     * `cos(float_dir * 2π / RES)`.  Kotlin stores radians directly, which is
     * internally consistent — but any code ported from C that reads `float_dir`
     * expecting a RES-unit value will be wrong if applied to this field without
     * conversion.
     *
     * Use [floatDirInRes] to obtain the RES-unit equivalent (needed for
     * wire-protocol code that serializes `float_dir`).
     * Use [ServerGameWorld.radToDir] to get the discrete integer direction for
     * normal game-logic use.
     *
     * **Invariant:** `floatDirCos == cos(floatDir)` and `floatDirSin == sin(floatDir)` at all times.
     * Always use [setFloatDir] to update the heading — never assign `floatDir`,
     * `floatDirCos`, or `floatDirSin` directly.
     */
    private var _floatDir: Double = 0.0
    private var _floatDirCos: Double = 1.0
    private var _floatDirSin: Double = 0.0

    val floatDir: Double get() = _floatDir
    val floatDirCos: Double get() = _floatDirCos
    val floatDirSin: Double get() = _floatDirSin

    /**
     * Set the float direction and atomically update the cached trig values.
     *
     * @param angle Heading in **radians**.  See [floatDir] for the unit-mismatch
     *              note vs the C `float_dir` field.
     */
    fun setFloatDir(angle: Double) {
        _floatDir = angle
        _floatDirCos = kotlin.math.cos(angle)
        _floatDirSin = kotlin.math.sin(angle)
    }

    /**
     * Return [floatDir] converted to **RES units** (0 ≤ result < RES).
     *
     * This matches the range and semantics of C's `float_dir` field.  Use this
     * whenever serializing the heading over the wire or comparing against C
     * RES-unit values.
     */
    fun floatDirInRes(): Double {
        val v = _floatDir / (2.0 * GameConst.PI_VALUE) * GameConst.RES
        return ((v % GameConst.RES) + GameConst.RES) % GameConst.RES
    }

    var wantedFloatDir: Double = 0.0
    var turnresistance: Double = 0.0
    var turnvel: Double = 0.0
    var oldTurnvel: Double = 0.0
    var turnacc: Double = 0.0
    var dir: Short = 0

    // Score / kills
    var score: Double = 0.0
    var updateScore: Boolean = false
    var kills: Int = 0
    var deaths: Int = 0

    // Equipment

    /**
     * Bit-set of items/abilities this player currently **has**.
     * Maps to C `unsigned int have` in player.h.
     *
     * **Wire-format note:** C serializes this field as a 4-byte unsigned integer.
     * When writing to the network, truncate to 32 bits: `(have and 0xFFFFFFFFL).toInt()`.
     * All defined [PlayerAbility] bit positions fit within bits 0–30, so no
     * information is lost in the truncation.
     */
    var have: Long = 0L

    /**
     * Bit-set of items/abilities this player is currently **using**.
     * Maps to C `unsigned int used` in player.h.
     *
     * Same wire-format constraint as [have]: serialize as a 4-byte unsigned integer.
     */
    var used: Long = 0L
    var shieldTime: Double = 0.0
    var fuel: PlayerFuel = PlayerFuel()
    var emptyMass: Double = 0.0

    var power: Double = 0.0
    var powerS: Double = 0.0
    var turnspeedS: Double = 0.0
    var turnresistanceS: Double = 0.0
    var sensorRange: Double = 0.0

    var shots: Int = 0
    var missileRack: Int = 0
    var numPulses: Int = 0

    var emergencyThrustLeft: Double = 0.0
    var emergencyShieldLeft: Double = 0.0
    var phasingLeft: Double = 0.0

    var pauseCount: Double = 0.0
    var recoveryCount: Double = 0.0
    var selfDestructCount: Double = 0.0

    val item: IntArray = IntArray(Item.NUM_ITEMS)
    val initialItem: IntArray = IntArray(Item.NUM_ITEMS)
    var loseItem: Int = 0
    var loseItemState: Int = 0

    var autoPowerS: Double = 0.0
    var autoTurnspeedS: Double = 0.0
    var autoTurnresistanceS: Double = 0.0

    val modbank: Array<Modifiers> = Array(GameConst.NUM_MODBANKS) { Modifiers.ZERO }

    var shotTime: Double = 0.0
    var laserTime: Double = 0.0
    var didShoot: Boolean = false
    var tractorIsPressor: Boolean = false

    var repairTarget: Int = 0
    var fs: Int = 0
    var check: Int = 0
    var prevCheck: Int = 0
    var time: Int = 0
    var round: Int = 0
    var prevRound: Int = 0
    var bestLap: Int = 0
    var lastLap: Int = 0
    var lastLapTime: Int = 0
    var lastCheckDir: Int = 0
    var lastWallTouch: Long = 0L
    var survivalTime: Double = 0.0

    var homeBase: Base? = null
    var lock: LockInfo = LockInfo()
    val lockbank: IntArray = IntArray(LOCK_BANK_MAX)

    var myChar: Char = ' '
    var name: String = ""
    var userName: String = ""
    var hostname: String = ""
    var pseudoTeam: UShort = 0u
    var alliance: Int = 0
    var invite: Int = 0
    var ball: BallObject? = null

    val shoveRecord: Array<Shove> = Array(MAX_RECORDED_SHOVES) { Shove() }
    var shoveNext: Int = 0

    var visibility: Array<Visibility>? = null
    var forceVisible: Double = 0.0
    var damaged: Double = 0.0
    var stunned: Double = 0.0
    var updateVisibility: Boolean = false

    var lastTargetUpdate: Int = 0
    var lastCannonUpdate: Int = 0
    var lastFuelUpdate: Int = 0
    var lastPolyStyleUpdate: Int = 0
    var ecmCount: Int = 0

    /** Version of the connected XPilot client. */
    var version: UInt = 0u

    /** Key-press state bit-vector (size = Key.NUM_KEYS). */
    val lastKeyv: BooleanArray = BooleanArray(org.lambertland.kxpilot.common.Key.NUM_KEYS)
    val prevKeyv: BooleanArray = BooleanArray(org.lambertland.kxpilot.common.Key.NUM_KEYS)

    var frameLastBusy: Long = 0L

    var playerFps: Int = 0
    var maxTurnsps: Int = 0
    var recType: Int = 0
    var rank: RankNode? = null

    var pauseTime: Double = 0.0
    var idleTime: Double = 0.0
    var flooding: Int = 0

    var muted: Boolean = false
    var isOperator: Boolean = false
    var wantAudio: Boolean = false
    var privs: Int = 0

    var snafuCount: Double = 0.0

    var ship: ShipShape? = null

    // -----------------------------------------------------------------
    // Convenience predicates (mirror C inline functions in player.h)
    // -----------------------------------------------------------------
    fun isWaiting(): Boolean = plState == PlayerState.WAITING

    fun isAppearing(): Boolean = plState == PlayerState.APPEARING

    fun isAlive(): Boolean = plState == PlayerState.ALIVE

    fun isKilled(): Boolean = plState == PlayerState.KILLED

    fun isDead(): Boolean = plState == PlayerState.DEAD

    fun isPaused(): Boolean = plState == PlayerState.PAUSED

    fun isActive(): Boolean = isAlive() || isKilled()

    fun isHuman(): Boolean = plType == PlayerType.HUMAN

    fun isRobot(): Boolean = plType == PlayerType.ROBOT

    fun isTank(): Boolean = plType == PlayerType.TANK

    fun isThrusting(): Boolean = (objStatus.toInt() and ObjStatus.THRUSTING) != 0

    fun isCloaked(): Boolean = (used and PlayerAbility.CLOAKING_DEVICE) != 0L

    fun isPhasing(): Boolean = (used and PlayerAbility.PHASING_DEVICE) != 0L

    fun isSelfDestructing(): Boolean = selfDestructCount > 0.0

    // -----------------------------------------------------------------
    // Reset helpers
    // -----------------------------------------------------------------

    /**
     * Full reset — wipe ALL fields back to zero/defaults, then call [super.reset]
     * to zero the [GameObjectBase] fields.
     *
     * This is the pool-safe reset: after calling it the object is indistinguishable
     * from a freshly constructed one.  Identity fields (name, hostname, rank) are
     * also cleared — set them again after [ObjectPool.allocate] returns.
     *
     * Mirrors the C server's zero-initialisation of `player_t` slots on (re)use.
     */
    override fun reset() {
        super.reset()

        // Identity
        plType = PlayerType.HUMAN
        plTypeMyChar = ' '
        plOldStatus = 0u

        // State
        plStatus = 0u
        plState = PlayerState.UNDEFINED
        plLife = 0
        plDeathsSinceJoin = 0
        plPrevTeam = 0u

        // Physics / movement
        turnspeed = 0.0
        velocity = 0.0
        setFloatDir(0.0)
        wantedFloatDir = 0.0
        turnresistance = 0.0
        turnvel = 0.0
        oldTurnvel = 0.0
        turnacc = 0.0
        dir = 0

        // Score / kills
        score = 0.0
        updateScore = false
        kills = 0
        deaths = 0

        // Equipment
        have = 0L
        used = 0L
        shieldTime = 0.0
        fuel.reset()
        emptyMass = 0.0
        power = 0.0
        powerS = 0.0
        turnspeedS = 0.0
        turnresistanceS = 0.0
        sensorRange = 0.0
        shots = 0
        missileRack = 0
        numPulses = 0
        emergencyThrustLeft = 0.0
        emergencyShieldLeft = 0.0
        phasingLeft = 0.0
        pauseCount = 0.0
        recoveryCount = 0.0
        selfDestructCount = 0.0
        item.fill(0)
        initialItem.fill(0)
        loseItem = 0
        loseItemState = 0
        autoPowerS = 0.0
        autoTurnspeedS = 0.0
        autoTurnresistanceS = 0.0
        for (i in modbank.indices) modbank[i] = Modifiers.ZERO

        // Weapon timing
        shotTime = 0.0
        laserTime = 0.0
        didShoot = false
        tractorIsPressor = false

        // Checkpoints / race
        repairTarget = 0
        fs = 0
        check = 0
        prevCheck = 0
        time = 0
        round = 0
        prevRound = 0
        bestLap = 0
        lastLap = 0
        lastLapTime = 0
        lastCheckDir = 0
        lastWallTouch = 0L
        survivalTime = 0.0

        // Lock / base
        homeBase = null
        lock.reset()
        lockbank.fill(0)

        // Identity / network
        myChar = ' '
        name = ""
        userName = ""
        hostname = ""
        pseudoTeam = 0u
        alliance = 0
        invite = 0
        ball = null

        // Shove record
        for (i in shoveRecord.indices) shoveRecord[i] = Shove()
        shoveNext = 0

        // Visibility
        visibility = null
        forceVisible = 0.0
        damaged = 0.0
        stunned = 0.0
        updateVisibility = false

        // Update counters
        lastTargetUpdate = 0
        lastCannonUpdate = 0
        lastFuelUpdate = 0
        lastPolyStyleUpdate = 0
        ecmCount = 0

        // Network / session
        version = 0u
        lastKeyv.fill(false)
        prevKeyv.fill(false)
        frameLastBusy = 0L
        playerFps = 0
        maxTurnsps = 0
        recType = 0
        rank = null
        pauseTime = 0.0
        idleTime = 0.0
        flooding = 0
        muted = false
        isOperator = false
        wantAudio = false
        privs = 0
        snafuCount = 0.0
        ship = null
    }

    /**
     * Respawn reset — wipe only combat state; preserve identity and session
     * fields (name, hostname, rank, type, version, etc.).
     *
     * Call when a player dies and re-enters the game.  Identity fields set
     * at connection time are intentionally left intact.
     *
     * Mirrors C `Player_init_state` / `Player_death` patterns in server/player.c.
     */
    fun resetForRespawn() {
        // Reset base object fields (pos, vel, acc, life, mods, status, …)
        super.reset()

        // State
        plStatus = 0u
        plState = PlayerState.DEAD
        plOldStatus = 0u

        // Physics / movement
        setFloatDir(0.0)
        wantedFloatDir = 0.0
        turnvel = 0.0
        oldTurnvel = 0.0
        turnacc = 0.0
        dir = 0

        // Equipment
        have = 0L
        used = 0L
        shieldTime = 0.0
        fuel.reset()
        power = 0.0
        powerS = 0.0
        shots = 0
        missileRack = 0
        numPulses = 0
        emergencyThrustLeft = 0.0
        emergencyShieldLeft = 0.0
        phasingLeft = 0.0
        pauseCount = 0.0
        recoveryCount = 0.0
        selfDestructCount = 0.0
        item.fill(0)
        loseItem = 0
        loseItemState = 0
        for (i in modbank.indices) modbank[i] = Modifiers.ZERO

        // Weapon timing
        shotTime = 0.0
        laserTime = 0.0
        didShoot = false
        tractorIsPressor = false

        // Lock
        lock.reset()
        lockbank.fill(0)
        ball = null

        // Shove record
        for (i in shoveRecord.indices) shoveRecord[i] = Shove()
        shoveNext = 0

        // Combat damage
        damaged = 0.0
        stunned = 0.0

        // Counters that should start fresh each life
        lastWallTouch = 0L
        survivalTime = 0.0
        ecmCount = 0
        snafuCount = 0.0
    }

    companion object {
        const val MAX_RECORDED_SHOVES: Int = 4

        /** Number of lock-bank slots (size of [lockbank] array). */
        const val LOCK_BANK_MAX: Int = 4

        // Privilege flags
        const val PRIV_NOAUTOKICK: Int = 1
        const val PRIV_AUTOKICKLAST: Int = 2
    }
}
