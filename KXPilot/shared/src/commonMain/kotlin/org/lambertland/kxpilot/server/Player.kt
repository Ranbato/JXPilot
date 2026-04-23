package org.lambertland.kxpilot.server

import org.lambertland.kxpilot.common.ClPos
import org.lambertland.kxpilot.common.GameConst
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
 * TODO (#20): decouple physics state (pos, vel, dir, fuel, shields, items, …)
 *             from network/identity fields (sessionId, nick, team, score, …).
 *             The current god-object design makes unit testing physics in
 *             isolation unnecessarily difficult.
 *
 * This is intentionally a mutable class — the server mutates player state
 * every frame.  [data class] would be wrong here.
 */
class Player : GameObjectBase() {
    // -----------------------------------------------------------------------
    // Physics state — all per-tick movement/combat fields live here so
    // ServerPhysics.tickPlayer can be tested without a full Player instance.
    // (#20 decoupling)
    // -----------------------------------------------------------------------
    val physics: PhysicsState = PhysicsState()

    // Delegating properties: forward reads/writes to physics so all existing
    // call sites outside ServerPhysics continue to compile unchanged.

    // State
    // UShort/UByte use explicit get/set instead of `by physics::` delegation:
    // property delegation for inline/unsigned types is unreliable on Kotlin/Native
    // and Kotlin/JS due to differences in boxing through KMutableProperty0.
    var plStatus: UShort
        get() = physics.plStatus
        set(v) {
            physics.plStatus = v
        }
    var plState: PlayerState by physics::plState
    var plOldStatus: UByte
        get() = physics.plOldStatus
        set(v) {
            physics.plOldStatus = v
        }

    // Turning
    var turnspeed: Double by physics::turnspeed
    var turnresistance: Double by physics::turnresistance
    var turnvel: Double by physics::turnvel
    var oldTurnvel: Double by physics::oldTurnvel
    var turnacc: Double by physics::turnacc
    var wantedFloatDir: Double by physics::wantedFloatDir
    var dir: Short by physics::dir

    // Float direction — forward through physics
    val floatDir: Double get() = physics.floatDir
    val floatDirCos: Double get() = physics.floatDirCos
    val floatDirSin: Double get() = physics.floatDirSin

    fun setFloatDir(angle: Double) = physics.setFloatDir(angle)

    fun floatDirInRes(): Double = physics.floatDirInRes()

    // Thrust / power
    var power: Double by physics::power
    var powerS: Double by physics::powerS
    var turnspeedS: Double by physics::turnspeedS
    var turnresistanceS: Double by physics::turnresistanceS
    var sensorRange: Double by physics::sensorRange
    var velocity: Double by physics::velocity

    // Fuel
    val fuel: PlayerFuel get() = physics.fuel
    var emptyMass: Double by physics::emptyMass

    // Items / abilities
    var have: Long by physics::have
    var used: Long by physics::used
    var shieldTime: Double by physics::shieldTime
    var shots: Int by physics::shots
    var missileRack: Int by physics::missileRack
    var numPulses: Int by physics::numPulses
    var emergencyThrustLeft: Double by physics::emergencyThrustLeft
    var emergencyShieldLeft: Double by physics::emergencyShieldLeft
    var phasingLeft: Double by physics::phasingLeft
    var pauseCount: Double by physics::pauseCount
    var recoveryCount: Double by physics::recoveryCount
    var selfDestructCount: Double by physics::selfDestructCount
    val item: IntArray get() = physics.item
    val initialItem: IntArray get() = physics.initialItem
    var loseItem: Int by physics::loseItem
    var loseItemState: Int by physics::loseItemState
    var autoPowerS: Double by physics::autoPowerS
    var autoTurnspeedS: Double by physics::autoTurnspeedS
    var autoTurnresistanceS: Double by physics::autoTurnresistanceS
    val modbank: Array<Modifiers> get() = physics.modbank

    // Weapon timing
    var shotTime: Double by physics::shotTime
    var laserTime: Double by physics::laserTime
    var didShoot: Boolean by physics::didShoot

    // Input
    val lastKeyv: BooleanArray get() = physics.lastKeyv

    // Collision memory
    var lastSafePos: ClPos by physics::lastSafePos
    var lastWallTouch: Long by physics::lastWallTouch

    // Refueling
    var refuelTarget: Fuel? by physics::refuelTarget

    // -----------------------------------------------------------------------
    // Identity / network — delegated to PlayerIdentity (D1)
    // -----------------------------------------------------------------------

    /** Extracted identity/session data — see [PlayerIdentity] for field docs. */
    val identity: PlayerIdentity = PlayerIdentity()

    // Delegating properties: forward reads/writes to identity so all existing
    // call sites outside PlayerIdentity continue to compile unchanged.
    var myChar: Char by identity::myChar
    var name: String by identity::name
    var userName: String by identity::userName
    var hostname: String by identity::hostname
    var pseudoTeam: UShort
        get() = identity.pseudoTeam
        set(v) {
            identity.pseudoTeam = v
        }
    var alliance: Int by identity::alliance
    var invite: Int by identity::invite
    var plType: PlayerType by identity::plType
    var plTypeMyChar: Char by identity::plTypeMyChar
    var version: UInt
        get() = identity.version
        set(v) {
            identity.version = v
        }
    var muted: Boolean by identity::muted
    var isOperator: Boolean by identity::isOperator
    var wantAudio: Boolean by identity::wantAudio
    var privs: Int by identity::privs
    var rank: RankNode? by identity::rank

    var plLife: Int = 0
    var plDeathsSinceJoin: Int = 0
    var plPrevTeam: UShort = 0u

    // Score / kills
    var score: Double = 0.0
    var updateScore: Boolean = false
    var kills: Int = 0
    var deaths: Int = 0

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
    var survivalTime: Double = 0.0

    var homeBase: Base? = null

    var lock: LockInfo = LockInfo()
    val lockbank: IntArray = IntArray(LOCK_BANK_MAX)

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

    /** Previous-frame key-press state (network/identity field — not in PhysicsState). */
    val prevKeyv: BooleanArray = BooleanArray(org.lambertland.kxpilot.common.Key.NUM_KEYS)

    var frameLastBusy: Long = 0L

    var playerFps: Int = 0
    var maxTurnsps: Int = 0
    var recType: Int = 0

    var pauseTime: Double = 0.0
    var idleTime: Double = 0.0
    var flooding: Int = 0

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

        // Reset all physics fields via PhysicsState
        physics.reset()

        // Identity
        plType = PlayerType.HUMAN
        plTypeMyChar = ' '
        plLife = 0
        plDeathsSinceJoin = 0
        plPrevTeam = 0u

        // Score / kills
        score = 0.0
        updateScore = false
        kills = 0
        deaths = 0

        // Non-physics combat flags
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
        survivalTime = 0.0

        // Lock / base
        homeBase = null
        lock.reset()
        lockbank.fill(0)
        ball = null

        // Identity (via PlayerIdentity)
        identity.myChar = ' '
        identity.name = ""
        identity.userName = ""
        identity.hostname = ""
        identity.pseudoTeam = 0u
        identity.alliance = 0
        identity.invite = 0
        identity.plType = PlayerType.HUMAN
        identity.plTypeMyChar = ' '
        identity.version = 0u
        identity.muted = false
        identity.isOperator = false
        identity.wantAudio = false
        identity.privs = 0
        identity.rank = null

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
        prevKeyv.fill(false)
        frameLastBusy = 0L
        playerFps = 0
        maxTurnsps = 0
        recType = 0
        pauseTime = 0.0
        idleTime = 0.0
        flooding = 0
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
    // R26: resetForRespawn is called exclusively from the server game loop on the
    // single game-loop coroutine.  No concurrent access occurs during reset, so no
    // @Volatile or synchronisation is required.  If this invariant ever changes
    // (e.g. admin commands resetting a live player from a different coroutine),
    // plState must be made @Volatile or the whole reset sequence must be
    // synchronised externally.
    fun resetForRespawn() {
        // Set APPEARING *before* physics.reset() to ensure the state machine
        // never transiently exposes UNDEFINED to a concurrent game-loop reader.
        // (physics.reset() would set plState = UNDEFINED; we override it here first
        //  so any concurrent reader between reset() and the explicit assignment below
        //  always sees APPEARING, not UNDEFINED.)
        plState = PlayerState.APPEARING

        // Reset base object fields (pos, vel, acc, life, mods, status, …)
        super.reset()

        // Reset all physics fields via PhysicsState.  plState was already set
        // to APPEARING above; physics.reset() will overwrite it to UNDEFINED,
        // so we re-assert APPEARING immediately after.
        physics.reset()

        // Re-assert correct post-reset state — physics.reset() zeroed plState.
        plStatus = 0u
        plState = PlayerState.APPEARING
        plOldStatus = 0u

        // Non-physics combat flags
        tractorIsPressor = false

        // Lock / base
        homeBase = null
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
