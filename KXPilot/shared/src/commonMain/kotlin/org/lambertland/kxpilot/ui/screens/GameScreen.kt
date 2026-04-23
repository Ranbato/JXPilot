package org.lambertland.kxpilot.ui.screens

import androidx.compose.runtime.Composable
import org.lambertland.kxpilot.ui.InGameScreen

/**
 * Entry point for the game view, navigated to from the main menu.
 *
 * [serverHost] and [serverPort] are reserved for when real network connection
 * replaces the local demo mode.  They are currently unused but retained so the
 * navigation call-site in App.kt continues to compile without changes when the
 * real client transport is wired in.
 *
 * R18: parameters are intentionally unused — suppress the IDE warning explicitly
 * so a future engineer does not accidentally delete them.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun GameScreen(
    serverHost: String,
    serverPort: Int,
) = InGameScreen()
