package org.lambertland.kxpilot.ui.screens

import androidx.compose.runtime.Composable
import org.lambertland.kxpilot.ui.InGameScreen

/** Desktop actual: delegates to [InGameScreen]. Host/port are plumbed for future use. */
@Composable
actual fun DemoGameScreen(
    serverHost: String,
    serverPort: Int,
) = InGameScreen()
