package com.artificialinsightsllc.synopticnetwork.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.artificialinsightsllc.synopticnetwork.ui.screens.CameraScreen
import com.artificialinsightsllc.synopticnetwork.ui.screens.ForgotPasswordScreen
import com.artificialinsightsllc.synopticnetwork.ui.screens.LoginScreen
import com.artificialinsightsllc.synopticnetwork.ui.screens.MainScreen
import com.artificialinsightsllc.synopticnetwork.ui.screens.MakeReportScreen
import com.artificialinsightsllc.synopticnetwork.ui.screens.SignUpScreen
import com.artificialinsightsllc.synopticnetwork.ui.screens.SplashScreen
import com.artificialinsightsllc.synopticnetwork.ui.screens.SettingsScreen
import com.artificialinsightsllc.synopticnetwork.ui.screens.ProductMenuScreen // NEW: Import ProductMenuScreen
import com.artificialinsightsllc.synopticnetwork.ui.screens.ProductDetailScreen // NEW: Import ProductDetailScreen
import com.artificialinsightsllc.synopticnetwork.ui.viewmodels.MakeReportViewModel

/**
 * Sealed class to define the routes for our application screens.
 * Using a sealed class provides type safety and autocompletion benefits.
 */
sealed class Screen(val route: String) {
    object Splash : Screen("splash_screen")
    object Login : Screen("login_screen")
    object SignUp : Screen("signup_screen")
    object ForgotPassword : Screen("forgot_password_screen")
    object Main : Screen("main_screen")
    object MakeReport : Screen("make_report_screen")
    object Camera : Screen("camera_screen")
    object Settings : Screen("settings_screen")
    // MODIFIED: Route for Product Menu, now with a WFO argument
    object ProductMenu : Screen("product_menu_screen/{wfo}") {
        fun createRoute(wfo: String): String {
            return "product_menu_screen/$wfo"
        }
    }
    // NEW: Route for Product Detail, with arguments for productCode and wfo
    object ProductDetail : Screen("product_detail_screen/{productCode}/{wfo}") {
        fun createRoute(productCode: String, wfo: String): String {
            return "product_detail_screen/$productCode/$wfo"
        }
    }
}

/**
 * The main navigation component of the app.
 * This composable sets up the NavHost and defines the navigation graph, linking
 * routes to their corresponding screen composables with custom animations.
 */
@Composable
fun AppNavigation() {
    val navController: NavHostController = rememberNavController()
    val animationDuration = 500 // ms

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // Splash Screen Route - Fades out
        composable(
            route = Screen.Splash.route,
            exitTransition = { fadeOut(animationSpec = tween(animationDuration)) }
        ) {
            SplashScreen(navController = navController)
        }

        // Login Screen Route - Fades in
        composable(
            route = Screen.Login.route,
            enterTransition = { fadeIn(animationSpec = tween(animationDuration)) }
        ) {
            LoginScreen(navController = navController)
        }

        // Sign Up Screen Route - Slides in from the right
        composable(
            route = Screen.SignUp.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(animationDuration)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(animationDuration)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(animationDuration)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(animationDuration)) }
        ) {
            SignUpScreen(navController = navController)
        }

        // Forgot Password Screen Route - Slides in from the right
        composable(
            route = Screen.ForgotPassword.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(animationDuration)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(animationDuration)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(animationDuration)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(animationDuration)) }
        ) {
            ForgotPasswordScreen(navController = navController)
        }

        // Main App Screen Route - Fades in
        composable(
            route = Screen.Main.route,
            enterTransition = { fadeIn(animationSpec = tween(animationDuration)) }
        ) {
            MainScreen(navController = navController)
        }

        // Make Report Screen Route
        composable(
            route = Screen.MakeReport.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(animationDuration)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(animationDuration)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(animationDuration)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(animationDuration)) }
        ) { backStackEntry ->
            // The ViewModel is scoped to this NavBackStackEntry so it can be shared.
            val makeReportViewModel: MakeReportViewModel = viewModel(backStackEntry as ViewModelStoreOwner)
            MakeReportScreen(navController = navController, makeReportViewModel = makeReportViewModel)
        }

        // Camera Screen Route
        composable(
            route = Screen.Camera.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(animationDuration)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(animationDuration)) }
        ) {
            // Get the NavBackStackEntry for the parent screen (MakeReport) to share its ViewModel
            val parentEntry = remember(it) {
                navController.getBackStackEntry(Screen.MakeReport.route)
            }
            val parentViewModel: MakeReportViewModel = viewModel(parentEntry as ViewModelStoreOwner)
            CameraScreen(navController = navController, viewModel = parentViewModel)
        }

        // Settings Screen Route - Slides in from the right (consistent with new screens)
        composable(
            route = Screen.Settings.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(animationDuration)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(animationDuration)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(animationDuration)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(animationDuration)) }
        ) {
            SettingsScreen(navController = navController)
        }

        // MODIFIED: Product Menu Screen Route - now expects a WFO argument
        composable(
            route = Screen.ProductMenu.route,
            arguments = listOf(
                androidx.navigation.navArgument("wfo") { type = androidx.navigation.NavType.StringType }
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(animationDuration)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(animationDuration)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(animationDuration)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(animationDuration)) }
        ) { backStackEntry ->
            val wfo = backStackEntry.arguments?.getString("wfo") ?: ""
            ProductMenuScreen(navController = navController, wfo = wfo) // Pass WFO to screen
        }

        // NEW: Product Detail Screen Route - Slides in from the right, expects arguments
        composable(
            route = Screen.ProductDetail.route,
            arguments = listOf(
                androidx.navigation.navArgument("productCode") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("wfo") { type = androidx.navigation.NavType.StringType }
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(animationDuration)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(animationDuration)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(animationDuration)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(animationDuration)) }
        ) { backStackEntry ->
            val productCode = backStackEntry.arguments?.getString("productCode") ?: ""
            val wfo = backStackEntry.arguments?.getString("wfo") ?: ""
            ProductDetailScreen(navController = navController, productCode = productCode, wfo = wfo)
        }
    }
}
