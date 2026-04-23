package org.lambertland.kxpilot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.lambertland.kxpilot.resources.ResourceLoader
import org.lambertland.kxpilot.ui.App

// R45: use ComponentActivity rather than AppCompatActivity — Compose does not
// need the AppCompat theme system and ComponentActivity has a smaller footprint.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialise Android asset loader before any composable calls readResourceText().
        ResourceLoader.init(assets)
        setContent {
            App()
        }
    }
}
