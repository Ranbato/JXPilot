package org.lambertland.kxpilot.resources

import org.lambertland.kxpilot.common.BlkVec
import org.lambertland.kxpilot.common.ClPos
import org.lambertland.kxpilot.common.ClickConst
import org.lambertland.kxpilot.common.GameConst
import org.lambertland.kxpilot.common.Vector
import org.lambertland.kxpilot.common.pixelToClick
import org.lambertland.kxpilot.common.toCenterClPos
import org.lambertland.kxpilot.server.AsteroidConcentrator
import org.lambertland.kxpilot.server.Base
import org.lambertland.kxpilot.server.Cannon
import org.lambertland.kxpilot.server.CellType
import org.lambertland.kxpilot.server.Check
import org.lambertland.kxpilot.server.Fuel
import org.lambertland.kxpilot.server.Grav
import org.lambertland.kxpilot.server.ItemConcentrator
import org.lambertland.kxpilot.server.FrictionArea
import org.lambertland.kxpilot.server.Target
import org.lambertland.kxpilot.server.Treasure
import org.lambertland.kxpilot.server.Wormhole
import org.lambertland.kxpilot.server.WormType
import org.lambertland.kxpilot.server.World
import org.lambertland.kxpilot.server.initGrid
import kotlin.math.roundToInt
import kotlin.math.sqrt

// ---------------------------------------------------------------------------
// XPilotMap → World converter
// ---------------------------------------------------------------------------

/**
 * Gravity force magnitude applied by a single gravity-tile cell (pixels/tick²).
 *
 * Matches the C server constant `GRAVS_POWER = 2.7` from `server/serverconst.h`.
 *
 * Note: the XPilot C source uses `GRAVS_POWER` (not `GRAVITY`) as the per-cell
 * gravity strength.  `GRAVITY` in that codebase is the *global* downward pull and
 * is a separate, unrelated constant.
 */
private const val GRAVS_POWER = 2.7f

/**
 * Convert a parsed [XPilotMap] into a runtime [World] ready for use by the game engine.
 *
 * ## Coordinate-space handling
 *
 * ### `.xp` maps (`isXp2 == false`)
 *  - `width` / `height` are in **blocks**.
 *  - Entity x/y are in **blocks**.
 *
 * ### `.xp2` maps (`isXp2 == true`)
 *  - `width` / `height` (from `mapwidth` / `mapheight` options) are in **pixels**.
 *    They are converted to blocks by dividing by `BLOCK_SZ` (35).
 *  - Entity x/y are in **clicks** (64 clicks = 1 pixel).
 *    They are stored as-is into `ClPos` fields.
 *
 * ## What is populated
 *  - `world.block[bx][by]`    — cell types
 *  - `world.gravity[bx][by]`  — per-cell gravity vectors (global bias + tile)
 *  - `world.bases`            — spawn bases
 *  - `world.cannons`          — cannon entities
 *  - `world.fuels`            — fuel stations
 *  - `world.wormholes`        — wormhole tiles
 *  - `world.treasures`        — treasure chests
 *  - `world.checks`           — checkpoints
 *  - `world.targets`          — destructible targets
 *  - `world.gravs`            — gravity-source records
 *  - `world.itemConcs`        — item concentrators
 *  - `world.asteroidConcs`    — asteroid concentrators
 *  - `world.frictionAreas`    — friction areas
 *  - `world.diagonal`         — pixel diagonal of the world
 *  - `world.hypotenuse`       — half of pixel diagonal
 *  - `world.name`, `world.author`
 *
 * @throws IllegalArgumentException if the map has zero width or height.
 */
fun XPilotMap.toWorld(): World {
    val blockPx = GameConst.BLOCK_SZ

    // Derive block dimensions.
    // For .xp2, width/height are in pixels (mapwidth/mapheight option).
    val cols: Int
    val rows: Int
    if (isXp2) {
        require(width > 0 && height > 0) {
            "Map \"$name\" has invalid pixel dimensions $width×$height"
        }
        cols = width / blockPx
        rows = height / blockPx
        require(cols > 0 && rows > 0) {
            "Map \"$name\" pixel dimensions $width×$height too small (BLOCK_SZ=$blockPx)"
        }
    } else {
        require(width > 0 && height > 0) {
            "Map \"$name\" has invalid dimensions $width×$height"
        }
        cols = width
        rows = height
    }

    val globalGravityUnits = options["gravity"]?.trim()?.toFloatOrNull() ?: 0f
    // Positive "gravity" option = pull downward; Y-up means downward = negative Y.
    val globalGravY = -globalGravityUnits * GRAVS_POWER

    val world = World().also { w ->
        w.initGrid(cols, rows)
        // Override the zero gravity set by initGrid with the global bias.
        if (globalGravY != 0f) {
            for (bx in 0 until cols) {
                for (by in 0 until rows) {
                    w.gravity[bx][by] = Vector(0f, globalGravY)
                }
            }
        }
        w.name = name
        w.author = author
        val diagPx = sqrt((w.width.toDouble() * w.width) + (w.height.toDouble() * w.height))
        w.diagonal = diagPx
        w.hypotenuse = diagPx / 2.0
    }

    // ---- Tiles (walls, diagonals, decor, grav tiles, etc.) ----
    for (tile in tiles) {
        val bx = tile.col
        val by = tile.row
        if (bx < 0 || bx >= cols || by < 0 || by >= rows) continue

        val cellType = blockTypeToCellType(tile.type)
        world.setBlock(bx, by, cellType)

        // Accumulate per-cell gravity on top of global gravity.
        val cellGrav = gravityVector(tile.type)
        if (cellGrav != null) {
            val existing = world.gravity[bx][by]
            world.gravity[bx][by] = Vector(existing.x + cellGrav.x, existing.y + cellGrav.y)
        }
    }

    // ---- Bases ----
    for (mb in bases) {
        val clPos: ClPos
        if (isXp2) {
            // x/y are in clicks
            clPos = ClPos(mb.x, mb.y)
            val bx = mb.x / ClickConst.BLOCK_CLICKS
            val by = mb.y / ClickConst.BLOCK_CLICKS
            if (bx in 0 until cols && by in 0 until rows) world.setBlock(bx, by, CellType.BASE)
        } else {
            val bx = mb.x
            val by = mb.y
            if (bx < 0 || bx >= cols || by < 0 || by >= rows) continue
            world.setBlock(bx, by, CellType.BASE)
            clPos = BlkVec(bx, by).toCenterClPos()
        }
        world.bases +=
            Base(
                pos = clPos,
                dir = degreesToHeading(mb.dir),
                ind = world.bases.size,
                team = mb.team,
                order = mb.order,
            )
    }

    // ---- Cannons ----
    for (mc in cannons) {
        val clPos: ClPos
        if (isXp2) {
            clPos = ClPos(mc.x, mc.y)
            val bx = mc.x / ClickConst.BLOCK_CLICKS
            val by = mc.y / ClickConst.BLOCK_CLICKS
            if (bx in 0 until cols && by in 0 until rows) world.setBlock(bx, by, CellType.CANNON)
        } else {
            val bx = mc.x
            val by = mc.y
            if (bx < 0 || bx >= cols || by < 0 || by >= rows) continue
            world.setBlock(bx, by, CellType.CANNON)
            // Cannon click position: 1/3 into block from the direction it faces.
            // Matches C World_place_cannon() which uses CANNON_OFFSET = BLOCK_CLICKS/3.
            val bcClicks = ClickConst.BLOCK_CLICKS
            val base = BlkVec(bx, by).toCenterClPos()
            val offset = bcClicks / 3
            val (dx, dy) = when {
                mc.dir == 0 -> Pair(offset, 0)                // RIGHT
                mc.dir == GameConst.RES / 4 -> Pair(0, offset)      // UP
                mc.dir == GameConst.RES / 2 -> Pair(-offset, 0)     // LEFT
                mc.dir == 3 * GameConst.RES / 4 -> Pair(0, -offset) // DOWN
                else -> Pair(0, 0)
            }
            clPos = ClPos(base.cx + dx, base.cy + dy)
        }
        world.cannons +=
            Cannon(
                pos = clPos,
                dir = if (isXp2) degreesToHeading(mc.dir) else mc.dir,
                connMask = 0u,
                team = mc.team,
            )
    }

    // ---- Fuel stations ----
    for (mf in fuels) {
        val clPos: ClPos
        if (isXp2) {
            clPos = ClPos(mf.x, mf.y)
            val bx = mf.x / ClickConst.BLOCK_CLICKS
            val by = mf.y / ClickConst.BLOCK_CLICKS
            if (bx in 0 until cols && by in 0 until rows) world.setBlock(bx, by, CellType.FUEL)
        } else {
            val bx = mf.x
            val by = mf.y
            if (bx < 0 || bx >= cols || by < 0 || by >= rows) continue
            // block is already set to FUEL by the tile loop; just record entity
            clPos = BlkVec(bx, by).toCenterClPos()
        }
        world.fuels +=
            Fuel(
                pos = clPos,
                fuel = GameConst.MAX_STATION_FUEL,
                connMask = 0u,
                lastChange = 0L,
                team = 0,
            )
    }

    // ---- Wormholes ----
    for (mw in wormholes) {
        val clPos: ClPos
        if (isXp2) {
            clPos = ClPos(mw.x, mw.y)
            val bx = mw.x / ClickConst.BLOCK_CLICKS
            val by = mw.y / ClickConst.BLOCK_CLICKS
            if (bx in 0 until cols && by in 0 until rows) world.setBlock(bx, by, CellType.WORMHOLE)
        } else {
            val bx = mw.x
            val by = mw.y
            if (bx < 0 || bx >= cols || by < 0 || by >= rows) continue
            clPos = BlkVec(bx, by).toCenterClPos()
            world.setBlock(bx, by, CellType.WORMHOLE)
        }
        val wormType = when (mw.type.lowercase()) {
            "in" -> WormType.IN
            "out" -> WormType.OUT
            "fixed" -> WormType.FIXED
            else -> WormType.NORMAL
        }
        world.wormholes +=
            Wormhole(
                pos = clPos,
                type = wormType,
            )
    }

    // ---- Treasures ----
    for (mt in treasures) {
        val clPos: ClPos
        if (isXp2) {
            clPos = ClPos(mt.x, mt.y)
            val bx = mt.x / ClickConst.BLOCK_CLICKS
            val by = mt.y / ClickConst.BLOCK_CLICKS
            if (bx in 0 until cols && by in 0 until rows) world.setBlock(bx, by, CellType.TREASURE)
        } else {
            val bx = mt.x
            val by = mt.y
            if (bx < 0 || bx >= cols || by < 0 || by >= rows) continue
            clPos = BlkVec(bx, by).toCenterClPos()
            world.setBlock(bx, by, CellType.TREASURE)
        }
        world.treasures +=
            Treasure(
                pos = clPos,
                team = mt.team,
            )
    }

    // ---- Checkpoints ----
    for (mc in checkpoints) {
        val clPos: ClPos
        if (isXp2) {
            clPos = ClPos(mc.x, mc.y)
            val bx = mc.x / ClickConst.BLOCK_CLICKS
            val by = mc.y / ClickConst.BLOCK_CLICKS
            if (bx in 0 until cols && by in 0 until rows) world.setBlock(bx, by, CellType.CHECK)
        } else {
            val bx = mc.x
            val by = mc.y
            if (bx < 0 || bx >= cols || by < 0 || by >= rows) continue
            clPos = BlkVec(bx, by).toCenterClPos()
            world.setBlock(bx, by, CellType.CHECK)
        }
        world.checks += Check(pos = clPos)
    }

    // ---- Targets ----
    for (mt in targets) {
        val clPos: ClPos
        if (isXp2) {
            clPos = ClPos(mt.x, mt.y)
            val bx = mt.x / ClickConst.BLOCK_CLICKS
            val by = mt.y / ClickConst.BLOCK_CLICKS
            if (bx in 0 until cols && by in 0 until rows) world.setBlock(bx, by, CellType.TARGET)
        } else {
            val bx = mt.x
            val by = mt.y
            if (bx < 0 || bx >= cols || by < 0 || by >= rows) continue
            clPos = BlkVec(bx, by).toCenterClPos()
            // block already set to TARGET by tile loop
        }
        world.targets +=
            Target(
                pos = clPos,
                team = mt.team,
            )
    }

    // ---- Gravity sources ----
    for (mg in gravs) {
        val clPos: ClPos
        val bx: Int
        val by: Int
        if (isXp2) {
            clPos = ClPos(mg.x, mg.y)
            bx = mg.x / ClickConst.BLOCK_CLICKS
            by = mg.y / ClickConst.BLOCK_CLICKS
        } else {
            bx = mg.x
            by = mg.y
            clPos = BlkVec(bx, by).toCenterClPos()
        }
        val cellType = gravTypeToCellType(mg.type)
        if (bx in 0 until cols && by in 0 until rows) {
            world.setBlock(bx, by, cellType)
            // Apply gravity vector to this block
            val gvec = gravTypeToVector(mg.type)
            val existing = world.gravity[bx][by]
            world.gravity[bx][by] = Vector(existing.x + gvec.x, existing.y + gvec.y)
        }
        world.gravs +=
            Grav(
                pos = clPos,
                force = GRAVS_POWER.toDouble(),
                type = cellType,
            )
    }

    // ---- Item concentrators ----
    for (mi in itemConcentrators) {
        val clPos: ClPos
        if (isXp2) {
            clPos = ClPos(mi.x, mi.y)
            val bx = mi.x / ClickConst.BLOCK_CLICKS
            val by = mi.y / ClickConst.BLOCK_CLICKS
            if (bx in 0 until cols && by in 0 until rows) world.setBlock(bx, by, CellType.ITEM_CONCENTRATOR)
        } else {
            val bx = mi.x
            val by = mi.y
            if (bx < 0 || bx >= cols || by < 0 || by >= rows) continue
            clPos = BlkVec(bx, by).toCenterClPos()
            // block already set by tile loop
        }
        world.itemConcs += ItemConcentrator(pos = clPos)
    }

    // ---- Asteroid concentrators ----
    for (ma in asteroidConcentrators) {
        val clPos: ClPos
        if (isXp2) {
            clPos = ClPos(ma.x, ma.y)
            val bx = ma.x / ClickConst.BLOCK_CLICKS
            val by = ma.y / ClickConst.BLOCK_CLICKS
            if (bx in 0 until cols && by in 0 until rows) world.setBlock(bx, by, CellType.ASTEROID_CONCENTRATOR)
        } else {
            val bx = ma.x
            val by = ma.y
            if (bx < 0 || bx >= cols || by < 0 || by >= rows) continue
            clPos = BlkVec(bx, by).toCenterClPos()
            // block already set by tile loop
        }
        world.asteroidConcs += AsteroidConcentrator(pos = clPos)
    }

    // ---- Friction areas ----
    for (mf in frictionAreas) {
        val clPos: ClPos
        if (isXp2) {
            clPos = ClPos(mf.x, mf.y)
            val bx = mf.x / ClickConst.BLOCK_CLICKS
            val by = mf.y / ClickConst.BLOCK_CLICKS
            if (bx in 0 until cols && by in 0 until rows) world.setBlock(bx, by, CellType.FRICTION)
        } else {
            val bx = mf.x
            val by = mf.y
            if (bx < 0 || bx >= cols || by < 0 || by >= rows) continue
            clPos = BlkVec(bx, by).toCenterClPos()
            // block already set by tile loop
        }
        world.frictionAreas +=
            FrictionArea(
                pos = clPos,
                frictionSetting = 1.0,
                friction = 1.0,
                group = 0,
            )
    }

    return world
}

// ---------------------------------------------------------------------------
// Internal helpers
// ---------------------------------------------------------------------------

/**
 * Map [BlockType] (resource-layer) to [CellType] (server-layer).
 */
private fun blockTypeToCellType(bt: BlockType): CellType =
    when (bt) {
        BlockType.FILLED -> CellType.FILLED
        BlockType.REC_LU -> CellType.REC_LU
        BlockType.REC_LD -> CellType.REC_LD
        BlockType.REC_RU -> CellType.REC_RU
        BlockType.REC_RD -> CellType.REC_RD
        BlockType.FUEL -> CellType.FUEL
        BlockType.CANNON -> CellType.CANNON
        BlockType.CHECK -> CellType.CHECK
        BlockType.POS_GRAV -> CellType.POS_GRAV
        BlockType.NEG_GRAV -> CellType.NEG_GRAV
        BlockType.CWISE_GRAV -> CellType.CWISE_GRAV
        BlockType.ACWISE_GRAV -> CellType.ACWISE_GRAV
        BlockType.UP_GRAV -> CellType.UP_GRAV
        BlockType.DOWN_GRAV -> CellType.DOWN_GRAV
        BlockType.RIGHT_GRAV -> CellType.RIGHT_GRAV
        BlockType.LEFT_GRAV -> CellType.LEFT_GRAV
        BlockType.WORMHOLE, BlockType.WORM_IN, BlockType.WORM_OUT -> CellType.WORMHOLE
        BlockType.TREASURE, BlockType.EMPTY_TREASURE -> CellType.TREASURE
        BlockType.TARGET -> CellType.TARGET
        BlockType.ITEM_CONCENTRATOR -> CellType.ITEM_CONCENTRATOR
        BlockType.ASTEROID_CONCENTRATOR -> CellType.ASTEROID_CONCENTRATOR
        BlockType.DECOR_FILLED -> CellType.DECOR_FILLED
        BlockType.DECOR_LU -> CellType.DECOR_LU
        BlockType.DECOR_LD -> CellType.DECOR_LD
        BlockType.DECOR_RU -> CellType.DECOR_RU
        BlockType.DECOR_RD -> CellType.DECOR_RD
        BlockType.FRICTION -> CellType.FRICTION
        BlockType.BASE, BlockType.BASE_ATTRACTOR -> CellType.BASE
        BlockType.SPACE, BlockType.UNKNOWN -> CellType.SPACE
    }

/** Map a gravity-type string to its [CellType]. */
private fun gravTypeToCellType(type: String): CellType =
    when (type.lowercase()) {
        "neg" -> CellType.NEG_GRAV
        "cwise" -> CellType.CWISE_GRAV
        "acwise" -> CellType.ACWISE_GRAV
        "up" -> CellType.UP_GRAV
        "down" -> CellType.DOWN_GRAV
        "right" -> CellType.RIGHT_GRAV
        "left" -> CellType.LEFT_GRAV
        else -> CellType.POS_GRAV  // "pos" and default
    }

/**
 * Return the gravity [Vector] (pixels/tick²) for a given gravity-type string.
 * XPilot Y-up: downward = negative Y, upward = positive Y.
 *
 * Note: CWISE/ACWISE rotational gravity is position-relative; callers that
 * need accurate rotational gravity must implement `Compute_local_gravity()`.
 * Here we approximate by returning a zero vector (no net directional force at
 * the tile itself for rotational types).
 */
private fun gravTypeToVector(type: String): Vector =
    when (type.lowercase()) {
        "pos" -> Vector(0f, -GRAVS_POWER)   // pulls downward
        "neg" -> Vector(0f, GRAVS_POWER)    // pushes upward
        "up" -> Vector(0f, GRAVS_POWER)
        "down" -> Vector(0f, -GRAVS_POWER)
        "right" -> Vector(GRAVS_POWER, 0f)
        "left" -> Vector(-GRAVS_POWER, 0f)
        "cwise", "acwise" -> Vector.ZERO    // rotational: position-dependent; skip
        else -> Vector.ZERO
    }

/**
 * Return the gravity [Vector] (pixels/tick²) to apply for a tile [BlockType], or
 * `null` if this block type has no gravity effect.
 */
private fun gravityVector(bt: BlockType): Vector? =
    when (bt) {
        BlockType.POS_GRAV -> Vector(0f, -GRAVS_POWER)
        BlockType.NEG_GRAV -> Vector(0f, GRAVS_POWER)
        BlockType.UP_GRAV -> Vector(0f, GRAVS_POWER)
        BlockType.DOWN_GRAV -> Vector(0f, -GRAVS_POWER)
        BlockType.RIGHT_GRAV -> Vector(GRAVS_POWER, 0f)
        BlockType.LEFT_GRAV -> Vector(-GRAVS_POWER, 0f)
        // Rotational gravity (CWISE/ACWISE) is position-relative; handled per-entity
        // via world.gravs records rather than a fixed tile vector.
        else -> null
    }

/**
 * Convert a map direction in degrees (0 = right, counter-clockwise) to an
 * XPilot integer heading (0..RES-1, 0 = right, counter-clockwise).
 *
 * Used for `.xp2` maps whose `dir` attribute is in degrees.
 * Formula: `round(degrees * RES / 360) mod RES`
 */
private fun degreesToHeading(degrees: Int): Int =
    ((degrees * GameConst.RES / 360.0).roundToInt()).rem(GameConst.RES)
