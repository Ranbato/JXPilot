package org.lambertland.kxpilot.common

/**
 * Format a [Double] to one decimal place without using [String.format],
 * which is JVM-only and not available in commonMain.
 *
 * Examples:
 *   3.0      → "3.0"
 *   3.14159  → "3.1"
 *   -1.95    → "-2.0"  (rounds half up in magnitude)
 */
fun Double.formatOneDecimal(): String {
    val scaled = (this * 10).toLong()
    val intPart = scaled / 10
    val fracPart = kotlin.math.abs(scaled % 10)
    return "$intPart.$fracPart"
}

/**
 * Format a [Float] to one decimal place.
 */
fun Float.formatOneDecimal(): String = this.toDouble().formatOneDecimal()
