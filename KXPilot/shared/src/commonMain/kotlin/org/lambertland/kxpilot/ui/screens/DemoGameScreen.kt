package org.lambertland.kxpilot.ui.screens

import androidx.compose.runtime.Composable

/**
 * Navigates into the game view connected to [serverHost]:[serverPort].
 *
 * Currently the desktop actual delegates to [InGameScreen] which runs in
 * local demo mode — the host/port parameters are plumbed through for when
 * real network connection is implemented.
 */
@Composable
expect fun DemoGameScreen(
    serverHost: String,
    serverPort: Int,
)
