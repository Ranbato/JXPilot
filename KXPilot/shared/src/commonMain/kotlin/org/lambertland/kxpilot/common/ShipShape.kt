package org.lambertland.kxpilot.common

// ---------------------------------------------------------------------------
// Ported from: common/shipshape.h
// ---------------------------------------------------------------------------
//
// Design notes:
//   - C `shape_t` / `shipshape_t` hold pre-rotated point arrays indexed by
//     direction (0 .. RES-1).  These are NOT simple value types; they carry
//     mutable cached rotation state.  Regular `class` is used.
//   - The C code stores pointer arrays (`clpos_t *pts[MAX_SHIP_PTS2]`) where
//     each element points into a separately allocated RES-sized array.  In
//     Kotlin we represent the whole thing as `Array<Array<ClPos>>` (points × dirs).
//   - Gun/light/rack positions are similarly indexed by direction.
//   - `shield_radius` is computed by `Calculate_shield_radius()` and cached.

/** Maximum / minimum ship polygon vertex counts (must not change — protocol-fixed). */
object ShipShapeConst {
    const val MIN_SHIP_PTS: Int = 3
    const val MAX_SHIP_PTS: Int = 24

    /** SSHACK doubles the vertex count. */
    const val MAX_SHIP_PTS2: Int = MAX_SHIP_PTS * 2
    const val MAX_GUN_PTS: Int = 3
    const val MAX_LIGHT_PTS: Int = 3
    const val MAX_RACK_PTS: Int = 4
}

/**
 * A generic rotatable wire-frame shape.
 * Maps to C `shape_t` in common/shipshape.h.
 *
 * [pts] is indexed [pointIndex][direction], where direction is in 0 until [GameConst.RES].
 * [cachedPts] / [cachedDir] cache the last computed rotation for fast lookup.
 */
class Shape(
    val numPoints: Int,
    val numOrigPoints: Int,
    /** Pre-computed rotated positions: pts[pointIndex][dir]. */
    val pts: Array<Array<ClPos>>,
) {
    var cachedPts: Array<ClPos> = Array(ShipShapeConst.MAX_SHIP_PTS2) { ClPos(0, 0) }
    var cachedDir: Int = -1

    fun getPoints(dir: Int): Array<ClPos> {
        if (cachedDir != dir) {
            val result = pts.map { it[dir] }
            result.forEachIndexed { i, pt -> cachedPts[i] = pt }
            cachedDir = dir
        }
        return cachedPts.copyOfRange(0, numPoints)
    }
}

/**
 * A complete ship wire-frame with engine, gun, light, and missile-rack positions.
 * Maps to C `shipshape_t` in common/shipshape.h.
 *
 * All position arrays are indexed by direction (0 until [GameConst.RES]).
 */
class ShipShape(
    val numPoints: Int,
    val numOrigPoints: Int,
    /** Pre-computed rotated hull vertices: pts[pointIndex][dir]. */
    val pts: Array<Array<ClPos>>,
    /** Engine thruster position per direction. */
    val engine: Array<ClPos>,
    /** Main-gun muzzle position per direction. */
    val mGun: Array<ClPos>,
    /** Additional left-side forward guns: lGun[gunIndex][dir]. */
    val lGun: Array<Array<ClPos>>,
    /** Additional right-side forward guns: rGun[gunIndex][dir]. */
    val rGun: Array<Array<ClPos>>,
    /** Additional left-side rear guns: lRgun[gunIndex][dir]. */
    val lRgun: Array<Array<ClPos>>,
    /** Additional right-side rear guns: rRgun[gunIndex][dir]. */
    val rRgun: Array<Array<ClPos>>,
    val numLGun: Int,
    val numRGun: Int,
    val numLRgun: Int,
    val numRRgun: Int,
    /** Left-side lights: lLight[lightIndex][dir]. */
    val lLight: Array<Array<ClPos>>,
    /** Right-side lights: rLight[lightIndex][dir]. */
    val rLight: Array<Array<ClPos>>,
    val numLLight: Int,
    val numRLight: Int,
    /** Missile racks: mRack[rackIndex][dir]. */
    val mRack: Array<Array<ClPos>>,
    val numMRack: Int,
    /** Cached shield radius (pixels), computed from hull vertices. */
    var shieldRadius: Int = 0,
    // Optional NAMEDSHIPS metadata
    val name: String? = null,
    val author: String? = null,
) {
    // Cached rotation state
    var cachedPts: Array<ClPos> = Array(ShipShapeConst.MAX_SHIP_PTS2) { ClPos(0, 0) }
    var cachedDir: Int = -1

    // -----------------------------------------------------------------
    // Accessors (mirrors the C inline functions in shipshape.h)
    // -----------------------------------------------------------------

    fun getPointClPos(
        i: Int,
        dir: Int,
    ): ClPos = pts[i][dir]

    fun getEngineClPos(dir: Int): ClPos = engine[dir]

    fun getMGunClPos(dir: Int): ClPos = mGun[dir]

    fun getLGunClPos(
        gun: Int,
        dir: Int,
    ): ClPos = lGun[gun][dir]

    fun getRGunClPos(
        gun: Int,
        dir: Int,
    ): ClPos = rGun[gun][dir]

    fun getLRgunClPos(
        gun: Int,
        dir: Int,
    ): ClPos = lRgun[gun][dir]

    fun getRRgunClPos(
        gun: Int,
        dir: Int,
    ): ClPos = rRgun[gun][dir]

    fun getLLightClPos(
        l: Int,
        dir: Int,
    ): ClPos = lLight[l][dir]

    fun getRLightClPos(
        l: Int,
        dir: Int,
    ): ClPos = rLight[l][dir]

    fun getMRackClPos(
        rack: Int,
        dir: Int,
    ): ClPos = mRack[rack][dir]

    fun getPointPosition(
        i: Int,
        dir: Int,
    ): Position = getPointClPos(i, dir).toPosition()

    fun getEnginePosition(dir: Int): Position = getEngineClPos(dir).toPosition()

    fun getMGunPosition(dir: Int): Position = getMGunClPos(dir).toPosition()

    fun getLGunPosition(
        gun: Int,
        dir: Int,
    ): Position = getLGunClPos(gun, dir).toPosition()

    fun getRGunPosition(
        gun: Int,
        dir: Int,
    ): Position = getRGunClPos(gun, dir).toPosition()

    fun getLRgunPosition(
        gun: Int,
        dir: Int,
    ): Position = getLRgunClPos(gun, dir).toPosition()

    fun getRRgunPosition(
        gun: Int,
        dir: Int,
    ): Position = getRRgunClPos(gun, dir).toPosition()

    fun getLLightPosition(
        l: Int,
        dir: Int,
    ): Position = getLLightClPos(l, dir).toPosition()

    fun getRLightPosition(
        l: Int,
        dir: Int,
    ): Position = getRLightClPos(l, dir).toPosition()

    fun getMRackPosition(
        rack: Int,
        dir: Int,
    ): Position = getMRackClPos(rack, dir).toPosition()
}

// ---------------------------------------------------------------------------
// Conversion helpers (replace C inline functions ipos2clpos / clpos2position)
// ---------------------------------------------------------------------------

/** Convert an [IPos] (pixel space) to a [ClPos] (click space). */
fun IPos.toClPos(): ClPos = ClPos(x.pixelToClick(), y.pixelToClick())

/** Convert a [ClPos] (click space) to a [Position] (float pixel space). */
fun ClPos.toPosition(): Position = Position(cx.clickToDouble().toFloat(), cy.clickToDouble().toFloat())
