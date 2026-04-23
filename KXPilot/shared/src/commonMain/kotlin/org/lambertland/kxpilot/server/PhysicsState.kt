package org.lambertland.kxpilot.server

import org.lambertland.kxpilot.common.ClPos
import org.lambertland.kxpilot.common.GameConst
import org.lambertland.kxpilot.common.Item
import org.lambertland.kxpilot.common.Key

// ---------------------------------------------------------------------------
// PhysicsState
//
// All per-tick mutable physics fields for a player, extracted from the Player
// god-object so that ServerPhysics.tickPlayer can be exercised without a full
// Player instance (no network, no identity, no score bookkeeping).
//
// Player holds a `val physics: PhysicsState` and exposes delegating properties
// for every field here to preserve backward-compatibility with all existing
// call sites.
//
// Fields kept in Player (not here):
//   - Identity / network:  name, nick, userName, hostname, myChar, rank, muted,
//                          isOperator, wantAudio, privs, flooding, version,
//                          playerFps, maxTurnsps, recType, pauseTime, idleTime,
//                          playerFps, frameLastBusy, prevKeyv
//   - Score / bookkeeping: score, kills, deaths, plLife, plDeathsSinceJoin,
//                          updateScore, survivalTime, check, prevCheck, time,
//                          round, prevRound, bestLap, lastLap, lastLapTime,
//                          lastCheckDir, ecmCount, snafuCount, fs,
//                          tractorIsPressor, repairTarget
//   - Social / world:      team (also in GameObjectBase), lock, lockbank, ball,
//                          shoveRecord, shoveNext, visibility, forceVisible,
//                          updateVisibility, lastTargetUpdate, lastCannonUpdate,
//                          lastFuelUpdate, lastPolyStyleUpdate, homeBase,
//                          alliance, invite, pseudoTeam, ship
//   - GameObjectBase:      pos, vel, acc, mass, objStatus (passed separately)
// ---------------------------------------------------------------------------

/**
 * Per-tick mutable physics state for a player.
 *
 * Passed to [ServerPhysics.tickPlayer] so the physics engine does not need a
 * full [Player] object.  Construct one directly in unit tests to verify
 * physics behaviour without any network or identity scaffolding.
 */
class PhysicsState {
    // -----------------------------------------------------------------------
    // State machine
    // -----------------------------------------------------------------------
    var plState: PlayerState = PlayerState.UNDEFINED
    var plStatus: UShort = 0u
    var plOldStatus: UByte = 0u

    // -----------------------------------------------------------------------
    // Turning
    // -----------------------------------------------------------------------
    var turnspeed: Double = 0.0
    var turnresistance: Double = 0.0
    var turnvel: Double = 0.0
    var oldTurnvel: Double = 0.0
    var turnacc: Double = 0.0

    // Float direction with cached trig (matches Player invariant)
    private var _floatDir: Double = 0.0
    private var _floatDirCos: Double = 1.0
    private var _floatDirSin: Double = 0.0

    val floatDir: Double get() = _floatDir
    val floatDirCos: Double get() = _floatDirCos
    val floatDirSin: Double get() = _floatDirSin

    fun setFloatDir(angle: Double) {
        _floatDir = angle
        _floatDirCos = kotlin.math.cos(angle)
        _floatDirSin = kotlin.math.sin(angle)
    }

    fun floatDirInRes(): Double {
        val v = _floatDir / (2.0 * GameConst.PI_VALUE) * GameConst.RES
        return ((v % GameConst.RES) + GameConst.RES) % GameConst.RES
    }

    var wantedFloatDir: Double = 0.0
    var dir: Short = 0

    // -----------------------------------------------------------------------
    // Thrust / power
    // -----------------------------------------------------------------------
    var power: Double = 0.0
    var powerS: Double = 0.0
    var turnspeedS: Double = 0.0
    var turnresistanceS: Double = 0.0
    var sensorRange: Double = 0.0
    var velocity: Double = 0.0

    // -----------------------------------------------------------------------
    // Fuel
    // -----------------------------------------------------------------------
    var fuel: PlayerFuel = PlayerFuel()
    var emptyMass: Double = 0.0

    // -----------------------------------------------------------------------
    // Items / abilities in use
    // -----------------------------------------------------------------------

    /**
     * Bit-set of items/abilities currently **had** by this player.
     * Maps to C `unsigned int have` in player.h.
     */
    var have: Long = 0L

    /**
     * Bit-set of items/abilities currently **in use** by this player.
     * Maps to C `unsigned int used` in player.h.
     */
    var used: Long = 0L

    var shieldTime: Double = 0.0
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

    // -----------------------------------------------------------------------
    // Weapon timing
    // -----------------------------------------------------------------------
    var shotTime: Double = 0.0
    var laserTime: Double = 0.0
    var didShoot: Boolean = false

    // -----------------------------------------------------------------------
    // Input
    // -----------------------------------------------------------------------

    /** Key-press state bit-vector (size = Key.NUM_KEYS). */
    val lastKeyv: BooleanArray = BooleanArray(Key.NUM_KEYS)

    // -----------------------------------------------------------------------
    // Collision memory
    // -----------------------------------------------------------------------

    /**
     * Last click-space position confirmed to be outside a solid block.
     * Initialised to the player's spawn position; updated each free-movement tick.
     * Used as the snap-to position when a diagonal corner-entry cannot be resolved
     * by reversing a single axis.
     */
    var lastSafePos: ClPos = ClPos(0, 0)

    /** Frame counter at which the player last touched a wall. */
    var lastWallTouch: Long = 0L

    /**
     * Session id of the last player to physically contact this player within
     * a small frame window before a wall kill (used for wall-death attacker credit).
     * Null if no recent contact.  Populated by [ServerPhysics.tickPlayerCollisions]
     * and consumed by [ServerController.tickWorld] when a wall kill is detected.
     */
    var lastWallAttacker: Int? = null

    // -----------------------------------------------------------------------
    // Refueling
    // -----------------------------------------------------------------------

    /** The fuel depot this player is currently docked to, or null. */
    var refuelTarget: Fuel? = null

    // -----------------------------------------------------------------------
    // Convenience predicates (mirror those on Player)
    // -----------------------------------------------------------------------
    fun isAlive(): Boolean = plState == PlayerState.ALIVE

    fun isAppearing(): Boolean = plState == PlayerState.APPEARING

    fun isKilled(): Boolean = plState == PlayerState.KILLED

    fun isPhasing(): Boolean = (used and PlayerAbility.PHASING_DEVICE) != 0L

    fun isCloaked(): Boolean = (used and PlayerAbility.CLOAKING_DEVICE) != 0L

    fun isSelfDestructing(): Boolean = selfDestructCount > 0.0

    // -----------------------------------------------------------------------
    // Reset
    // -----------------------------------------------------------------------

    /**
     * Zero all fields back to defaults.
     *
     * **Internal use only.** After calling this, [lastSafePos] is `ClPos(0, 0)` —
     * a position that may be inside a wall.  Callers **must** immediately set
     * [lastSafePos] to a valid spawn position before the physics engine runs, or
     * a diagonal wall bounce will teleport the player to (0, 0).
     *
     * Use [Player.reset] or [Player.resetForRespawn] rather than calling this directly.
     */
    internal fun reset() {
        plState = PlayerState.UNDEFINED
        plStatus = 0u
        plOldStatus = 0u
        turnspeed = 0.0
        turnresistance = 0.0
        turnvel = 0.0
        oldTurnvel = 0.0
        turnacc = 0.0
        setFloatDir(0.0)
        wantedFloatDir = 0.0
        dir = 0
        power = 0.0
        powerS = 0.0
        turnspeedS = 0.0
        turnresistanceS = 0.0
        sensorRange = 0.0
        velocity = 0.0
        fuel.reset()
        emptyMass = 0.0
        have = 0L
        used = 0L
        shieldTime = 0.0
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
        shotTime = 0.0
        laserTime = 0.0
        didShoot = false
        lastKeyv.fill(false)
        lastSafePos = ClPos(0, 0)
        lastWallTouch = 0L
        lastWallAttacker = null
        refuelTarget = null
    }
}
