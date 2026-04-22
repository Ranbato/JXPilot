package org.lambertland.kxpilot.ui

import androidx.compose.ui.geometry.Offset

// ---------------------------------------------------------------------------
// Camera — player-centred viewport for the game world
// ---------------------------------------------------------------------------

/**
 * Tracks a scrolling view into the game world.
 *
 * The world uses pixel-space coordinates with origin at bottom-left
 * (XPilot convention: Y increases upward).  The screen uses Compose
 * canvas coordinates with origin at top-left (Y increases downward).
 *
 * [worldW] / [worldH]   — world dimensions in pixels (toroidal).
 * [viewW]  / [viewH]    — canvas / viewport dimensions in pixels.
 *
 * [worldOriginX] / [worldOriginY] — the world-space coordinates of the
 *   top-left corner of the current view (in screen-Y orientation, i.e.
 *   worldOriginY corresponds to the highest screen-Y row visible).
 */
class Camera(
    val worldW: Float,
    val worldH: Float,
) {
    var viewW: Float = 1f
        private set
    var viewH: Float = 1f
        private set

    /** World-pixel X of the view's left edge. */
    var worldOriginX: Float = 0f
        private set

    /**
     * World-pixel Y of the view's top edge.
     *
     * Note: world Y=0 is at the bottom of the map; worldOriginY increases
     * as we scroll upward in the world (visually scrolling the view up).
     * The transform [worldToScreen] handles the Y-flip internally.
     */
    var worldOriginY: Float = 0f
        private set

    // -----------------------------------------------------------------------
    // Update
    // -----------------------------------------------------------------------

    /** Update viewport dimensions (call each frame when canvas size changes). */
    fun resize(
        w: Float,
        h: Float,
    ) {
        viewW = w
        viewH = h
    }

    /**
     * Centre the view on world-pixel position ([px], [py]).
     * Clamps so the view never scrolls outside a toroidal world; when the
     * world is smaller than the viewport the origin is simply centred.
     */
    fun follow(
        px: Float,
        py: Float,
    ) {
        worldOriginX = wrap(px - viewW / 2f, worldW)
        // py is world-space (Y up); convert to screen-top = world-bottom direction
        worldOriginY = wrap(py - viewH / 2f, worldH)
    }

    // -----------------------------------------------------------------------
    // Coordinate transforms
    // -----------------------------------------------------------------------

    /**
     * Convert a world-pixel position to a Compose screen [Offset].
     *
     * World X increases right  → screen X increases right (same direction).
     * World Y increases up     → screen Y increases down  (flip).
     *
     * Handles toroidal wrap: if [wx] is to the left of [worldOriginX] by
     * more than half the world width it is assumed to have wrapped and is
     * shifted by [worldW].
     */
    fun worldToScreen(
        wx: Float,
        wy: Float,
    ): Offset {
        // Shortest-path horizontal delta (handle wrap)
        var dx = wx - worldOriginX
        if (dx < -worldW / 2f) dx += worldW
        if (dx > worldW / 2f) dx -= worldW

        // Y: world origin is bottom-left; view origin is top-left.
        // worldOriginY is the world-Y at the bottom of the view.
        var dy = wy - worldOriginY
        if (dy < -worldH / 2f) dy += worldH
        if (dy > worldH / 2f) dy -= worldH

        return Offset(dx, viewH - dy) // flip Y for screen space
    }

    /**
     * Returns true if the world-pixel point ([wx], [wy]) is within the
     * visible viewport (with an optional [margin] in pixels for culling).
     */
    fun isVisible(
        wx: Float,
        wy: Float,
        margin: Float = 32f,
    ): Boolean {
        val s = worldToScreen(wx, wy)
        return s.x >= -margin && s.x <= viewW + margin &&
            s.y >= -margin && s.y <= viewH + margin
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private fun wrap(
        v: Float,
        size: Float,
    ): Float {
        if (size <= 0f) return v
        var r = v % size
        if (r < 0f) r += size
        return r
    }
}
