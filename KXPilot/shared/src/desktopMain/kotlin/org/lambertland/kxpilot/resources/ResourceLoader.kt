package org.lambertland.kxpilot.resources

// R3: use a named companion object reference instead of the anonymous `object {}.javaClass`
// idiom, which allocates a new anonymous class per call-site and is opaque to readers.
private object ResourceLoaderRef

actual fun readResourceText(path: String): String? =
    try {
        // R2: wrap the stream in .use {} so it is closed even on exception.
        ResourceLoaderRef::class.java
            .getResourceAsStream(path)
            ?.use { stream -> stream.bufferedReader().readText() }
    } catch (_: Exception) {
        null
    }
