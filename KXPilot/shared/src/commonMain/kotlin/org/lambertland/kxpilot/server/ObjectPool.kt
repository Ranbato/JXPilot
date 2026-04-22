package org.lambertland.kxpilot.server

// ---------------------------------------------------------------------------
// Ported from: server/object.c  (Alloc_shots, Object_allocate, Object_free_ind)
// ---------------------------------------------------------------------------

/**
 * Fixed-capacity pool of pre-allocated game objects of type [T].
 *
 * ## Design
 * The C server pre-allocates a flat `anyobject_t[MAX_TOTAL_SHOTS]` array and
 * uses a **swap-to-end** free strategy: active objects occupy indices `0 until
 * count`; freeing an index swaps it with the last active slot and decrements
 * `count`.  This keeps iteration tight and free O(1).
 *
 * This class mirrors that pattern for a single type [T].  Mixed-type storage
 * is handled by instantiating one pool per concrete object type and letting
 * the game engine iterate each pool separately (see [ObjectPools]).
 *
 * ## Thread-safety
 * NOT thread-safe.  All access must occur on the game-loop coroutine
 * (single-threaded dispatcher).  No locks, no atomics.
 *
 * @param capacity Maximum number of live objects this pool can hold.
 * @param factory  Called once per slot at construction to pre-allocate instances.
 */
class ObjectPool<T : GameObjectBase>(
    val capacity: Int,
    factory: (index: Int) -> T,
) {
    /** Pre-allocated backing array; all slots always exist. */
    @Suppress("UNCHECKED_CAST")
    private val slots: Array<T> = Array<Any>(capacity) { factory(it) } as Array<T>

    /** Number of currently active (live) objects; active indices are `0 until count`. */
    private var _count: Int = 0

    val count: Int get() = _count

    // ------------------------------------------------------------------
    // Allocation / free
    // ------------------------------------------------------------------

    /**
     * Mark the next free slot as active, call [GameObjectBase.reset] on it,
     * and return it — or return `null` if the pool is full.
     *
     * `reset()` is called automatically so callers always receive an object in
     * its canonical zero-state.  This matches C `Object_allocate` which
     * zero-initialises the slot before returning it.
     */
    fun allocate(): T? {
        if (_count >= capacity) return null
        val obj = slots[_count++]
        obj.reset()
        return obj
    }

    fun freeAt(index: Int) {
        require(index in 0 until _count) {
            "ObjectPool.freeAt: index $index out of active range 0 until $_count"
        }
        _count--
        val tmp = slots[index]
        slots[index] = slots[_count]
        slots[_count] = tmp
    }

    fun free(obj: T) {
        for (i in _count - 1 downTo 0) {
            if (slots[i] === obj) {
                freeAt(i)
                return
            }
        }
        error("ObjectPool.free: object not found in active slots")
    }

    // ------------------------------------------------------------------
    // Iteration helpers (iterate only active slots)
    // ------------------------------------------------------------------

    /** Iterate active objects. */
    fun forEach(action: (T) -> Unit) {
        var i = 0
        while (i < _count) action(slots[i++])
    }

    /**
     * Iterate active objects with index.
     *
     * **Note:** if [action] calls [freeAt] with the supplied index, the
     * slot is swapped and the same index will hold a different object next
     * iteration.  Use [forEachAlive] for that pattern.
     */
    fun forEachIndexed(action: (index: Int, obj: T) -> Unit) {
        var i = 0
        while (i < _count) {
            action(i, slots[i])
            i++
        }
    }

    /**
     * Iterate active objects, automatically handling swap-to-end frees.
     *
     * If [action] returns `true` the object is freed (via [freeAt]) and the
     * index is NOT advanced — the swapped-in object is visited next.
     * If [action] returns `false` the index advances normally.
     *
     * Mirrors the common C pattern:
     * ```c
     * for (i = 0; i < ObjCount; ) {
     *     if (should_free(Obj[i])) Object_free_ind(i);
     *     else i++;
     * }
     * ```
     */
    fun forEachAlive(action: (T) -> Boolean) {
        var i = 0
        while (i < _count) {
            if (action(slots[i])) freeAt(i) else i++
        }
    }

    /** Return the active object at [index].  No bounds check in release. */
    operator fun get(index: Int): T = slots[index]
}

// ---------------------------------------------------------------------------
// ObjectPools — typed sub-pools for all game object kinds
// ---------------------------------------------------------------------------

/**
 * Container for all per-type object pools.
 *
 * Each field is a separate [ObjectPool] so that iteration over one kind of
 * object (e.g. all shots) does not pay the cost of skipping other kinds.
 * This replaces the C `anyobject_t[]` union array with type-safe pools.
 *
 * Capacity constants mirror `serverconst.h`.
 */
class ObjectPools(
    shotCapacity: Int = MAX_SHOTS,
    mineCapacity: Int = MAX_MINES,
    missileCapacity: Int = MAX_MISSILES,
    smartCapacity: Int = MAX_SMART,
    torpCapacity: Int = MAX_TORPS,
    heatCapacity: Int = MAX_HEAT,
    ballCapacity: Int = MAX_BALLS,
    wireCapacity: Int = MAX_WIRE,
    pulseCapacity: Int = MAX_PULSES,
    itemCapacity: Int = MAX_ITEMS,
) {
    val shots: ObjectPool<GameObject> = ObjectPool(shotCapacity) { GameObject() }
    val mines: ObjectPool<MineObject> = ObjectPool(mineCapacity) { MineObject() }
    val missiles: ObjectPool<MissileObject> = ObjectPool(missileCapacity) { MissileObject() }
    val smart: ObjectPool<SmartObject> = ObjectPool(smartCapacity) { SmartObject() }
    val torps: ObjectPool<TorpObject> = ObjectPool(torpCapacity) { TorpObject() }
    val heat: ObjectPool<HeatObject> = ObjectPool(heatCapacity) { HeatObject() }
    val balls: ObjectPool<BallObject> = ObjectPool(ballCapacity) { BallObject() }
    val wire: ObjectPool<WireObject> = ObjectPool(wireCapacity) { WireObject() }
    val pulses: ObjectPool<PulseObject> = ObjectPool(pulseCapacity) { PulseObject() }
    val items: ObjectPool<ItemObject> = ObjectPool(itemCapacity) { ItemObject() }

    /** Total active objects across all pools. */
    val totalCount: Int get() =
        shots.count + mines.count + missiles.count + smart.count +
            torps.count + heat.count + balls.count + wire.count +
            pulses.count + items.count

    companion object {
        // Capacities mirror serverconst.h.
        // Derivation notes:
        //   MAX_SHOTS  = 2048 — up to ~16 players × 128 shots each at max fire rate.
        //   MAX_MINES  =  256 — 1 mine per item slot × max players, with headroom.
        //   MAX_MISSILES = 256 — same reasoning as mines; missiles are rarer.
        //   MAX_SMART  =  256 — smart missiles; same as MAX_MISSILES for symmetry.
        //   MAX_TORPS  =  256 — torpedoes; same as MAX_MISSILES for symmetry.
        //   MAX_HEAT   =  256 — heat-seekers; same as MAX_MISSILES for symmetry.
        //   MAX_BALLS  =   32 — one ball per treasure tile; maps rarely exceed 32.
        //   MAX_WIRE   =  512 — wreckage + asteroid debris fragments; two per object
        //                       gives ~256 simultaneous wreckage events.
        //   MAX_PULSES =  512 — laser pulses; cannons + players can fire simultaneously.
        //   MAX_ITEMS  =  256 — item objects; one per item tile with generous headroom.
        const val MAX_SHOTS: Int = 2048
        const val MAX_MINES: Int = 256
        const val MAX_MISSILES: Int = 256
        const val MAX_SMART: Int = 256
        const val MAX_TORPS: Int = 256
        const val MAX_HEAT: Int = 256
        const val MAX_BALLS: Int = 32
        const val MAX_WIRE: Int = 512 // wreckage + asteroids
        const val MAX_PULSES: Int = 512
        const val MAX_ITEMS: Int = 256
    }
}
