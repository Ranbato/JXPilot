package org.lambertland.kxpilot.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.FilenameFilter
import kotlin.coroutines.resume

/**
 * XPilot map file extensions recognised by the parser.
 * `.xp` is the legacy tile/bitmap format; `.xp2` is the XML polygon format;
 * `.map` is an alternate legacy extension.
 */
private val MAP_EXTENSIONS = setOf("xp", "xp2", "map")

actual suspend fun showFilePicker(
    title: String,
    extension: String,
): String? =
    withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { cont ->
            java.awt.EventQueue.invokeLater {
                val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)

                // Start in the current working directory, not the user's home.
                dialog.directory = System.getProperty("user.dir")

                // Filter: if a specific extension was requested use it,
                // otherwise accept all known XPilot map types.
                val allowed = if (extension.isNotEmpty()) setOf(extension) else MAP_EXTENSIONS
                dialog.filenameFilter =
                    FilenameFilter { _, name ->
                        val dot = name.lastIndexOf('.')
                        dot >= 0 && name.substring(dot + 1).lowercase() in allowed
                    }

                dialog.isVisible = true // blocks until the user dismisses

                val dir = dialog.directory
                val file = dialog.file
                cont.resume(if (dir != null && file != null) dir + file else null)
            }
        }
    }
