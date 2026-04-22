package org.lambertland.kxpilot.server

actual fun currentTimeMs(): Long =
    kotlin.js.Date
        .now()
        .toLong()
