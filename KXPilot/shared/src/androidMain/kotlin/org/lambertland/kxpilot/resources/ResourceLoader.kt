package org.lambertland.kxpilot.resources

import android.content.res.AssetManager
import java.io.IOException

/**
 * Android actual for [readResourceText].
 *
 * Call [ResourceLoader.init] once from your Activity/Application onCreate
 * before any composable that calls [readResourceText].
 */
object ResourceLoader {
    // R4: @Volatile provides the JMM write-before-read guarantee so that any
    // thread calling readResourceText() after init() sees the non-null value.
    @Volatile internal var assetManager: AssetManager? = null
        private set

    fun init(assets: AssetManager) {
        assetManager = assets
    }
}

actual fun readResourceText(path: String): String? {
    val am = ResourceLoader.assetManager ?: return null
    // Android asset paths must not start with '/'
    val assetPath = path.trimStart('/')
    return try {
        // R6: use .use {} so the InputStream is closed even on exception.
        // R5: catch only IOException — other throwables (e.g. OOM) should propagate.
        am.open(assetPath).use { stream -> stream.bufferedReader().readText() }
    } catch (_: IOException) {
        null
    }
}
