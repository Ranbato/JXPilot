package org.lambertland.kxpilot.server

import org.lambertland.kxpilot.common.ClPos
import org.lambertland.kxpilot.common.Vector

// ---------------------------------------------------------------------------
// Ported from: server/object.h
// ---------------------------------------------------------------------------

// ------------------------------------------------------------------
// Object type constants
// ------------------------------------------------------------------

/**
 * Server game-object type codes.
 * Maps to `OBJ_*` `#define` constants in server/object.h.
 */
enum class ObjType(
    val code: Int,
) {
    PLAYER(0),
    DEBRIS(1),
    SPARK(2),
    BALL(3),
    SHOT(4),
    SMART_SHOT(5),
    MINE(6),
    TORPEDO(7),
    HEAT_SHOT(8),
    PULSE(9),
    ITEM(10),
    WRECKAGE(11),
    ASTEROID(12),
    CANNON_SHOT(13),
    ;

    init {
        require(code in 0..31) {
            "ObjType code $code is out of range 0..31 (UInt shl limit)"
        }
    }

    val bit: UInt get() = 1u shl code

    companion object {
        private val byCode = entries.associateBy { it.code }

        fun fromCode(code: Int): ObjType? = byCode[code]
    }
}

/**
 * Object status bit-flags (stored in [GameObjectBase.objStatus]).
 * Maps to `GRAVITY`, `WARPING`, … constants in server/object.h.
 *
 * Stored as `const val Int` so the values are compile-time constants with no
 * heap allocation.  [GameObjectBase.objStatus] is `UShort`; cast with
 * `.toUShort()` when assigning, and use `.toInt()` when testing bits.
 */
object ObjStatus {
    const val GRAVITY: Int = 1 shl 0
    const val WARPING: Int = 1 shl 1
    const val WARPED: Int = 1 shl 2
    const val CONFUSED: Int = 1 shl 3
    const val FROMCANNON: Int = 1 shl 4
    const val RECREATE: Int = 1 shl 5
    const val THRUSTING: Int = 1 shl 6
    const val OWNERIMMUNE: Int = 1 shl 7
    const val NOEXPLOSION: Int = 1 shl 8
    const val COLLISIONSHOVE: Int = 1 shl 9
    const val RANDOM_ITEM: Int = 1 shl 10
}

/**
 * Lock mode constants.
 * Maps to `LOCK_NONE`, `LOCK_PLAYER`, `LOCK_VISIBLE` in server/object.h.
 *
 * [BANK_MAX] has been moved to [Player.LOCK_BANK_MAX] — it is a capacity
 * constant governing the lock-bank array size, not a lock-mode flag.
 */
object LockMode {
    const val NONE: Int = 0x00
    const val PLAYER: Int = 0x01
    const val VISIBLE: Int = 0x02
}

// ------------------------------------------------------------------
// Object base – fields shared by all game objects and players
// (corresponds to OBJECT_BASE + OBJECT_EXTEND macros)
// ------------------------------------------------------------------

/**
 * Fields common to every server game object (and player).
 * Maps to the `OBJECT_BASE` and `OBJECT_EXTEND` C macros in server/object.h.
 *
 * This is an abstract class rather than an interface so that subclasses
 * can be instantiated with concrete mutable state while sharing the
 * inheritance chain used by the C struct-embedding pattern.
 */
abstract class GameObjectBase {
    // OBJECT_BASE fields
    var id: Short = 0
    var team: UShort = 0u
    var pos: ClPos = ClPos.ZERO
    var prevPos: ClPos = ClPos.ZERO
    var extMove: ClPos = ClPos.ZERO
    var wallTime: Float = 0f
    var vel: Vector = Vector.ZERO
    var acc: Vector = Vector.ZERO
    var mass: Float = 0f
    var life: Float = 0f
    var mods: Modifiers = Modifiers.ZERO
    var type: UByte = 0u
    var color: UByte = 0u
    var collMode: UByte = 0u
    var missileDir: UByte = 0u
    var wormHoleHit: Short = 0
    var wormHoleDest: Short = 0
    var objStatus: UShort = 0u

    // OBJECT_EXTEND fields
    var plRange: Short = 0
    var plRadius: Short = 0
    var fuse: Float = 0f

    // ------------------------------------------------------------------
    // Lifecycle helpers
    // ------------------------------------------------------------------

    /** True while this object has remaining life (life > 0). */
    val hasLife: Boolean get() = life > 0f

    /**
     * Decrement [life] by one tick.
     * Returns `true` if the object expired this tick (life just reached ≤ 0).
     * The caller is responsible for returning the object to its [ObjectPool].
     */
    fun tickLife(): Boolean {
        if (life > 0f) life -= 1f
        return life <= 0f
    }

    /**
     * Reset all base fields to defaults before reuse from an [ObjectPool].
     *
     * Subclasses should override and call `super.reset()` to also wipe their
     * own extra fields.
     *
     * This mirrors the implicit zero-initialisation the C server relies on
     * when reusing pre-allocated `anyobject_t` slots.
     */
    open fun reset() {
        id = 0
        team = 0u
        pos = ClPos.ZERO
        prevPos = ClPos.ZERO
        extMove = ClPos.ZERO
        wallTime = 0f
        vel = Vector.ZERO
        acc = Vector.ZERO
        mass = 0f
        life = 0f
        mods = Modifiers.ZERO
        type = 0u
        color = 0u
        collMode = 0u
        missileDir = 0u
        wormHoleHit = 0
        wormHoleDest = 0
        objStatus = 0u
        plRange = 0
        plRadius = 0
        fuse = 0f
    }
}

// ------------------------------------------------------------------
// Concrete game-object types
// ------------------------------------------------------------------

/**
 * Generic game object.  Maps to C `object_t`.
 *
 * **Pool-eligible:** used by [ObjectPool] for shots and generic debris.
 * Final — do not subclass.  If a new object variant needs extra state,
 * extend [GameObjectBase] directly (same as [MineObject], [BallObject], etc.)
 * rather than subclassing [GameObject].  Subclassing would bypass the reset
 * contract enforced by [ObjectPool.allocate].
 */
class GameObject : GameObjectBase()

/** Mine object.  Maps to C `mineobject_t`.  Pool-eligible (mines pool). */
class MineObject : GameObjectBase() {
    var mineCount: Float = 0f
    var mineEcmRange: Float = 0f
    var mineSpreadLeft: Float = 0f
    var mineOwner: Short = 0

    override fun reset() {
        super.reset()
        mineCount = 0f
        mineEcmRange = 0f
        mineSpreadLeft = 0f
        mineOwner = 0
    }
}

/**
 * Base for all missile types; holds shared missile fields.
 *
 * **Not pool-eligible directly** — do not instantiate [MissileObjectBase] in
 * an [ObjectPool].  Use the concrete subclasses ([MissileObject],
 * [SmartObject], [TorpObject], [HeatObject]) which each have their own pool
 * in [ObjectPools].  [MissileObjectBase] exists only to share the two common
 * fields and their reset logic.
 */
open class MissileObjectBase : GameObjectBase() {
    var missileMaxSpeed: Float = 0f
    var missileTurnspeed: Float = 0f

    override fun reset() {
        super.reset()
        missileMaxSpeed = 0f
        missileTurnspeed = 0f
    }
}

/**
 * Generic missile.  Maps to C `missileobject_t`.  Pool-eligible (missiles pool).
 *
 * Final: no fields to add. Subclass [MissileObjectBase] if new missile
 * variants need extra state; do not subclass [MissileObject] itself.
 */
class MissileObject : MissileObjectBase()

/** Smart missile.  Maps to C `smartobject_t`.  Pool-eligible (smart pool). */
class SmartObject : MissileObjectBase() {
    var smartEcmRange: Float = 0f
    var smartCount: Float = 0f
    var smartLockId: Short = 0
    var smartRelockId: Short = 0

    override fun reset() {
        super.reset()
        smartEcmRange = 0f
        smartCount = 0f
        smartLockId = 0
        smartRelockId = 0
    }
}

/** Torpedo.  Maps to C `torpobject_t`.  Pool-eligible (torps pool). */
class TorpObject : MissileObjectBase() {
    var torpSpreadLeft: Float = 0f
    var torpCount: Float = 0f

    override fun reset() {
        super.reset()
        torpSpreadLeft = 0f
        torpCount = 0f
    }
}

/** Heat-seeker missile.  Maps to C `heatobject_t`.  Pool-eligible (heat pool). */
class HeatObject : MissileObjectBase() {
    var heatCount: Float = 0f
    var heatLockId: Short = 0

    override fun reset() {
        super.reset()
        heatCount = 0f
        heatLockId = 0
    }
}

/** Ball (ballgame object).  Maps to C `ballobject_t`.  Pool-eligible (balls pool). */
class BallObject : GameObjectBase() {
    var ballLooseTicks: Double = 0.0
    var ballTreasure: Treasure? = null
    var ballOwner: Short = 0
    var ballStyle: Short = 0

    override fun reset() {
        super.reset()
        ballLooseTicks = 0.0
        ballTreasure = null
        ballOwner = 0
        ballStyle = 0
    }
}

/** Wire-frame rotating object (wreckage, asteroid).  Maps to C `wireobject_t`.  Pool-eligible (wire pool). */
class WireObject : GameObjectBase() {
    var wireTurnspeed: Float = 0f
    var wireType: UByte = 0u
    var wireSize: UByte = 0u
    var wireRotation: UByte = 0u

    override fun reset() {
        super.reset()
        wireTurnspeed = 0f
        wireType = 0u
        wireSize = 0u
        wireRotation = 0u
    }
}

/** Laser pulse.  Maps to C `pulseobject_t`.  Pool-eligible (pulses pool). */
class PulseObject : GameObjectBase() {
    var pulseLen: Float = 0f
    var pulseDir: UByte = 0u
    var pulseRefl: Boolean = false

    override fun reset() {
        super.reset()
        pulseLen = 0f
        pulseDir = 0u
        pulseRefl = false
    }
}

/** Pickable item floating in space.  Maps to C `itemobject_t`.  Pool-eligible (items pool). */
class ItemObject : GameObjectBase() {
    var itemType: Int = 0
    var itemCount: Int = 0

    override fun reset() {
        super.reset()
        itemType = 0
        itemCount = 0
    }
}

// Note: AnyObject sealed class was retired.
// The typed ObjectPool<T> hierarchy (see ObjectPool.kt) supersedes it.
// Each concrete type (GameObject, MineObject, etc.) is stored in its own
// pool and iterated directly — no wrapping union is needed.
