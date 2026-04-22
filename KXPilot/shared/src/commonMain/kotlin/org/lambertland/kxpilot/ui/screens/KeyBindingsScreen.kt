package org.lambertland.kxpilot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.lambertland.kxpilot.model.BindingMode
import org.lambertland.kxpilot.model.GameAction
import org.lambertland.kxpilot.model.KeyBinding
import org.lambertland.kxpilot.model.chipLabel
import org.lambertland.kxpilot.model.defaultBindings
import org.lambertland.kxpilot.model.displayName
import org.lambertland.kxpilot.model.serializeBindings
import org.lambertland.kxpilot.ui.LocalNavigator
import org.lambertland.kxpilot.ui.components.GameButton
import org.lambertland.kxpilot.ui.components.GameButtonDanger
import org.lambertland.kxpilot.ui.theme.KXPilotColors
import androidx.compose.ui.input.key.Key as ComposeKey

// ---------------------------------------------------------------------------
// State holder
// ---------------------------------------------------------------------------

class KeyBindingsStateHolder(
    initialBindings: List<KeyBinding> = defaultBindings(),
) {
    var bindings by mutableStateOf(initialBindings)
    var mode: BindingMode by mutableStateOf(BindingMode.Idle)

    /**
     * After capturing a key on KeyDown, the matching KeyUp must also be
     * consumed so the chip that focus was on doesn't receive a click event.
     * (Compose's clickable fires onClick on isClick = KeyUp of Space/Enter.)
     */
    private var pendingConsumeKeyUp: ComposeKey? = null

    /**
     * Enter capture mode: next key press will be **added** to [action]'s key list.
     * Mirrors XPilot's keydefs[] append semantics — no conflicts are cleared.
     */
    fun startAddKey(action: GameAction) {
        mode = BindingMode.Awaiting(action)
    }

    /**
     * Remove one key from an action's key list.
     * Because XPilot allows the same key on multiple actions, removing from
     * one action does not affect others.
     */
    fun removeKey(
        action: GameAction,
        key: ComposeKey,
    ) {
        bindings =
            bindings.map { b ->
                if (b.action == action) b.copy(keys = b.keys - key) else b
            }
    }

    /**
     * Called from the screen's onKeyEvent.
     * - Escape cancels without changing anything.
     * - Any other key is **appended** to the awaiting action's list (if not already present).
     * Returns true if the event was consumed.
     */
    fun applyKey(key: ComposeKey): Boolean {
        val current = mode
        if (current !is BindingMode.Awaiting) return false
        if (key == ComposeKey.Escape) {
            mode = BindingMode.Idle
            return true
        }
        val action = current.action
        bindings =
            bindings.map { b ->
                if (b.action == action && key !in b.keys) {
                    b.copy(keys = b.keys + key)
                } else {
                    b
                }
            }
        mode = BindingMode.Idle
        pendingConsumeKeyUp = key
        return true
    }

    /**
     * Called on KeyUp. Returns true (consume) if this key was the one just
     * captured on KeyDown, clearing the pending flag in the process.
     */
    fun consumeKeyUpIfPending(key: ComposeKey): Boolean {
        if (pendingConsumeKeyUp == key) {
            pendingConsumeKeyUp = null
            return true
        }
        return false
    }

    fun resetDefaults() {
        bindings = defaultBindings()
        mode = BindingMode.Idle
    }

    /**
     * Build a reverse lookup: ComposeKey → all GameActions it triggers.
     * Used by InGameScreen to dispatch key events to the game engine.
     */
    fun buildKeyMap(): Map<ComposeKey, List<GameAction>> {
        val map = mutableMapOf<ComposeKey, MutableList<GameAction>>()
        for (b in bindings) {
            for (k in b.keys) {
                map.getOrPut(k) { mutableListOf() }.add(b.action)
            }
        }
        return map
    }
}

// ---------------------------------------------------------------------------
// Screen composable
// ---------------------------------------------------------------------------

@Composable
fun KeyBindingsScreen(
    initialBindings: List<KeyBinding> = defaultBindings(),
    onSaveBindings: (String) -> Unit = {},
) {
    val navigator = LocalNavigator.current
    val state = remember { KeyBindingsStateHolder(initialBindings) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(KXPilotColors.Background)
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    // Top-down interception so Spacebar/Enter are captured before
                    // child clickable composables see them.
                    // KeyDown: if in awaiting mode, capture the key and record that
                    //          the matching KeyUp must also be suppressed.
                    // KeyUp:   if this key was just captured on KeyDown, consume it
                    //          so the focused chip does not fire a click.
                    when (event.type) {
                        KeyEventType.KeyDown -> state.applyKey(event.key)
                        KeyEventType.KeyUp -> state.consumeKeyUpIfPending(event.key)
                        else -> false
                    }
                },
    ) {
        // ---- Title bar ----
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(KXPilotColors.SurfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Key Bindings",
                style =
                    TextStyle(
                        color = KXPilotColors.AccentBright,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    ),
                modifier = Modifier.weight(1f),
            )
            GameButtonDanger("RESET", onClick = { state.resetDefaults() })
            Spacer(Modifier.width(8.dp))
            GameButton("DONE", onClick = {
                onSaveBindings(serializeBindings(state.bindings))
                navigator.pop()
            })
        }

        // ---- Help text ----
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(KXPilotColors.Surface)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Text(
                "Click [+] to add a key to an action.  Click a key chip to remove it.  " +
                    "One key may trigger multiple actions simultaneously.",
                style =
                    TextStyle(
                        color = KXPilotColors.OnSurfaceDim,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
            )
        }

        // ---- Status banner when awaiting a key ----
        val currentMode = state.mode
        if (currentMode is BindingMode.Awaiting) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(KXPilotColors.Warning.copy(alpha = 0.2f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    "Press a key to add to \"${currentMode.action.description}\"  (Esc to cancel)",
                    style =
                        TextStyle(
                            color = KXPilotColors.Warning,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                        ),
                )
            }
        }

        // ---- Binding rows ----
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }
            items(state.bindings, key = { it.action.name }) { binding ->
                KeyBindingRow(binding, state)
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

// ---------------------------------------------------------------------------
// Row composable
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeyBindingRow(
    binding: KeyBinding,
    state: KeyBindingsStateHolder,
) {
    val isAwaiting =
        state.mode is BindingMode.Awaiting &&
            (state.mode as BindingMode.Awaiting).action == binding.action

    val rowBg = if (isAwaiting) KXPilotColors.Accent.copy(alpha = 0.15f) else KXPilotColors.Surface

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(rowBg)
                .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Action name — fixed width column
        Text(
            binding.action.description,
            style =
                TextStyle(
                    color = if (isAwaiting) KXPilotColors.AccentBright else KXPilotColors.OnSurface,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                ),
            modifier = Modifier.width(160.dp),
        )

        // Key chips — wrap as many keys as needed
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (key in binding.keys) {
                KeyChip(
                    label = key.chipLabel(),
                    tooltip = "Click to remove",
                    isAwaiting = false,
                    onClick = { state.removeKey(binding.action, key) },
                )
            }

            // "+" chip — add another key
            KeyChip(
                label = if (isAwaiting) "…" else "+",
                tooltip = "Click to add a key",
                isAwaiting = isAwaiting,
                onClick = { state.startAddKey(binding.action) },
            )
        }
    }
}

@Composable
private fun KeyChip(
    label: String,
    tooltip: String,
    isAwaiting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isAwaiting) KXPilotColors.Warning else KXPilotColors.SurfaceVariant
    val textColor =
        when {
            isAwaiting -> KXPilotColors.Warning
            label == "+" -> KXPilotColors.Accent
            else -> KXPilotColors.OnSurface
        }
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .background(KXPilotColors.SurfaceVariant)
                .border(1.dp, borderColor)
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(label, style = TextStyle(color = textColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace))
    }
}
