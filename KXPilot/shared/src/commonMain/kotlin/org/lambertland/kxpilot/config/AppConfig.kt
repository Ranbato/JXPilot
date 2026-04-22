package org.lambertland.kxpilot.config

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf

/**
 * UI-layer config state.  Holds a typed [OptionEntry] for each option in
 * [XpOptionRegistry.all].  Distributed via [LocalAppConfig].
 *
 * Create with [AppConfig.load] (reads from disk) or [AppConfig.defaults]
 * (all defaults, no file I/O — suitable for tests and previews).
 */
class AppConfig private constructor(
    private val defs: List<XpOptionDef<*>>,
    private val entries: Map<String, OptionEntry<*>>,
) {
    /**
     * Typed wrapper pairing a definition with its observable state.
     * Stored as [OptionEntry<*>] in the map; retrieved with a single
     * isolated cast in [stateOf].
     */
    private class OptionEntry<T : Any>(
        val def: XpOptionDef<T>,
        val state: MutableState<T>,
    )

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> stateOf(def: XpOptionDef<T>): MutableState<T> = (entries[def.name] as OptionEntry<T>).state

    fun <T : Any> get(def: XpOptionDef<T>): T = stateOf(def).value

    fun <T : Any> set(
        def: XpOptionDef<T>,
        v: T,
    ) {
        stateOf(def).value = v
    }

    fun allDefs(): List<XpOptionDef<*>> = defs

    /**
     * Serialise current values to xpilotrc text.
     * Call from a background coroutine; this function is pure (no I/O itself).
     */
    fun toRcText(): String =
        XpilotrcParser.write(defs) { def ->
            @Suppress("UNCHECKED_CAST")
            get(def as XpOptionDef<Any>)
        }

    companion object {
        /**
         * Build an [AppConfig] from optional saved text.
         * Pass [rcText] = null (or omit) for all-defaults behaviour.
         */
        fun load(rcText: String? = null): AppConfig {
            val defs = XpOptionRegistry.all
            val fileValues: Map<String, Any> =
                if (rcText != null) XpilotrcParser.read(rcText, defs) else emptyMap()

            @Suppress("UNCHECKED_CAST")
            val entries: Map<String, OptionEntry<*>> =
                defs.associate { def ->
                    def as XpOptionDef<Any>
                    def.name to OptionEntry(def, mutableStateOf(fileValues[def.name] ?: def.defaultValue))
                }

            return AppConfig(defs, entries)
        }

        /** All-defaults, no file I/O — for tests and desktop previews. */
        fun defaults(): AppConfig = load(rcText = null)
    }
}

val LocalAppConfig =
    compositionLocalOf<AppConfig> {
        error("No AppConfig provided — wrap with CompositionLocalProvider(LocalAppConfig provides …)")
    }
