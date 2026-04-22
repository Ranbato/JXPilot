package org.lambertland.kxpilot

import org.lambertland.kxpilot.common.ClickConst
import org.lambertland.kxpilot.common.GameConst
import org.lambertland.kxpilot.engine.GameEngine
import org.lambertland.kxpilot.engine.GameEngineFactory
import org.lambertland.kxpilot.engine.ShotData
import org.lambertland.kxpilot.resources.BlockType
import org.lambertland.kxpilot.resources.MapBase
import org.lambertland.kxpilot.resources.MapTile
import org.lambertland.kxpilot.resources.XPilotMap
import org.lambertland.kxpilot.resources.toWorld
import org.lambertland.kxpilot.server.CellType
import org.lambertland.kxpilot.server.PlayerState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [XPilotMap.toWorld] and [GameEngine.fromMap] / [GameEngine.spawnAtBase].
 */
class MapToWorldTest {
    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun simpleMap(
        cols: Int = 10,
        rows: Int = 10,
        tiles: List<MapTile> = emptyList(),
        bases: List<MapBase> = emptyList(),
    ): XPilotMap =
        XPilotMap(
            name = "test",
            width = cols,
            height = rows,
            tiles = tiles,
            bases = bases,
        )

    // -----------------------------------------------------------------------
    // toWorld — dimensions
    // -----------------------------------------------------------------------

    @Test
    fun worldDimensionsMatchMap() {
        val map = simpleMap(cols = 10, rows = 8)
        val world = map.toWorld()
        assertEquals(10, world.x, "world.x should equal map columns")
        assertEquals(8, world.y, "world.y should equal map rows")
        assertEquals(10 * GameConst.BLOCK_SZ, world.width)
        assertEquals(8 * GameConst.BLOCK_SZ, world.height)
        assertEquals(world.width * ClickConst.CLICK, world.cwidth)
        assertEquals(world.height * ClickConst.CLICK, world.cheight)
    }

    // -----------------------------------------------------------------------
    // toWorld — tile types written to block array
    // -----------------------------------------------------------------------

    @Test
    fun filledTileWrittenToBlock() {
        val map =
            simpleMap(
                tiles = listOf(MapTile(3, 4, BlockType.FILLED)),
            )
        val world = map.toWorld()
        assertEquals(CellType.FILLED, world.getBlock(3, 4), "FILLED tile should write CellType.FILLED")
    }

    @Test
    fun diagonalTilesWrittenCorrectly() {
        val map =
            simpleMap(
                tiles =
                    listOf(
                        MapTile(0, 0, BlockType.REC_LU),
                        MapTile(1, 0, BlockType.REC_LD),
                        MapTile(2, 0, BlockType.REC_RU),
                        MapTile(3, 0, BlockType.REC_RD),
                    ),
            )
        val world = map.toWorld()
        assertEquals(CellType.REC_LU, world.getBlock(0, 0))
        assertEquals(CellType.REC_LD, world.getBlock(1, 0))
        assertEquals(CellType.REC_RU, world.getBlock(2, 0))
        assertEquals(CellType.REC_RD, world.getBlock(3, 0))
    }

    @Test
    fun fuelTileWrittenToBlock() {
        val map = simpleMap(tiles = listOf(MapTile(5, 5, BlockType.FUEL)))
        val world = map.toWorld()
        assertEquals(CellType.FUEL, world.getBlock(5, 5))
    }

    @Test
    fun spaceTileRemainsSpace() {
        val map = simpleMap() // no tiles → all SPACE
        val world = map.toWorld()
        for (bx in 0 until world.x) {
            for (by in 0 until world.y) {
                assertEquals(CellType.SPACE, world.getBlock(bx, by), "Empty map should have all SPACE cells")
            }
        }
    }

    // -----------------------------------------------------------------------
    // toWorld — gravity vectors
    // -----------------------------------------------------------------------

    @Test
    fun posGravWritesDownwardGravityVector() {
        val map = simpleMap(tiles = listOf(MapTile(2, 3, BlockType.POS_GRAV)))
        val world = map.toWorld()
        val g = world.gravity[2][3]
        assertEquals(0f, g.x, "POS_GRAV X component should be 0")
        assertTrue(g.y < 0f, "POS_GRAV should produce negative Y gravity (downward in Y-up space)")
    }

    @Test
    fun negGravWritesUpwardGravityVector() {
        val map = simpleMap(tiles = listOf(MapTile(2, 3, BlockType.NEG_GRAV)))
        val world = map.toWorld()
        val g = world.gravity[2][3]
        assertEquals(0f, g.x)
        assertTrue(g.y > 0f, "NEG_GRAV should produce positive Y gravity (upward in Y-up space)")
    }

    @Test
    fun rightGravWritesPositiveXVector() {
        val map = simpleMap(tiles = listOf(MapTile(2, 3, BlockType.RIGHT_GRAV)))
        val world = map.toWorld()
        val g = world.gravity[2][3]
        assertTrue(g.x > 0f, "RIGHT_GRAV should produce positive X gravity")
        assertEquals(0f, g.y)
    }

    @Test
    fun spaceCellHasZeroGravity() {
        val map = simpleMap()
        val world = map.toWorld()
        val g = world.gravity[0][0]
        assertEquals(0f, g.x, "SPACE cell should have zero X gravity")
        assertEquals(0f, g.y, "SPACE cell should have zero Y gravity")
    }

    // -----------------------------------------------------------------------
    // toWorld — bases
    // -----------------------------------------------------------------------

    @Test
    fun baseWrittenToWorldBases() {
        val map =
            simpleMap(
                bases = listOf(MapBase(x = 5, y = 5, team = 1, dir = 0)),
            )
        val world = map.toWorld()
        assertEquals(1, world.bases.size, "World should have one base")
        assertEquals(1, world.bases[0].team)
    }

    @Test
    fun baseBlockSetToBaseType() {
        val map =
            simpleMap(
                bases = listOf(MapBase(x = 4, y = 6, team = 0, dir = 0)),
            )
        val world = map.toWorld()
        assertEquals(CellType.BASE, world.getBlock(4, 6), "Base tile should set block to CellType.BASE")
    }

    @Test
    fun basePositionInClickSpaceCentresOnBlock() {
        val bs = GameConst.BLOCK_SZ
        val map =
            simpleMap(
                bases = listOf(MapBase(x = 3, y = 2, team = 0, dir = 0)),
            )
        val world = map.toWorld()
        val base = world.bases[0]
        val bc = ClickConst.BLOCK_CLICKS
        val expectedCx = 3 * bc + bc / 2
        val expectedCy = 2 * bc + bc / 2
        assertEquals(expectedCx, base.pos.cx, "Base click-X should be block centre")
        assertEquals(expectedCy, base.pos.cy, "Base click-Y should be block centre")
    }

    @Test
    fun multipleBases() {
        val map =
            simpleMap(
                cols = 20,
                rows = 20,
                bases =
                    listOf(
                        MapBase(x = 2, y = 2, team = 1, dir = 0),
                        MapBase(x = 17, y = 17, team = 2, dir = 0),
                    ),
            )
        val world = map.toWorld()
        assertEquals(2, world.bases.size)
    }

    // -----------------------------------------------------------------------
    // GameEngine.fromMap — spawns at base
    // -----------------------------------------------------------------------

    @Test
    fun fromMapSpawnsPlayerAlive() {
        val map =
            simpleMap(
                bases = listOf(MapBase(x = 5, y = 5, team = 0, dir = 0)),
            )
        val engine = GameEngineFactory.fromMap(map)
        assertTrue(engine.player.isAlive(), "Player should be alive after fromMap")
        assertEquals(PlayerState.ALIVE, engine.player.plState)
    }

    @Test
    fun fromMapSpawnsPlayerAtBase() {
        val bs = GameConst.BLOCK_SZ
        val map =
            simpleMap(
                bases = listOf(MapBase(x = 3, y = 7, team = 0, dir = 0)),
            )
        val engine = GameEngineFactory.fromMap(map)
        val expectedPx = (3 * bs + bs / 2).toFloat()
        val expectedPy = (7 * bs + bs / 2).toFloat()
        assertEquals(
            expectedPx,
            engine.playerPixelX,
            absoluteTolerance = 1f,
            "Player X should be at base block centre",
        )
        assertEquals(
            expectedPy,
            engine.playerPixelY,
            absoluteTolerance = 1f,
            "Player Y should be at base block centre",
        )
    }

    @Test
    fun fromMapWithoutBasesFallsBackToWorldCentre() {
        val cols = 10
        val rows = 10
        val map = simpleMap(cols = cols, rows = rows, bases = emptyList())
        val engine = GameEngineFactory.fromMap(map)
        val expectedPx = (cols * GameConst.BLOCK_SZ / 2).toFloat()
        val expectedPy = (rows * GameConst.BLOCK_SZ / 2).toFloat()
        assertEquals(expectedPx, engine.playerPixelX, absoluteTolerance = 1f)
        assertEquals(expectedPy, engine.playerPixelY, absoluteTolerance = 1f)
    }

    // -----------------------------------------------------------------------
    // GameEngine.spawnAtBase
    // -----------------------------------------------------------------------

    @Test
    fun spawnAtBaseSecondBase() {
        val bs = GameConst.BLOCK_SZ
        val map =
            simpleMap(
                cols = 20,
                rows = 20,
                bases =
                    listOf(
                        MapBase(x = 2, y = 2, team = 1, dir = 0),
                        MapBase(x = 17, y = 17, team = 2, dir = 0),
                    ),
            )
        val engine = GameEngineFactory.fromMap(map, baseIndex = 1)
        val expectedPx = (17 * bs + bs / 2).toFloat()
        val expectedPy = (17 * bs + bs / 2).toFloat()
        assertEquals(
            expectedPx,
            engine.playerPixelX,
            absoluteTolerance = 1f,
            "fromMap(baseIndex=1) should place player at second base",
        )
        assertEquals(expectedPy, engine.playerPixelY, absoluteTolerance = 1f)
    }

    @Test
    fun spawnAtBaseClearsShots() {
        val map =
            simpleMap(
                bases = listOf(MapBase(x = 5, y = 5, team = 0, dir = 0)),
            )
        val engine = GameEngineFactory.fromMap(map)
        // Actually add a shot so the assertion is meaningful
        engine.shots.add(
            ShotData(
                pos = engine.player.pos,
                vel =
                    org.lambertland.kxpilot.common
                        .Vector(1f, 0f),
                life = 60f,
                ownerId = engine.player.id,
            ),
        )
        engine.spawnAtBase(0)
        assertTrue(engine.shots.isEmpty(), "spawnAtBase should clear all shots")
    }

    @Test
    fun spawnAtBaseResetsVelocity() {
        val map =
            simpleMap(
                bases = listOf(MapBase(x = 5, y = 5, team = 0, dir = 0)),
            )
        val engine = GameEngineFactory.fromMap(map)
        engine.player.vel =
            org.lambertland.kxpilot.common
                .Vector(50f, 30f)
        engine.spawnAtBase(0)
        assertEquals(0f, engine.player.vel.x, "spawnAtBase should reset vel.x to 0")
        assertEquals(0f, engine.player.vel.y, "spawnAtBase should reset vel.y to 0")
    }

    // -----------------------------------------------------------------------
    // Invalid map guard
    // -----------------------------------------------------------------------

    @Test
    fun toWorldThrowsOnZeroDimensions() {
        val bad = XPilotMap(name = "empty", width = 0, height = 0)
        assertFailsWith<IllegalArgumentException>("toWorld should throw on zero-dimension map") {
            bad.toWorld()
        }
    }

    // -----------------------------------------------------------------------
    // BlockType.TARGET → CellType.TARGET (#12)
    // -----------------------------------------------------------------------

    @Test
    fun targetTileMappedToTargetCellType() {
        val map = simpleMap(tiles = listOf(MapTile(2, 2, BlockType.TARGET)))
        val world = map.toWorld()
        assertEquals(CellType.TARGET, world.getBlock(2, 2), "BlockType.TARGET must map to CellType.TARGET")
    }

    // -----------------------------------------------------------------------
    // Out-of-bounds tiles are silently skipped (#13)
    // -----------------------------------------------------------------------

    @Test
    fun outOfBoundsTilesAreSkipped() {
        // Tiles with negative coords or coords ≥ map size must not throw.
        val map =
            simpleMap(
                cols = 5,
                rows = 5,
                tiles =
                    listOf(
                        MapTile(col = -1, row = 0, type = BlockType.FILLED),
                        MapTile(col = 0, row = -1, type = BlockType.FILLED),
                        MapTile(col = 999, row = 0, type = BlockType.FILLED),
                        MapTile(col = 0, row = 999, type = BlockType.FILLED),
                    ),
            )
        // Should not throw; all cells remain SPACE
        val world = map.toWorld()
        for (bx in 0 until 5) {
            for (by in 0 until 5) {
                assertEquals(CellType.SPACE, world.getBlock(bx, by), "All in-bounds cells should remain SPACE")
            }
        }
    }

    // -----------------------------------------------------------------------
    // Base dir=90° → heading = RES/4 = 32 (#14)
    // -----------------------------------------------------------------------

    @Test
    fun baseDirDegreesConvertedToHeading() {
        // dir = 90° (up in XPilot's CCW convention) should produce heading = RES/4 = 32
        val map = simpleMap(bases = listOf(MapBase(x = 3, y = 3, team = 0, dir = 90)))
        val world = map.toWorld()
        val expectedHeading = GameConst.RES / 4 // 32
        assertEquals(expectedHeading, world.bases[0].dir, "Base dir=90° should produce heading RES/4")
    }
}
