package org.lambertland.kxpilot.model

// ---------------------------------------------------------------------------
// Server / main-menu domain models (no Compose imports)
// ---------------------------------------------------------------------------

enum class ServerSource { LOCAL, INTERNET }

data class ServerInfo(
    val host: String,
    val port: Int,
    val mapName: String,
    val playerCount: Int,
    val queueCount: Int,
    val maxPlayers: Int,
    val fps: Int,
    val version: String,
    val pingMs: Int?, // null = not yet measured
    val status: String,
    val players: List<String> = emptyList(), // player names; populated from local scan or ReportStatus reply
    val source: ServerSource = ServerSource.INTERNET,
)

sealed class ServerBrowserState {
    object Idle : ServerBrowserState()

    object Scanning : ServerBrowserState()

    data class Loaded(
        val servers: List<ServerInfo>,
    ) : ServerBrowserState()

    /**
     * Detail view for a selected server.
     * Player names are accessed via [server].players — no duplicate field.
     */
    data class Detail(
        val server: ServerInfo,
    ) : ServerBrowserState()

    data class Error(
        val message: String,
    ) : ServerBrowserState()

    /**
     * Shown when the LOCAL tab is active.
     *
     * Carries only scan-derived data.  The direct-connect text field values
     * (`directHost`, `directPort`) live in the state holder as plain compose
     * state — they are UI input state, not domain state.
     *
     * @param localServer  A local server found by UDP scan, or null if none was detected.
     * @param scanning     True while a UDP scan is in progress.
     */
    data class ConnectLocal(
        val localServer: ServerInfo? = null,
        val scanning: Boolean = false,
    ) : ServerBrowserState()
}
