package org.lambertland.kxpilot.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.lambertland.kxpilot.AppInfo
import org.lambertland.kxpilot.common.Item
import org.lambertland.kxpilot.ui.LocalNavigator
import org.lambertland.kxpilot.ui.components.GameButton
import org.lambertland.kxpilot.ui.drawItemIcon
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "About KXPilot",
                    style =
                        TextStyle(
                            color = KXPilotColors.AccentBright,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        ),
                )
                Text(
                    AppInfo.VERSION_LABEL,
                    style =
                        TextStyle(
                            color = KXPilotColors.OnSurface,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        ),
                )
            }
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
        Canvas(modifier = Modifier.size(20.dp)) {
            drawItemIcon(itemType = item.itemType)
        }
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
    /**
     * @param itemType  The [Item] enum value — compile-time safe; no stringly-typed dispatch.
     * @param name      Display name shown in the list.
     * @param description  Short description of the item's effect.
     */
    data class BonusItem(
        val itemType: Item,
        val name: String,
        val description: String,
    )

    val bonusItems =
        listOf(
            BonusItem(Item.FUEL, "Fuel", "Extra fuel capacity."),
            BonusItem(Item.WIDEANGLE, "Extra Shot", "Additional shot in flight at once."),
            BonusItem(Item.REARSHOT, "Rear Shot", "Fires a shot directly behind the ship."),
            BonusItem(Item.AFTERBURNER, "Shot Power", "More damage per shot."),
            BonusItem(Item.MISSILE, "Missile", "Homing missile that tracks the nearest enemy."),
            BonusItem(Item.MINE, "Mine", "Stationary explosive — arm and leave in a corridor."),
            BonusItem(Item.CLOAK, "Cloak", "Makes your ship invisible to enemies."),
            BonusItem(Item.SENSOR, "Sensor", "Reveals cloaked ships within detection range."),
            BonusItem(Item.ECM, "ECM", "Disrupts nearby enemy missiles and sensors."),
            BonusItem(Item.TRANSPORTER, "Transporter", "Steal items from nearby ships."),
            BonusItem(Item.TRACTOR_BEAM, "Tractor Beam", "Pull nearby ships (or the ball) toward you."),
            BonusItem(Item.EMERGENCY_THRUST, "Emergency Thrust", "Burst of speed to escape danger."),
            BonusItem(Item.LASER, "Laser", "Continuous beam weapon — high power drain."),
            BonusItem(Item.DEFLECTOR, "Deflector", "Redirects incoming shots away from your ship."),
            BonusItem(Item.HYPERJUMP, "Hyperjump", "Teleport to a random location on the map."),
            BonusItem(Item.PHASING, "Phasing", "Pass through walls briefly."),
            BonusItem(Item.MIRROR, "Mirror", "Reflects shots back at the attacker."),
            BonusItem(Item.ARMOR, "Armor", "Absorbs damage from one shot."),
            BonusItem(Item.AUTOPILOT, "Autopilot", "AI steers toward the nearest fuel depot."),
            BonusItem(Item.EMERGENCY_SHIELD, "Emergency Shield", "Automatically activates shield on incoming shot detection."),
            BonusItem(Item.TANK, "Tank", "Detachable fuel tank — acts as a shield or fuel reserve."),
        )
}
