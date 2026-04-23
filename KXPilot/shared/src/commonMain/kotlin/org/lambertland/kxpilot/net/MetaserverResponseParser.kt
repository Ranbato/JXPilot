package org.lambertland.kxpilot.net

import org.lambertland.kxpilot.model.ServerInfo

/**
 * Parse the colon-delimited text response from the XPilot-NG metaserver CGI.
 *
 * Each line has 18+ colon-separated fields (from meta.c `Add_meta_line`):
 *   [0]  version
 *   [1]  hostname
 *   [2]  port
 *   [3]  users (current player count)
 *   [4]  mapname
 *   [5]  mapsize
 *   [6]  author
 *   [7]  status
 *   [8]  bases (max players)
 *   [9]  fps
 *   [10] playlist
 *   [11] sound
 *   [12] uptime
 *   [13] teambases
 *   [14] timing
 *   [15] ip_str
 *   [16] freebases
 *   [17] queue (waiting players)
 *
 * Malformed or incomplete lines are silently skipped.
 */
fun parseMetaserverResponse(text: String): List<ServerInfo> {
    if (text.isBlank()) return emptyList()
    val result = mutableListOf<ServerInfo>()
    for (rawLine in text.lines()) {
        val line = rawLine.trim()
        if (line.isEmpty()) continue
        val fields = line.split(":", limit = 18) // 18 known fields; remainder folded into fields[17]
        if (fields.size < 18) continue
        val host = fields[1].trim()
        if (host.isEmpty()) continue
        val port = fields[2].toIntOrNull() ?: continue
        val playerCount = fields[3].toIntOrNull() ?: continue
        val mapName = fields[4]
        val status = fields[7]
        val maxPlayers = fields[8].toIntOrNull() ?: 0
        val fps = fields[9].toIntOrNull() ?: 0
        val version = fields[0]
        val queue = fields[17].toIntOrNull() ?: 0
        result +=
            ServerInfo(
                host = host,
                port = port,
                mapName = mapName,
                playerCount = playerCount,
                queueCount = queue,
                maxPlayers = maxPlayers,
                fps = fps,
                version = version,
                pingMs = null,
                status = status,
            )
    }
    return result
}
