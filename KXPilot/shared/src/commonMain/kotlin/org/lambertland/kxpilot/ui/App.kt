package org.lambertland.kxpilot.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import org.lambertland.kxpilot.config.AppConfig
import org.lambertland.kxpilot.config.LocalAppConfig
import org.lambertland.kxpilot.model.KeyBinding
import org.lambertland.kxpilot.model.defaultBindings
import org.lambertland.kxpilot.resources.ShipShapeDef
import org.lambertland.kxpilot.ui.screens.AboutScreen
import org.lambertland.kxpilot.ui.screens.ConfigScreen
import org.lambertland.kxpilot.ui.screens.GameScreen
import org.lambertland.kxpilot.ui.screens.KeyBindingsScreen
import org.lambertland.kxpilot.ui.screens.MainMenuScreen
import org.lambertland.kxpilot.ui.screens.MotdScreen
import org.lambertland.kxpilot.ui.screens.ServerDashboardScreen

// ---------------------------------------------------------------------------
// CompositionLocals
// ---------------------------------------------------------------------------

val LocalNavigator =
    staticCompositionLocalOf<Navigator> {
        error("No Navigator provided — wrap with CompositionLocalProvider(LocalNavigator provides ...)")
    }

// ---------------------------------------------------------------------------
// Root composable
// ---------------------------------------------------------------------------

/**
 * Application root.  Provides [Navigator] via [LocalNavigator] and renders
 * the appropriate screen for the current navigation state.
 *
 * @param navigator          Pre-configured navigator (e.g. with exitApplication wired).
 * @param config             Loaded app config (player name, ship, settings, colors).
 * @param onSaveConfig       Called with the full rc-file text whenever any setting changes.
 *                           Pass a no-op lambda (the default) in tests/previews.
 * @param onSaveBindings     Called with the serialised key-bindings block whenever the
 *                           user presses DONE in the key-bindings screen.
 * @param availableShips     Ship shapes available for selection on the main menu.
 * @param initialKeyBindings Key bindings loaded from disk; defaults to [defaultBindings].
 */
@Composable
fun App(
    navigator: Navigator = remember { Navigator() },
    config: AppConfig = remember { AppConfig.defaults() },
    onSaveConfig: (String) -> Unit = {},
    onSaveBindings: (String) -> Unit = {},
    availableShips: List<ShipShapeDef> = emptyList(),
    initialKeyBindings: List<KeyBinding> = defaultBindings(),
) {
    val currentScreen by navigator.current.collectAsState()

    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalAppConfig provides config,
    ) {
        when (currentScreen) {
            Screen.MainMenu -> {
                MainMenuScreen(
                    onSaveConfig = onSaveConfig,
                    availableShips = availableShips,
                )
            }

            Screen.About -> {
                AboutScreen()
            }

            Screen.KeyBindings -> {
                KeyBindingsScreen(
                    initialBindings = initialKeyBindings,
                    onSaveBindings = onSaveBindings,
                )
            }

            Screen.Config -> {
                ConfigScreen(onSaveConfig = onSaveConfig)
            }

            Screen.Motd -> {
                MotdScreen()
            }

            is Screen.InGame -> {
                GameScreen(
                    serverHost = screen.serverHost,
                    serverPort = screen.serverPort,
                )
            }

            Screen.ServerDashboard -> {
                ServerDashboardScreen()
            }
        }
    }
}
