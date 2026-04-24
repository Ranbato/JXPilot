package org.lambertland.kxpilot.engine

/**
 * Minimal view of a target ship that [GameEngine] needs for lock-on, tractor
 * beam, and missile homing.  Implemented by [DemoShip] (and any future
 * networked-player type) so that [GameEngine] does not depend on the
 * demo-specific class directly.
 */
interface EngineTarget {
    val id: Int
    val x: Float
    val y: Float
    var vx: Float
    var vy: Float

    /** Whether the ship's shield is currently active (used for hit feedback). */
    var shield: Boolean

    /** Hit-points remaining.  When ≤ 0 the NPC is dead. */
    var hp: Float
}
