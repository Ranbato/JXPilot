package org.lambertland.kxpilot.model

import androidx.compose.ui.input.key.Key as ComposeKey

// ---------------------------------------------------------------------------
// Key bindings — domain models (no Compose UI imports)
// ---------------------------------------------------------------------------

/**
 * Abstract game actions that can be bound to keyboard keys.
 * Corresponds to the subset of [org.lambertland.kxpilot.common.Key] that a
 * player can usefully rebind from the UI.
 */
enum class GameAction(
    val description: String,
) {
    TURN_LEFT("Turn left"),
    TURN_RIGHT("Turn right"),
    THRUST("Thrust"),
    FIRE_SHOT("Fire shot"),
    SHIELD("Shield"),
    RESPAWN("Respawn at base"),
    TALK("Open chat"),
    SCOREBOARD("Toggle scoreboard"),
    FIRE_MISSILE("Fire missile"),
    DROP_MINE("Drop mine"),
    CLOAK("Toggle cloak"),
    SWAP_SETTINGS("Swap alt settings"),
    LOCK_NEXT("Lock next target"),
    LOCK_PREV("Lock previous target"),
    TRACTOR_BEAM("Tractor/pressor beam"),
    GRAB_BALL("Grab ball (connector)"),
    EXIT_TO_MENU("Exit to main menu"),
}

/**
 * One action → an unbounded list of keyboard keys, mirroring the original
 * XPilot keydefs[] model: any number of keysyms can be bound to one action,
 * and the same keysym may appear in multiple actions (one keypress triggers
 * all matching actions simultaneously).  There is no conflict resolution —
 * adding a key never removes it from another action.
 */
data class KeyBinding(
    val action: GameAction,
    /** All keys currently bound to this action.  Order is user-defined. */
    val keys: List<ComposeKey> = emptyList(),
)

/**
 * Describes the current rebind-capture state.
 * [Awaiting] means "the next key press will be added to [action]'s key list".
 */
sealed class BindingMode {
    object Idle : BindingMode()

    data class Awaiting(
        val action: GameAction,
    ) : BindingMode()
}

// ---------------------------------------------------------------------------
// Default bindings
// ---------------------------------------------------------------------------

fun defaultBindings(): List<KeyBinding> =
    listOf(
        KeyBinding(GameAction.TURN_LEFT, listOf(ComposeKey.DirectionLeft, ComposeKey.A)),
        KeyBinding(GameAction.TURN_RIGHT, listOf(ComposeKey.DirectionRight, ComposeKey.D)),
        KeyBinding(GameAction.THRUST, listOf(ComposeKey.DirectionUp, ComposeKey.W)),
        KeyBinding(GameAction.FIRE_SHOT, listOf(ComposeKey.Spacebar)),
        KeyBinding(GameAction.SHIELD, listOf(ComposeKey.S)),
        KeyBinding(GameAction.RESPAWN, listOf(ComposeKey.R)),
        KeyBinding(GameAction.TALK, listOf(ComposeKey.T)),
        KeyBinding(GameAction.SCOREBOARD, listOf(ComposeKey.Tab)),
        KeyBinding(GameAction.FIRE_MISSILE, listOf(ComposeKey.M)),
        KeyBinding(GameAction.DROP_MINE, listOf(ComposeKey.B)),
        KeyBinding(GameAction.CLOAK, listOf(ComposeKey.C)),
        KeyBinding(GameAction.SWAP_SETTINGS, listOf(ComposeKey.X)),
        KeyBinding(GameAction.LOCK_NEXT, listOf(ComposeKey.N)),
        KeyBinding(GameAction.LOCK_PREV, listOf(ComposeKey.P)),
        KeyBinding(GameAction.TRACTOR_BEAM, listOf(ComposeKey.G)),
        KeyBinding(GameAction.GRAB_BALL, listOf(ComposeKey.F)),
        KeyBinding(GameAction.EXIT_TO_MENU, listOf(ComposeKey.Escape)),
    )

// ---------------------------------------------------------------------------
// Key display name helper
// ---------------------------------------------------------------------------

/** Returns a short human-readable label for a Compose [Key]. */
fun ComposeKey.displayName(): String =
    when (this) {
        ComposeKey.DirectionLeft -> "←"

        ComposeKey.DirectionRight -> "→"

        ComposeKey.DirectionUp -> "↑"

        ComposeKey.DirectionDown -> "↓"

        ComposeKey.Spacebar -> "Space"

        ComposeKey.Enter -> "Enter"

        ComposeKey.Escape -> "Esc"

        ComposeKey.Backspace -> "Bksp"

        ComposeKey.Tab -> "Tab"

        ComposeKey.Delete -> "Del"

        ComposeKey.ShiftLeft -> "LShift"

        ComposeKey.ShiftRight -> "RShift"

        ComposeKey.CtrlLeft -> "LCtrl"

        ComposeKey.CtrlRight -> "RCtrl"

        ComposeKey.AltLeft -> "LAlt"

        ComposeKey.AltRight -> "RAlt"

        ComposeKey.MetaLeft -> "LMeta"

        ComposeKey.MetaRight -> "RMeta"

        ComposeKey.CapsLock -> "Caps"

        ComposeKey.PageUp -> "PgUp"

        ComposeKey.PageDown -> "PgDn"

        ComposeKey.MoveHome -> "Home"

        ComposeKey.MoveEnd -> "End"

        ComposeKey.Insert -> "Ins"

        ComposeKey.A -> "A"

        ComposeKey.B -> "B"

        ComposeKey.C -> "C"

        ComposeKey.D -> "D"

        ComposeKey.E -> "E"

        ComposeKey.F -> "F"

        ComposeKey.G -> "G"

        ComposeKey.H -> "H"

        ComposeKey.I -> "I"

        ComposeKey.J -> "J"

        ComposeKey.K -> "K"

        ComposeKey.L -> "L"

        ComposeKey.M -> "M"

        ComposeKey.N -> "N"

        ComposeKey.O -> "O"

        ComposeKey.P -> "P"

        ComposeKey.Q -> "Q"

        ComposeKey.R -> "R"

        ComposeKey.S -> "S"

        ComposeKey.T -> "T"

        ComposeKey.U -> "U"

        ComposeKey.V -> "V"

        ComposeKey.W -> "W"

        ComposeKey.X -> "X"

        ComposeKey.Y -> "Y"

        ComposeKey.Z -> "Z"

        ComposeKey.Zero -> "0"

        ComposeKey.One -> "1"

        ComposeKey.Two -> "2"

        ComposeKey.Three -> "3"

        ComposeKey.Four -> "4"

        ComposeKey.Five -> "5"

        ComposeKey.Six -> "6"

        ComposeKey.Seven -> "7"

        ComposeKey.Eight -> "8"

        ComposeKey.Nine -> "9"

        ComposeKey.F1 -> "F1"

        ComposeKey.F2 -> "F2"

        ComposeKey.F3 -> "F3"

        ComposeKey.F4 -> "F4"

        ComposeKey.F5 -> "F5"

        ComposeKey.F6 -> "F6"

        ComposeKey.F7 -> "F7"

        ComposeKey.F8 -> "F8"

        ComposeKey.F9 -> "F9"

        ComposeKey.F10 -> "F10"

        ComposeKey.F11 -> "F11"

        ComposeKey.F12 -> "F12"

        // Fallback: Key.toString() on desktop returns "Key: <AWT name>"; strip prefix.
        else -> this.toString().removePrefix("Key: ").ifEmpty { "?" }
    }

// ---------------------------------------------------------------------------
// Key bindings serialisation helpers (used by XpilotrcParser integration)
// ---------------------------------------------------------------------------

/**
 * Serialises a list of [KeyBinding]s to a multi-line string block suitable
 * for embedding in a kxpilotrc file.  Each action occupies one line:
 *
 *   xpilot.key.TurnLeft : 37 65
 *
 * Key identity is stored as the raw [ComposeKey.keyCode] (Long, decimal).
 * Actions with no keys are emitted as commented-out blank entries.
 */
fun serializeBindings(bindings: List<KeyBinding>): String {
    val sb = StringBuilder()
    for (b in bindings) {
        val keyPart = "xpilot.key.${b.action.name}"
        val valuePart = b.keys.joinToString(" ") { it.keyCode.toString() }
        if (b.keys.isEmpty()) {
            sb.appendLine("; $keyPart :")
        } else {
            sb.appendLine("$keyPart : $valuePart")
        }
    }
    return sb.toString()
}

/**
 * Parses the binding block produced by [serializeBindings] back into a
 * [KeyBinding] list.  Unknown action names are silently ignored.
 * Falls back to [defaultBindings] for any action not present in [text].
 */
fun deserializeBindings(text: String): List<KeyBinding> {
    val actionByName: Map<String, GameAction> = GameAction.entries.associateBy { it.name }
    val parsed = mutableMapOf<GameAction, List<ComposeKey>>()

    for (raw in text.lines()) {
        val line = raw.trim()
        if (line.startsWith(";") || line.startsWith("#") || line.isBlank()) continue
        val colonIdx = line.indexOf(':')
        if (colonIdx < 0) continue
        val keyPart = line.substring(0, colonIdx).trim()
        val valuePart = line.substring(colonIdx + 1).trim()

        val actionName = keyPart.removePrefix("xpilot.key.")
        val action = actionByName[actionName] ?: continue
        val keys =
            if (valuePart.isEmpty()) {
                emptyList()
            } else {
                valuePart.split(" ").mapNotNull { token ->
                    token.toLongOrNull()?.let { ComposeKey(it) }
                }
            }
        parsed[action] = keys
    }

    // Merge: use parsed values where available, default otherwise
    return defaultBindings().map { default ->
        KeyBinding(default.action, parsed.getOrDefault(default.action, default.keys))
    }
}

/**
 * Returns the label shown inside a key chip.
 *
 * For keys whose [displayName] is a directional arrow or other non-obvious
 * symbol, appends the AWT name so the chip reads e.g. "← Left" instead of
 * just "←".  For all other keys the chip label equals [displayName].
 */
fun ComposeKey.chipLabel(): String {
    val short = displayName()
    // If the short name is a single Unicode symbol (not a letter, digit, or
    // multi-character word), supplement it with the AWT key name.
    val isSymbol = short.length == 1 && !short[0].isLetterOrDigit()
    return if (isSymbol) {
        val awtName = this.toString().removePrefix("Key: ")
        if (awtName.isNotEmpty() && awtName != short) "$short $awtName" else short
    } else {
        short
    }
}
