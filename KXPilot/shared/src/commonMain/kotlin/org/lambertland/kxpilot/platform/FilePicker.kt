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

/**
 * Show a native save-file dialog and write [content] to the chosen path.
 * Returns `true` on success, `false` if the user cancelled or an error occurred.
 *
 * @param title        Title for the save dialog.
 * @param defaultName  Suggested filename (e.g. "kxpilot.log").
 * @param content      Text to write.
 */
expect suspend fun saveTextFile(
    title: String,
    defaultName: String,
    content: String,
): Boolean
