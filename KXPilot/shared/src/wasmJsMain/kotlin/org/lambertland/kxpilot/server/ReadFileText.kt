package org.lambertland.kxpilot.server

// wasmJs: no filesystem access — always return null.
actual fun readFileTextOrNull(path: String): String? = null
