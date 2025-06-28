package com.artificialinsightsllc.synopticnetwork.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Defines the light color scheme for the application, using the custom NWS/NOAA colors.
 * This is the only color scheme used, as the app is light-theme only.
 */
private val AppLightColorScheme = lightColorScheme(
    primary = NOAA_Blue,
    secondary = NWS_Sky_Blue,
    background = Background,
    surface = Surface,
    onPrimary = Color.White, // Text on top of primary color
    onSecondary = Color.White, // Text on top of secondary color
    onBackground = PrimaryText, // Text on top of background color
    onSurface = OnSurface, // Text on top of surface color
    error = Accent_Error,
    outline = Outline
)

/**
 * This is the main theme composable for "The Synoptic Network" application.
 * It applies the custom color scheme and typography.
 * It is hardcoded to use the light theme only, as per the project requirements.
 *
 * @param content The composable content to be themed.
 */
@Composable
fun SynopticNetworkTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = AppLightColorScheme
    val view = LocalView.current

    // This side effect ensures that the system UI (like the status bar) matches our theme.
    // It's crucial for a clean, immersive look.
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb() // Set status bar color
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false // Set status bar icons to be light
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Assumes Typography.kt is defined
        content = content
    )
}
