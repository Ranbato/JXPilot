package org.lambertland.kxpilot

import org.lambertland.kxpilot.server.CellType
import org.lambertland.kxpilot.server.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Helper: creates a [World] with a fully initialised [World.block] array of the given size. */
private fun makeWorld(
    cols: Int,
    rows: Int,
): World =
    World().also { w ->
        w.x = cols
        w.y = rows
        w.cwidth = cols * 256 // arbitrary click scale
        w.cheight = rows * 256
        w.block = Array(cols) { Array(rows) { CellType.SPACE } }
    }

class WorldTest {
    // -----------------------------------------------------------------------
    // getBlock / setBlock — happy paths
    // -----------------------------------------------------------------------

    @Test fun setAndGetBlock() {
        val w = makeWorld(10, 10)
        w.setBlock(3, 7, CellType.CANNON)
        assertEquals(CellType.CANNON, w.getBlock(3, 7))
    }

    @Test fun setBlockAtOrigin() {
        val w = makeWorld(5, 5)
        w.setBlock(0, 0, CellType.BASE)
        assertEquals(CellType.BASE, w.getBlock(0, 0))
    }

    @Test fun setBlockAtMaxCorner() {
        val w = makeWorld(5, 5)
        w.setBlock(4, 4, CellType.BASE_ATTRACTOR)
        assertEquals(CellType.BASE_ATTRACTOR, w.getBlock(4, 4))
    }

    @Test fun unsetBlockDefaultsToZero() {
        val w = makeWorld(4, 4)
        assertEquals(CellType.SPACE, w.getBlock(2, 2))
    }

    @Test fun setBlockDoesNotAffectNeighbours() {
        val w = makeWorld(5, 5)
        w.setBlock(2, 2, CellType.FUEL)
        assertEquals(CellType.SPACE, w.getBlock(1, 2))
        assertEquals(CellType.SPACE, w.getBlock(3, 2))
        assertEquals(CellType.SPACE, w.getBlock(2, 1))
        assertEquals(CellType.SPACE, w.getBlock(2, 3))
    }

    // -----------------------------------------------------------------------
    // getBlock — out-of-bounds throws
    // -----------------------------------------------------------------------

    @Test fun getBlockNegativeXThrows() {
        val w = makeWorld(4, 4)
        assertFailsWith<IllegalArgumentException> { w.getBlock(-1, 0) }
    }

    @Test fun getBlockNegativeYThrows() {
        val w = makeWorld(4, 4)
        assertFailsWith<IllegalArgumentException> { w.getBlock(0, -1) }
    }

    @Test fun getBlockXEqualToWidthThrows() {
        val w = makeWorld(4, 4)
        assertFailsWith<IllegalArgumentException> { w.getBlock(4, 0) }
    }

    @Test fun getBlockYEqualToHeightThrows() {
        val w = makeWorld(4, 4)
        assertFailsWith<IllegalArgumentException> { w.getBlock(0, 4) }
    }

    // -----------------------------------------------------------------------
    // setBlock — out-of-bounds throws
    // -----------------------------------------------------------------------

    @Test fun setBlockOutOfBoundsThrows() {
        val w = makeWorld(3, 3)
        assertFailsWith<IllegalArgumentException> { w.setBlock(3, 0, CellType.BASE) }
    }

    // -----------------------------------------------------------------------
    // Uninitialised block array throws
    // -----------------------------------------------------------------------

    @Test fun getBlockOnUninitWorldThrows() {
        val w = World() // block is emptyArray()
        assertFailsWith<IllegalArgumentException> { w.getBlock(0, 0) }
    }

    @Test fun setBlockOnUninitWorldThrows() {
        val w = World()
        assertFailsWith<IllegalArgumentException> { w.setBlock(0, 0, CellType.BASE) }
    }

    // -----------------------------------------------------------------------
    // wrapXClick / wrapYClick
    // -----------------------------------------------------------------------

    @Test fun wrapXClickIdentityInsideRange() {
        val w = makeWorld(10, 10) // cwidth = 2560
        assertEquals(100, w.wrapXClick(100))
    }

    @Test fun wrapXClickWrapsPositiveOverflow() {
        val w = makeWorld(10, 10) // cwidth = 2560
        assertEquals(0, w.wrapXClick(2560))
        assertEquals(1, w.wrapXClick(2561))
    }

    @Test fun wrapXClickWrapsNegativeInput() {
        val w = makeWorld(10, 10) // cwidth = 2560
        assertEquals(2559, w.wrapXClick(-1))
        assertEquals(2558, w.wrapXClick(-2))
    }

    @Test fun wrapXClickLargeNegative() {
        val w = makeWorld(10, 10) // cwidth = 2560
        // -99999 mod 2560: (-99999 % 2560) + 2560 = (-2559 + 2560) ... check result is in [0, cwidth)
        val result = w.wrapXClick(-99999)
        assertTrue(result in 0 until 2560, "Expected result in [0, cwidth) but was $result")
    }

    @Test fun wrapYClickWrapsNegativeInput() {
        val w = makeWorld(10, 10) // cheight = 2560
        assertEquals(2559, w.wrapYClick(-1))
    }

    // -----------------------------------------------------------------------
    // containsClPos
    // -----------------------------------------------------------------------

    @Test fun containsClPosInsideReturnTrue() {
        val w = makeWorld(10, 10)
        assertTrue(w.containsClPos(0, 0))
        assertTrue(w.containsClPos(w.cwidth - 1, w.cheight - 1))
    }

    @Test fun containsClPosOnBoundaryReturnsFalse() {
        val w = makeWorld(10, 10)
        assertFalse(w.containsClPos(w.cwidth, 0))
        assertFalse(w.containsClPos(0, w.cheight))
    }

    @Test fun containsClPosNegativeReturnsFalse() {
        val w = makeWorld(10, 10)
        assertFalse(w.containsClPos(-1, 0))
        assertFalse(w.containsClPos(0, -1))
    }
}
