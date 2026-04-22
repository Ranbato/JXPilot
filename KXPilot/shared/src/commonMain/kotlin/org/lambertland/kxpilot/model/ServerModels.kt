package org.lambertland.kxpilot.model

// ---------------------------------------------------------------------------
// Server / main-menu domain models (no Compose imports)
// ---------------------------------------------------------------------------

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
)

sealed class ServerBrowserState {
    object Idle : ServerBrowserState()

    object Scanning : ServerBrowserState()

    data class Loaded(
        val servers: List<ServerInfo>,
    ) : ServerBrowserState()

    data class Detail(
        val server: ServerInfo,
        val players: List<String>,
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

enum class ServerTab { LOCAL, INTERNET }

// ---------------------------------------------------------------------------
// Stub data — replaced by real network calls later
// ---------------------------------------------------------------------------

val STUB_INTERNET_SERVERS =
    listOf(
        ServerInfo("xpilot.example.com", 15345, "dogfight", 8, 2, 16, 25, "4.7.3", 42, "running"),
        ServerInfo("play.xpilot.net", 15345, "teamcup", 4, 0, 10, 30, "4.7.3", 110, "running"),
        ServerInfo("slow.xpilot.org", 15345, "asteroids", 0, 0, 8, 20, "4.7.2", 390, "idle"),
    )
