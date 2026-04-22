package org.lambertland.kxpilot.config

// Domain layer — NO androidx.compose imports

enum class OptionFlag { CONFIG_DEFAULT, CONFIG_COLORS, KEEP, NEVER_SAVE }

enum class OptionOrigin { DEFAULT, CMDLINE, ENV, XPILOTRC, CONFIG }

sealed class XpOptionDef<T>(
    val name: String,
    val help: String,
    val flags: Set<OptionFlag>,
    val defaultValue: T,
) {
    class Bool(
        name: String,
        help: String,
        flags: Set<OptionFlag>,
        defaultValue: Boolean,
    ) : XpOptionDef<Boolean>(name, help, flags, defaultValue)

    class Int(
        name: String,
        help: String,
        flags: Set<OptionFlag>,
        defaultValue: kotlin.Int,
        val min: kotlin.Int,
        val max: kotlin.Int,
    ) : XpOptionDef<kotlin.Int>(name, help, flags, defaultValue)

    class Double(
        name: String,
        help: String,
        flags: Set<OptionFlag>,
        defaultValue: kotlin.Double,
        val min: kotlin.Double,
        val max: kotlin.Double,
    ) : XpOptionDef<kotlin.Double>(name, help, flags, defaultValue)

    class Str(
        name: String,
        help: String,
        flags: Set<OptionFlag>,
        defaultValue: String,
    ) : XpOptionDef<String>(name, help, flags, defaultValue)

    /**
     * Color-index option: an integer in [0, 15], always CONFIG_COLORS.
     * Sibling of [Int] (not a subclass) to avoid fragile IS-A dispatch.
     */
    class ColorIndex(
        name: String,
        help: String,
        defaultValue: kotlin.Int,
        val min: kotlin.Int = 0,
        val max: kotlin.Int = 15,
    ) : XpOptionDef<kotlin.Int>(name, help, setOf(OptionFlag.CONFIG_COLORS), defaultValue)
}
