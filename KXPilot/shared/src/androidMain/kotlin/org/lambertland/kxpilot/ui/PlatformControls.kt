package org.lambertland.kxpilot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.lambertland.kxpilot.client.KeyState
import org.lambertland.kxpilot.common.Key

/** Android actual: virtual D-pad on the left, weapon buttons on the right. */
@Composable
actual fun PlatformControls(keys: KeyState) {
    Box(Modifier.fillMaxSize()) {
        // Left cluster: movement D-pad
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TouchButton(label = "▲", key = Key.KEY_THRUST, keys = keys)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TouchButton(label = "◀", key = Key.KEY_TURN_LEFT, keys = keys)
                TouchButton(label = "SH", key = Key.KEY_SHIELD, keys = keys)
                TouchButton(label = "▶", key = Key.KEY_TURN_RIGHT, keys = keys)
            }
        }

        // Right cluster: weapon buttons
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TouchButton(label = "MSL", key = Key.KEY_FIRE_MISSILE, keys = keys)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TouchButton(label = "MNE", key = Key.KEY_DROP_MINE, keys = keys)
                TouchButton(label = "FIRE", key = Key.KEY_FIRE_SHOT, keys = keys)
            }
        }
    }
}

@Composable
private fun TouchButton(
    label: String,
    key: Key,
    keys: KeyState,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(56.dp)
                .background(Color(0x884488FF), shape = CircleShape)
                .pointerInput(key) {
                    awaitEachGesture {
                        // R7: capture the specific pointer id from the first-down event
                        // so that we only wait for THAT pointer to be lifted, rather
                        // than any pointer.  Without this, two simultaneous touches on
                        // different buttons can cause the first button's release to be
                        // missed when the second finger lifts, leaving a key stuck down.
                        val down = awaitFirstDown()
                        val pointerId = down.id
                        keys.press(key)
                        try {
                            // Wait until the tracked pointer is released.
                            do {
                                val event = awaitPointerEvent()
                                val tracked = event.changes.firstOrNull { it.id == pointerId }
                            } while (tracked != null && tracked.pressed)
                        } finally {
                            keys.release(key)
                        }
                    }
                },
    ) {
        Text(text = label, color = Color.White, fontSize = 12.sp)
    }
}
