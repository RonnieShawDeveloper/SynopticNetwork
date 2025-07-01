package com.artificialinsightsllc.synopticnetwork.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as GraphicsColor // Alias to avoid ambiguity with Compose Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Log // Import Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Streetview
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.artificialinsightsllc.synopticnetwork.R
import com.artificialinsightsllc.synopticnetwork.data.models.AlertFeature
import com.artificialinsightsllc.synopticnetwork.data.models.AlertSeverity
import com.artificialinsightsllc.synopticnetwork.data.models.Comment
import com.artificialinsightsllc.synopticnetwork.data.models.MapReport
import com.artificialinsightsllc.synopticnetwork.data.models.Report
import com.artificialinsightsllc.synopticnetwork.navigation.Screen
import com.artificialinsightsllc.synopticnetwork.ui.theme.SynopticNetworkTheme
import com.artificialinsightsllc.synopticnetwork.ui.theme.Transparent_Black
import com.artificialinsightsllc.synopticnetwork.ui.viewmodels.MainViewModel
import com.artificialinsightsllc.synopticnetwork.ui.viewmodels.DisplayMarker // Import DisplayMarker
import com.artificialinsightsllc.synopticnetwork.ui.viewmodels.DisplayAlert // NEW: Import DisplayAlert
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.artificialinsightsllc.synopticnetwork.data.models.getReportTypesWithEmojis
import com.google.maps.android.compose.Polyline // Import Polyline for drawing lines
import androidx.compose.ui.geometry.Offset // Import Offset for marker anchor
import com.google.android.gms.maps.model.CameraPosition
import com.artificialinsightsllc.synopticnetwork.data.services.RadarTileProvider // Import the new TileProvider
import okhttp3.OkHttpClient // Import OkHttpClient


/**
 * The main screen of the app.
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    mainViewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val mapState by mainViewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val reportSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val alertsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false) // Allow partial expansion
    val legendSheetState = rememberModalBottomSheetState()
    var showAddCommentDialog by remember { mutableStateOf(false) }
    var showLegendSheet by remember { mutableStateOf(false) }
    var showAlertsSheet by remember { mutableStateOf(false) } // State to control alerts bottom sheet visibility
    var selectedAlertForDialog by remember { mutableStateOf<AlertFeature?>(null) } // New state for dialog alert
    var showReflectivityRadarOverlay by remember { mutableStateOf(false) } // MODIFIED: State for reflectivity radar visibility
    var showVelocityRadarOverlay by remember { mutableStateOf(false) } // NEW: State for velocity radar visibility


    // Removed local `selectedGroupedReports` state, as it will now be sourced from ViewModel
    var showGroupedReportsDialog by remember { mutableStateOf(false) }


    val defaultLocation = LatLng(28.3486, -82.6826)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 10f)
    }

    val uiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                zoomControlsEnabled = false, compassEnabled = true, myLocationButtonEnabled = true,
                mapToolbarEnabled = false, rotationGesturesEnabled = true, scrollGesturesEnabled = true,
                tiltGesturesEnabled = true, zoomGesturesEnabled = true
            )
        )
    }

    val locationPermissionState = rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(locationPermissionState.status) {
        if (locationPermissionState.status.isGranted) mainViewModel.onMapReady(context)
    }

    // Connect map zoom to ViewModel
    LaunchedEffect(cameraPositionState.position.zoom) {
        mainViewModel.onMapZoomChanged(cameraPositionState.position.zoom)
    }

    LaunchedEffect(mapState.currentLocation) {
        mapState.currentLocation?.let {
            cameraPositionState.animate(CameraUpdateFactory.newCameraPosition(CameraPosition(it, 10f, 0f, 0f)), 1000)
        }
    }

    // Observe selectedGroupedFullReports from ViewModel to trigger dialog visibility
    LaunchedEffect(mapState.selectedGroupedFullReports) {
        if (mapState.selectedGroupedFullReports.isNotEmpty()) {
            showGroupedReportsDialog = true
        } else {
            showGroupedReportsDialog = false
        }
    }


    // Show the Report Details Bottom Sheet
    if (mapState.selectedReport != null) {
        ModalBottomSheet(
            onDismissRequest = { mainViewModel.onMarkerClicked(null) },
            sheetState = reportSheetState,
            modifier = Modifier.fillMaxHeight(0.9f)
        ) {
            ReportBottomSheetContent(
                report = mapState.selectedReport!!,
                comments = mapState.comments,
                isLoadingComments = mapState.isLoadingComments,
                onAddCommentClicked = { showAddCommentDialog = true }
            )
        }
    }

    // Show the Legend Bottom Sheet
    if (showLegendSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLegendSheet = false },
            sheetState = legendSheetState
        ) {
            LegendBottomSheetContent(
                filters = mapState.reportTypeFilters,
                onFilterChanged = { type, isVisible -> mainViewModel.onFilterChanged(type, isVisible) }
            )
        }
    }

    // Show the Active Alerts Bottom Sheet
    if (showAlertsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAlertsSheet = false },
            sheetState = alertsSheetState,
            modifier = Modifier.fillMaxHeight(0.9f)
        ) {
            AlertsBottomSheetContent(
                activeAlerts = mapState.activeAlerts, // MODIFIED: Pass DisplayAlert list
                isLoadingAlerts = mapState.alertsLoading,
                radarWfo = mapState.radarWfo
            )
        }
    }

    // Show Alert Details Dialog when an alert polygon is clicked
    selectedAlertForDialog?.let { alert ->
        AlertDetailsDialog(
            alert = alert,
            onDismiss = { selectedAlertForDialog = null }
        )
    }

    // Show Grouped Reports Dialog when a group marker is clicked
    if (showGroupedReportsDialog && mapState.selectedGroupedFullReports.isNotEmpty()) {
        GroupedReportsDialog(
            reports = mapState.selectedGroupedFullReports, // Use the reports from ViewModel state
            onDismiss = {
                showGroupedReportsDialog = false
                mainViewModel.onGroupMarkerClicked("") // Clear the selected grouped reports in ViewModel
            },
            onReportSelected = { report ->
                mainViewModel.onMarkerClicked(report.toMapReport()) // Pass MapReport for consistency, or adjust onMarkerClicked
                showGroupedReportsDialog = false // Dismiss group dialog
                mainViewModel.onGroupMarkerClicked("") // Clear selected grouped reports after selection
                scope.launch { reportSheetState.show() }
            }
        )
    }

    if (showAddCommentDialog) {
        AddCommentDialog(
            onDismiss = { showAddCommentDialog = false },
            onPostComment = { commentText ->
                mapState.selectedReport?.let { mainViewModel.addComment(it.reportId, commentText) }
                showAddCommentDialog = false
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val markerIconFactory = remember { MarkerIconFactory(context) }
        // NEW: Initialize OkHttpClient for RadarTileProvider
        val okHttpClient = remember { OkHttpClient() }

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapState.mapProperties,
            uiSettings = uiSettings
        ) {
            // Iterate over displayedMarkers instead of rawReports
            mapState.displayedMarkers.forEach { displayMarker ->
                when (displayMarker) {
                    is DisplayMarker.IndividualReport -> {
                        val report = displayMarker.report
                        val position = displayMarker.displayLatLng
                        val markerState = rememberMarkerState(position = position)
                        Marker(
                            state = markerState,
                            title = report.reportType,
                            snippet = report.reportType,
                            icon = markerIconFactory.createMarkerIcon(report),
                            anchor = Offset(0.5f, 0.5f), // Set anchor to center of icon for consistent line attachment
                            onClick = {
                                mainViewModel.onMarkerClicked(report)
                                scope.launch { reportSheetState.show() }
                                true
                            }
                        )
                        // Draw line from center to individual marker if it was spread
                        displayMarker.groupCenterLatLng?.let { center ->
                            Polyline(
                                points = listOf(center, position), // position is now center of marker
                                color = Color.Blue, // Changed color to blue
                                width = 3f // Line width
                            )
                        }
                    }
                    is DisplayMarker.GroupMarker -> {
                        val position = displayMarker.centerLatLng
                        val markerState = rememberMarkerState(position = position)
                        Marker(
                            state = markerState,
                            title = "${displayMarker.count} Reports",
                            snippet = "Click to view reports",
                            icon = markerIconFactory.createGroupMarkerIcon(displayMarker.count),
                            onClick = {
                                // When a group marker is clicked, trigger the ViewModel to fetch details
                                mainViewModel.onGroupMarkerClicked(displayMarker.geohash)
                                // The dialog will show via LaunchedEffect when ViewModel updates selectedGroupedFullReports
                                true
                            }
                        )
                    }
                    is DisplayMarker.SpreadCenter -> {
                        // This marker represents the yellow circle at the center
                        val position = displayMarker.centerLatLng
                        val markerState = rememberMarkerState(position = position)
                        Marker(
                            state = markerState,
                            title = "Report Location",
                            icon = markerIconFactory.createSpreadCenterIcon()
                        )
                    }
                }
            }

            // Draw NWS Alert Polygons
            mapState.activeAlerts.forEach { displayAlert -> // MODIFIED: Iterate over DisplayAlert
                val alert = displayAlert.alert // Get the actual AlertFeature
                alert.geometry?.let { geometry ->
                    if (geometry.type == "Polygon" && !geometry.coordinates.isNullOrEmpty()) {
                        // NWS polygon coordinates are [lon, lat], Google Maps expects LatLng(lat, lon)
                        val polygonPoints = geometry.coordinates.firstOrNull()?.mapNotNull { coords ->
                            if (coords.size == 2) LatLng(coords[1], coords[0]) else null
                        } ?: emptyList()

                        if (polygonPoints.isNotEmpty()) {
                            val alertColor = getAlertSeverityColor(AlertSeverity.fromString(alert.properties.severity))
                            Polygon(
                                points = polygonPoints,
                                strokeWidth = 5f, // Thick border as requested
                                strokeColor = alertColor,
                                fillColor = alertColor.copy(alpha = 0.2f), // Slightly transparent fill
                                clickable = true, // Make polygon clickable
                                onClick = {
                                    selectedAlertForDialog = alert // Set the clicked alert to show dialog
                                },
                                zIndex = 2f // NEW: Set a higher zIndex for alert polygons
                            )
                        }
                    }
                }
            }

            // MODIFIED: Reflectivity Radar Tile Overlay
            if (showReflectivityRadarOverlay && mapState.radarWfo != null && mapState.latestRadarTimestamp != null) {
                val radarOfficeCodeForTileProvider = mapState.radarWfo!!.lowercase(Locale.US) // Ensure lowercase with Locale
                val reflectivityLayerName = "${radarOfficeCodeForTileProvider}_sr_bref" // Explicit layer name
                Log.d("MainScreen", "Creating Reflectivity RadarTileProvider with officeCode: $radarOfficeCodeForTileProvider, layer: $reflectivityLayerName and timestamp: ${mapState.latestRadarTimestamp}")
                if (radarOfficeCodeForTileProvider.isNotBlank()) {
                    com.google.maps.android.compose.TileOverlay(
                        tileProvider = remember(radarOfficeCodeForTileProvider, mapState.latestRadarTimestamp, reflectivityLayerName) {
                            RadarTileProvider(radarOfficeCodeForTileProvider, okHttpClient, mapState.latestRadarTimestamp, reflectivityLayerName)
                        },
                        fadeIn = true,
                        zIndex = 0f // Set zIndex to 0f for radar overlay
                    )
                }
            }

            // NEW: Velocity Radar Tile Overlay
            if (showVelocityRadarOverlay && mapState.radarWfo != null && mapState.latestRadarTimestamp != null) {
                val radarOfficeCodeForTileProvider = mapState.radarWfo!!.lowercase(Locale.US)
                val velocityLayerName = "${radarOfficeCodeForTileProvider}_sr_bvel" // Explicit layer name for velocity
                Log.d("MainScreen", "Creating Velocity RadarTileProvider with officeCode: $radarOfficeCodeForTileProvider, layer: $velocityLayerName and timestamp: ${mapState.latestRadarTimestamp}")
                if (radarOfficeCodeForTileProvider.isNotBlank()) {
                    com.google.maps.android.compose.TileOverlay(
                        tileProvider = remember(radarOfficeCodeForTileProvider, mapState.latestRadarTimestamp, velocityLayerName) {
                            RadarTileProvider(radarOfficeCodeForTileProvider, okHttpClient, mapState.latestRadarTimestamp, velocityLayerName)
                        },
                        fadeIn = true,
                        zIndex = 1f // Set zIndex to 1f for velocity radar (above reflectivity, below alerts)
                    )
                }
            }
        }

        if (mapState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        // Map Overlay UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Legend FAB (Left) and Map Type Selector (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Legend FAB
                androidx.compose.material3.FloatingActionButton( // Explicitly qualify
                    onClick = { showLegendSheet = true },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    androidx.compose.material3.Icon(Icons.Default.Layers, contentDescription = "Map Legend") // Explicitly qualify
                }
                // Map Type Selector
                MapTypeSelector(
                    currentMapType = mapState.mapProperties.mapType,
                    onMapTypeSelected = { mainViewModel.onMapTypeChanged(it) }
                )
            }

            // Bottom Row: Alerts FAB (Left) and Action Buttons (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Alerts FAB with Badge
                androidx.compose.material3.FloatingActionButton( // Explicitly qualify
                    onClick = { showAlertsSheet = true },
                    containerColor = getAlertsFabColor(mapState.highestSeverity),
                    contentColor = Color.White,
                    modifier = Modifier.padding(bottom = 0.dp) // Adjust padding as needed
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Icon(Icons.Default.Info, contentDescription = "Active Alerts") // Explicitly qualify
                        if (mapState.activeAlerts.size > 0) {
                            Badge(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 10.dp, y = (-10).dp), // Offset badge to top-right of icon
                                containerColor = MaterialTheme.colorScheme.error // Use error color for visibility
                            ) {
                                Text(mapState.activeAlerts.size.toString())
                            }
                        }
                    }
                }

                // Action Buttons are now aligned to the bottom end of the column
                // Pass radar overlay states and toggles
                ActionButtons(
                    navController = navController,
                    radarWfo = mapState.radarWfo,
                    showReflectivityRadarOverlay = showReflectivityRadarOverlay, // MODIFIED: Pass reflectivity state
                    onToggleReflectivityRadarOverlay = { newValue -> // MODIFIED: Handle mutual exclusivity
                        showReflectivityRadarOverlay = newValue
                        if (newValue) showVelocityRadarOverlay = false
                    },
                    showVelocityRadarOverlay = showVelocityRadarOverlay, // NEW: Pass velocity state
                    onToggleVelocityRadarOverlay = { newValue -> // NEW: Handle mutual exclusivity
                        showVelocityRadarOverlay = newValue
                        if (newValue) showReflectivityRadarOverlay = false
                    }
                )
            }
        }
    }
}

/**
 * Helper function to convert a full Report object to a MapReport object.
 * This is needed because `onMarkerClicked` expects a MapReport, but the dialog
 * now provides full Report objects.
 */
private fun Report.toMapReport(): MapReport {
    return MapReport(
        reportId = this.reportId,
        location = this.location,
        direction = this.direction,
        reportType = this.reportType,
        geohash = this.geohash
    )
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

/**
 * Composable for the Floating Action Button displaying active alert count and severity color.
 * This composable is now directly integrated into the MainScreen's layout,
 * and its FloatingActionButton is explicitly qualified there.
 * This function itself is not directly causing the error, but its usage was.
 * Keeping it here for logical separation, but the fix is applied in the caller.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveAlertsFAB(alertCount: Int, highestSeverity: AlertSeverity, onClick: () -> Unit) {
    val fabColor = getAlertsFabColor(highestSeverity)

    // This composable is now directly integrated into the MainScreen's layout,
    // and its FloatingActionButton is explicitly qualified there.
    // This function itself is not directly causing the error, but its usage was.
    // Keeping it here for logical separation, but the fix is applied in the caller.
}

/**
 * Composable for the content of the Alerts Bottom Sheet.
 * Displays a radar map and a list of active NWS alerts.
 */
@Composable
fun AlertsBottomSheetContent(
    activeAlerts: List<DisplayAlert>, // MODIFIED: Now takes DisplayAlert list
    isLoadingAlerts: Boolean,
    radarWfo: String?
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "These are the current Active Alerts for your area.",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Radar Map Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Local Radar",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (isLoadingAlerts) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    } else if (radarWfo != null) {
                        // Construct the radar image URL
                        val radarUrl = "https://radar.weather.gov/ridge/standard/${radarWfo}_0.gif"
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(radarUrl)
                                .addHeader("User-Agent", "SynopticNetwork (rdspromo@gmail.com)") // Required by NWS
                                .crossfade(true)
                                .build(),
                            contentDescription = "Local Radar Map",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.0f) // Changed to 1.0f for a square aspect ratio to maximize vertical space
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit, // Changed to Fit to ensure the whole image is visible
                            error = painterResource(id = R.drawable.ic_splash_logo) // Placeholder for errors
                        )
                    } else {
                        Text(
                            "Radar map not available for your location.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Active Alerts List
        item {
            Text(
                "Active Alerts (${activeAlerts.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            if (isLoadingAlerts && activeAlerts.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (activeAlerts.isEmpty()) {
                Text(
                    "No active alerts for your area at this time.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic
                )
            }
        }

        // Sort alerts by severity (Extreme first)
        // MODIFIED: Sort DisplayAlerts, then access their alert.properties.severity
        val sortedAlerts = activeAlerts.sortedByDescending { AlertSeverity.fromString(it.alert.properties.severity).level }

        items(sortedAlerts) { displayAlert -> // MODIFIED: Iterate over DisplayAlert
            AlertItem(displayAlert = displayAlert) // MODIFIED: Pass DisplayAlert
        }

        // Spacer at the bottom
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

/**
 * Composable for displaying a single NWS alert item.
 * MODIFIED: Now takes DisplayAlert to access isLocal flag.
 */
@Composable
private fun AlertItem(displayAlert: DisplayAlert) {
    val alert = displayAlert.alert // Extract the AlertFeature
    val isLocal = displayAlert.isLocal // Get the local status

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isLocal) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent) // Highlight local alerts
            .border(
                width = if (isLocal) 2.dp else 0.dp, // Add a border for local alerts
                color = if (isLocal) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Headline and Severity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    alert.properties.event ?: "N/A",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = getAlertSeverityColor(AlertSeverity.fromString(alert.properties.severity))
                )
                Text(
                    alert.properties.severity ?: "Unknown",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = getAlertSeverityColor(AlertSeverity.fromString(alert.properties.severity))
                )
            }
            // NEW: Display remaining time until expiration
            alert.properties.expires?.let { expiresTimestamp ->
                val remainingTime = getRemainingTime(expiresTimestamp)
                if (remainingTime != null) {
                    Text(
                        "Expires in: $remainingTime",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(alert.properties.headline ?: "", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(alert.properties.areaDesc ?: "", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)

            Spacer(modifier = Modifier.height(8.dp))

            // Timestamps
            alert.properties.sent?.let {
                Text("Issued: ${formatTimestamp(it)}", style = MaterialTheme.typography.bodySmall)
            }
            alert.properties.expires?.let {
                Text("Expires: ${formatTimestamp(it)}", style = MaterialTheme.typography.bodySmall)
            }

            // Description and Instruction
            if (!alert.properties.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(alert.properties.description, style = MaterialTheme.typography.bodySmall)
            }
            if (!alert.properties.instruction.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Instructions: ${alert.properties.instruction}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Composable for displaying alert details in an AlertDialog when a polygon is clicked.
 */
@Composable
private fun AlertDetailsDialog(alert: AlertFeature, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    alert.properties.event ?: "Alert Details",
                    fontWeight = FontWeight.Bold,
                    color = getAlertSeverityColor(AlertSeverity.fromString(alert.properties.severity))
                )
                Text(
                    "Severity: ${alert.properties.severity ?: "Unknown"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = getAlertSeverityColor(AlertSeverity.fromString(alert.properties.severity))
                )
            }
        },
        text = {
            // Use LazyColumn for scrollable content within the dialog if it gets long
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                alert.properties.headline?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                // NEW: Display remaining time until expiration in dialog
                alert.properties.expires?.let { expiresTimestamp ->
                    val remainingTime = getRemainingTime(expiresTimestamp)
                    if (remainingTime != null) {
                        Text(
                            "Expires in: $remainingTime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
                alert.properties.areaDesc?.let {
                    Text("Affected Area: $it", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                alert.properties.sent?.let {
                    Text("Issued: ${formatTimestamp(it)}", style = MaterialTheme.typography.bodySmall)
                }
                alert.properties.expires?.let {
                    Text("Expires: ${formatTimestamp(it)}", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                alert.properties.description?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                alert.properties.instruction?.let {
                    Text("Instructions: ${it}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

/**
 * Composable for displaying a list of reports within a group.
 * Now displays full Report objects.
 */
@Composable
private fun GroupedReportsDialog(
    reports: List<Report>, // Changed to List<Report> to access full details
    onDismiss: () -> Unit,
    onReportSelected: (Report) -> Unit // Changed to take Report
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reports in this Area (${reports.size})") },
        text = {
            LazyColumn {
                items(reports) { report ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onReportSelected(report) },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Display Report Type and relative time
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(report.reportType, fontWeight = FontWeight.Bold)
                                report.timestamp?.time?.let {
                                    Text(getRelativeTime(it), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            // Display comments if available
                            report.comments?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}


/**
 * Helper function to map AlertSeverity to a Color for display.
 */
@Composable
private fun getAlertSeverityColor(severity: AlertSeverity): Color {
    return when (severity) {
        AlertSeverity.EXTREME -> Color(0xFFD32F2F) // Red
        AlertSeverity.SEVERE -> Color(0xFFF57C00) // Orange
        AlertSeverity.MODERATE -> Color(0xFFFFA000) // Darker Orange/Amber
        AlertSeverity.MINOR -> Color(0xFF1976D2) // Blue
        AlertSeverity.UNKNOWN -> Color.Gray
        AlertSeverity.NONE -> Color.Black // Should not be called for individual alerts
    }
}

/**
 * Helper function to format ISO 8601 timestamps into a readable format.
 */
private fun formatTimestamp(isoTimestamp: String): String {
    return try {
        // Example format: 2025-06-29T11:48:00-04:00
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        val formatter = SimpleDateFormat("MMM dd,yyyy HH:mm z", Locale.US)
        formatter.format(parser.parse(isoTimestamp) ?: Date())
    } catch (e: Exception) {
        e.printStackTrace()
        isoTimestamp // Return original if parsing fails
    }
}

/**
 * Helper function to calculate and format the remaining time until an alert expires.
 * Returns a human-readable string (e.g., "1 hour", "2 days", "5 minutes").
 */
private fun getRemainingTime(isoTimestamp: String): String? {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        val expirationDate = parser.parse(isoTimestamp) ?: return null
        val now = Date()

        val diffMillis = expirationDate.time - now.time

        if (diffMillis <= 0) {
            return "Expired"
        }

        val days = TimeUnit.MILLISECONDS.toDays(diffMillis)
        val hours = TimeUnit.MILLISECONDS.toHours(diffMillis - TimeUnit.DAYS.toMillis(days))
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis - TimeUnit.DAYS.toMillis(days) - TimeUnit.HOURS.toMillis(hours))

        return when {
            days > 0 -> "$days day${if (days > 1) "s" else ""}"
            hours > 0 -> "$hours hour${if (hours > 1) "s" else ""}"
            minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""}"
            else -> "Less than a minute"
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


@Composable
private fun LegendBottomSheetContent(
    filters: Map<String, Boolean>,
    onFilterChanged: (String, Boolean) -> Unit
) {
    // Get all report types with their corresponding emojis
    val reportTypes = getReportTypesWithEmojis()

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            Text("Map Legend & Filters", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 16.dp))
        }
        // Display each report type with its emoji and a toggle switch for filtering
        items(reportTypes) { (name, emoji) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(emoji, fontSize = 24.sp) // Display emoji
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(name, style = MaterialTheme.typography.bodyLarge) // Display report type name
                }
                Switch(
                    checked = filters[name] ?: true, // Check if the filter is currently active for this type
                    onCheckedChange = { onFilterChanged(name, it) } // Update filter state
                )
            }
        }
    }
}

@Composable
private fun ReportBottomSheetContent(
    report: Report,
    comments: List<Comment>,
    isLoadingComments: Boolean,
    onAddCommentClicked: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Report Image
        item {
            AsyncImage(
                model = report.imageUrl,
                contentDescription = "Report Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }
        // Report Details
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(report.reportType, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(getRelativeTime(report.timestamp?.time ?: 0), style = MaterialTheme.typography.bodyMedium)
            }
            Text("WFO: ${report.wfo}  |  Zone: ${report.zone}", style = MaterialTheme.typography.bodyLarge)
            // Display comments if available
            if (!report.comments.isNullOrBlank()) {
                Text(report.comments, style = MaterialTheme.typography.bodyMedium)
            }
        }
        // NWS Status Section
        item { NwsStatusSection(report = report) }
        // Comments Section header and "Add Comment" button
        item {
            Divider() // Visual separator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Comments", style = MaterialTheme.typography.titleLarge)
                Button(onClick = onAddCommentClicked) {
                    Icon(Icons.Default.Add, contentDescription = "Add Comment", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Comment")
                }
            }
        }
        // Comments List
        if (isLoadingComments) {
            // Show a progress indicator while comments are loading
            item { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        } else if (comments.isEmpty()) {
            // Message when no comments are available
            item { Text("No comments yet.", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, modifier = Modifier.padding(vertical = 8.dp)) }
        } else {
            // Display each comment
            items(comments) { comment -> CommentItem(comment = comment) }
        }
        // Spacer at the bottom
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun NwsStatusSection(report: Report) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (report.sendToNws) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Send, contentDescription = "Sent to NWS", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("This report was sent to the NWS.")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (report.nwsAcknowledged) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Acknowledged", tint = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Acknowledged by the NWS.", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.HourglassTop, contentDescription = "Pending Acknowledgement", tint = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pending NWS acknowledgement.", color = Color.Gray, fontStyle = FontStyle.Italic)
                }
            }
        }
    }
}

@Composable
private fun CommentItem(comment: Comment) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(comment.screenName, fontWeight = FontWeight.Bold)
                Text(text = getRelativeTime(comment.timestamp?.time ?: 0), style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(comment.text)
        }
    }
}

@Composable
private fun AddCommentDialog(onDismiss: () -> Unit, onPostComment: (String) -> Unit) {
    var commentText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a Comment") },
        text = { OutlinedTextField(value = commentText, onValueChange = { commentText = it }, label = { Text("Your comment") }, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(onClick = { onPostComment(commentText) }, enabled = commentText.isNotBlank()) { Text("Post") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}


private fun getRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diffMillis = now - timestamp
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
    val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)
    val days = TimeUnit.MILLISECONDS.toDays(diffMillis)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes minute${if (minutes > 1) "s" else ""}"
        hours < 24 -> "$hours hour${if (hours > 1) "s" else ""}"
        else -> "$days day${if (days > 1) "s" else ""}"
    }
}


@Composable
private fun MapTypeSelector(currentMapType: MapType, onMapTypeSelected: (MapType) -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Transparent_Black)) {
        Row {
            IconButton(onClick = { onMapTypeSelected(MapType.NORMAL) }) {
                androidx.compose.material3.Icon(Icons.Default.Streetview, "Street View", tint = if (currentMapType == MapType.NORMAL) MaterialTheme.colorScheme.primary else Color.White)
            }
            IconButton(onClick = { onMapTypeSelected(MapType.SATELLITE) }) {
                androidx.compose.material3.Icon(Icons.Default.Satellite, "Satellite View", tint = if (currentMapType == MapType.SATELLITE) MaterialTheme.colorScheme.primary else Color.White)
            }
        }
    }
}

@Composable
private fun ActionButtons(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    radarWfo: String?,
    showReflectivityRadarOverlay: Boolean, // MODIFIED: Reflectivity state
    onToggleReflectivityRadarOverlay: (Boolean) -> Unit, // MODIFIED: Reflectivity toggle
    showVelocityRadarOverlay: Boolean, // NEW: Velocity state
    onToggleVelocityRadarOverlay: (Boolean) -> Unit // NEW: Velocity toggle
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
        // MODIFIED: FAB for Reflectivity Radar Toggle
        androidx.compose.material3.FloatingActionButton(
            onClick = {
                val newState = !showReflectivityRadarOverlay
                onToggleReflectivityRadarOverlay(newState)
                // If turning reflectivity ON, turn velocity OFF
                if (newState) {
                    onToggleVelocityRadarOverlay(false)
                }
            },
            containerColor = if (showReflectivityRadarOverlay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            contentColor = Color.White
        ) {
            androidx.compose.material3.Icon(Icons.Default.Satellite, "Toggle Reflectivity Radar")
        }

        // NEW: FAB for Velocity Radar Toggle
        androidx.compose.material3.FloatingActionButton(
            onClick = {
                val newState = !showVelocityRadarOverlay
                onToggleVelocityRadarOverlay(newState)
                // If turning velocity ON, turn reflectivity OFF
                if (newState) {
                    onToggleReflectivityRadarOverlay(false)
                }
            },
            containerColor = if (showVelocityRadarOverlay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            contentColor = Color.White
        ) {
            androidx.compose.material3.Icon(Icons.Default.Streetview, "Toggle Velocity Radar")
        }

        // FAB for Weather Products
        androidx.compose.material3.FloatingActionButton(
            onClick = productFabOnClick, // Use the conditional onClick
            containerColor = productFabContainerColor, // Use the conditional color
            contentColor = Color.White
        ) {
            androidx.compose.material3.Icon(Icons.Default.Description, "Weather Products")
        }
        // FAB for User Settings (always enabled)
        androidx.compose.material3.FloatingActionButton(
            onClick = { navController.navigate(Screen.Settings.route) },
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = Color.White
        ) {
            androidx.compose.material3.Icon(Icons.Default.Settings, "User Settings")
        }
        // FAB for Make Report (always enabled)
        androidx.compose.material3.FloatingActionButton(
            onClick = { navController.navigate(Screen.MakeReport.route) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            androidx.compose.material3.Icon(Icons.Default.AddAPhoto, "Make Report")
        }
    }
}

/**
 * Helper function to create and cache custom marker icons for the map.
 * Each icon combines a base weather pin with an emoji representing the report type,
 * and is rotated to show the direction the photo was taken.
 */
class MarkerIconFactory(private val context: Context) {
    private val iconCache = mutableMapOf<String, BitmapDescriptor>()

    /**
     * Creates a custom marker icon.
     * The icon is a combination of a base weather pin, rotated according to the report's direction,
     * with an emoji representing the report type drawn on top.
     * Icons are cached to improve performance for repeated requests.
     *
     * @param report The MapReport containing details for icon creation (report type, direction).
     * @return A BitmapDescriptor ready to be used as a marker icon, or null if creation fails.
     */
    fun createMarkerIcon(report: MapReport): BitmapDescriptor? {
        // Create a unique cache key based on report type and direction for efficient caching.
        // We only care about the integer part of the direction for caching, as small float
        // differences shouldn't generate new bitmaps.
        val cacheKey = "${report.reportType}_${report.direction.toInt()}"

        // Check if the icon is already in the cache
        if (iconCache.containsKey(cacheKey)) {
            return iconCache[cacheKey]
        }

        // 1. Load the base weather pin drawable and convert it to a mutable bitmap.
        // The size (120x120) is chosen to provide enough space for the emoji.
        val baseBitmap = ContextCompat.getDrawable(context, R.drawable.weatherpin)?.let {
            val bmp = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            it.setBounds(0, 0, canvas.width, it.intrinsicHeight) // Use intrinsic height for correct scaling
            it.draw(canvas)
            bmp
        } ?: return null // Return null if the base drawable cannot be loaded

        // 2. Rotate the base bitmap according to the report's direction.
        val rotatedBitmap = baseBitmap.rotate(report.direction)

        // 3. Get the emoji corresponding to the report type.
        // Falls back to a question mark emoji if the type is not found.
        val emoji = getReportTypesWithEmojis().find { it.first == report.reportType }?.second ?: "❓"

        // 4. Create a new mutable bitmap to draw the emoji on the rotated pin.
        val finalBitmap = rotatedBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(finalBitmap)
        val paint = Paint().apply {
            textSize = 60f // Set emoji size
            color = GraphicsColor.BLACK // Use GraphicsColor.BLACK
            textAlign = Paint.Align.CENTER // Center the text horizontally
        }

        // Calculate the center of the canvas
        val centerX = canvas.width / 2f
        val centerY = canvas.height / 2f

        // Calculate the opposite angle in radians
        val oppositeAngleInDegrees = (report.direction + 180) % 360
        val oppositeAngleInRadians = Math.toRadians(oppositeAngleInDegrees.toDouble()).toFloat()

        // Calculate the x and y offsets based on the opposite direction and 20dp distance
        // In Android Canvas: positive X is right, positive Y is down.
        // dx = distance * sin(angle)
        // dy = -distance * cos(angle)
        val offsetDistance = 20f // 20 dp equivalent in pixels for drawing
        val offsetX = (offsetDistance * Math.sin(oppositeAngleInRadians.toDouble())).toFloat()
        val offsetY = (-offsetDistance * Math.cos(oppositeAngleInRadians.toDouble())).toFloat()

        // Calculate the final emoji drawing position, adjusting for text baseline
        val finalEmojiX = centerX + offsetX
        // The `paint.descent() + paint.ascent()) / 2f` centers the text vertically around the given Y.
        // So, we add the calculated offsetY to the centerY, and then apply the text baseline correction.
        val finalEmojiY = centerY + offsetY - ((paint.descent() + paint.ascent()) / 2f)

        canvas.drawText(emoji, finalEmojiX, finalEmojiY, paint)

        // 5. Convert the final bitmap into a BitmapDescriptor for Google Maps.
        val descriptor = BitmapDescriptorFactory.fromBitmap(finalBitmap)

        // Cache the newly created descriptor
        iconCache[cacheKey] = descriptor
        return descriptor
    }

    /**
     * Creates a custom marker icon for a group of reports, displaying the count.
     *
     * @param count The number of reports in the group.
     * @return A BitmapDescriptor for the group marker.
     */
    fun createGroupMarkerIcon(count: Int): BitmapDescriptor? {
        val cacheKey = "group_$count"
        if (iconCache.containsKey(cacheKey)) {
            return iconCache[cacheKey]
        }

        // Define bitmap size for group marker (larger than individual for visibility)
        val size = 150 // pixels
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw a solid circle background
        val circlePaint = Paint().apply {
            color = GraphicsColor.BLUE // Group marker color
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, circlePaint)

        // Draw the count text
        val textPaint = Paint().apply {
            color = GraphicsColor.WHITE
            textSize = 70f // Adjust text size
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        // Center the text vertically
        val xPos = canvas.width / 2f
        val yPos = (canvas.height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(count.toString(), xPos, yPos, textPaint)

        val descriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
        iconCache[cacheKey] = descriptor
        return descriptor
    }

    /**
     * Creates a small blue circle icon for the center of a spread group.
     */
    fun createSpreadCenterIcon(): BitmapDescriptor? {
        val cacheKey = "spread_center"
        if (iconCache.containsKey(cacheKey)) {
            return iconCache[cacheKey]
        }

        val size = 40 // pixels (smaller size for the center marker)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint().apply {
            color = GraphicsColor.BLUE // Changed color to blue
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        val descriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
        iconCache[cacheKey] = descriptor
        return descriptor
    }


    /**
     * Rotates a given Bitmap by a specified number of degrees.
     *
     * @param degrees The rotation angle in degrees.
     * @return A new Bitmap rotated by the specified degrees.
     */
    private fun Bitmap.rotate(degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }
}

/**
 * Preview for the Main Screen.
 * Provides a placeholder for the Google Map and shows the overlay UI elements.
 */
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    SynopticNetworkTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Text("Google Map Placeholder", modifier = Modifier.align(Alignment.Center))
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MapTypeSelector(currentMapType = MapType.NORMAL, onMapTypeSelected = {})
                // Provide dummy values for preview
                ActionButtons(navController = rememberNavController(), radarWfo = "KTBW", showReflectivityRadarOverlay = false, onToggleReflectivityRadarOverlay = {}, showVelocityRadarOverlay = false, onToggleVelocityRadarOverlay = {})
            }
        }
    }
}
