package org.lambertland.kxpilot.server

import java.io.File

actual fun readFileTextOrNull(path: String): String? =
    try {
        File(path).readText(Charsets.UTF_8)
    } catch (_: Exception) {
        null
    }
