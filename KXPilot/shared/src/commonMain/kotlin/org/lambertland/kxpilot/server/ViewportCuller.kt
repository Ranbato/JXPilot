package org.lambertland.kxpilot.server

import org.lambertland.kxpilot.common.ClickConst

// ---------------------------------------------------------------------------
// ViewportCuller
// ---------------------------------------------------------------------------
//
// Determines whether a click-space position falls within a client's viewport,
// mirroring the C `clpos_inview()` function in server/frame.c.
//
// The C algorithm (Frame_parameters + clpos_inview):
//
//   view_cwidth  = viewWidth  * CLICK   // pixel→click conversion
//   view_cheight = viewHeight * CLICK
//
//   cv.unrealWorld.cx = player.pos.cx - view_cwidth  / 2   // top-left corner
//   cv.unrealWorld.cy = player.pos.cy - view_cheight / 2
//   cv.realWorld = cv.unrealWorld
//
//   // Wrap adjustment for worlds with WRAP_PLAY:
//   if (unrealWorld.cx < 0 && unrealWorld.cx + view_cwidth  < world.cwidth)
//       unrealWorld.cx += world.cwidth       // normalise negative x
//   else if (unrealWorld.cx > 0 && unrealWorld.cx + view_cwidth >= world.cwidth)
//       realWorld.cx -= world.cwidth         // second copy wraps past right edge
//   // same for y
//
//   bool inview(pos):
//       return (pos.cx in (wpos.cx, wpos.cx+view_cwidth)
//            OR pos.cx in (rwpos.cx, rwpos.cx+view_cwidth))
//          && (pos.cy in (wpos.cy, wpos.cy+view_cheight)
//            OR pos.cy in (rwpos.cy, rwpos.cy+view_cheight))
//
// The C checks are exclusive on both ends (strict `>` and `<`), which matches
// the fact that objects exactly at the viewport boundary are not rendered.

/**
 * Viewport visibility state for one client in one frame.
 *
 * Construct per-client with [ViewportCuller.forClient], then call [isInView]
 * for each object position.  All coordinates are in click-space.
 *
 * @param unwrapCx     Click-space left edge of the viewport (may be negative,
 *                     or adjusted to alias across the world wrap boundary).
 * @param unwrapCy     Click-space top edge.
 * @param realCx       Second copy of left edge, adjusted for world-wrap aliasing.
 * @param realCy       Second copy of top edge.
 * @param viewCWidth   Viewport width in clicks.
 * @param viewCHeight  Viewport height in clicks.
 */
class ViewportCuller(
    private val unwrapCx: Int,
    private val unwrapCy: Int,
    private val realCx: Int,
    private val realCy: Int,
    private val viewCWidth: Int,
    private val viewCHeight: Int,
) {
    /**
     * Returns `true` if the click-space position ([cx], [cy]) is visible
     * within this viewport.
     *
     * Mirrors C `clpos_inview` — exclusive bounds on both sides.
     */
    fun isInView(
        cx: Int,
        cy: Int,
    ): Boolean {
        val xOk =
            (cx > unwrapCx && cx < unwrapCx + viewCWidth) ||
                (cx > realCx && cx < realCx + viewCWidth)
        if (!xOk) return false
        val yOk =
            (cy > unwrapCy && cy < unwrapCy + viewCHeight) ||
                (cy > realCy && cy < realCy + viewCHeight)
        return yOk
    }

    companion object {
        /**
         * Build a [ViewportCuller] for a client whose player is at ([playerCx], [playerCy])
         * in click-space, with a view measured in pixels of ([viewWidthPx] × [viewHeightPx]).
         *
         * [worldCWidth] and [worldCHeight] are the world dimensions in clicks; pass
         * 0 for a non-wrapping world (the wrap adjustment is skipped).
         *
         * Mirrors C `Frame_parameters` from `server/frame.c`.
         */
        fun forClient(
            playerCx: Int,
            playerCy: Int,
            viewWidthPx: Int,
            viewHeightPx: Int,
            worldCWidth: Int = 0,
            worldCHeight: Int = 0,
        ): ViewportCuller {
            val vcw = viewWidthPx * ClickConst.CLICK
            val vch = viewHeightPx * ClickConst.CLICK

            var unwrapX = playerCx - vcw / 2
            var unwrapY = playerCy - vch / 2
            var realX = unwrapX
            var realY = unwrapY

            // Wrap adjustment (mirrors BIT(WRAP_PLAY) branch in Frame_parameters).
            // Only applied when world dimensions are known (> 0).
            if (worldCWidth > 0) {
                if (unwrapX < 0 && unwrapX + vcw < worldCWidth) {
                    unwrapX += worldCWidth
                } else if (unwrapX > 0 && unwrapX + vcw >= worldCWidth) {
                    realX -= worldCWidth
                }
            }
            if (worldCHeight > 0) {
                if (unwrapY < 0 && unwrapY + vch < worldCHeight) {
                    unwrapY += worldCHeight
                } else if (unwrapY > 0 && unwrapY + vch >= worldCHeight) {
                    realY -= worldCHeight
                }
            }

            return ViewportCuller(
                unwrapCx = unwrapX,
                unwrapCy = unwrapY,
                realCx = realX,
                realCy = realY,
                viewCWidth = vcw,
                viewCHeight = vch,
            )
        }

        /**
         * A culler that admits every position in the world (used for spectators
         * without a physical ship position, or in tests where culling is irrelevant).
         */
        fun fullWorld(
            worldCWidth: Int,
            worldCHeight: Int,
        ): ViewportCuller =
            ViewportCuller(
                unwrapCx = Int.MIN_VALUE / 2,
                unwrapCy = Int.MIN_VALUE / 2,
                realCx = Int.MIN_VALUE / 2,
                realCy = Int.MIN_VALUE / 2,
                viewCWidth = worldCWidth + Int.MAX_VALUE / 2,
                viewCHeight = worldCHeight + Int.MAX_VALUE / 2,
            )
    }
}
