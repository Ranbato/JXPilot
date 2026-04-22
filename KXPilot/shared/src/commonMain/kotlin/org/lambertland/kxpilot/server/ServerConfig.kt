package org.lambertland.kxpilot.server

import org.lambertland.kxpilot.common.GameConst

/**
 * Immutable configuration snapshot for the embedded XPilot-NG server.
 *
 * All fields have sensible defaults so callers only need to override what they care about.
 *
 * @param port            UDP port to listen on (XPilot-NG well-known: 15345).
 * @param mapPath         Absolute path to the .xp map file, or null to use the built-in default.
 * @param maxPlayers      Maximum simultaneous connected players.
 * @param targetFps       Target game-loop rate in Hz (1..[GameConst.MAX_SERVER_FPS]).
 * @param welcomeMessage  Message-of-the-day shown to players on join.
 * @param serverName      Human-readable server name broadcast to metaserver.
 * @param teamPlay        Whether team mode is enabled.
 * @param allowRobots     Whether the server spawns robot ships when human count is low.
 */
data class ServerConfig(
    val port: Int = DEFAULT_PORT,
    val mapPath: String? = null,
    val maxPlayers: Int = 16,
    val targetFps: Int = 30,
    val welcomeMessage: String = "Welcome to KXPilot!",
    val serverName: String = "KXPilot Server",
    val teamPlay: Boolean = false,
    val allowRobots: Boolean = true,
) {
    companion object {
        /** XPilot-NG RFC well-known UDP port. */
        const val DEFAULT_PORT: Int = 15345
    }

    init {
        require(port in 1..65535) { "port must be 1..65535, was $port" }
        require(maxPlayers in 1..256) { "maxPlayers must be 1..256, was $maxPlayers" }
        require(targetFps in 1..GameConst.MAX_SERVER_FPS) {
            "targetFps must be 1..${GameConst.MAX_SERVER_FPS}, was $targetFps"
        }
    }
}
