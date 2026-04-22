package org.lambertland.kxpilot.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.lambertland.kxpilot.model.MessageColor
import org.lambertland.kxpilot.model.MessageEntry
import org.lambertland.kxpilot.ui.theme.KXPilotColors
import kotlin.math.cos
import kotlin.math.sin

// ---------------------------------------------------------------------------
// MeterBar
// ---------------------------------------------------------------------------

/**
 * A horizontal labelled progress bar suitable for HUD gauges (fuel, power, etc.).
 *
 * @param value  Current value (0 ≤ value ≤ max).
 * @param max    Maximum value.
 * @param color  Fill color of the bar.
 * @param label  Short label shown to the left.
 * @param suffix Optional text shown to the right (e.g. "450 / 1000").
 * @param width  Total width of the widget.
 * @param barHeight Height of the bar track.
 */
@Composable
fun MeterBar(
    value: Float,
    max: Float,
    color: Color,
    label: String,
    suffix: String = "",
    width: Dp = 200.dp,
    barHeight: Dp = 8.dp,
) {
    val fraction = (value / max.coerceAtLeast(0.001f)).coerceIn(0f, 1f)
    val labelStyle = TextStyle(color = KXPilotColors.OnSurfaceDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    val suffixStyle = TextStyle(color = KXPilotColors.OnSurface, fontSize = 10.sp, fontFamily = FontFamily.Monospace)

    Row(
        modifier = Modifier.width(width),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label.padEnd(4), style = labelStyle)
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(barHeight)
                    .background(KXPilotColors.SurfaceVariant),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .background(color),
            )
        }
        if (suffix.isNotEmpty()) {
            Text(suffix, style = suffixStyle)
        }
    }
}

// ---------------------------------------------------------------------------
// HeadingCompass
// ---------------------------------------------------------------------------

/**
 * A small circular compass dial showing the ship's current heading.
 *
 * Draws a dim ring and a bright needle pointing in the ship's facing direction.
 * Uses XPilot heading convention (0 = right, 32 = up, 64 = left, 96 = down;
 * Y-up world space) and converts to canvas angles correctly.
 *
 * @param headingRad  Ship heading in radians (Y-up; 0 = right, π/2 = up).
 * @param size        Diameter of the compass widget.
 */
@Composable
fun HeadingCompass(
    headingRad: Float,
    size: Dp = 36.dp,
) {
    Canvas(modifier = Modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val radius = (this.size.width / 2f) - 2f
        val needleLen = radius * 0.75f

        // Outer ring
        drawCircle(
            color = KXPilotColors.SurfaceVariant,
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5f),
        )

        // Needle: world heading (Y-up, 0=right) → canvas angle (Y-down, 0=right)
        // In canvas: angle measured clockwise from east.
        // headingRad in Y-up: cos=east, sin=north(up).
        // In canvas Y-down: north is -Y, so canvasAngle = -headingRad.
        val nx = cx + cos(-headingRad) * needleLen
        val ny = cy + sin(-headingRad) * needleLen

        drawLine(
            color = KXPilotColors.Hud,
            start = Offset(cx, cy),
            end = Offset(nx, ny),
            strokeWidth = 2f,
            cap = StrokeCap.Round,
        )

        // Centre dot
        drawCircle(
            color = KXPilotColors.HudDim,
            radius = 2f,
            center = Offset(cx, cy),
        )
    }
}

// ---------------------------------------------------------------------------
// ShieldIndicator
// ---------------------------------------------------------------------------

/**
 * A small badge that glows when the shield is active and dims when inactive.
 *
 * @param active  `true` while the shield key is held.
 */
@Composable
fun ShieldIndicator(active: Boolean) {
    val color = if (active) KXPilotColors.Accent else KXPilotColors.SurfaceVariant
    val textColor = if (active) KXPilotColors.AccentBright else KXPilotColors.OnSurfaceDim
    Box(
        modifier =
            Modifier
                .background(color)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "SHIELD",
            style =
                TextStyle(
                    color = textColor,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                ),
        )
    }
}

// ---------------------------------------------------------------------------
// MessageLog
// ---------------------------------------------------------------------------

/** Display-duration for HUD messages in milliseconds. */
const val MESSAGE_DISPLAY_MS = 8_000L

/**
 * Renders the in-game message log as a plain [Column].  Messages fade out as
 * they age; expired messages are filtered before rendering.
 *
 * @param messages   Full message list (may contain expired items).
 * @param maxMessages Maximum number of messages to display at once.
 * @param displayMs   Time before a message fades to alpha=0 (millis).
 * @param nowMs       Current wall-clock time in millis.
 */
@Composable
fun MessageLog(
    messages: List<MessageEntry>,
    maxMessages: Int = 8,
    displayMs: Long = MESSAGE_DISPLAY_MS,
    nowMs: Long,
) {
    val visible =
        messages
            .filter { (nowMs - it.arrivedAt) < displayMs }
            .takeLast(maxMessages)

    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        for (msg in visible) {
            val age = (nowMs - msg.arrivedAt).toFloat() / displayMs
            val alpha = (1f - age.coerceIn(0f, 1f))
            val baseColor = msg.color.toComposeColor()
            Text(
                text = msg.text,
                style =
                    TextStyle(
                        color = baseColor.copy(alpha = alpha),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                    ),
            )
        }
    }
}

private fun MessageColor.toComposeColor(): Color =
    when (this) {
        MessageColor.NORMAL -> KXPilotColors.OnSurface
        MessageColor.BALL -> Color(0xFF00CC44)
        MessageColor.SAFE -> Color(0xFF44CCFF)
        MessageColor.COVER -> Color(0xFFFFCC44)
        MessageColor.POP -> Color(0xFFFF4444)
    }
