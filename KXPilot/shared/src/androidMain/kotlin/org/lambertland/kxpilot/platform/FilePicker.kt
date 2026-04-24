package org.lambertland.kxpilot.platform

actual suspend fun showFilePicker(
    title: String,
    extension: String,
): String? = null

actual suspend fun saveTextFile(
    title: String,
    defaultName: String,
    content: String,
): Boolean = false // not yet implemented on Android
