package org.lambertland.kxpilot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.lambertland.kxpilot.common.formatOneDecimal
import org.lambertland.kxpilot.model.PlayerInfo
import org.lambertland.kxpilot.ui.theme.KXPilotColors

private val OVERLAY_BG = Color.Black.copy(alpha = 0.75f)
private val TEAM_HEADER_BG = Color.Black.copy(alpha = 0.40f)

/**
 * Semi-transparent scoreboard overlay.  Rendered as a plain [Column] —
 * XPilot servers have ≤ 32 players; LazyColumn overhead is unwarranted.
 *
 * @param players    List of all players in the game.
 * @param visible    Whether the overlay is shown.
 */
@Composable
fun ScoreOverlay(
    players: List<PlayerInfo>,
    visible: Boolean,
) {
    if (!visible) return

    val grouped: Map<Int, List<PlayerInfo>> = players.groupBy { it.team }
    val teamOrder =
        grouped.keys
            .sorted() // -1 (no team) last — note: UI convention, wire uses 0xffff
            .sortedWith(compareBy { if (it == -1) Int.MAX_VALUE else it })

    Box(
        modifier =
            Modifier
                .widthIn(min = 240.dp, max = 320.dp)
                .background(OVERLAY_BG)
                .padding(8.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            // ---- Title ----
            Text(
                "SCOREBOARD",
                style =
                    TextStyle(
                        color = KXPilotColors.AccentBright,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    ),
            )
            // ---- Divider ----
            Box(Modifier.fillMaxWidth().height(1.dp).background(KXPilotColors.Accent))
            Spacer(Modifier.height(4.dp))

            for (team in teamOrder) {
                val group = grouped[team] ?: continue
                // Team header
                // team == -1 is the UI sentinel for "no team" (wire protocol uses 0xffff → mapped on decode)
                val teamLabel = if (team == -1) "No team" else "Team $team"
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(TEAM_HEADER_BG)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Text(
                        teamLabel,
                        style =
                            TextStyle(
                                color = KXPilotColors.OnSurfaceDim,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                            ),
                    )
                }
                // Player rows
                for (p in group) {
                    val nameColor =
                        when {
                            p.isSelf -> KXPilotColors.AccentBright
                            p.lives == 0 -> KXPilotColors.ZeroLives
                            else -> KXPilotColors.OnSurface
                        }
                    val prefix = if (p.isSelf) "● " else "  "
                    val scoreStr = if (p.lives == 0) "—" else p.score.formatOneDecimal()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 1.dp),
                    ) {
                        Text(
                            "$prefix${p.name}",
                            style = TextStyle(color = nameColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            p.lives.toString(),
                            style = TextStyle(color = KXPilotColors.OnSurfaceDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                            modifier = Modifier.width(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            scoreStr,
                            style = TextStyle(color = KXPilotColors.OnSurface, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                        )
                    }
                }
            }
        }
    }
}
