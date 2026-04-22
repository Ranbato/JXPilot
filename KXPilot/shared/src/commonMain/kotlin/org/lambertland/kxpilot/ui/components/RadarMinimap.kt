package org.lambertland.kxpilot.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.lambertland.kxpilot.resources.BlockType
import org.lambertland.kxpilot.resources.MapTile

// ---------------------------------------------------------------------------
// Radar minimap composable
// ---------------------------------------------------------------------------
//
// The static tile layer is rendered once into an ImageBitmap (keyed on the
// tile list identity) and blitted each frame.  Only ship blips and the
// viewport rect are redrawn every tick.
//
// Coordinate system:
//   • World space is Y-up (origin bottom-left), same as the game engine.
//   • The minimap canvas is Y-down (Compose default).
//   • minimap Y = canvasSize - (worldY / worldH) * canvasSize  (Y-flip)
//
// All tile positions in XPilotMap are in block units (col, row).
// Block (0,0) is the bottom-left corner of the world (XPilot convention).
// ---------------------------------------------------------------------------

private val COL_BACKGROUND = Color(0xFF0A0A14)
private val COL_WALL = Color(0xFF4466AA)
private val COL_DIAGONAL = Color(0xFF3355AA)
private val COL_FUEL = Color(0xFF226622)
private val COL_BASE = Color(0xFF664400)
private val COL_VIEWPORT = Color(0x44FFFFFF)
private val COL_NPC = Color(0xFF00CCCC)
private val COL_PLAYER = Color(0xFFFFFFFF)
private val COL_BORDER = Color(0xFF334466)

private val SOLID_TYPES =
    setOf(
        BlockType.FILLED,
        BlockType.REC_LU,
        BlockType.REC_LD,
        BlockType.REC_RU,
        BlockType.REC_RD,
        BlockType.DECOR_FILLED,
        BlockType.DECOR_LU,
        BlockType.DECOR_LD,
        BlockType.DECOR_RU,
        BlockType.DECOR_RD,
    )

private val DIAGONAL_TYPES =
    setOf(
        BlockType.REC_LU,
        BlockType.REC_LD,
        BlockType.REC_RU,
        BlockType.REC_RD,
        BlockType.DECOR_LU,
        BlockType.DECOR_LD,
        BlockType.DECOR_RU,
        BlockType.DECOR_RD,
    )

/** Pixel size used for the off-screen tile bitmap. */
private const val TILE_BITMAP_PX = 256

/**
 * Radar minimap overlay for the in-game HUD.
 *
 * @param tiles             All map tiles from [XPilotMap.tiles] — should be stable/remembered.
 * @param mapWidthBlocks    Total map width in blocks.
 * @param mapHeightBlocks   Total map height in blocks.
 * @param worldW            World pixel width.
 * @param worldH            World pixel height.
 * @param playerX           Engine player X in world pixels (Y-up).
 * @param playerY           Engine player Y in world pixels (Y-up).
 * @param npcPositions      NPC ship positions as (x, y) pairs in world pixels (Y-up).
 * @param viewportOriginX   Camera world-pixel X of the left edge of the viewport.
 * @param viewportOriginY   Camera world-pixel Y of the bottom edge (Y-up).
 * @param viewportW         Viewport width in world pixels.
 * @param viewportH         Viewport height in world pixels.
 * @param size              Side length of the square minimap canvas.
 */
@Composable
fun RadarMinimap(
    tiles: List<MapTile>,
    mapWidthBlocks: Int,
    mapHeightBlocks: Int,
    worldW: Float,
    worldH: Float,
    playerX: Float,
    playerY: Float,
    npcPositions: List<Pair<Float, Float>>,
    viewportOriginX: Float,
    viewportOriginY: Float,
    viewportW: Float,
    viewportH: Float,
    size: Dp = 180.dp,
    modifier: Modifier = Modifier,
) {
    // Build the static tile bitmap once (tiles never change at runtime).
    val tileBitmap: ImageBitmap =
        remember(tiles, mapWidthBlocks, mapHeightBlocks) {
            buildTileBitmap(tiles, mapWidthBlocks, mapHeightBlocks)
        }

    Canvas(
        modifier =
            modifier
                .size(size)
                .border(1.dp, COL_BORDER),
    ) {
        val canvasW = this.size.width
        val canvasH = this.size.height

        // Blit pre-rendered tile layer
        drawIntoCanvas { canvas ->
            canvas.drawImageRect(
                image = tileBitmap,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(TILE_BITMAP_PX, TILE_BITMAP_PX),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(canvasW.toInt(), canvasH.toInt()),
                paint = Paint(),
            )
        }

        val scaleX = canvasW / worldW
        val scaleY = canvasH / worldH

        // --- Viewport rectangle ---
        val vpLeft = viewportOriginX * scaleX
        val vpBottom = viewportOriginY * scaleY
        val vpW = viewportW * scaleX
        val vpH = viewportH * scaleY
        val vpTop = canvasH - (vpBottom + vpH)
        drawRect(color = COL_VIEWPORT, topLeft = Offset(vpLeft, vpTop), size = Size(vpW, vpH))

        // --- NPC blips ---
        val npcRadius = (canvasW / mapWidthBlocks).coerceAtLeast(2f) * 0.7f
        for ((nx, ny) in npcPositions) {
            drawCircle(color = COL_NPC, radius = npcRadius, center = Offset(nx * scaleX, canvasH - ny * scaleY))
        }

        // --- Player blip ---
        val playerRadius = (canvasW / mapWidthBlocks).coerceAtLeast(2f) * 1.0f
        val px = playerX * scaleX
        val py = canvasH - playerY * scaleY
        drawCircle(color = COL_PLAYER.copy(alpha = 0.35f), radius = playerRadius * 2.2f, center = Offset(px, py))
        drawCircle(color = COL_PLAYER, radius = playerRadius, center = Offset(px, py))
    }
}

// ---------------------------------------------------------------------------
// Off-screen tile bitmap (rendered once)
// ---------------------------------------------------------------------------

private fun buildTileBitmap(
    tiles: List<MapTile>,
    mapWidthBlocks: Int,
    mapHeightBlocks: Int,
): ImageBitmap {
    val bmp = ImageBitmap(TILE_BITMAP_PX, TILE_BITMAP_PX)
    val canvas = Canvas(bmp)
    val paint = Paint()

    val bW = TILE_BITMAP_PX.toFloat() / mapWidthBlocks
    val bH = TILE_BITMAP_PX.toFloat() / mapHeightBlocks

    // Background
    paint.color = COL_BACKGROUND
    canvas.drawRect(
        androidx.compose.ui.geometry
            .Rect(0f, 0f, TILE_BITMAP_PX.toFloat(), TILE_BITMAP_PX.toFloat()),
        paint,
    )

    for (tile in tiles) {
        val color =
            when {
                tile.type in DIAGONAL_TYPES -> COL_DIAGONAL
                tile.type == BlockType.FUEL -> COL_FUEL
                tile.type == BlockType.BASE -> COL_BASE
                tile.type in SOLID_TYPES -> COL_WALL
                else -> continue
            }
        paint.color = color
        val left = tile.col * bW
        val top = TILE_BITMAP_PX - (tile.row + 1) * bH
        canvas.drawRect(
            androidx.compose.ui.geometry
                .Rect(left, top, left + bW, top + bH),
            paint,
        )
    }

    return bmp
}
