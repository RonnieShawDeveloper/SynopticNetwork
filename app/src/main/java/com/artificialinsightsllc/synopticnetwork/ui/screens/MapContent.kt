package com.artificialinsightsllc.synopticnetwork.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.artificialinsightsllc.synopticnetwork.data.models.AlertFeature
import com.artificialinsightsllc.synopticnetwork.data.models.AlertSeverity
import com.artificialinsightsllc.synopticnetwork.data.models.StormCell
import com.artificialinsightsllc.synopticnetwork.data.services.RadarTileProvider
import com.artificialinsightsllc.synopticnetwork.ui.viewmodels.DisplayMarker
import com.artificialinsightsllc.synopticnetwork.ui.viewmodels.MainViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.TileOverlay
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import okhttp3.OkHttpClient

/**
 * Composable that encapsulates the Google Map and all its overlays and markers.
 *
 * @param mainViewModel The ViewModel providing map state and handling map interactions.
 * @param markerIconFactory An instance of MarkerIconFactory to create custom marker icons.
 * @param okHttpClient An OkHttpClient instance for network requests (e.g., for radar tiles).
 * @param onMarkerClick Callback for when a user report marker is clicked.
 * @param onGroupMarkerClick Callback for when a group marker is clicked.
 * @param onAlertPolygonClick Callback for when an NWS alert polygon is clicked.
 * @param onStormCellClick Callback for when a storm cell marker is clicked.
 */
@Composable
fun MapContent(
    mainViewModel: MainViewModel,
    markerIconFactory: MarkerIconFactory,
    okHttpClient: OkHttpClient,
    onMarkerClick: (com.artificialinsightsllc.synopticnetwork.data.models.MapReport?) -> Unit,
    onGroupMarkerClick: (String) -> Unit,
    onAlertPolygonClick: (AlertFeature) -> Unit,
    onStormCellClick: (StormCell) -> Unit
) {
    val mapState by mainViewModel.uiState.collectAsState()

    // Default location for the map camera if current location is not yet available
    val defaultLocation = LatLng(28.3486, -82.6826) // Centered near Florida

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 10f)
    }

    // Connect map zoom to ViewModel
    LaunchedEffect(cameraPositionState.position.zoom) {
        mainViewModel.onMapZoomChanged(cameraPositionState.position.zoom)
    }

    // Animate camera to current location when it becomes available
    LaunchedEffect(mapState.currentLocation) {
        mapState.currentLocation?.let {
            cameraPositionState.animate(
                com.google.android.gms.maps.CameraUpdateFactory.newCameraPosition(
                    CameraPosition(it, 10f, 0f, 0f)
                ), 1000
            )
        }
    }

    // UI settings for the map, controlling built-in UI elements
    val uiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            compassEnabled = true,
            myLocationButtonEnabled = true,
            mapToolbarEnabled = false,
            rotationGesturesEnabled = true,
            scrollGesturesEnabled = true,
            tiltGesturesEnabled = true,
            zoomGesturesEnabled = true
        )
    }

    GoogleMap(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
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
                            onMarkerClick(report)
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
                            onGroupMarkerClick(displayMarker.geohash)
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
                                onAlertPolygonClick(alert) // Pass the clicked alert
                            },
                            zIndex = 2f // Set a higher zIndex for alert polygons
                        )
                    }
                }
            }
        }

        // Reflectivity Radar Tile Overlay
        if (mapState.isReflectivityRadarActive && mapState.radarWfo != null && mapState.latestRadarTimestamp != null) {
            val radarOfficeCodeForTileProvider = mapState.radarWfo!!.lowercase(java.util.Locale.US) // Ensure lowercase with Locale
            val reflectivityLayerName = "${radarOfficeCodeForTileProvider}_sr_bref" // Explicit layer name
            Log.d("MapContent", "Creating Reflectivity RadarTileProvider with officeCode: $radarOfficeCodeForTileProvider, layer: $reflectivityLayerName and timestamp: ${mapState.latestRadarTimestamp}")
            if (radarOfficeCodeForTileProvider.isNotBlank()) {
                TileOverlay(
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
            val radarOfficeCodeForTileProvider = mapState.radarWfo!!.lowercase(java.util.Locale.US)
            val velocityLayerName = "${radarOfficeCodeForTileProvider}_sr_bvel" // Explicit layer name for velocity
            Log.d("MapContent", "Creating Velocity RadarTileProvider with officeCode: $radarOfficeCodeForTileProvider, layer: $velocityLayerName and timestamp: ${mapState.latestRadarTimestamp}")
            if (radarOfficeCodeForTileProvider.isNotBlank()) {
                TileOverlay(
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

            placefile?.stormCells?.forEach { stormCell ->
                // Main Storm Cell Marker
                Marker(
                    state = rememberMarkerState(position = stormCell.initialLocation),
                    title = stormCell.mainIconText.substringBefore("\n").trim(), // Use first line as title
                    snippet = null, // REMOVED: No longer showing snippet, will use bottom sheet instead
                    icon = markerIconFactory.createStormCellIcon(stormCell),
                    onClick = {
                        onStormCellClick(stormCell) // Pass the clicked storm cell
                        true // Consume the event
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
                        startCap = RoundCap(),
                        endCap = RoundCap(),
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
