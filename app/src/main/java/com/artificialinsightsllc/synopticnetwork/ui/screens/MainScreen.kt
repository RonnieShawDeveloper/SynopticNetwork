package com.artificialinsightsllc.synopticnetwork.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as GraphicsColor // Alias to avoid ambiguity with Compose Color
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import com.artificialinsightsllc.synopticnetwork.data.models.ReportClusterItem // Import ReportClusterItem
import com.artificialinsightsllc.synopticnetwork.navigation.Screen
import com.artificialinsightsllc.synopticnetwork.ui.theme.SynopticNetworkTheme
import com.artificialinsightsllc.synopticnetwork.ui.theme.Transparent_Black
import com.artificialinsightsllc.synopticnetwork.ui.viewmodels.MainViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect // Keep MapEffect for initial map access
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
    val mapState by mainViewModel.mapState.collectAsState()
    val scope = rememberCoroutineScope()
    val reportSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val alertsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false) // Allow partial expansion
    val legendSheetState = rememberModalBottomSheetState()
    var showAddCommentDialog by remember { mutableStateOf(false) }
    var showLegendSheet by remember { mutableStateOf(false) }
    var showAlertsSheet by remember { mutableStateOf(false) } // State to control alerts bottom sheet visibility
    var selectedAlertForDialog by remember { mutableStateOf<AlertFeature?>(null) } // New state for dialog alert

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

    LaunchedEffect(mapState.currentLocation) {
        mapState.currentLocation?.let {
            cameraPositionState.animate(CameraUpdateFactory.newCameraPosition(CameraPosition(it, 10f, 0f, 0f)), 1000)
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

        // Use remember to hold the ClusterManager and its Renderer
        // These will be initialized once and retained across recompositions.
        // The ClusterManager itself is tightly coupled with the GoogleMap instance.
        var clusterManagerInstance: ClusterManager<ReportClusterItem>? by remember { mutableStateOf(null) }

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapState.mapProperties,
            uiSettings = uiSettings
        ) {
            // MapEffect is for side-effects that need access to the GoogleMap object.
            // It runs once when the map becomes available and re-runs if `mapState.reports` changes.
            // Also, `googleMap` is the actual GoogleMap object instance.
            MapEffect(mapState.reports) { googleMap ->
                if (clusterManagerInstance == null) {
                    // Initialize ClusterManager and Renderer only once per GoogleMap instance
                    val newClusterManager = ClusterManager<ReportClusterItem>(context, googleMap)
                    val newRenderer = ReportMarkerRenderer(context, googleMap, newClusterManager, markerIconFactory)
                    newClusterManager.renderer = newRenderer

                    // Set listeners for cluster item clicks
                    newClusterManager.setOnClusterItemClickListener { item ->
                        mainViewModel.onMarkerClicked(item.report)
                        scope.launch { reportSheetState.show() }
                        true
                    }

                    // Set listener for cluster clicks (e.g., zoom in)
                    newClusterManager.setOnClusterClickListener { cluster ->
                        val position = cameraPositionState.position
                        val newZoom = position.zoom + 2 // Zoom in by 2 levels
                        val newCameraPosition = CameraPosition.fromLatLngZoom(cluster.position, newZoom)
                        scope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.newCameraPosition(newCameraPosition), 500)
                        }
                        true
                    }

                    // Delegate map listeners to the cluster manager
                    googleMap.setOnCameraIdleListener(newClusterManager)
                    googleMap.setOnMarkerClickListener(newClusterManager)

                    // Store instance in remembered state
                    clusterManagerInstance = newClusterManager
                }

                // Update cluster items whenever mapState.reports changes
                clusterManagerInstance?.clearItems()
                clusterManagerInstance?.addItems(mapState.reports)
                clusterManagerInstance?.cluster() // Trigger clustering immediately after updating items
            }

            // DisposableEffect for cleaning up ClusterManager resources when this composable leaves composition.
            // This is correctly placed inside the @Composable scope of MainScreen.
            // The key is `clusterManagerInstance` to ensure dispose/re-setup if the instance itself changes (e.g., on re-composition)
            DisposableEffect(clusterManagerInstance) {
                onDispose {
                    // Access the current ClusterManager instance captured in this DisposableEffect's scope.
                    clusterManagerInstance?.let { cm ->
                        // Detach listeners from the GoogleMap directly using the map instance that was used to initialize the CM.
                        // The ClusterManager's internal GoogleMap reference should be the same.
                        // Note: .map is not a public property, so we are removing the listener directly from the GoogleMap instance.
                        //cm.map?.setOnCameraIdleListener(null) // This line requires access to cm.map
                        // cm.map?.setOnMarkerClickListener(null) // This line requires access to cm.map

                        // Clear items. For setMap(null) and setRenderer(null), these are internal to ClusterManager
                        // and not part of its public cleanup API. Clearing items and detaching listeners is sufficient.
                        cm.clearItems()
                        // The following lines are not part of ClusterManager's public API for direct control:
                        // cm.setMap(null)
                        // cm.setRenderer(null)
                    }
                }
            }


            // Draw NWS Alert Polygons
            mapState.activeAlerts.forEach { alert ->
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
                                }
                            )
                        }
                    }
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
                FloatingActionButton(
                    onClick = { showLegendSheet = true },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Icon(Icons.Default.Layers, contentDescription = "Map Legend")
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
                ActiveAlertsFAB(
                    alertCount = mapState.activeAlerts.size,
                    highestSeverity = mapState.highestSeverity,
                    onClick = { showAlertsSheet = true }
                )

                // Action Buttons are now aligned to the bottom end of the column
                ActionButtons(navController = navController)
            }
        }
    }
}

/**
 * Custom Cluster Renderer for ReportClusterItem.
 * This class is responsible for drawing individual markers and clusters on the map,
 * utilizing the existing MarkerIconFactory for individual report icons.
 */
class ReportMarkerRenderer(
    context: Context,
    map: GoogleMap,
    clusterManager: ClusterManager<ReportClusterItem>,
    private val markerIconFactory: MarkerIconFactory
) : DefaultClusterRenderer<ReportClusterItem>(context, map, clusterManager) {

    // This method is called to render an individual ClusterItem (ReportClusterItem in our case).
    override fun onBeforeClusterItemRendered(item: ReportClusterItem, markerOptions: MarkerOptions) {
        // Use our MarkerIconFactory to create the custom icon for the individual report
        markerIconFactory.createMarkerIcon(item.report)?.let { icon ->
            markerOptions.icon(icon)
        }
        // Set anchor to center-bottom of the icon if the pin is at the bottom center
        markerOptions.anchor(0.5f, 1.0f)
        markerOptions.title(item.title)
        markerOptions.snippet(item.snippet)
    }

    // This method is called to render a Cluster (a group of markers).
    override fun onBeforeClusterRendered(cluster: Cluster<ReportClusterItem>, markerOptions: MarkerOptions) {
        // Here you can customize the appearance of cluster markers (e.g., a circle with the count).
        // For simplicity, we'll use the default cluster icon which is a tinted circle with the count.
        // You might want to create a custom drawable for clusters too, similar to MarkerIconFactory.
        super.onBeforeClusterRendered(cluster, markerOptions)
    }

    // This method is called after the marker is added to the map.
    override fun onClusterItemRendered(clusterItem: ReportClusterItem, marker: Marker) {
        super.onClusterItemRendered(clusterItem, marker)
        // You can add additional setup here if needed after the marker is placed.
    }

    // Override shouldRenderAsCluster to control when clustering happens.
    // Return true to cluster, false to render as individual markers always.
    override fun shouldRenderAsCluster(cluster: Cluster<ReportClusterItem>): Boolean {
        // Only cluster if there are more than 1 item in the cluster
        return cluster.size > 1
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
        AlertSeverity.MODERATE, AlertSeverity.MINOR, AlertSeverity.UNKNOWN -> Color(0xFF1976D2) // Blue
        AlertSeverity.NONE -> Color(0xFF388E3C) // Green (Success)
    }
}

/**
 * Composable for the Floating Action Button displaying active alert count and severity color.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveAlertsFAB(alertCount: Int, highestSeverity: AlertSeverity, onClick: () -> Unit) {
    val fabColor = getAlertsFabColor(highestSeverity)

    FloatingActionButton(
        onClick = onClick,
        containerColor = fabColor,
        contentColor = Color.White,
        modifier = Modifier.padding(bottom = 0.dp) // Adjust padding as needed
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Info, contentDescription = "Active Alerts")
            if (alertCount > 0) {
                Badge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 10.dp, y = (-10).dp), // Offset badge to top-right of icon
                    containerColor = MaterialTheme.colorScheme.error // Use error color for visibility
                ) {
                    Text(alertCount.toString())
                }
            }
        }
    }
}

/**
 * Composable for the content of the Alerts Bottom Sheet.
 * Displays a radar map and a list of active NWS alerts.
 */
@Composable
fun AlertsBottomSheetContent(
    activeAlerts: List<AlertFeature>,
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
        val sortedAlerts = activeAlerts.sortedByDescending { AlertSeverity.fromString(it.properties.severity).level }

        items(sortedAlerts) { alert ->
            AlertItem(alert = alert)
        }

        // Spacer at the bottom
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

/**
 * Composable for displaying a single NWS alert item.
 */
@Composable
private fun AlertItem(alert: AlertFeature) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
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
                    Text("Instructions: $it", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
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
                    Spacer(modifier = Modifier.size(8.dp))
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
    val diff = now - timestamp
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes minutes ago"
        hours < 24 -> "$hours hours ago"
        else -> "$days days ago"
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
private fun ActionButtons(navController: NavHostController, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FloatingActionButton(
            onClick = { navController.navigate(Screen.Settings.route) }, // Navigate to Settings Screen
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Settings, "User Settings")
        }
        FloatingActionButton(onClick = { navController.navigate(Screen.MakeReport.route) }, containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White) {
            Icon(Icons.Default.AddAPhoto, "Make Report")
        }
    }
}

/**
 * Helper class to create and cache custom marker icons for the map.
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
            it.setBounds(0, 0, canvas.width, canvas.height)
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
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                MapTypeSelector(currentMapType = MapType.NORMAL, onMapTypeSelected = {})
                ActionButtons(navController = rememberNavController())
            }
        }
    }
}
