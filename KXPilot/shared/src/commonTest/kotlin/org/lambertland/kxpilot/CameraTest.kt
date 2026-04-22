package org.lambertland.kxpilot

import org.lambertland.kxpilot.ui.Camera
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [Camera] coordinate transforms and visibility culling.
 *
 * World convention: Y increases upward (XPilot / game space).
 * Screen convention: Y increases downward (Compose canvas).
 * [Camera.worldToScreen] flips Y: screenY = viewH - dy.
 *
 * All tests use a world (2000×2000) larger than the viewport (800×600) to
 * avoid edge cases where toroidal shortest-path arithmetic differs from the
 * simple centring case.
 */
class CameraTest {
    companion object {
        private const val WORLD_W = 2000f
        private const val WORLD_H = 2000f
        private const val VIEW_W = 800f
        private const val VIEW_H = 600f
    }

    private fun makeCamera(
        followX: Float = WORLD_W / 2f,
        followY: Float = WORLD_H / 2f,
    ): Camera {
        val cam = Camera(WORLD_W, WORLD_H)
        cam.resize(VIEW_W, VIEW_H)
        cam.follow(followX, followY)
        return cam
    }

    // -----------------------------------------------------------------------
    // 1. Y-flip: a point at the view's bottom-left world position maps to
    //    the bottom row of the screen (screenY = viewH).
    // -----------------------------------------------------------------------
    @Test
    fun yFlipBottomLeftViewOriginMapsToBottomScreenRow() {
        val cam = makeCamera()
        // Point exactly at the view's bottom-left corner in world space:
        // worldOriginX, worldOriginY maps to screenX=0, screenY=viewH.
        val s = cam.worldToScreen(cam.worldOriginX, cam.worldOriginY)
        assertEquals(0f, s.x, absoluteTolerance = 1f, "screenX at origin should be 0")
        assertEquals(VIEW_H, s.y, absoluteTolerance = 1f, "screenY at world bottom-left should be viewH")
    }

    // -----------------------------------------------------------------------
    // 2. A point at worldOriginY + viewH maps to the top screen row (screenY ≈ 0).
    // -----------------------------------------------------------------------
    @Test
    fun yFlipTopViewEdgeMapsToTopScreenRow() {
        val cam = makeCamera()
        val topWorldY = cam.worldOriginY + VIEW_H
        val s = cam.worldToScreen(cam.worldOriginX, topWorldY)
        assertTrue(s.y < 1f, "Top-of-view world Y should map to screenY ≈ 0, was ${s.y}")
    }

    // -----------------------------------------------------------------------
    // 3. follow centres the followed point on screen.
    // -----------------------------------------------------------------------
    @Test
    fun followCentresPlayerOnScreen() {
        val cx = WORLD_W / 2f
        val cy = WORLD_H / 2f
        val cam = makeCamera(followX = cx, followY = cy)
        val s = cam.worldToScreen(cx, cy)
        assertEquals(VIEW_W / 2f, s.x, absoluteTolerance = 1f, "Centred player should be at screen X=viewW/2")
        assertEquals(VIEW_H / 2f, s.y, absoluteTolerance = 1f, "Centred player should be at screen Y=viewH/2")
    }

    // -----------------------------------------------------------------------
    // 4. Toroidal wrap: with the camera origin near the right world edge,
    //    a point near the left world edge (near 0) should appear on-screen.
    // -----------------------------------------------------------------------
    @Test
    fun toroidalWrapLeftEdgeAppearsOnScreenWhenCameraAtRightEdge() {
        // Camera centred near the right edge of the world.
        // worldOriginX = wrap(1950 - 400, 2000) = wrap(1550, 2000) = 1550.
        val cam = makeCamera(followX = 1950f, followY = WORLD_H / 2f)

        // A point at worldX=10 (near left/wrapped edge).
        // Without wrap handling: dx = 10 - 1550 = -1540 (off screen).
        // With wrap: -1540 < -1000 → dx += 2000 = 460 (on screen).
        val s = cam.worldToScreen(10f, WORLD_H / 2f)
        assertTrue(
            s.x >= 0f && s.x <= VIEW_W,
            "Point near left-world-edge should appear on screen when camera is at right edge, screenX=${s.x}",
        )
    }

    // -----------------------------------------------------------------------
    // 5. isVisible: point at screen centre is visible.
    // -----------------------------------------------------------------------
    @Test
    fun isVisibleCentrePoint() {
        val cx = WORLD_W / 2f
        val cy = WORLD_H / 2f
        val cam = makeCamera(followX = cx, followY = cy)
        assertTrue(cam.isVisible(cx, cy), "Centre-of-follow point should be visible")
    }

    // -----------------------------------------------------------------------
    // 6. isVisible: point far outside viewport is not visible (margin=0).
    // -----------------------------------------------------------------------
    @Test
    fun isVisibleFarOutsideReturnsFalse() {
        // Camera centred at world centre; follow world-centre; test a point
        // 500px above the top of the viewport (well outside).
        val cam = makeCamera()
        val farY = cam.worldOriginY + VIEW_H + 500f
        assertFalse(
            cam.isVisible(cam.worldOriginX + VIEW_W / 2f, farY, margin = 0f),
            "Point far above viewport should not be visible",
        )
    }

    // -----------------------------------------------------------------------
    // 7. isVisible: point just inside margin is considered visible.
    // -----------------------------------------------------------------------
    @Test
    fun isVisibleWithinMarginReturnsTrue() {
        val cam = makeCamera()
        // Point 20px to the left of the viewport left edge, with margin=32.
        val pointX = cam.worldOriginX - 20f
        val pointY = cam.worldOriginY + VIEW_H / 2f // vertically centred
        assertTrue(
            cam.isVisible(pointX, pointY, margin = 32f),
            "Point 20px outside but within 32px margin should be visible",
        )
    }

    // -----------------------------------------------------------------------
    // 8. resize updates view dimensions.
    // -----------------------------------------------------------------------
    @Test
    fun resizeUpdatesViewDimensions() {
        val cam = Camera(WORLD_W, WORLD_H)
        cam.resize(1024f, 768f)
        assertEquals(1024f, cam.viewW)
        assertEquals(768f, cam.viewH)
    }
}
