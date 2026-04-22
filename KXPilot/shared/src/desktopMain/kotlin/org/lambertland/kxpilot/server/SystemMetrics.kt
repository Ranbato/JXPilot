package org.lambertland.kxpilot.server

import com.sun.management.OperatingSystemMXBean
import java.lang.management.ManagementFactory

// ---------------------------------------------------------------------------
// Desktop (JVM) actual implementation of sampleSystemMetrics
// ---------------------------------------------------------------------------
//
// Uses com.sun.management.OperatingSystemMXBean (available in all Oracle /
// OpenJDK / Temurin distributions) for CPU usage, and Runtime for heap.
// If the bean is not available (unlikely, but possible on some JVMs) we fall
// back to UNAVAILABLE.

internal actual fun sampleSystemMetrics(): SystemSnapshot =
    try {
        val osMxBean = ManagementFactory.getOperatingSystemMXBean()
        val cpuPercent =
            if (osMxBean is OperatingSystemMXBean) {
                val raw = osMxBean.processCpuLoad // 0.0–1.0, or -1.0 if unavailable
                if (raw < 0.0) -1.0 else raw * 100.0
            } else {
                -1.0
            }
        val rt = Runtime.getRuntime()
        val heapUsedBytes = rt.totalMemory() - rt.freeMemory()
        val heapUsedMb = heapUsedBytes / (1024L * 1024L)
        SystemSnapshot(cpuPercent = cpuPercent, heapUsedMb = heapUsedMb)
    } catch (_: Exception) {
        SystemSnapshot.UNAVAILABLE
    }
