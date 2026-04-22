package org.lambertland.kxpilot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.lambertland.kxpilot.ui.theme.KXPilotColors

// ---------------------------------------------------------------------------
// Shared low-level UI components
// ---------------------------------------------------------------------------

/**
 * A minimal game-aesthetic button: dark background, coloured border, monospace
 * label.  Avoids Material3 button chrome (rounded corners, ripple fill, etc.).
 */
@Composable
fun GameButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = KXPilotColors.Accent,
    enabled: Boolean = true,
) {
    val effectiveColor = if (enabled) color else KXPilotColors.OnSurfaceDim
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .background(KXPilotColors.SurfaceVariant)
                .border(1.dp, effectiveColor)
                .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style =
                TextStyle(
                    color = effectiveColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                ),
        )
    }
}

/** A dimmer variant used for secondary / destructive actions. */
@Composable
fun GameButtonDanger(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = GameButton(label, onClick, modifier, KXPilotColors.Danger)
