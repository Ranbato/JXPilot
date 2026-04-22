package org.lambertland.kxpilot.ui.stateholder

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.lambertland.kxpilot.model.MotdState

/**
 * UI-layer state holder for [MotdState].
 *
 * Holds Compose [mutableStateOf] state so it lives in the UI layer.
 * The pure domain model [MotdState] lives in [org.lambertland.kxpilot.model].
 */
class MotdStateHolder(
    private val serverName: String,
) {
    var state: MotdState by mutableStateOf(MotdState.Loading)

    private val buffer = StringBuilder()
    private var expectedSize = 0L

    /**
     * Called by the network layer (on Main thread) with each arriving chunk.
     */
    fun appendChunk(
        offset: Long,
        chunk: String,
        totalSize: Long,
    ) {
        expectedSize = totalSize
        buffer.append(chunk)
        state =
            MotdState.Receiving(
                progress = buffer.length.toFloat() / totalSize.coerceAtLeast(1),
            )
    }

    fun onComplete() {
        state =
            if (buffer.isBlank()) {
                MotdState.Empty
            } else {
                MotdState.Loaded(buffer.toString(), serverName)
            }
    }

    fun onError(message: String) {
        state = MotdState.Error(message)
    }
}
