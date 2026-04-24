package org.lambertland.kxpilot.server

// Android: file loading via a raw path is not generally useful (content URIs
// are the Android idiom); return null so the server falls back to the default world.
actual fun readFileTextOrNull(path: String): String? = null
