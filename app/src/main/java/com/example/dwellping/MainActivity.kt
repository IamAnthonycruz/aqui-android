package com.example.dwellping

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.dwellping.ui.DwellPingApp
import com.example.dwellping.ui.theme.DwellPingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Every screen is on the dark Dusk gradient, so the system bars always get light icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            DwellPingTheme { DwellPingApp() }
        }
    }

    override fun onResume() {
        super.onResume()
        // Opening the app wakes the tracker: it asks for location again, from the foreground, where
        // Android always allows it. Fixes a tracker that came back from a background restart blind.
        if (Prefs.isEnabled(this) && Perms.hasAll(this)) DwellService.start(this)
    }
}
