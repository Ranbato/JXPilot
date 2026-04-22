package org.lambertland.kxpilot.model

import org.lambertland.kxpilot.server.ConnectedPlayer

// ---------------------------------------------------------------------------
// Dashboard player row (UI-facing projection of ConnectedPlayer)
// ---------------------------------------------------------------------------

/**
 * Flat row model used to populate the player table in [ServerDashboardScreen].
 * Derived from [ConnectedPlayer] with display-ready string fields.
 */
data class DashboardPlayerRow(
    val id: Int,
    val name: String,
    val team: String,
    val score: String,
    val ping: String,
    val address: String,
    val isMuted: Boolean,
) {
    companion object {
        fun from(p: ConnectedPlayer): DashboardPlayerRow =
            DashboardPlayerRow(
                id = p.id,
                name = p.name,
                team = if (p.team == 0) "—" else p.team.toString(),
                score = p.score.toString(),
                ping = p.pingMs?.let { "${it}ms" } ?: "—",
                address = p.address,
                isMuted = p.isMuted,
            )
    }
}

// ---------------------------------------------------------------------------
// Server event log entry
// ---------------------------------------------------------------------------

/** Severity level for a [ServerEvent]. */
enum class ServerEventLevel { INFO, WARN, ERROR }

/**
 * A single entry in the server event log shown at the bottom of the dashboard.
 *
 * @param uptimeMs  Server uptime at the moment this event was emitted.
 * @param level     Severity.
 * @param message   Human-readable description.
 */
data class ServerEvent(
    val uptimeMs: Long,
    val level: ServerEventLevel,
    val message: String,
)
