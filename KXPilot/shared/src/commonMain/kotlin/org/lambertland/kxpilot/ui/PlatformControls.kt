package org.lambertland.kxpilot.ui

import androidx.compose.runtime.Composable
import org.lambertland.kxpilot.client.KeyState

/**
 * Platform-specific input overlay rendered on top of the game canvas.
 *
 * Desktop: empty — keyboard handles all input.
 * Android: virtual D-pad + action buttons that call [keys.press]/[keys.release].
 * wasmJs:  empty for now.
 */
@Composable
expect fun PlatformControls(keys: KeyState)
