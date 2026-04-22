package org.lambertland.kxpilot.model

// ---------------------------------------------------------------------------
// MotdState — pure sealed class, no Compose dependency
// ---------------------------------------------------------------------------

sealed class MotdState {
    object Loading : MotdState()

    data class Receiving(
        val progress: Float,
    ) : MotdState() // 0f–1f

    data class Loaded(
        val text: String,
        val server: String,
    ) : MotdState()

    object Empty : MotdState()

    data class Error(
        val message: String,
    ) : MotdState()
}
