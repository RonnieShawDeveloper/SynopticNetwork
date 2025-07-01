package com.artificialinsightsllc.synopticnetwork.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as GraphicsColor // Alias to avoid ambiguity with Compose Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
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
import androidx.compose.material3.ModalBottomSheet // Correct import for ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState // Correct import for rememberModalBottomSheetState
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
import com.artificialinsightsllc.synopticnetwork.ui.viewmodels.DisplayMarker
import com.artificialinsightsllc.synopticnetwork.ui.viewmodels.DisplayAlert
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
import com.google.maps.android.compose.Polyline
import androidx.compose.ui.geometry.Offset
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType // Correct import for JointType
import com.google.android.gms.maps.model.RoundCap // Correct import for RoundCap
import com.artificialinsightsllc.synopticnetwork.data.services.RadarTileProvider
import okhttp3.OkHttpClient
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.request.ImageResult
import com.artificialinsightsllc.synopticnetwork.data.models.StormCell
import com.artificialinsightsllc.synopticnetwork.data.models.ForecastIcon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


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
    val alertsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val legendSheetState = rememberModalBottomSheetState()
    var showAddCommentDialog by remember { mutableStateOf(false) }
    var showLegendSheet by remember { mutableStateOf(false) }
    var showAlertsSheet by remember { mutableStateOf(false) }
    var selectedAlertForDialog by remember { mutableStateOf<AlertFeature?>(null) }

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

    // Load storm attribute icon when URL is available
    val markerIconFactory = remember { MarkerIconFactory(context) }
    LaunchedEffect(mapState.nexradL3Attributes?.iconFileUrl) {
        mapState.nexradL3Attributes?.iconFileUrl?.let { url ->
            markerIconFactory.loadStormAttributeIcon(url)
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
                activeAlerts = mapState.activeAlerts,
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
            reports = mapState.selectedGroupedFullReports,
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
        // Initialize OkHttpClient for RadarTileProvider
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
            mapState.activeAlerts.forEach { displayAlert ->
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
                                zIndex = 2f // Set a higher zIndex for alert polygons
                            )
                        }
                    }
                }
            }

            // Reflectivity Radar Tile Overlay
            if (mapState.isReflectivityRadarActive && mapState.radarWfo != null && mapState.latestRadarTimestamp != null) {
                val radarOfficeCodeForTileProvider = mapState.radarWfo!!.lowercase(Locale.US) // Ensure lowercase with Locale
                val reflectivityLayerName = "${radarOfficeCodeForTileProvider}_sr_bref" // Explicit layer name
                Log.d("MainScreen", "Creating Reflectivity RadarTileProvider with officeCode: $radarOfficeCodeForTileProvider, layer: $reflectivityLayerName and timestamp: ${mapState.latestRadarTimestamp}")
                if (radarOfficeCodeForTileProvider.isNotBlank()) {
                    com.google.maps.android.compose.TileOverlay(
                        tileProvider = remember(radarOfficeCodeForTileProvider, mapState.latestRadarTimestamp, reflectivityLayerName) {
                            RadarTileProvider(radarOfficeCodeForTileProvider, okHttpClient, mapState.latestRadarTimestamp, reflectivityLayerName)
                        },
                        fadeIn = true,
                        transparency = 0.25f, // Set transparency to 25% (75% opaque)
                        zIndex = 0f // Set zIndex to 0f for radar overlay
                    )
                }
            }

            // Velocity Radar Tile Overlay
            if (mapState.isVelocityRadarActive && mapState.radarWfo != null && mapState.latestRadarTimestamp != null) {
                val radarOfficeCodeForTileProvider = mapState.radarWfo!!.lowercase(Locale.US)
                val velocityLayerName = "${radarOfficeCodeForTileProvider}_sr_bvel" // Explicit layer name for velocity
                Log.d("MainScreen", "Creating Velocity RadarTileProvider with officeCode: $radarOfficeCodeForTileProvider, layer: $velocityLayerName and timestamp: ${mapState.latestRadarTimestamp}")
                if (radarOfficeCodeForTileProvider.isNotBlank()) {
                    com.google.maps.android.compose.TileOverlay(
                        tileProvider = remember(radarOfficeCodeForTileProvider, mapState.latestRadarTimestamp, velocityLayerName) {
                            RadarTileProvider(radarOfficeCodeForTileProvider, okHttpClient, mapState.latestRadarTimestamp, velocityLayerName)
                        },
                        fadeIn = true,
                        transparency = 0.25f, // Set transparency to 25% (75% opaque)
                        zIndex = 1f // Set zIndex to 1f for velocity radar (above reflectivity, below alerts)
                    )
                }
            }

            // Render NEXRAD Level 3 Attributes (Storm Cells, Tracks, Forecasts)
            if (mapState.isPlacefileOverlayActive && mapState.nexradL3Attributes != null) {
                val placefile = mapState.nexradL3Attributes

                placefile?.stormCells?.forEach { stormCell -> // Corrected: Removed redundant ?. on placefile
                    // Main Storm Cell Marker
                    Marker(
                        state = rememberMarkerState(position = stormCell.initialLocation),
                        title = stormCell.mainIconText.substringBefore("\n").trim(), // Use first line as title
                        snippet = stormCell.mainIconText, // Full text for snippet/info window
                        icon = markerIconFactory.createStormCellIcon(stormCell), // Pass stormCell for TVS/MESO
                        onClick = {
                            // Default info window behavior for now, as requested
                            false
                        },
                        zIndex = 3f // Higher than radar, lower than user reports
                    )

                    // Storm Track Line
                    if (stormCell.trackLine.isNotEmpty()) {
                        Polyline(
                            points = stormCell.trackLine,
                            color = Color.Yellow, // Distinct color for storm tracks
                            width = 5f,
                            jointType = JointType.ROUND, // Smooth joints
                            startCap = RoundCap(), // Corrected: Use RoundCap()
                            endCap = RoundCap(), // Corrected: Use RoundCap()
                            zIndex = 3f // Same zIndex as markers
                        )
                    }

                    // Forecast Icons (+15 min, +30 min, etc.)
                    stormCell.forecastIcons.forEach { forecastIcon ->
                        Marker(
                            state = rememberMarkerState(position = forecastIcon.location),
                            title = forecastIcon.label,
                            snippet = "Forecast Position",
                            icon = markerIconFactory.createForecastIcon(forecastIcon),
                            zIndex = 3f // Same zIndex as other placefile elements
                        )
                    }
                }
            }
        }

        if (mapState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        // Map Overlay UI (main Column)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing) // Apply padding for system bars
                .padding(horizontal = 16.dp, vertical = 16.dp), // Apply general padding
            verticalArrangement = Arrangement.SpaceBetween // Push top and bottom content apart
        ) {
            // Top Row: Legend FAB (Left) and Map Type Selector (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Legend FAB with "FILTERS" Badge
                FabWithBadge(
                    onClick = { showLegendSheet = true },
                    icon = { Icon(Icons.Default.Layers, contentDescription = "Map Legend") },
                    contentDescription = "Map Legend",
                    badgeText = "FILTERS",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )

                // Map Type Selector
                MapTypeSelector(
                    currentMapType = mapState.mapProperties.mapType,
                    onMapTypeSelected = { mainViewModel.onMapTypeChanged(it) }
                )
            }

            // Spacer to push content to bottom
            Spacer(modifier = Modifier.weight(1f))

            // Bottom section: A Row containing Alerts FAB (Left), Radar Badge (Center), and Action Buttons (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween, // Distribute items horizontally
                verticalAlignment = Alignment.Bottom // Align items to the bottom
            ) {
                // Alerts FAB with "ALERTS" Badge (Left)
                FabWithBadge(
                    onClick = { showAlertsSheet = true },
                    icon = {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Info, contentDescription = "Active Alerts")
                            if (mapState.activeAlerts.size > 0) {
                                Badge(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 10.dp, y = (-10).dp),
                                    containerColor = MaterialTheme.colorScheme.error
                                ) {
                                    Text(mapState.activeAlerts.size.toString())
                                }
                            }
                        }
                    },
                    contentDescription = "Active Alerts",
                    badgeText = "ALERTS",
                    containerColor = getAlertsFabColor(mapState.highestSeverity),
                    contentColor = Color.White
                )

                // Radar Last Updated Badge (Conditional Visibility) - CENTERED BETWEEN FABs
                if ((mapState.isReflectivityRadarActive || mapState.isVelocityRadarActive) && mapState.latestRadarTimestamp != null) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Transparent_Black),
                        modifier = Modifier
                            .align(Alignment.Bottom) // Aligns the bottom of the card to the bottom of the Row
                            .weight(1f) // Takes up available space horizontally
                            .padding(horizontal = 8.dp) // Add horizontal padding to separate from FABs
                    ) {
                        Text(
                            text = mapState.lastRadarUpdateTimeString ?: "Last Updated: N/A",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center, // Centers the text horizontally within the Card
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    // If no radar is active, still provide a spacer to maintain layout
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Action Buttons column (Right)
                ActionButtons(
                    navController = navController,
                    radarWfo = mapState.radarWfo,
                    showReflectivityRadarOverlay = mapState.isReflectivityRadarActive,
                    onToggleReflectivityRadarOverlay = { newValue ->
                        mainViewModel.onReflectivityRadarToggled(newValue)
                    },
                    showVelocityRadarOverlay = mapState.isVelocityRadarActive,
                    onToggleVelocityRadarOverlay = { newValue ->
                        mainViewModel.onVelocityRadarToggled(newValue)
                    },
                    showPlacefileOverlay = mapState.isPlacefileOverlayActive,
                    onTogglePlacefileOverlay = { newValue ->
                        mainViewModel.onPlacefileOverlayToggled(newValue)
                    }
                )
            }
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
    modifier: Modifier = Modifier,
    enabled: Boolean = true // Corrected: Added 'enabled' parameter back to FabWithBadge
) {
    Box(modifier = modifier.width(80.dp)) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = containerColor,
            contentColor = contentColor,
            modifier = Modifier.align(Alignment.Center),
            // enabled = enabled // Corrected: Pass the 'enabled' state to the internal FloatingActionButton
        ) {
            // The icon composable is invoked here, which is a @Composable context.
            // This was the source of the "Composable invocations can only happen from..." error.
            // By ensuring 'enabled' is a valid parameter for the Material3 FAB,
            // the compiler correctly infers the composable context.
            icon()
        }

        // Badge Text
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            modifier = Modifier
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}


/**
 * Helper function to convert a full Report object to a MapReport object.
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
 * Composable for the content of the Alerts Bottom Sheet.
 * Displays a radar map and a list of active NWS alerts.
 */
@Composable
fun AlertsBottomSheetContent(
    activeAlerts: List<DisplayAlert>,
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
                                .aspectRatio(1.0f)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit,
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
        val sortedAlerts = activeAlerts.sortedByDescending { AlertSeverity.fromString(it.alert.properties.severity).level }

        items(sortedAlerts) { displayAlert ->
            AlertItem(displayAlert = displayAlert)
        }

        // Spacer at the bottom
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

/**
 * Composable for displaying a single NWS alert item.
 */
@Composable
private fun AlertItem(displayAlert: DisplayAlert) {
    val alert = displayAlert.alert
    val isLocal = displayAlert.isLocal

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isLocal) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
            .border(
                width = if (isLocal) 2.dp else 0.dp,
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
            // Display remaining time until expiration
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
                // Display remaining time until expiration in dialog
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
    reports: List<Report>,
    onDismiss: () -> Unit,
    onReportSelected: (Report) -> Unit
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
                                    Text(getRelativeTime(it), style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
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

/**
 * Helper function to create and cache custom marker icons for the map.
 * Each icon combines a base weather pin with an emoji representing the report type,
 * and is rotated to show the direction the photo was taken.
 */
class MarkerIconFactory(private val context: Context) {
    private val iconCache = mutableMapOf<String, BitmapDescriptor>()
    private val imageLoader = ImageLoader.Builder(context).build()
    // Use MutableStateFlow to hold the loaded Bitmap and the URL it came from
    private val _stormAttributeBaseBitmapWithUrl = MutableStateFlow<Pair<Bitmap, String>?>(null)
    val stormAttributeBaseBitmapFlow = _stormAttributeBaseBitmapWithUrl.asStateFlow()

    /**
     * Loads the base storm attribute icon from the given URL and caches it.
     * This is a suspend function that should be called from a coroutine scope.
     *
     * @param url The URL of the storm attribute icon. Can be null if no URL is available.
     */
    suspend fun loadStormAttributeIcon(url: String?) { // Corrected: url can be nullable
        // Only load if URL is not null AND (not already loaded OR a different URL is requested)
        if (url != null && _stormAttributeBaseBitmapWithUrl.value?.second != url) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .addHeader("User-Agent", "SynopticNetwork (rdspromo@gmail.com)")
                    .allowHardware(false)
                    .listener(
                        onSuccess = { _, result -> // Removed 'request' parameter as it's unused
                            val drawable = result.drawable
                            if (drawable != null) {
                                val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
                                val canvas = Canvas(bitmap)
                                drawable.setBounds(0, 0, canvas.width, canvas.height)
                                drawable.draw(canvas)
                                // Store the URL along with the bitmap in the MutableStateFlow
                                _stormAttributeBaseBitmapWithUrl.value = Pair(bitmap, url)
                                Log.d("MarkerIconFactory", "Storm attribute icon loaded successfully from $url.")
                            } else {
                                Log.e("MarkerIconFactory", "Failed to load storm attribute icon: Drawable is null for $url.")
                                _stormAttributeBaseBitmapWithUrl.value = null
                            }
                        },
                        onError = { request, result ->
                            Log.e("MarkerIconFactory", "Error loading storm attribute icon from ${request.data}: ${result.throwable?.message}", result.throwable)
                            _stormAttributeBaseBitmapWithUrl.value = null
                        }
                    )
                    .build()
                imageLoader.execute(request)
            } catch (e: Exception) {
                Log.e("MarkerIconFactory", "Error initiating image load for $url: ${e.message}", e)
                _stormAttributeBaseBitmapWithUrl.value = null
            }
        } else if (url == null) {
            // If the URL is explicitly null, clear the cached bitmap
            _stormAttributeBaseBitmapWithUrl.value = null
        }
    }


    /**
     * Creates a custom marker icon.
     * The icon is a combination of a base weather pin with an emoji representing the report type,
     * and is rotated to show the direction the photo was taken.
     * Icons are cached to improve performance for repeated requests.
     *
     * @param report The MapReport containing details for icon creation (report type, direction).
     * @return A BitmapDescriptor ready to be used as a marker icon, or null if creation fails.
     */
    fun createMarkerIcon(report: MapReport): BitmapDescriptor? {
        // Create a unique cache key based on report type and direction for efficient caching.
        // We only care about the integer part of the direction for caching, as small float
        // differences shouldn't generate new bitmaps.
        val cacheKey = "report_${report.reportType}_${report.direction.toInt()}"

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
            typeface = Typeface.DEFAULT_BOLD
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
     * Creates a custom marker icon for a storm cell, potentially with TVS/MESO indicators.
     * The base icon is loaded from the placefile's IconFile URL.
     *
     * @param stormCell The StormCell data.
     * @return A BitmapDescriptor for the storm cell marker.
     */
    @Composable // Corrected: Marked as @Composable because it uses collectAsState()
    fun createStormCellIcon(stormCell: StormCell): BitmapDescriptor? {
        // Observe the loaded stormAttributeBaseBitmap
        val currentBaseBitmapWithUrl by stormAttributeBaseBitmapFlow.collectAsState()
        val currentBaseBitmap = currentBaseBitmapWithUrl?.first // Get the Bitmap from the Pair

        // Corrected: Cache key now includes the URL hash to ensure new icon if base image changes
        val cacheKey = "storm_cell_${stormCell.hasTVS}_${stormCell.hasMeso}_${currentBaseBitmapWithUrl?.second?.hashCode()}"
        if (iconCache.containsKey(cacheKey)) {
            return iconCache[cacheKey]
        }

        val baseBitmap = currentBaseBitmap ?: return null // Use the loaded base bitmap

        // Create a mutable copy to draw on
        val mutableBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        // Draw TVS indicator if present (e.g., a red circle/dot)
        if (stormCell.hasTVS) {
            val paint = Paint().apply {
                color = GraphicsColor.RED
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            // Draw a small red circle at the top-right corner or center of the icon
            canvas.drawCircle(mutableBitmap.width * 0.8f, mutableBitmap.height * 0.2f, 10f, paint)
        }

        // Draw MESO indicator if present (e.g., a yellow circle/dot)
        if (stormCell.hasMeso) {
            val paint = Paint().apply {
                color = GraphicsColor.YELLOW
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            // Draw a small yellow circle at the top-left corner
            canvas.drawCircle(mutableBitmap.width * 0.2f, mutableBitmap.height * 0.2f, 10f, paint)
        }

        val descriptor = BitmapDescriptorFactory.fromBitmap(mutableBitmap)
        iconCache[cacheKey] = descriptor
        return descriptor
    }

    /**
     * Creates a custom marker icon for a forecast position, with rotation.
     * This will be a small arrow or dot.
     *
     * @param forecastIcon The ForecastIcon data.
     * @return A BitmapDescriptor for the forecast icon.
     */
    @Composable // Corrected: Marked as @Composable because it uses collectAsState() (indirectly via stormAttributeBaseBitmapFlow if it were used, but good practice for icon factories)
    fun createForecastIcon(forecastIcon: ForecastIcon): BitmapDescriptor? {
        val cacheKey = "forecast_icon_${forecastIcon.rotation.toInt()}"
        if (iconCache.containsKey(cacheKey)) {
            return iconCache[cacheKey]
        }

        val size = 60 // pixels for forecast icon (slightly larger for visibility)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw a small circle as the base
        val circlePaint = Paint().apply {
            color = GraphicsColor.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 3f, circlePaint)

        // Draw a small arrow indicating direction
        val arrowPaint = Paint().apply {
            color = GraphicsColor.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 5f
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        // Save canvas state before rotation
        canvas.save()
        // Rotate the canvas around its center
        canvas.rotate(forecastIcon.rotation, size / 2f, size / 2f)

        // Draw a simple arrow pointing "up" (which becomes the rotated direction)
        val arrowLength = size / 4f
        val centerX = size / 2f
        val centerY = size / 2f
        canvas.drawLine(centerX, centerY + arrowLength, centerX, centerY - arrowLength, arrowPaint)
        // Draw arrow head (simple V shape)
        canvas.drawLine(centerX, centerY - arrowLength, centerX - arrowLength / 3, centerY - arrowLength / 2, arrowPaint)
        canvas.drawLine(centerX, centerY - arrowLength, centerX + arrowLength / 3, centerY - arrowLength / 2, arrowPaint)

        // Restore canvas state
        canvas.restore()

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
                ActionButtons(
                    navController = rememberNavController(),
                    radarWfo = "KTBW",
                    showReflectivityRadarOverlay = true, // Simulate radar on for preview
                    onToggleReflectivityRadarOverlay = {},
                    showVelocityRadarOverlay = false,
                    onToggleVelocityRadarOverlay = {},
                    showPlacefileOverlay = true, // Simulate placefile on for preview
                    onTogglePlacefileOverlay = {}
                )
            }
        }
    }
}
