package org.lambertland.kxpilot.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import org.lambertland.kxpilot.common.Item

// ---------------------------------------------------------------------------
// ItemRenderer — shared bonus-item draw code
// ---------------------------------------------------------------------------
//
// Used by both InGameScreen (to draw WorldItems on the game canvas) and
// AboutScreen (to draw item icons in the bonus-items list).
//
// Each item is drawn centred at (cx, cy) within a square of side `size`.
// Colours match the C client's item colours (client/paint.c).

/** Canonical game-world colours for each item type.  Alpha = fully opaque. */
val ITEM_COLORS: Map<Item, Color> =
    mapOf(
        Item.FUEL to Color(0xFF226622),
        Item.WIDEANGLE to Color(0xFFFFFFFF),
        Item.REARSHOT to Color(0xFFCCCCCC),
        Item.AFTERBURNER to Color(0xFFFFAA00),
        Item.CLOAK to Color(0xFF444488),
        Item.SENSOR to Color(0xFF88FF88),
        Item.TRANSPORTER to Color(0xFFFF88FF),
        Item.TANK to Color(0xFF664422),
        Item.MINE to Color(0xFF00FFFF),
        Item.MISSILE to Color(0xFFFF4444),
        Item.ECM to Color(0xFFFFFF00),
        Item.LASER to Color(0xFFFF2200),
        Item.EMERGENCY_THRUST to Color(0xFFFFAA00),
        Item.TRACTOR_BEAM to Color(0xFF4488FF),
        Item.AUTOPILOT to Color(0xFF44FF44),
        Item.EMERGENCY_SHIELD to Color(0xFF4444FF),
        Item.DEFLECTOR to Color(0xFF888888),
        Item.HYPERJUMP to Color(0xFF8800FF),
        Item.PHASING to Color(0xFF00FFCC),
        Item.MIRROR to Color(0xFFAABBCC),
        Item.ARMOR to Color(0xFF886600),
    )

/**
 * Draw a recognisable game-style icon for [itemType] centred within the
 * current [DrawScope].  The icon is scaled to fit a square of [iconSize]
 * pixels.  [color] overrides the default [ITEM_COLORS] entry when non-null.
 *
 * Call this inside a `translate(cx, cy)` block to position the icon, or
 * pass explicit [cx]/[cy] offsets.
 */
fun DrawScope.drawItemIcon(
    itemType: Item,
    iconSize: Float = minOf(size.width, size.height),
    color: Color = ITEM_COLORS[itemType] ?: Color.White,
    cx: Float = size.width / 2f,
    cy: Float = size.height / 2f,
) {
    val half = iconSize / 2f
    val r = half - 1f
    val strokeW = iconSize * 0.10f
    val left = cx - half + 1f
    val top = cy - half + 1f
    val right = cx + half - 1f
    val bottom = cy + half - 1f

    when (itemType) {
        // Fuel — green filled rectangle with a horizontal stripe
        Item.FUEL -> {
            drawRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
            )
            drawLine(
                color = Color.Black.copy(alpha = 0.4f),
                start = Offset(left, cy),
                end = Offset(right, cy),
                strokeWidth = strokeW,
            )
        }

        // Wide-angle / rear shot — small bright circle (extra shot)
        Item.WIDEANGLE, Item.REARSHOT -> {
            drawCircle(color = color, radius = r * 0.45f, center = Offset(cx, cy))
        }

        // Afterburner — orange filled circle with glow ring (shot power)
        Item.AFTERBURNER -> {
            drawCircle(color = color.copy(alpha = 0.35f), radius = r, center = Offset(cx, cy))
            drawCircle(color = color, radius = r * 0.5f, center = Offset(cx, cy))
        }

        // Missile — red right-pointing filled triangle
        Item.MISSILE -> {
            val path =
                Path().apply {
                    moveTo(right, cy)
                    lineTo(left, top)
                    lineTo(left, bottom)
                    close()
                }
            drawPath(path, color = color)
        }

        // Mine — cyan circle outline with four spikes (NSEW)
        Item.MINE -> {
            drawCircle(color = color, radius = r - 1f, center = Offset(cx, cy), style = Stroke(strokeW))
            drawLine(color = color, start = Offset(cx, top), end = Offset(cx, bottom), strokeWidth = strokeW)
            drawLine(color = color, start = Offset(left, cy), end = Offset(right, cy), strokeWidth = strokeW)
        }

        // Cloak — dark blue filled diamond
        Item.CLOAK -> {
            val path =
                Path().apply {
                    moveTo(cx, top)
                    lineTo(right, cy)
                    lineTo(cx, bottom)
                    lineTo(left, cy)
                    close()
                }
            drawPath(path, color = color)
        }

        // Sensor — green diamond outline (distinct from Cloak's filled diamond)
        Item.SENSOR -> {
            val path =
                Path().apply {
                    moveTo(cx, top)
                    lineTo(right, cy)
                    lineTo(cx, bottom)
                    lineTo(left, cy)
                    close()
                }
            drawPath(path, color = color, style = Stroke(strokeW))
            // Inner dot to distinguish from mine
            drawCircle(color = color, radius = r * 0.2f, center = Offset(cx, cy))
        }

        // ECM — yellow 4-pointed star
        Item.ECM -> {
            val arm = r * 0.9f
            val inner = r * 0.35f
            val path =
                Path().apply {
                    moveTo(cx, cy - arm)
                    lineTo(cx + inner, cy - inner)
                    lineTo(cx + arm, cy)
                    lineTo(cx + inner, cy + inner)
                    lineTo(cx, cy + arm)
                    lineTo(cx - inner, cy + inner)
                    lineTo(cx - arm, cy)
                    lineTo(cx - inner, cy - inner)
                    close()
                }
            drawPath(path, color = color)
        }

        // Transporter — magenta filled circle with inner void
        Item.TRANSPORTER -> {
            drawCircle(color = color, radius = r - 1f, center = Offset(cx, cy))
            drawCircle(color = Color.Black.copy(alpha = 0.5f), radius = r * 0.3f, center = Offset(cx, cy))
        }

        // Tractor Beam — blue left-pointing arrow (pulling)
        Item.TRACTOR_BEAM -> {
            val shaft = strokeW * 1.2f
            val path =
                Path().apply {
                    moveTo(left, cy)
                    lineTo(cx, top)
                    lineTo(cx, cy - shaft)
                    lineTo(right, cy - shaft)
                    lineTo(right, cy + shaft)
                    lineTo(cx, cy + shaft)
                    lineTo(cx, bottom)
                    close()
                }
            drawPath(path, color = color)
        }

        // Emergency Thrust — orange right-pointing arrow (pushing, same shape, different color)
        Item.EMERGENCY_THRUST -> {
            val shaft = strokeW * 1.2f
            val path =
                Path().apply {
                    moveTo(right, cy)
                    lineTo(cx, top)
                    lineTo(cx, cy - shaft)
                    lineTo(left, cy - shaft)
                    lineTo(left, cy + shaft)
                    lineTo(cx, cy + shaft)
                    lineTo(cx, bottom)
                    close()
                }
            drawPath(path, color = color)
        }

        // Laser — red horizontal beam line with bright core
        Item.LASER -> {
            drawLine(
                color = color,
                start = Offset(left - 1f, cy),
                end = Offset(right + 1f, cy),
                strokeWidth = strokeW * 1.8f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(left - 1f, cy),
                end = Offset(right + 1f, cy),
                strokeWidth = strokeW * 0.5f,
                cap = StrokeCap.Round,
            )
        }

        // Deflector — gray double chevron
        Item.DEFLECTOR -> {
            val sw = strokeW * 1.5f
            drawLine(color = color, start = Offset(left, top), end = Offset(cx, cy), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(color = color, start = Offset(cx, cy), end = Offset(left, bottom), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(color = color, start = Offset(cx + 2f, top), end = Offset(right, cy), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(color = color, start = Offset(right, cy), end = Offset(cx + 2f, bottom), strokeWidth = sw, cap = StrokeCap.Round)
        }

        // Hyperjump — purple circle outline with inner dot
        Item.HYPERJUMP -> {
            drawCircle(color = color, radius = r - 1f, center = Offset(cx, cy), style = Stroke(strokeW))
            drawCircle(color = color, radius = r * 0.3f, center = Offset(cx, cy))
        }

        // Phasing — cyan rectangle outline (semi-transparent inner fill)
        Item.PHASING -> {
            drawRect(
                color = color.copy(alpha = 0.5f),
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(strokeW),
            )
            drawRect(
                color = color.copy(alpha = 0.2f),
                topLeft = Offset(left + 3f, top + 3f),
                size = Size(right - left - 6f, bottom - top - 6f),
            )
        }

        // Mirror — light blue diagonal line (reflective surface)
        Item.MIRROR -> {
            drawLine(
                color = color,
                start = Offset(left, bottom),
                end = Offset(right, top),
                strokeWidth = strokeW * 1.5f,
                cap = StrokeCap.Round,
            )
        }

        // Armor — orange upward-pointing filled triangle
        Item.ARMOR -> {
            val path =
                Path().apply {
                    moveTo(cx, top)
                    lineTo(right, bottom)
                    lineTo(left, bottom)
                    close()
                }
            drawPath(path, color = color)
        }

        // Autopilot — green circle outline with center dot
        Item.AUTOPILOT -> {
            drawCircle(color = color, radius = r - 1f, center = Offset(cx, cy), style = Stroke(strokeW))
            drawCircle(color = color, radius = r * 0.25f, center = Offset(cx, cy))
        }

        // Emergency Shield — blue pentagon outline
        Item.EMERGENCY_SHIELD -> {
            val pts = 5
            val startAngle = -kotlin.math.PI / 2.0
            val path =
                Path().apply {
                    for (i in 0 until pts) {
                        val angle = startAngle + i * 2.0 * kotlin.math.PI / pts
                        val px = cx + (r - 1f) * kotlin.math.cos(angle).toFloat()
                        val py = cy + (r - 1f) * kotlin.math.sin(angle).toFloat()
                        if (i == 0) moveTo(px, py) else lineTo(px, py)
                    }
                    close()
                }
            drawPath(path, color = color, style = Stroke(strokeW))
        }

        // Tank — brown filled rectangle (squat tank body with cap)
        Item.TANK -> {
            drawRect(
                color = color,
                topLeft = Offset(left, cy - half * 0.4f),
                size = Size(right - left, half * 0.8f),
            )
            drawRect(
                color = color.copy(alpha = 0.7f),
                topLeft = Offset(left + 3f, top + 3f),
                size = Size(right - left - 6f, 4f),
            )
        }

        // NO_ITEM — should never be drawn; fall through to default
        Item.NO_ITEM -> {
            // Fallback — small gray circle
            drawCircle(color = Color.Gray, radius = r * 0.4f, center = Offset(cx, cy))
        }
    }
}
