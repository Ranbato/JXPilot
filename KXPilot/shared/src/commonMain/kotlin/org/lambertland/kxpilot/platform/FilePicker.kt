package org.lambertland.kxpilot.platform

/**
 * Show a native file picker dialog and return the selected file path,
 * or `null` if the user cancelled.
 *
 * @param title     Title for the dialog window.
 * @param extension File extension filter without dot (e.g. "xp"), or empty to allow all.
 */
expect suspend fun showFilePicker(
    title: String,
    extension: String = "",
): String?
