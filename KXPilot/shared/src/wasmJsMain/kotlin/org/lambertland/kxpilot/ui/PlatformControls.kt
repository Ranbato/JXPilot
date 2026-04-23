package org.lambertland.kxpilot.ui

import androidx.compose.runtime.Composable
import org.lambertland.kxpilot.client.KeyState

/** wasmJs actual: no touch controls on desktop browser. */
@Composable
actual fun PlatformControls(keys: KeyState) { /* no touch on desktop browser */ }
