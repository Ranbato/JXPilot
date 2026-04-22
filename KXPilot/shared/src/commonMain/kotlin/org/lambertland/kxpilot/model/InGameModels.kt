package org.lambertland.kxpilot.model

// Domain layer — NO Compose imports

enum class MessageColor { NORMAL, BALL, SAFE, COVER, POP }

data class MessageEntry(
    val text: String,
    val color: MessageColor,
    /** Wall-clock millis (System.currentTimeMillis equivalent). NOT frame count. */
    val arrivedAt: Long,
)

data class HudState(
    val fuel: Float, // current fuel 0–1000
    val fuelMax: Float, // max fuel capacity
    val power: Float, // normalised 0–1 for power meter
    val turnSpeed: Float, // normalised 0–1 for turn-speed meter
    val packetLoss: Float, // normalised 0–1
    val packetLag: Float, // normalised 0–1
    val directionRad: Float, // ship heading in radians (Y-up)
    val messages: List<MessageEntry>, // bounded by maxMessages config option
    val score: Double,
    val lives: Int,
) {
    companion object {
        val EMPTY =
            HudState(
                fuel = 500f,
                fuelMax = 1000f,
                power = 0.55f,
                turnSpeed = 0.18f,
                packetLoss = 0f,
                packetLag = 0.1f,
                directionRad = 0f,
                messages = emptyList(),
                score = 0.0,
                lives = 3,
            )
    }
}

data class PlayerInfo(
    val id: Int,
    val name: String,
    val lives: Int,
    val score: Double,
    /**
     * UI-layer team identifier.
     * - `-1` means "no team" (used by [ScoreOverlay] as a sort sentinel).
     * - Wire protocol uses `GameConst.TEAM_NOT_SET = 0xffff` for the same concept.
     * Callers that decode wire packets **must** map `0xffff → -1` when constructing [PlayerInfo].
     */
    val team: Int, // -1 = no team (UI convention; wire protocol uses 0xffff)
    val isSelf: Boolean,
)

sealed class TalkResult {
    data class Send(
        val message: String,
    ) : TalkResult()

    object Cancel : TalkResult()
}
