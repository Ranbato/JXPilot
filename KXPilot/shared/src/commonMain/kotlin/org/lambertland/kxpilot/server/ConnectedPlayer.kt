package org.lambertland.kxpilot.server

/**
 * A player currently connected to the embedded server.
 *
 * Instances are created by [ServerController] when a client completes the
 * XPilot-NG handshake and destroyed on disconnect or kick.
 *
 * @param id        Stable numeric id assigned at join (wraps at Int.MAX_VALUE).
 * @param name      Player's declared nick name.
 * @param team      Team number (0 = no team / free-for-all).
 * @param score     Current game score.
 * @param pingMs    Last measured round-trip latency in milliseconds, or null if unknown.
 * @param isMuted   True when the server has silenced this player's chat messages.
 * @param address   Network address string "host:port" for display purposes.
 */
data class ConnectedPlayer(
    val id: Int,
    val name: String,
    val team: Int = 0,
    val score: Int = 0,
    val pingMs: Int? = null,
    val isMuted: Boolean = false,
    val address: String = "",
)

/**
 * Parse the "host:port" address string into a (host, port) pair.
 * Returns null if the address is malformed.
 */
fun ConnectedPlayer.parseHostPort(): Pair<String, Int>? {
    val colon = address.lastIndexOf(':')
    if (colon < 0) return null
    val port = address.substring(colon + 1).toIntOrNull() ?: return null
    return address.substring(0, colon) to port
}
