package org.lambertland.kxpilot.ui

import androidx.compose.runtime.Composable
import org.lambertland.kxpilot.client.KeyState

/** Desktop actual: keyboard handles all input — no on-screen controls needed. */
@Composable
actual fun PlatformControls(keys: KeyState) { /* keyboard only */ }
