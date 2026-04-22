package org.lambertland.kxpilot.config

/**
 * Pure Kotlin parser/writer for ~/.kxpilotrc files.
 *
 * File format (xpilot-ng style):
 *   xpilot.power            : 55.0
 *   xpilot.showShipShapes   : yes
 *   ; xpilot.sparkSize      : 2      <- commented-out default
 *
 * No Compose imports — safe to call from any coroutine dispatcher.
 */
object XpilotrcParser {
    private const val PREFIX = "xpilot."

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    /**
     * Parse [text] (the full contents of an xpilotrc file) and return a map
     * of option name -> parsed value.  Unknown options and parse failures are
     * silently ignored.
     */
    fun read(
        text: String,
        defs: List<XpOptionDef<*>>,
    ): Map<String, Any> {
        val defsByName: Map<String, XpOptionDef<*>> = defs.associateBy { it.name.lowercase() }
        val result = mutableMapOf<String, Any>()

        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.startsWith(";") || line.startsWith("#") || line.isBlank()) continue

            val colonIdx = line.indexOf(':')
            if (colonIdx < 0) continue

            val keyPart = line.substring(0, colonIdx).trim().lowercase()
            val valuePart = line.substring(colonIdx + 1).trim()

            val name = if (keyPart.startsWith(PREFIX)) keyPart.removePrefix(PREFIX) else keyPart
            val def = defsByName[name] ?: continue
            if (OptionFlag.NEVER_SAVE in def.flags) continue

            val parsed = parseValue(def, valuePart) ?: continue
            result[def.name] = parsed
        }
        return result
    }

    private fun parseValue(
        def: XpOptionDef<*>,
        raw: String,
    ): Any? =
        when (def) {
            is XpOptionDef.Bool -> {
                when (raw.lowercase()) {
                    "yes", "true", "1", "on" -> true
                    "no", "false", "0", "off" -> false
                    else -> null
                }
            }

            is XpOptionDef.ColorIndex -> {
                raw.toIntOrNull()?.coerceIn(def.min, def.max)
            }

            is XpOptionDef.Int -> {
                raw.toIntOrNull()?.coerceIn(def.min, def.max)
            }

            is XpOptionDef.Double -> {
                raw.toDoubleOrNull()?.coerceIn(def.min, def.max)
            }

            is XpOptionDef.Str -> {
                raw
            }
        }

    // -------------------------------------------------------------------------
    // Write
    // -------------------------------------------------------------------------

    /**
     * Serialise all options from [defs] to xpilotrc text, using [getValue] to
     * retrieve each current value.  Options equal to their default are emitted
     * as commented-out lines.  NEVER_SAVE options are skipped entirely.
     */
    fun write(
        defs: List<XpOptionDef<*>>,
        getValue: (XpOptionDef<*>) -> Any,
    ): String {
        val sb = StringBuilder()
        sb.appendLine("; KXPilot configuration file — auto-generated")
        sb.appendLine()

        for (def in defs) {
            if (OptionFlag.NEVER_SAVE in def.flags) continue

            val value = getValue(def)
            val formatted = formatValue(def, value)
            val keyPart = "$PREFIX${def.name}"
            val isDefault = value == def.defaultValue

            if (isDefault) {
                sb.appendLine("; $keyPart : $formatted")
            } else {
                sb.appendLine("$keyPart : $formatted")
            }
        }
        return sb.toString()
    }

    private fun formatValue(
        def: XpOptionDef<*>,
        value: Any,
    ): String =
        when (def) {
            is XpOptionDef.Bool -> if (value as Boolean) "yes" else "no"
            else -> value.toString()
        }
}
