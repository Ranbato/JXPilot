package org.lambertland.kxpilot.server

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// ---------------------------------------------------------------------------
// ServerGameLoop — fixed-rate game tick coroutine
// ---------------------------------------------------------------------------
//
// Responsibilities:
//   1. Drive a fixed-rate tick (configurable via ServerConfig.targetFps).
//   2. Emit a ServerMetrics snapshot every second.
//   3. Scan for timed-out ClientSessions and remove them.
//
// The loop does NOT do physics yet (that is M5).  Each tick it:
//   a. Advances the tick counter.
//   b. Once per second: samples system metrics, updates playerCount,
//      recomputes tickRateActual, and publishes via [onMetrics].
//   c. Calls [onTimeoutScan] so the caller can evict stale sessions.

/**
 * Runs the server game loop as a class so it can carry state and be extended.
 *
 * Instantiate and call [run] as a child coroutine:
 * ```
 * val loop = ServerGameLoop(config, onMetrics, onTimeoutScan, onTick)
 * scope.launch { loop.run(this) }
 * ```
 *
 * @param config        Server configuration (reads targetFps).
 * @param onMetrics     Called with a fresh [ServerMetrics] snapshot once per second.
 * @param onTimeoutScan Called every tick with current time; caller checks & evicts stale sessions.
 * @param onTick        Called every tick to run physics and broadcast the game frame.
 *                      Defaults to no-op so callers that don't have a world yet still compile.
 */
internal class ServerGameLoop(
    private val config: ServerConfig,
    private val onMetrics: (ServerMetrics) -> Unit,
    private val onTimeoutScan: suspend (nowMs: Long) -> Unit,
    /** Called every tick to run physics and broadcast the game frame. May suspend. */
    private val onTick: suspend () -> Unit = {},
) {
    /**
     * Run the loop until [scope] is cancelled.
     */
    suspend fun run(scope: CoroutineScope) {
        val tickMs = (1000.0 / config.targetFps).toLong().coerceAtLeast(1L)
        var ticksSinceLastMetric = 0L
        var totalTicks = 0L
        var metricsWindowStartMs = currentTimeMs()

        while (scope.isActive) {
            val tickStartMs = currentTimeMs()

            // Physics + frame broadcast for the current tick.
            onTick()

            // Timeout scan every tick (cheap — just checks a timestamp per session).
            onTimeoutScan(currentTimeMs())

            totalTicks++
            ticksSinceLastMetric++

            // Emit metrics once every targetFps ticks (= once per second at target rate)
            if (ticksSinceLastMetric >= config.targetFps) {
                val windowMs = currentTimeMs() - metricsWindowStartMs
                val actualFps = if (windowMs > 0) ticksSinceLastMetric * 1000.0 / windowMs else config.targetFps.toDouble()
                ticksSinceLastMetric = 0L
                metricsWindowStartMs = currentTimeMs()

                val sys = sampleSystemMetrics()
                onMetrics(
                    ServerMetrics(
                        // uptimeMs uses synthetic tick-derived time for determinism in tests
                        uptimeMs = totalTicks * tickMs,
                        tickRateActual = actualFps,
                        tickRateTarget = config.targetFps,
                        cpuPercent = sys.cpuPercent,
                        heapUsedMb = sys.heapUsedMb,
                    ),
                )
            }

            // Fixed-rate: delay only the remaining time in this tick period
            val elapsed = currentTimeMs() - tickStartMs
            val remaining = tickMs - elapsed
            if (remaining > 0) delay(remaining)
        }
    }
}

/**
 * Convenience top-level function that creates and runs a [ServerGameLoop].
 *
 * @param config       Server configuration (reads targetFps).
 * @param scope        The coroutine scope — loop runs while [CoroutineScope.isActive].
 * @param onMetrics    Called with a fresh [ServerMetrics] snapshot once per second.
 * @param onTimeoutScan Called every tick with current time; caller checks & evicts stale sessions.
 * @param onTick       Called every tick to run physics and broadcast the game frame.
 */
internal suspend fun runGameLoop(
    config: ServerConfig,
    scope: CoroutineScope,
    onMetrics: (ServerMetrics) -> Unit,
    onTimeoutScan: suspend (nowMs: Long) -> Unit,
    onTick: suspend () -> Unit = {},
) = ServerGameLoop(config, onMetrics, onTimeoutScan, onTick).run(scope)
