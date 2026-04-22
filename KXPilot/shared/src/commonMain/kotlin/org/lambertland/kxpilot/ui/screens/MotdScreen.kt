package org.lambertland.kxpilot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.lambertland.kxpilot.model.MotdState
import org.lambertland.kxpilot.ui.LocalNavigator
import org.lambertland.kxpilot.ui.components.GameButton
import org.lambertland.kxpilot.ui.stateholder.MotdStateHolder
import org.lambertland.kxpilot.ui.theme.KXPilotColors

// ---------------------------------------------------------------------------
// Screen composable
// ---------------------------------------------------------------------------

private val STUB_MOTD =
    """
    Welcome to KXPilot Test Server!

    Rules:
      - Play fair
      - No lag cheating
      - Have fun

    Server version: kxpilot-ng 0.1-alpha
    Map: classic.xp
    Max players: 16
    """.trimIndent()

@Composable
fun MotdScreen(serverName: String = "demo-server") {
    val navigator = LocalNavigator.current
    val holder =
        remember(serverName) {
            MotdStateHolder(serverName).also { h ->
                h.appendChunk(0, STUB_MOTD, STUB_MOTD.length.toLong())
                h.onComplete()
            }
        }

    Column(
        modifier = Modifier.fillMaxSize().background(KXPilotColors.Background),
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
                "MOTD — $serverName",
                style =
                    TextStyle(
                        color = KXPilotColors.AccentBright,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    ),
            )
            Spacer(Modifier.weight(1f))
            GameButton("CLOSE", onClick = { navigator.pop() })
        }

        // ---- Divider ----
        Box(Modifier.fillMaxWidth().height(1.dp).background(KXPilotColors.Accent))

        // ---- Content ----
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.TopStart,
        ) {
            when (val s = holder.state) {
                MotdState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = KXPilotColors.Accent)
                    }
                }

                is MotdState.Receiving -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            "Receiving MOTD… ${(s.progress * 100).toInt()}%",
                            style = TextStyle(color = KXPilotColors.OnSurfaceDim, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = s.progress,
                            color = KXPilotColors.Accent,
                            backgroundColor = KXPilotColors.SurfaceVariant,
                            modifier = Modifier.fillMaxWidth(0.6f),
                        )
                    }
                }

                is MotdState.Loaded -> {
                    val lines = remember(s.text) { s.text.lines() }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(lines) { line ->
                            Text(
                                line,
                                style =
                                    TextStyle(
                                        color = KXPilotColors.OnSurface,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                    ),
                            )
                        }
                    }
                }

                MotdState.Empty -> {
                    Text(
                        "This server has no MOTD.",
                        style = TextStyle(color = KXPilotColors.OnSurfaceDim, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                    )
                }

                is MotdState.Error -> {
                    Text(
                        s.message,
                        style = TextStyle(color = KXPilotColors.Danger, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                    )
                }
            }
        }
    }
}
