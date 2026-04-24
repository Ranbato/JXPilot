package org.lambertland.kxpilot.server

/**
 * Read the entire contents of the file at [path] as a UTF-8 string, or return
 * null if the path is invalid, the file does not exist, or the platform does
 * not support filesystem access (e.g. wasmJs).
 *
 * Used by [ServerGameWorld] to load `.xp` / `.xp2` map files.
 */
internal expect fun readFileTextOrNull(path: String): String?
