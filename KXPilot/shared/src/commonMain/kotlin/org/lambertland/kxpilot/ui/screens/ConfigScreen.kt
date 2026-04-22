package org.lambertland.kxpilot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.lambertland.kxpilot.config.AppConfig
import org.lambertland.kxpilot.config.LocalAppConfig
import org.lambertland.kxpilot.config.OptionFlag
import org.lambertland.kxpilot.config.XpOptionDef
import org.lambertland.kxpilot.ui.LocalNavigator
import org.lambertland.kxpilot.ui.components.GameButton
import org.lambertland.kxpilot.ui.components.OptionRow
import org.lambertland.kxpilot.ui.theme.KXPilotColors

// ---------------------------------------------------------------------------
// Tab
// ---------------------------------------------------------------------------

private enum class ConfigTab { DEFAULT, COLORS }

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@Composable
fun ConfigScreen(onSaveConfig: (String) -> Unit = {}) {
    val navigator = LocalNavigator.current
    val config = LocalAppConfig.current

    var tab by remember { mutableStateOf(ConfigTab.DEFAULT) }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(KXPilotColors.Background),
    ) {
        // ---- Title bar ----
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(KXPilotColors.SurfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Settings",
                style =
                    TextStyle(
                        color = KXPilotColors.AccentBright,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    ),
            )
            Spacer(Modifier.weight(1f))
            if (saved) {
                Text(
                    "Saved.",
                    style = TextStyle(color = KXPilotColors.AccentBright, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                )
                Spacer(Modifier.width(12.dp))
            }
            GameButton("SAVE", onClick = {
                onSaveConfig(config.toRcText())
                saved = true
            })
            Spacer(Modifier.width(8.dp))
            GameButton("CLOSE", onClick = { navigator.pop() })
        }

        // ---- Tab row ----
        Row(
            modifier = Modifier.fillMaxWidth().background(KXPilotColors.Surface),
        ) {
            ConfigTab.values().forEach { t ->
                ConfigTabButton(t.name, selected = tab == t) {
                    tab = t
                    saved = false
                }
            }
        }

        // ---- Divider ----
        Box(Modifier.fillMaxWidth().height(1.dp).background(KXPilotColors.Accent))

        // ---- Option list ----
        val visibleDefs =
            remember(tab) {
                config
                    .allDefs()
                    .filter { def ->
                        when (tab) {
                            ConfigTab.DEFAULT -> OptionFlag.CONFIG_DEFAULT in def.flags
                            ConfigTab.COLORS -> OptionFlag.CONFIG_COLORS in def.flags
                        }
                    }.filterNot { it is XpOptionDef.Str }
            }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(vertical = 4.dp)) {
            items(visibleDefs, key = { it.name }) { def ->
                OptionRow(def = def, config = config)
                // Thin divider between rows
                Box(Modifier.fillMaxWidth().height(1.dp).background(KXPilotColors.SurfaceVariant))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Tab button
// ---------------------------------------------------------------------------

@Composable
private fun ConfigTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) KXPilotColors.Background else KXPilotColors.Surface
    val textColor = if (selected) KXPilotColors.Accent else KXPilotColors.OnSurfaceDim
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .background(bg)
                .then(if (selected) Modifier.border(0.dp, KXPilotColors.Accent) else Modifier)
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            style =
                TextStyle(
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    fontFamily = FontFamily.Monospace,
                ),
        )
    }
}
