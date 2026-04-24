package org.lambertland.kxpilot.resources

/**
 * Reads a resource file by path and returns its text content, or null if unavailable.
 *
 * Path convention (matches JVM classpath layout):
 *   "/data/shipshapes.json"  — ship shape definitions
 *   "/maps/teamcup.xp"       — bundled maps
 *
 * Platform actuals:
 *   jvmMain   — reads from classpath via getResourceAsStream (covers desktop + Android JVM unit tests)
 *   androidMain — reads from Android assets via AssetManager (strips leading '/')
 *   wasmJsMain  — returns null (no bundled resources in browser)
 */
expect fun readResourceText(path: String): String?
