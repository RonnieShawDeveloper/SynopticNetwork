package com.artificialinsightsllc.synopticnetwork.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Streetview
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artificialinsightsllc.synopticnetwork.data.models.AlertSeverity
import com.artificialinsightsllc.synopticnetwork.navigation.Screen
import com.artificialinsightsllc.synopticnetwork.ui.theme.Transparent_Black
import com.google.maps.android.compose.MapType

/**
 * Composable that contains all the floating UI controls for the map screen.
 *
 * @param navController The navigation controller.
 * @param alertsCount The number of active alerts to display on the badge.
 * @param highestSeverity The highest severity of active alerts, to determine FAB color.
 * @param radarWfo The WFO code for radar, used to enable/disable product menu.
 * @param showReflectivityRadarOverlay Current state of reflectivity radar overlay.
 * @param onToggleReflectivityRadarOverlay Callback to toggle reflectivity radar.
 * @param showVelocityRadarOverlay Current state of velocity radar overlay.
 * @param onToggleVelocityRadarOverlay Callback to toggle velocity radar.
 * @param showPlacefileOverlay Current state of placefile overlay.
 * @param onTogglePlacefileOverlay Callback to toggle placefile overlay.
 * @param currentMapType Current map type (NORMAL or SATELLITE).
 * @param onMapTypeSelected Callback to change map type.
 * @param lastRadarUpdateTimeString Formatted string for last radar update time.
 * @param isRadarActive Added to simplify radar badge visibility.
 * @param onLegendFabClick Callback for when the Legend (Filters) FAB is clicked.
 * @param onAlertsFabClick Callback for when the Alerts FAB is clicked.
 */
@Composable
fun MapControls(
    navController: androidx.navigation.NavHostController,
    alertsCount: Int,
    highestSeverity: AlertSeverity,
    radarWfo: String?,
    showReflectivityRadarOverlay: Boolean,
    onToggleReflectivityRadarOverlay: (Boolean) -> Unit,
    showVelocityRadarOverlay: Boolean,
    onToggleVelocityRadarOverlay: (Boolean) -> Unit,
    showPlacefileOverlay: Boolean,
    onTogglePlacefileOverlay: (Boolean) -> Unit,
    currentMapType: MapType,
    onMapTypeSelected: (MapType) -> Unit,
    lastRadarUpdateTimeString: String?,
    isRadarActive: Boolean, // Added to simplify radar badge visibility
    onLegendFabClick: () -> Unit, // NEW: Callback for Legend FAB
    onAlertsFabClick: () -> Unit // NEW: Callback for Alerts FAB
) {
    Column(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween, // Push top and bottom content apart
        horizontalAlignment = Alignment.End // Align content to the right
    ) {
        // Top Row: Legend FAB (Left) and Map Type Selector (Right)
        Row(
            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Legend FAB with "FILTERS" Badge
            FabWithBadge(
                onClick = onLegendFabClick, // Corrected: Use the passed callback
                icon = { Icon(Icons.Default.Layers, contentDescription = "Map Legend") },
                contentDescription = "Map Legend",
                badgeText = "FILTERS",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )

            // Map Type Selector
            MapTypeSelector(
                currentMapType = currentMapType,
                onMapTypeSelected = onMapTypeSelected
            )
        }

        // Spacer to push content to bottom
        Spacer(modifier = androidx.compose.ui.Modifier.weight(1f))

        // Bottom section: A Row containing Alerts FAB (Left), Radar Badge (Center), and Action Buttons (Right)
        Row(
            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween, // Distribute items horizontally
            verticalAlignment = Alignment.Bottom // Align items to the bottom
        ) {
            // Alerts FAB with "ALERTS" Badge (Left)
            FabWithBadge(
                onClick = onAlertsFabClick, // Corrected: Use the passed callback
                icon = {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Info, contentDescription = "Active Alerts")
                        if (alertsCount > 0) {
                            Badge(
                                modifier = androidx.compose.ui.Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 10.dp, y = (-10).dp),
                                containerColor = MaterialTheme.colorScheme.error
                            ) {
                                Text(alertsCount.toString())
                            }
                        }
                    }
                },
                contentDescription = "Active Alerts",
                badgeText = "ALERTS",
                containerColor = getAlertsFabColor(highestSeverity),
                contentColor = Color.White
            )

            // Radar Last Updated Badge (Conditional Visibility) - CENTERED BETWEEN FABs
            if (isRadarActive && lastRadarUpdateTimeString != null) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Transparent_Black),
                    modifier = androidx.compose.ui.Modifier
                        .align(Alignment.Bottom) // Aligns the bottom of the card to the bottom of the Row
                        .weight(1f) // Takes up available space horizontally
                        .padding(horizontal = 8.dp) // Add horizontal padding to separate from FABs
                ) {
                    Text(
                        text = lastRadarUpdateTimeString,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center, // Centers the text horizontally within the Card
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            } else {
                // If no radar is active, still provide a spacer to maintain layout
                Spacer(modifier = androidx.compose.ui.Modifier.weight(1f))
            }

            // Action Buttons column (Right)
            ActionButtons(
                navController = navController,
                radarWfo = radarWfo,
                showReflectivityRadarOverlay = showReflectivityRadarOverlay,
                onToggleReflectivityRadarOverlay = onToggleReflectivityRadarOverlay,
                showVelocityRadarOverlay = showVelocityRadarOverlay,
                onToggleVelocityRadarOverlay = onToggleVelocityRadarOverlay,
                showPlacefileOverlay = showPlacefileOverlay,
                onTogglePlacefileOverlay = onTogglePlacefileOverlay
            )
        }
    }
}

/**
 * Reusable Composable for a Floating Action Button with a text badge.
 */
@Composable
fun FabWithBadge(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    contentDescription: String,
    badgeText: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = androidx.compose.ui.Modifier,
    enabled: Boolean = true
) {
    Box(modifier = modifier.width(80.dp)) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = containerColor,
            contentColor = contentColor,
            modifier = androidx.compose.ui.Modifier.align(Alignment.Center),
        ) {
            icon()
        }

        // Badge Text
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            modifier = androidx.compose.ui.Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Text(
                text = badgeText,
                color = Color.White,
                fontSize = 10.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

/**
 * Determines the background color of the Alerts FAB based on the highest alert severity.
 */
@Composable
private fun getAlertsFabColor(highestSeverity: AlertSeverity): Color {
    return when (highestSeverity) {
        AlertSeverity.EXTREME -> Color(0xFFD32F2F) // Red (Error)
        AlertSeverity.SEVERE -> Color(0xFFF57C00) // Orange
        AlertSeverity.MODERATE -> Color(0xFFFFA000) // Darker Orange/Amber
        AlertSeverity.MINOR -> Color(0xFF1976D2) // Blue
        AlertSeverity.UNKNOWN -> Color(0xFF1976D2) // Blue for unknown as well
        AlertSeverity.NONE -> Color(0xFF388E3C) // Green (Success)
    }
}

@Composable
private fun MapTypeSelector(currentMapType: MapType, onMapTypeSelected: (MapType) -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Transparent_Black)) {
        Row {
            IconButton(onClick = { onMapTypeSelected(MapType.NORMAL) }) {
                Icon(Icons.Default.Streetview, "Street View", tint = if (currentMapType == MapType.NORMAL) MaterialTheme.colorScheme.primary else Color.White)
            }
            IconButton(onClick = { onMapTypeSelected(MapType.SATELLITE) }) {
                Icon(Icons.Default.Satellite, "Satellite View", tint = if (currentMapType == MapType.SATELLITE) MaterialTheme.colorScheme.primary else Color.White)
            }
        }
    }
}

@Composable
private fun ActionButtons(
    navController: androidx.navigation.NavHostController,
    modifier: Modifier = androidx.compose.ui.Modifier,
    radarWfo: String?,
    showReflectivityRadarOverlay: Boolean,
    onToggleReflectivityRadarOverlay: (Boolean) -> Unit,
    showVelocityRadarOverlay: Boolean,
    onToggleVelocityRadarOverlay: (Boolean) -> Unit,
    showPlacefileOverlay: Boolean,
    onTogglePlacefileOverlay: (Boolean) -> Unit
) {
    // Determine if the "Weather Products" FAB should be enabled
    val isProductFabEnabled = radarWfo != null && radarWfo.removePrefix("K").isNotBlank()

    // Define the onClick lambda for the "Weather Products" FAB
    val productFabOnClick: () -> Unit = if (isProductFabEnabled) {
        {
            radarWfo?.let { wfo ->
                val cleanWfo = wfo.removePrefix("K")
                Log.d("ActionButtons", "Navigating to ProductMenu with cleanWFO: $cleanWfo")
                if (cleanWfo.isNotBlank()) {
                    navController.navigate(Screen.ProductMenu.createRoute(cleanWfo))
                } else {
                    Log.w("ActionButtons", "Cleaned WFO is blank, cannot navigate to ProductMenu.")
                }
            }
        }
    } else {
        // Provide a no-op lambda when disabled
        {}
    }

    // Determine the container color for the "Weather Products" FAB based on its enabled state
    val productFabContainerColor = if (isProductFabEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)

    Column(modifier = modifier, horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // FAB for Reflectivity Radar Toggle
        FabWithBadge(
            onClick = {
                val newState = !showReflectivityRadarOverlay
                onToggleReflectivityRadarOverlay(newState)
            },
            icon = { Icon(Icons.Default.Satellite, "Toggle Reflectivity Radar") },
            contentDescription = "Toggle Reflectivity Radar",
            badgeText = "RADAR",
            containerColor = if (showReflectivityRadarOverlay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            contentColor = Color.White,
            enabled = true // This FAB is always enabled
        )

        // FAB for NEXRAD Level 3 Attributes Placefile Toggle
        val isPlacefileFabEnabled = showReflectivityRadarOverlay // Only enabled if reflectivity radar is on
        FabWithBadge(
            onClick = {
                val newState = !showPlacefileOverlay
                onTogglePlacefileOverlay(newState)
            },
            icon = { Icon(Icons.Default.Layers, "Toggle NEXRAD L3 Attributes") }, // Using Layers icon for attributes
            contentDescription = "Toggle NEXRAD L3 Attributes",
            badgeText = "ATTRIBUTES",
            containerColor = if (showPlacefileOverlay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            contentColor = Color.White,
            enabled = isPlacefileFabEnabled // This is correctly passed and will disable the FAB if radar is off
        )


        // FAB for Velocity Radar Toggle
        FabWithBadge(
            onClick = {
                // Corrected: Calculate newValue within the lambda's scope
                val newState = !showVelocityRadarOverlay
                onToggleVelocityRadarOverlay(newState)
            },
            icon = { Icon(Icons.Default.Streetview, "Toggle Velocity Radar") },
            contentDescription = "Toggle Velocity Radar",
            badgeText = "VELOCITY",
            containerColor = if (showVelocityRadarOverlay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            contentColor = Color.White,
            enabled = true // This FAB is always enabled
        )

        // FAB for Weather Products
        FabWithBadge(
            onClick = productFabOnClick,
            icon = { Icon(Icons.Default.Description, "Weather Products") },
            contentDescription = "Weather Products",
            badgeText = "PRODUCTS",
            containerColor = productFabContainerColor,
            contentColor = Color.White,
            enabled = isProductFabEnabled
        )

        // FAB for User Settings (always enabled)
        FabWithBadge(
            onClick = { navController.navigate(Screen.Settings.route) },
            icon = { Icon(Icons.Default.Settings, "User Settings") },
            contentDescription = "User Settings",
            badgeText = "SETTINGS",
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = Color.White,
            enabled = true // This FAB is always enabled
        )

        // FAB for Make Report (always enabled)
        FabWithBadge(
            onClick = { navController.navigate(Screen.MakeReport.route) },
            icon = { Icon(Icons.Default.AddAPhoto, "Make Report") },
            contentDescription = "Make Report",
            badgeText = "REPORT",
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            enabled = true // This FAB is always enabled
        )
    }
}
