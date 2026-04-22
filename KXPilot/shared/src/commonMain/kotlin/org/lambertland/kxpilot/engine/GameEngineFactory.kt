package org.lambertland.kxpilot.engine

import kotlinx.coroutines.CoroutineScope
import org.lambertland.kxpilot.resources.XPilotMap
import org.lambertland.kxpilot.resources.toWorld

/**
 * Factory functions for constructing a [GameEngine] from high-level inputs.
 *
 * Kept separate from [GameEngine] so the engine class itself has no direct
 * dependency on the resource-layer ([XPilotMap]).
 */
object GameEngineFactory {
    /**
     * Build a [GameEngine] from a parsed [XPilotMap].
     *
     * The map is converted to a [World][org.lambertland.kxpilot.server.World]
     * via [XPilotMap.toWorld], the player is spawned at the first available
     * base, and one treasure ball is spawned per map treasure tile.
     *
     * @param map        The parsed map.
     * @param baseIndex  Which base to spawn at (0 = first).
     * @param scope      Optional [CoroutineScope] that will own the game loop.
     *                   If provided, [GameEngine.startLoop] is **not** called here —
     *                   the caller should call it with the desired [onTick] callback.
     *                   This parameter is reserved for factory variants that wire
     *                   up the loop automatically (e.g. a headless server mode).
     */
    fun fromMap(
        map: XPilotMap,
        baseIndex: Int = 0,
        @Suppress("UNUSED_PARAMETER") scope: CoroutineScope? = null,
    ): GameEngine {
        val world = map.toWorld()
        val engine = GameEngine(world)
        engine.spawnAtBase(baseIndex)
        engine.spawnBallsFromTreasures(
            map.treasures.map { TreasurePlacement(blockX = it.x, blockY = it.y, team = it.team) },
        )
        return engine
    }
}
