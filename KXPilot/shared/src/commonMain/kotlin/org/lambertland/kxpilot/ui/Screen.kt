package org.lambertland.kxpilot.ui

import org.lambertland.kxpilot.server.ServerConfig

// ---------------------------------------------------------------------------
// Navigation route vocabulary
// ---------------------------------------------------------------------------

/**
 * All top-level navigation destinations.  [Screen.InGame] carries the server
 * host string so the game screen knows which server to connect to.
 *
 * Note: the Config colors sub-tab is *not* a separate Screen — it lives as a
 * TabRow tab inside [Screen.Config].
 */
sealed class Screen {
    data object MainMenu : Screen()

    data object Config : Screen()

    data object KeyBindings : Screen()

    data object About : Screen()

    data object Motd : Screen()

    data class InGame(
        val serverHost: String,
        val serverPort: Int = ServerConfig.DEFAULT_PORT,
    ) : Screen()

    data object ServerDashboard : Screen()
}
