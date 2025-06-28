package com.artificialinsightsllc.synopticnetwork

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.artificialinsightsllc.synopticnetwork.navigation.AppNavigation
import com.artificialinsightsllc.synopticnetwork.ui.theme.SynopticNetworkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Set the app's theme.
            SynopticNetworkTheme {
                // The AppNavigation composable is now the entry point of the UI.
                // It will handle displaying the splash screen and all subsequent navigation.
                AppNavigation()
            }
        }
    }
}
