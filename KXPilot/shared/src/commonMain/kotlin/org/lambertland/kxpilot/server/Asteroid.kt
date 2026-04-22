package org.lambertland.kxpilot.server

import org.lambertland.kxpilot.common.GameConst

/**
 * Asteroid size and physics constants.  Maps to `ASTEROID_*` defines in `server/asteroid.h`.
 *
 * Omitted (runtime-dependent or random):
 *  - `ASTEROID_BASE_MASS` — depends on `options.shipMass` at server start.
 *  - `ASTEROID_START_SPEED` — depends on `rfrac()` (random fraction) at spawn.
 *  - `ASTEROID_MIN_DIST` — depends on `BLOCK_CLICKS` at runtime resolution.
 *  - `ASTEROID_RADIUS(size)` — depends on `SHIP_SZ` and `CLICK` at runtime.
 *  - `ASTEROID_MASS(size)` — depends on `options.shipMass`.
 *  - `ASTEROID_FUEL_HIT(fuel, size)` — parameterised macro, not a constant.
 */
object AsteroidConst {
    /** Maximum size of an asteroid (sizes 1–4). */
    const val MAX_SIZE: Int = 4

    /**
     * Maximum angular deviation between child asteroids when breaking, in RES units.
     * C: `ASTEROID_DELTA_DIR = RES / 8` = 16 heading-units (≈ 45°).
     * Callers use this value directly (e.g. `heading + AsteroidConst.DELTA_DIR`).
     */
    const val DELTA_DIR: Int = GameConst.RES / 8

    /** Lifetime of an asteroid in ticks before it breaks apart on its own. */
    const val LIFE: Int = 1000

    /**
     * Mass of dust debris produced when an asteroid is struck.
     * C: `ASTEROID_DUST_MASS = 0.25`
     */
    const val DUST_MASS: Double = 0.25

    /**
     * Velocity exchange fraction for the asteroid–dust momentum split.
     * C: `ASTEROID_DUST_FACT = 1 / (1 + 2 / ASTEROID_DUST_MASS)` = 1/9 ≈ 0.1111.
     * Derived from conservation of momentum; the dust carries this fraction
     * of the relative velocity of the collision.
     */
    const val DUST_FACT: Double = 1.0 / (1.0 + 2.0 / DUST_MASS)

    /**
     * Number of hits required to break an asteroid of the given [size].
     * C: `ASTEROID_HITS(size) = 1 << (size - 1)`
     * Size 1 → 1 hit, size 2 → 2 hits, size 3 → 4 hits, size 4 → 8 hits.
     */
    fun hitsRequired(size: Int): Int = 1 shl (size - 1)
}
