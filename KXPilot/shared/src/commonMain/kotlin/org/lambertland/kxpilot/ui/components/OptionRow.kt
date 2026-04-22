package org.lambertland.kxpilot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.lambertland.kxpilot.common.formatOneDecimal
import org.lambertland.kxpilot.config.AppConfig
import org.lambertland.kxpilot.config.XpOptionDef
import org.lambertland.kxpilot.ui.theme.KXPilotColors

// ---------------------------------------------------------------------------
// OptionRowRenderer interface — visitor pattern
// ---------------------------------------------------------------------------

interface OptionRowRenderer {
    @Composable fun Bool(
        def: XpOptionDef.Bool,
        value: Boolean,
        onSet: (Boolean) -> Unit,
    )

    @Composable fun Int(
        def: XpOptionDef.Int,
        value: Int,
        onSet: (Int) -> Unit,
    )

    @Composable fun Double(
        def: XpOptionDef.Double,
        value: Double,
        onSet: (Double) -> Unit,
    )

    @Composable fun ColorIndex(
        def: XpOptionDef.ColorIndex,
        value: Int,
        onSet: (Int) -> Unit,
    )
}

// ---------------------------------------------------------------------------
// Default implementation
// ---------------------------------------------------------------------------

object DefaultOptionRowRenderer : OptionRowRenderer {
    @Composable
    override fun Bool(
        def: XpOptionDef.Bool,
        value: Boolean,
        onSet: (Boolean) -> Unit,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OptionLabel(def.name)
            Spacer(Modifier.weight(1f))
            Switch(
                checked = value,
                onCheckedChange = onSet,
                colors =
                    SwitchDefaults.colors(
                        checkedThumbColor = KXPilotColors.Accent,
                        checkedTrackColor = KXPilotColors.Accent.copy(alpha = 0.5f),
                        uncheckedThumbColor = KXPilotColors.OnSurfaceDim,
                        uncheckedTrackColor = KXPilotColors.SurfaceVariant,
                    ),
            )
        }
    }

    @Composable
    override fun Int(
        def: XpOptionDef.Int,
        value: Int,
        onSet: (Int) -> Unit,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OptionLabel(def.name)
            Spacer(Modifier.weight(1f))
            StepButton("◀") { if (value > def.min) onSet(value - 1) }
            Spacer(Modifier.width(6.dp))
            ValueLabel(value.toString())
            Spacer(Modifier.width(6.dp))
            StepButton("▶") { if (value < def.max) onSet(value + 1) }
        }
    }

    @Composable
    override fun Double(
        def: XpOptionDef.Double,
        value: Double,
        onSet: (Double) -> Unit,
    ) {
        val step = if (def.max - def.min >= 10.0) 1.0 else 0.1
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OptionLabel(def.name)
            Spacer(Modifier.weight(1f))
            StepButton("◀") { if (value > def.min) onSet((value - step).coerceAtLeast(def.min)) }
            Spacer(Modifier.width(6.dp))
            ValueLabel(value.formatOneDecimal())
            Spacer(Modifier.width(6.dp))
            StepButton("▶") { if (value < def.max) onSet((value + step).coerceAtMost(def.max)) }
        }
    }

    @Composable
    override fun ColorIndex(
        def: XpOptionDef.ColorIndex,
        value: Int,
        onSet: (Int) -> Unit,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OptionLabel(def.name)
            Spacer(Modifier.weight(1f))
            // Show all 16 palette swatches; highlight selected
            for (i in 0..15) {
                val color = KXPilotColors.palette[i]
                val isSelected = i == value
                Box(
                    modifier =
                        Modifier
                            .size(18.dp)
                            .background(color)
                            .then(if (isSelected) Modifier.border(2.dp, KXPilotColors.AccentBright) else Modifier)
                            .clickable { onSet(i) },
                )
                if (i < 15) Spacer(Modifier.width(2.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// OptionRow dispatch composable
// ---------------------------------------------------------------------------

@Composable
fun OptionRow(
    def: XpOptionDef<*>,
    config: AppConfig,
    renderer: OptionRowRenderer = DefaultOptionRowRenderer,
) {
    @Suppress("UNCHECKED_CAST")
    when (def) {
        is XpOptionDef.ColorIndex -> {
            renderer.ColorIndex(def, config.get(def)) { config.set(def, it) }
        }

        is XpOptionDef.Int -> {
            renderer.Int(def, config.get(def)) { config.set(def, it) }
        }

        is XpOptionDef.Bool -> {
            renderer.Bool(def, config.get(def)) { config.set(def, it) }
        }

        is XpOptionDef.Double -> {
            renderer.Double(def, config.get(def)) { config.set(def, it) }
        }

        is XpOptionDef.Str -> { /* string options not shown in config UI */ }
    }
}

// ---------------------------------------------------------------------------
// Private helpers
// ---------------------------------------------------------------------------

private val labelStyle
    @Composable get() =
        TextStyle(
            color = KXPilotColors.OnSurface,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
        )

@Composable
private fun OptionLabel(text: String) {
    Text(text, style = labelStyle)
}

@Composable
private fun ValueLabel(text: String) {
    Text(
        text,
        style = TextStyle(color = KXPilotColors.Accent, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
        modifier = Modifier.width(52.dp),
    )
}

@Composable
private fun StepButton(
    label: String,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(22.dp)
                .background(KXPilotColors.Surface)
                .border(1.dp, KXPilotColors.Accent)
                .clickable(onClick = onClick),
    ) {
        Text(label, style = TextStyle(color = KXPilotColors.Accent, fontSize = 11.sp, fontFamily = FontFamily.Monospace))
    }
}
