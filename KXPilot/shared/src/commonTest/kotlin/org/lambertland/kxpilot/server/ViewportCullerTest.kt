package org.lambertland.kxpilot.server

import org.lambertland.kxpilot.common.ClickConst
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// ---------------------------------------------------------------------------
// ViewportCullerTest
//
// Verifies that ViewportCuller.isInView() exactly mirrors the C
// clpos_inview() logic from server/frame.c.
// ---------------------------------------------------------------------------

class ViewportCullerTest {
    // Helpers: CLICK = 64, so 1 px = 64 clicks

    private fun px(pixels: Int): Int = pixels * ClickConst.CLICK

    // -----------------------------------------------------------------------
    // Basic in-view / out-of-view
    // -----------------------------------------------------------------------

    /**
     * Player at centre of a 1000×1000-pixel world (cwidth=64000, cheight=64000).
     * View = 800×600 px.  Centre of view is player's position.
     * Viewport in click-space: x ∈ (playerCx - 400*64, playerCx + 400*64).
     *
     * Mirrors C: cv.unrealWorld.cx = player.pos.cx - view_cwidth/2
     * clpos_inview: pos.cx > wpos.cx && pos.cx < wpos.cx + view_cwidth
     */
    @Test
    fun centreOfViewIsInView() {
        val playerCx = px(500)
        val playerCy = px(500)
        val culler =
            ViewportCuller.forClient(
                playerCx = playerCx,
                playerCy = playerCy,
                viewWidthPx = 800,
                viewHeightPx = 600,
            )
        // Exactly at player centre — well within viewport
        assertTrue(culler.isInView(playerCx, playerCy), "Player centre should be in view")
    }

    @Test
    fun positionAtViewportEdgeIsNotInView() {
        // C bounds are exclusive: pos.cx > wpos.cx && pos.cx < wpos.cx + view_cwidth
        // An object exactly at the viewport left edge (wpos.cx) must NOT be in view.
        val playerCx = px(500)
        val playerCy = px(500)
        val viewWidthPx = 800
        val culler = ViewportCuller.forClient(playerCx, playerCy, viewWidthPx, 600)
        val leftEdgeCx = playerCx - px(viewWidthPx / 2) // exactly at wpos.cx
        assertFalse(culler.isInView(leftEdgeCx, playerCy), "Left edge (exclusive) should not be in view")
    }

    @Test
    fun positionOnePxInsideLeftEdgeIsInView() {
        val playerCx = px(500)
        val playerCy = px(500)
        val viewWidthPx = 800
        val culler = ViewportCuller.forClient(playerCx, playerCy, viewWidthPx, 600)
        val justInside = playerCx - px(viewWidthPx / 2) + 1 // one click inside left edge
        assertTrue(culler.isInView(justInside, playerCy), "One click inside left edge should be in view")
    }

    @Test
    fun positionFarToRightIsOutOfView() {
        val playerCx = px(500)
        val playerCy = px(500)
        val culler = ViewportCuller.forClient(playerCx, playerCy, 800, 600)
        assertFalse(culler.isInView(px(5000), playerCy), "Far right should not be in view")
    }

    @Test
    fun positionAboveViewportIsOutOfView() {
        val playerCx = px(500)
        val playerCy = px(500)
        val culler = ViewportCuller.forClient(playerCx, playerCy, 800, 600)
        assertFalse(culler.isInView(playerCx, px(1)), "Far above should not be in view")
    }

    // -----------------------------------------------------------------------
    // World-wrap: viewport crossing the left world edge
    // -----------------------------------------------------------------------

    /**
     * Player near the left world edge (x = 100 px), view 800 px wide.
     * View left edge = 100*64 - 400*64 = -300*64 (negative).
     * C normalises: if unrealWorld.cx < 0 && unrealWorld.cx + view_cwidth < worldCWidth
     *   → unrealWorld.cx += worldCWidth
     * A ball at rightmost edge (e.g. 900 px in a 1000-px world) should be in view
     * via the wrapped copy.
     */
    @Test
    fun wrapLeftEdge_ballOnRightSideOfWorldIsInView() {
        val worldW = 1000 // pixels
        val worldH = 1000
        val worldCW = px(worldW)
        val worldCH = px(worldH)

        val playerCx = px(100)
        val playerCy = px(500)
        val viewW = 800

        val culler =
            ViewportCuller.forClient(
                playerCx = playerCx,
                playerCy = playerCy,
                viewWidthPx = viewW,
                viewHeightPx = 600,
                worldCWidth = worldCW,
                worldCHeight = worldCH,
            )

        // Ball at x=900px: it is 200 px to the player's left across the wrap boundary.
        // The wrapped viewport should cover it.
        val ballCx = px(900)
        val ballCy = playerCy
        assertTrue(culler.isInView(ballCx, ballCy), "Ball across left-wrap boundary should be in view")
    }

    @Test
    fun wrapLeftEdge_ballInMiddleOfWorldIsNotInView() {
        val worldCW = px(1000)
        val worldCH = px(1000)
        val playerCx = px(100)
        val playerCy = px(500)

        val culler =
            ViewportCuller.forClient(
                playerCx = playerCx,
                playerCy = playerCy,
                viewWidthPx = 800,
                viewHeightPx = 600,
                worldCWidth = worldCW,
                worldCHeight = worldCH,
            )
        // Ball at x=500px — in the middle of the world, not near player
        assertFalse(culler.isInView(px(500), playerCy), "Ball in middle of world should not be in wrap-left viewport")
    }

    // -----------------------------------------------------------------------
    // World-wrap: viewport crossing the right world edge
    // -----------------------------------------------------------------------

    /**
     * Player near the right world edge (x = 900 px), view 800 px wide.
     * View right edge = 900*64 + 400*64 = 1300*64 ≥ worldCWidth (1000*64).
     * C: realWorld.cx -= worldCWidth
     * A ball at x = 50px (near left edge) should be in view via realWorld copy.
     */
    @Test
    fun wrapRightEdge_ballOnLeftSideOfWorldIsInView() {
        val worldCW = px(1000)
        val worldCH = px(1000)
        val playerCx = px(900)
        val playerCy = px(500)

        val culler =
            ViewportCuller.forClient(
                playerCx = playerCx,
                playerCy = playerCy,
                viewWidthPx = 800,
                viewHeightPx = 600,
                worldCWidth = worldCW,
                worldCHeight = worldCH,
            )
        // Ball at x=50px — 150 px to the right across the wrap boundary
        assertTrue(culler.isInView(px(50), playerCy), "Ball across right-wrap boundary should be in view")
    }

    // -----------------------------------------------------------------------
    // fullWorld factory
    // -----------------------------------------------------------------------

    @Test
    fun fullWorldAdmitsAnyPosition() {
        val culler = ViewportCuller.fullWorld(px(1000), px(1000))
        assertTrue(culler.isInView(px(1), px(1)))
        assertTrue(culler.isInView(px(500), px(500)))
        assertTrue(culler.isInView(px(999), px(999)))
    }

    // -----------------------------------------------------------------------
    // forClient without world dimensions — no wrap adjustment
    // -----------------------------------------------------------------------

    @Test
    fun noWorldDimsDisablesWrapAdjustment() {
        // Player near left edge, no world dims → no wrap normalisation
        val playerCx = px(10)
        val playerCy = px(500)
        val culler = ViewportCuller.forClient(playerCx, playerCy, 800, 600)
        // Ball at far right of an imaginary 1000px world — should NOT be in view
        // since no wrap adjustment is applied
        assertFalse(culler.isInView(px(900), playerCy), "Without world dims, wrap should not apply")
    }
}
