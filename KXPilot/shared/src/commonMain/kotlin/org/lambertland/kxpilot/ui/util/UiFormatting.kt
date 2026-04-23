package org.lambertland.kxpilot.ui.util

// ---------------------------------------------------------------------------
// Shared UI formatting helpers (commonMain-safe — no String.format / JVM API)
// ---------------------------------------------------------------------------

/**
 * Zero-pads a [Long] to 2 digits.
 *
 * Equivalent to `"%02d".format(this)` but does not require JVM `String.format`,
 * keeping this file safe for `commonMain`.
 */
internal fun Long.pad2(): String = if (this < 10L) "0$this" else toString()

/**
 * Formats a [Double] with one decimal place using pure Kotlin arithmetic.
 *
 * Correctly handles negative values (e.g. `-0.3` → `"-0.3"`).
 * Equivalent to `"%.1f".format(this)` but safe for `commonMain`.
 */
internal fun Double.roundOne(): String {
    val rounded = kotlin.math.round(this * 10.0) / 10.0
    val negative = rounded < 0.0
    val abs = if (negative) -rounded else rounded
    val intPart = abs.toLong()
    val fracPart =
        kotlin.math
            .round((abs - intPart) * 10.0)
            .toInt()
            .coerceIn(0, 9)
    return if (negative) "-$intPart.$fracPart" else "$intPart.$fracPart"
}

/**
 * Formats an uptime duration in milliseconds as `HH:MM:SS`.
 */
internal fun formatUptime(uptimeMs: Long): String {
    val sec = uptimeMs / 1000L
    val h = sec / 3600L
    val min = (sec % 3600L) / 60L
    val s = sec % 60L
    return "${h.pad2()}:${min.pad2()}:${s.pad2()}"
}
