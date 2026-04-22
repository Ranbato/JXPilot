package org.lambertland.kxpilot.server

// ---------------------------------------------------------------------------
// SystemMetrics — platform-specific CPU and heap sampling
// ---------------------------------------------------------------------------
//
// On desktop (JVM) this reads from the JVM management MXBeans.
// On other platforms it returns sentinel values (-1.0 / -1L) indicating
// "unavailable" so the dashboard can display "N/A".

/**
 * Samples CPU and heap usage from the runtime.
 *
 * @return A [SystemSnapshot] with the current CPU and heap readings.
 *         Fields are -1.0 / -1L when not available on this platform.
 */
internal expect fun sampleSystemMetrics(): SystemSnapshot

/**
 * Immutable snapshot of system-level resource usage.
 *
 * @param cpuPercent  JVM process CPU usage 0.0–100.0, or -1.0 if unavailable.
 * @param heapUsedMb  JVM heap used in megabytes, or -1L if unavailable.
 */
data class SystemSnapshot(
    val cpuPercent: Double,
    val heapUsedMb: Long,
) {
    companion object {
        val UNAVAILABLE = SystemSnapshot(cpuPercent = -1.0, heapUsedMb = -1L)
    }
}
