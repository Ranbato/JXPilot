package org.lambertland.kxpilot.server

/**
 * A point-in-time snapshot of server performance metrics.
 *
 * Collected on the game-loop thread and exposed via [ServerController] as an
 * [kotlinx.coroutines.flow.StateFlow] updated every second.
 *
 * @param uptimeMs           Milliseconds since the server started.
 * @param tickRateActual     Measured game-loop ticks per second over the last window.
 * @param tickRateTarget     Configured target tick rate (from [ServerConfig.targetFps]).
 * @param playerCount        Number of currently connected players.
 * @param bandwidthInBps     Inbound UDP bytes per second (last measurement window).
 * @param bandwidthOutBps    Outbound UDP bytes per second (last measurement window).
 * @param cpuPercent         JVM process CPU usage 0–100, or -1 if unavailable.
 * @param heapUsedMb         JVM heap used in megabytes, or -1 if unavailable.
 */
data class ServerMetrics(
    val uptimeMs: Long = 0L,
    val tickRateActual: Double = 0.0,
    val tickRateTarget: Int = 0,
    val playerCount: Int = 0,
    val bandwidthInBps: Long = 0L,
    val bandwidthOutBps: Long = 0L,
    val cpuPercent: Double = -1.0,
    val heapUsedMb: Long = -1L,
) {
    companion object {
        val EMPTY = ServerMetrics()
    }
}
