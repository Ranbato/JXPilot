package org.lambertland.kxpilot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.lambertland.kxpilot.ui.stateholder.TalkStateHolder
import org.lambertland.kxpilot.ui.theme.KXPilotColors

// ---------------------------------------------------------------------------
// TalkOverlay composable
// ---------------------------------------------------------------------------

/**
 * Semi-transparent text-entry bar rendered at the bottom of the game screen.
 * Visible only when [state.isVisible] is true.
 *
 * Key handling (Enter/Escape/Up/Down) must be wired at the platform level
 * (e.g. `onKeyEvent` in desktopMain) and delegated to [state.submit],
 * [state.close], and [state.browseHistory].
 */
@Composable
fun TalkOverlay(state: TalkStateHolder) {
    if (!state.isVisible) return

    Row(
        modifier =
            Modifier
                .fillMaxWidth(0.8f)
                .background(KXPilotColors.Surface.copy(alpha = 0.85f))
                .border(1.dp, KXPilotColors.Accent)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Talk: ",
            style =
                TextStyle(
                    color = KXPilotColors.OnSurfaceDim,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                ),
        )
        Spacer(Modifier.width(4.dp))
        BasicTextField(
            value = state.text,
            onValueChange = { state.text = it },
            singleLine = true,
            textStyle =
                TextStyle(
                    color = KXPilotColors.OnSurface,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                ),
            cursorBrush = SolidColor(KXPilotColors.Accent),
            modifier = Modifier.weight(1f),
        )
    }
}
