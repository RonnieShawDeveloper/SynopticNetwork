package com.artificialinsightsllc.synopticnetwork.ui.screens

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.artificialinsightsllc.synopticnetwork.data.models.AlertFeature
import com.artificialinsightsllc.synopticnetwork.data.models.AlertSeverity
import com.artificialinsightsllc.synopticnetwork.data.models.Report
import com.artificialinsightsllc.synopticnetwork.data.models.MapReport
import com.artificialinsightsllc.synopticnetwork.data.models.StormCell
import com.artificialinsightsllc.synopticnetwork.ui.theme.SynopticNetworkTheme
import com.artificialinsightsllc.synopticnetwork.ui.viewmodels.MainViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

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

    // Bottom Sheet States
    val reportSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val alertsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val legendSheetState = rememberModalBottomSheetState()
    val stormCellDetailsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Dialog States
    var showAddCommentDialog by remember { mutableStateOf(false) }
    var showLegendSheet by remember { mutableStateOf(false) }
    var showAlertsSheet by remember { mutableStateOf(false) }
    var selectedAlertForDialog by remember { mutableStateOf<AlertFeature?>(null) }
    var showGroupedReportsDialog by remember { mutableStateOf(false) }
    var selectedStormCellForDetails by remember { mutableStateOf<StormCell?>(null) }

    val locationPermissionState = rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)

    // Initialize OkHttpClient for RadarTileProvider and MarkerIconFactory
    val okHttpClient = remember { OkHttpClient() }
    val markerIconFactory = remember { MarkerIconFactory(context) }

    // Lifecycle Effects
    LaunchedEffect(locationPermissionState.status) {
        if (locationPermissionState.status.isGranted) {
            mainViewModel.onMapReady(context)
        }
    }

    LaunchedEffect(mapState.selectedGroupedFullReports) {
        if (mapState.selectedGroupedFullReports.isNotEmpty()) {
            showGroupedReportsDialog = true
        } else {
            showGroupedReportsDialog = false
        }
    }

    LaunchedEffect(mapState.nexradL3Attributes?.iconFileUrl) {
        mapState.nexradL3Attributes?.iconFileUrl?.let { url ->
            markerIconFactory.loadStormAttributeIcon(url)
        }
    }

    // Report Details Bottom Sheet
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

    // Legend Bottom Sheet
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

    // Active Alerts Bottom Sheet
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

    // Storm Cell Details Bottom Sheet
    if (selectedStormCellForDetails != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedStormCellForDetails = null },
            sheetState = stormCellDetailsSheetState,
            modifier = Modifier.fillMaxHeight(0.9f)
        ) {
            StormCellDetailsBottomSheetContent(stormCell = selectedStormCellForDetails!!)
        }
    }

    // Alert Details Dialog
    selectedAlertForDialog?.let { alert ->
        AlertDetailsDialog(
            alert = alert,
            onDismiss = { selectedAlertForDialog = null }
        )
    }

    // Grouped Reports Dialog
    if (showGroupedReportsDialog && mapState.selectedGroupedFullReports.isNotEmpty()) {
        GroupedReportsDialog(
            reports = mapState.selectedGroupedFullReports,
            onDismiss = {
                showGroupedReportsDialog = false
                mainViewModel.onGroupMarkerClicked("") // Clear the selected grouped reports in ViewModel
            },
            onReportSelected = { report ->
                mainViewModel.onMarkerClicked(report.toMapReport())
                showGroupedReportsDialog = false
                mainViewModel.onGroupMarkerClicked("")
                scope.launch { reportSheetState.show() }
            }
        )
    }

    // Add Comment Dialog
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
        // Map content
        MapContent(
            mainViewModel = mainViewModel,
            markerIconFactory = markerIconFactory,
            okHttpClient = okHttpClient,
            onMarkerClick = { report ->
                mainViewModel.onMarkerClicked(report)
                scope.launch { reportSheetState.show() }
            },
            onGroupMarkerClick = { geohash ->
                mainViewModel.onGroupMarkerClicked(geohash)
            },
            onAlertPolygonClick = { alert ->
                selectedAlertForDialog = alert
            },
            onStormCellClick = { stormCell ->
                selectedStormCellForDetails = stormCell
                scope.launch { stormCellDetailsSheetState.show() }
            }
        )

        if (mapState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        // Map Overlay UI (main Column)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            MapControls(
                navController = navController,
                alertsCount = mapState.activeAlerts.size,
                highestSeverity = mapState.highestSeverity,
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
                },
                currentMapType = mapState.mapProperties.mapType,
                onMapTypeSelected = { mainViewModel.onMapTypeChanged(it) },
                lastRadarUpdateTimeString = mapState.lastRadarUpdateTimeString,
                isRadarActive = mapState.isReflectivityRadarActive || mapState.isVelocityRadarActive,
                onLegendFabClick = { showLegendSheet = true }, // Corrected: Replaced TODO()
                onAlertsFabClick = { showAlertsSheet = true } // Corrected: Replaced TODO()
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
 * Composable for adding a new comment via an AlertDialog.
 */
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

/**
 * Helper function to map AlertSeverity to a Color for display.
 */
@Composable
private fun getAlertSeverityColor(severity: AlertSeverity): androidx.compose.ui.graphics.Color {
    return when (severity) {
        AlertSeverity.EXTREME -> androidx.compose.ui.graphics.Color(0xFFD32F2F) // Red
        AlertSeverity.SEVERE -> androidx.compose.ui.graphics.Color(0xFFF57C00) // Orange
        AlertSeverity.MODERATE -> androidx.compose.ui.graphics.Color(0xFFFFA000) // Darker Orange/Amber
        AlertSeverity.MINOR -> androidx.compose.ui.graphics.Color(0xFF1976D2) // Blue
        AlertSeverity.UNKNOWN -> androidx.compose.ui.graphics.Color.Gray
        AlertSeverity.NONE -> androidx.compose.ui.graphics.Color.Black // Should not be called for individual alerts
    }
}

/**
 * Helper function to format ISO 8601 timestamps into a readable format.
 */
private fun formatTimestamp(isoTimestamp: String): String {
    return try {
        // Example format: 2025-06-29T11:48:00-04:00
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US)
        val formatter = SimpleDateFormat("MMM dd,yyyy HH:mm z", java.util.Locale.US)
        formatter.format(parser.parse(isoTimestamp) ?: java.util.Date())
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
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US)
        val expirationDate = parser.parse(isoTimestamp) ?: return null
        val now = java.util.Date()

        val diffMillis = expirationDate.time - now.time

        if (diffMillis <= 0) {
            return "Expired"
        }

        val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffMillis)
        val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(diffMillis - java.util.concurrent.TimeUnit.DAYS.toMillis(days))
        val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(diffMillis - java.util.concurrent.TimeUnit.DAYS.toMillis(days) - java.util.concurrent.TimeUnit.HOURS.toMillis(hours))

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

/**
 * Helper function to get relative time string from a timestamp.
 */
private fun getRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diffMillis = now - timestamp
    val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(diffMillis)
    val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(diffMillis)
    val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffMillis)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes minute${if (minutes > 1) "s" else ""}"
        hours < 24 -> "$hours hour${if (hours > 1) "s" else ""}"
        else -> "$days day${if (days > 1) "s" else ""}"
    }
}

/**
 * Preview for the Main Screen.
 */
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    SynopticNetworkTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            // Placeholder for the MapContent
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Google Map Placeholder")
            }
            // Preview MapControls
            MapControls(
                navController = rememberNavController(),
                alertsCount = 5,
                highestSeverity = AlertSeverity.SEVERE,
                radarWfo = "KTBW",
                showReflectivityRadarOverlay = true,
                onToggleReflectivityRadarOverlay = {},
                showVelocityRadarOverlay = false,
                onToggleVelocityRadarOverlay = {},
                showPlacefileOverlay = true,
                onTogglePlacefileOverlay = {},
                currentMapType = com.google.maps.android.compose.MapType.NORMAL,
                onMapTypeSelected = {},
                lastRadarUpdateTimeString = "Last Updated: 10:30 AM",
                isRadarActive = true,
                onLegendFabClick = {}, // Dummy lambda for preview
                onAlertsFabClick = {} // Dummy lambda for preview
            )
        }
    }
}
