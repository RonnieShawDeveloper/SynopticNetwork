package com.artificialinsightsllc.synopticnetwork.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import com.artificialinsightsllc.synopticnetwork.R
import com.artificialinsightsllc.synopticnetwork.navigation.Screen
import com.artificialinsightsllc.synopticnetwork.ui.theme.SynopticNetworkTheme
import kotlinx.coroutines.delay

/**
 * The application's splash screen.
 * It displays the app logo with a fade-in, hold, and fade-out animation sequence.
 * After the animation, it navigates to the Login screen.
 *
 * @param navController The navigation controller used for navigating to the next screen.
 */
@Composable
fun SplashScreen(navController: NavHostController) {
    var startAnimation by remember { mutableStateOf(false) }

    // Animate alpha (for fade in/out) and scale (for a subtle zoom effect)
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 500), // Fade in duration
        label = "alphaAnimation"
    )

    val scaleAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f,
        animationSpec = tween(durationMillis = 500), // Scale in duration
        label = "scaleAnimation"
    )

    // This LaunchedEffect triggers the animation sequence and navigation logic
    // once the composable enters the composition.
    LaunchedEffect(key1 = true) {
        startAnimation = true // Start the fade-in animation (500ms)
        delay(3000) // Hold duration for 3000ms after fade-in completes
        // After the hold, we navigate away. The NavHost exit transition (500ms) will handle the fade-out.
        navController.navigate(Screen.Login.route) {
            // Remove the splash screen from the back stack
            popUpTo(Screen.Splash.route) {
                inclusive = true
            }
        }
    }

    // UI layout for the splash screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background), // Use theme's background color
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.splashscreen), // Using your specified drawable
            contentDescription = "Application Logo",
            // Make the image fill the width of the screen, scaling it appropriately.
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth() // <-- This ensures the image takes the full width
                .alpha(alphaAnim.value) // Apply the alpha animation
                .scale(scaleAnim.value) // Apply the scale animation
        )
    }
}

// Preview function to see the splash screen in Android Studio's design view
@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    SynopticNetworkTheme {
        // In preview, we can't use a real NavController, so we create a dummy Box
        // to display the initial state of the splash screen.
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.splashscreen),
                contentDescription = "Application Logo",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
