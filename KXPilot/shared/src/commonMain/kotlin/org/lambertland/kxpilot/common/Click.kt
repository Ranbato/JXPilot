package org.lambertland.kxpilot.common

// ---------------------------------------------------------------------------
// Ported from: common/click.h
// ---------------------------------------------------------------------------

/**
 * A sub-pixel coordinate value (integer, in "clicks").
 * Maps to C `click_t` (typedef int).
 *
 * 1 pixel = CLICK (64) clicks.  This fixed-point scheme gives repeatable
 * collision detection without floating-point drift.
 */
typealias Click = Int

/**
 * A 2D position in click-space.  Maps to C `clpos_t { click_t cx, cy; }`.
 */
data class ClPos(
    val cx: Click,
    val cy: Click,
) {
    companion object {
        /** Zero-position sentinel.  Use instead of `ClPos(0, 0)` in reset paths. */
        val ZERO: ClPos = ClPos(0, 0)
    }
}

/**
 * A 2D vector (displacement) in click-space.  Maps to C `clvec_t { click_t cx, cy; }`.
 */
data class ClVec(
    val cx: Click,
    val cy: Click,
)

// ---------------------------------------------------------------------------
// Click conversion constants and helpers
// Maps to the #define macros in common/click.h
// ---------------------------------------------------------------------------
object ClickConst {
    const val CLICK_SHIFT: Int = 6

    /** Clicks per pixel. */
    const val CLICK: Int = 1 shl CLICK_SHIFT // 64
    const val PIXEL_CLICKS: Int = CLICK

    /** Clicks per map block (block size in pixels × clicks-per-pixel). */
    const val BLOCK_CLICKS: Int = GameConst.BLOCK_SZ shl CLICK_SHIFT
}

// ---------------------------------------------------------------------------
// Extension / utility functions (replaces inline C macros)
// ---------------------------------------------------------------------------

/** Convert a click coordinate to pixels (integer truncation). */
fun Click.toPixel(): Int = this shr ClickConst.CLICK_SHIFT

/** Convert a click coordinate to blocks. */
fun Click.toBlock(): Int = this / ClickConst.BLOCK_CLICKS

/** Convert a click coordinate to a floating-point value. */
fun Click.clickToDouble(): Double = this.toDouble() / ClickConst.CLICK

/** Convert a pixel integer to clicks. */
fun Int.pixelToClick(): Click = this shl ClickConst.CLICK_SHIFT

/** Convert a floating-point value to clicks (truncated). */
fun Double.floatToClick(): Click = (this * ClickConst.CLICK).toInt()

/** Return the [BlkPos] that this [ClPos] falls inside. */
fun ClPos.toBlkPos(): BlkPos = BlkVec(cx.toBlock(), cy.toBlock())

/** Return the [ClPos] of the centre of the given [BlkPos]. */
fun BlkPos.toCenterClPos(): ClPos {
    val bc = ClickConst.BLOCK_CLICKS
    return ClPos(bx * bc + bc / 2, by * bc + bc / 2)
}
