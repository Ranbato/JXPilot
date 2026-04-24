package org.lambertland.kxpilot.resources

import org.lambertland.kxpilot.AppLogger

/**
 * Parser for XPilot `defaults.txt` key:value config format.
 *
 * Rules:
 * - Lines starting with `#` are comments; blank lines are ignored.
 * - Key-value pairs are `key: value` or `key: value` (colon-separated,
 *   leading/trailing whitespace trimmed from both sides).
 * - `define: <name> \override: \multiline: <endMarker>` introduces a named
 *   block; subsequent lines until `endMarker` are collected as the block body.
 * - `expand: <name>` references a previously defined block (not evaluated
 *   here — the block map is returned for the caller to handle).
 */
data class GameDefaults(
    /** All top-level key:value pairs (outside any define block). */
    val settings: Map<String, String>,
    /** Named define blocks (name → raw body lines joined with newline). */
    val defines: Map<String, String>,
    /** Names referenced via `expand:` directives, in order. */
    val expands: List<String>,
)

fun parseDefaults(text: String): GameDefaults {
    val settings = LinkedHashMap<String, String>()
    val defines = LinkedHashMap<String, String>()
    val expands = mutableListOf<String>()

    val lines = text.lines()
    var i = 0
    while (i < lines.size) {
        val raw = lines[i].trim()
        i++
        if (raw.isEmpty() || raw.startsWith('#')) continue

        // `define: NAME \override: \multiline: ENDMARKER`
        if (raw.startsWith("define:")) {
            val rest = raw.removePrefix("define:").trim()
            // Extract name (everything before first \)
            val nameEnd = rest.indexOf('\\').let { if (it < 0) rest.length else it }
            val name = rest.substring(0, nameEnd).trim()
            // Extract end-marker: `\multiline: MARKER`
            val multilineIdx = rest.indexOf("\\multiline:")
            val endMarker =
                if (multilineIdx >= 0) {
                    rest.substring(multilineIdx + "\\multiline:".length).trim()
                } else {
                    ""
                }
            // Collect block body
            val body = StringBuilder()
            if (endMarker.isNotEmpty()) {
                while (i < lines.size && lines[i].trim() != endMarker) {
                    body.appendLine(lines[i].trim())
                    i++
                }
                if (i < lines.size) {
                    i++ // consume endMarker line
                } else {
                    AppLogger.log(
                        "parseDefaults: define block '$name' end marker '$endMarker' not found — reached EOF",
                    )
                }
            }
            if (name.isNotEmpty()) defines[name] = body.toString().trimEnd()
            continue
        }

        // `expand: NAME`
        if (raw.startsWith("expand:")) {
            val name = raw.removePrefix("expand:").trim()
            if (name.isNotEmpty()) expands += name
            continue
        }

        // Regular key: value
        val colonIdx = raw.indexOf(':')
        if (colonIdx > 0) {
            val key = raw.substring(0, colonIdx).trim()
            val value = raw.substring(colonIdx + 1).trim()
            if (key.isNotEmpty()) settings[key] = value
        }
    }

    return GameDefaults(settings, defines, expands)
}

// ---------------------------------------------------------------------------
// Typed accessors for the item probability keys used by the demo / server
// ---------------------------------------------------------------------------

val GameDefaults.itemProbabilities: Map<String, Double> get() =
    settings
        .filterKeys { it.endsWith("Prob") }
        .mapValues { it.value.toDoubleOrNull() ?: 0.0 }

val GameDefaults.gravity: Double get() =
    settings["gravity"]?.toDoubleOrNull() ?: 0.0

val GameDefaults.framesPerSecond: Int get() =
    settings["framesPerSecond"]?.toIntOrNull() ?: 50

val GameDefaults.maxItemDensity: Double get() =
    settings["maxItemDensity"]?.toDoubleOrNull() ?: 0.0
