package org.lambertland.kxpilot.common

// ---------------------------------------------------------------------------
// Ported from: common/types.h
// ---------------------------------------------------------------------------

/**
 * A 2D floating-point vector.  Maps to C `vector_t { float x, y; }`.
 */
data class Vector(
    val x: Float,
    val y: Float,
) {
    companion object {
        /** Zero vector.  Use instead of `Vector(0f, 0f)` in reset paths. */
        val ZERO: Vector = Vector(0f, 0f)
    }
}

/**
 * A 2D floating-point position.  Identical in layout to [Vector].
 * Maps to C `position_t` (typedef of vector_t).
 */
typealias Position = Vector

/**
 * A 2D integer vector.  Maps to C `ivec_t { int x, y; }`.
 */
data class IVec(
    val x: Int,
    val y: Int,
)

/**
 * A 2D integer position.  Identical in layout to [IVec].
 * Maps to C `ipos_t` (typedef of ivec_t).
 */
typealias IPos = IVec

/**
 * An integer axis-aligned rectangle.  Maps to C `irec_t { int x, y, w, h; }`.
 */
data class IRect(
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int,
)

/**
 * A 2D block-grid vector (coordinates in block units).
 * Maps to C `blkvec_t { int bx, by; }`.
 */
data class BlkVec(
    val bx: Int,
    val by: Int,
)

/**
 * A 2D block-grid position.  Identical in layout to [BlkVec].
 * Maps to C `blkpos_t` (typedef of blkvec_t).
 */
typealias BlkPos = BlkVec
