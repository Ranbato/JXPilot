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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.lambertland.kxpilot.ui.LocalNavigator
import org.lambertland.kxpilot.ui.components.GameButton
import org.lambertland.kxpilot.ui.theme.KXPilotColors

// ---------------------------------------------------------------------------
// About screen
// ---------------------------------------------------------------------------

@Composable
fun AboutScreen() {
    val navigator = LocalNavigator.current
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(KXPilotColors.Background),
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
                "About KXPilot",
                style =
                    TextStyle(
                        color = KXPilotColors.AccentBright,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    ),
                modifier = Modifier.weight(1f),
            )
            GameButton("CLOSE", onClick = { navigator.pop() })
        }

        // ---- Scrollable content ----
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item { Spacer(Modifier.height(16.dp)) }

            // Section 1 — About KXPilot
            item { SectionHeader("About KXPilot") }
            item {
                BodyText(
                    """
                    KXPilot is a Kotlin Multiplatform port of XPilot NG 4.7.3.

                    XPilot is a multi-player 2D space combat game originally written in C
                    for Unix/X11 by Bjørn Stabell, Ken Ronny Schouten, and Bert Gijsbers in
                    1991.  XPilot NG is a community continuation maintained at xpilot.sf.net.

                    KXPilot reimplements the game engine, rendering pipeline, and client UI
                    in Kotlin using Compose Multiplatform, targeting Desktop (JVM), Android,
                    and Web (wasmJs).
                    """.trimIndent(),
                )
            }

            item { Spacer(Modifier.height(12.dp)) }
            item { AboutDivider() }

            // Section 2 — Game Objective
            item { SectionHeader("Game Objective") }
            item {
                BodyText(
                    """
                    Pilot your ship in a toroidal (wrapping) 2D world.  Collect bonus items,
                    refuel at fuel depots, and destroy enemy ships with shots, missiles, and
                    mines.  Servers support team play, ball capture, and other game modes.

                    Your ship has limited fuel — running out means drifting helplessly until
                    you can reach a fuel depot.  Shields protect you from shots but drain fuel
                    fast.  Manage both carefully.
                    """.trimIndent(),
                )
            }

            item { Spacer(Modifier.height(12.dp)) }
            item { AboutDivider() }

            // Section 3 — Bonus Items
            item { SectionHeader("Bonus Items") }
            item { Spacer(Modifier.height(8.dp)) }
            items(AboutContent.bonusItems) { item ->
                BonusItemRow(item)
                Spacer(Modifier.height(6.dp))
            }

            item { Spacer(Modifier.height(12.dp)) }
            item { AboutDivider() }

            // Section 4 — Credits
            item { SectionHeader("Credits") }
            item {
                BodyText(
                    """
                    XPilot original:    Bjørn Stabell, Ken Ronny Schouten, Bert Gijsbers
                    XPilot NG:          The XPilot NG team (xpilot.sourceforge.net)
                    KXPilot:            KXPilot contributors

                    Source code:        https://github.com/ (KXPilot repository)
                    Bug reports:        See repository issues page

                    KXPilot is free software distributed under the terms of the GNU General
                    Public License version 2 (or later), the same licence as XPilot NG.
                    """.trimIndent(),
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ---------------------------------------------------------------------------
// Sub-composables
// ---------------------------------------------------------------------------

@Composable
private fun SectionHeader(title: String) {
    Column {
        Spacer(Modifier.height(12.dp))
        Text(
            title,
            style =
                TextStyle(
                    color = KXPilotColors.AccentBright,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                ),
        )
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun BodyText(text: String) {
    Text(
        text,
        style =
            TextStyle(
                color = KXPilotColors.OnSurface,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 18.sp,
            ),
    )
}

@Composable
private fun AboutDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(KXPilotColors.SurfaceVariant),
    )
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun BonusItemRow(item: AboutContent.BonusItem) {
    Row(verticalAlignment = Alignment.Top) {
        // Placeholder icon — coloured square
        Box(
            modifier =
                Modifier
                    .size(20.dp)
                    .background(item.iconColor),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                item.name,
                style =
                    TextStyle(
                        color = KXPilotColors.Success,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    ),
            )
            Text(
                item.description,
                style =
                    TextStyle(
                        color = KXPilotColors.OnSurface,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp,
                    ),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Static content
// ---------------------------------------------------------------------------

private object AboutContent {
    data class BonusItem(
        val name: String,
        val description: String,
        val iconColor: Color,
    )

    val bonusItems =
        listOf(
            BonusItem("Fuel", "Extra fuel capacity.", Color(0xFF226622)),
            BonusItem("Extra Shot", "Additional shot in flight at once.", Color(0xFFFFFFFF)),
            BonusItem("Shot Speed", "Increases shot velocity.", Color(0xFFCCCCCC)),
            BonusItem("Shot Power", "More damage per shot.", Color(0xFFFF8800)),
            BonusItem("Missile", "Homing missile that tracks the nearest enemy.", Color(0xFFFF4444)),
            BonusItem("Mine", "Stationary explosive — arm and leave in a corridor.", Color(0xFF00FFFF)),
            BonusItem("Cloak", "Makes your ship invisible to enemies.", Color(0xFF444488)),
            BonusItem("Sensor", "Reveals cloaked ships within detection range.", Color(0xFF88FF88)),
            BonusItem("ECM", "Disrupts nearby enemy missiles and sensors.", Color(0xFFFFFF00)),
            BonusItem("Transporter", "Steal items from nearby ships.", Color(0xFFFF88FF)),
            BonusItem("Tractor Beam", "Pull nearby ships (or the ball) toward you.", Color(0xFF4488FF)),
            BonusItem("Pressor Beam", "Push nearby ships away.", Color(0xFF44AAFF)),
            BonusItem("Laser", "Continuous beam weapon — high power drain.", Color(0xFFFF2200)),
            BonusItem("Deflector", "Redirects incoming shots away from your ship.", Color(0xFF888888)),
            BonusItem("Hyperjump", "Teleport to a random location on the map.", Color(0xFF8800FF)),
            BonusItem("Phasing", "Pass through walls briefly.", Color(0xFF00FFCC)),
            BonusItem("Armor", "Absorbs damage from one shot.", Color(0xFF886600)),
            BonusItem("Autopilot", "AI steers toward the nearest fuel depot.", Color(0xFF44FF44)),
            BonusItem("Emergency Shield", "Automatically activates shield on incoming shot detection.", Color(0xFF4444FF)),
            BonusItem("Tank", "Detachable fuel tank — acts as a shield or fuel reserve.", Color(0xFF664422)),
            BonusItem("Nuclear Mine", "Large-radius explosive — handle with extreme caution.", Color(0xFFFF0000)),
        )
}
