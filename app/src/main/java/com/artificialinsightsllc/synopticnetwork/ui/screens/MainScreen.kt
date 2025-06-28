package com.artificialinsightsllc.synopticnetwork.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Streetview
import androidx.compose.material3.AlertDialog
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
import com.artificialinsightsllc.synopticnetwork.R
import com.artificialinsightsllc.synopticnetwork.data.models.Comment
import com.artificialinsightsllc.synopticnetwork.data.models.MapReport
import com.artificialinsightsllc.synopticnetwork.data.models.Report
import com.artificialinsightsllc.synopticnetwork.navigation.Screen
import com.artificialinsightsllc.synopticnetwork.ui.theme.SynopticNetworkTheme
import com.artificialinsightsllc.synopticnetwork.ui.theme.Transparent_Black
import com.artificialinsightsllc.synopticnetwork.ui.viewmodels.MainViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch
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
    val mapState by mainViewModel.mapState.collectAsState()
    val scope = rememberCoroutineScope()
    val reportSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val legendSheetState = rememberModalBottomSheetState()
    var showAddCommentDialog by remember { mutableStateOf(false) }
    var showLegendSheet by remember { mutableStateOf(false) }

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
            cameraPositionState.animate(CameraUpdateFactory.newCameraPosition(CameraPosition(it, 14f, 0f, 0f)), 1000)
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

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapState.mapProperties,
            uiSettings = uiSettings
        ) {
            // Filter the reports based on the toggle switches before displaying them
            val filteredReports = remember(mapState.reports, mapState.reportTypeFilters) {
                mapState.reports.filter { mapState.reportTypeFilters[it.reportType] ?: true }
            }

            filteredReports.forEach { report ->
                report.location?.let { geoPoint ->
                    val icon = markerIconFactory.createMarkerIcon(report)
                    Marker(
                        state = rememberMarkerState(position = LatLng(geoPoint.latitude, geoPoint.longitude)),
                        icon = icon,
                        onClick = {
                            mainViewModel.onMarkerClicked(report)
                            scope.launch { reportSheetState.show() }
                            true
                        }
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
            // Action Buttons are now aligned to the bottom end of the column
            ActionButtons(navController = navController, modifier = Modifier.align(Alignment.End))
        }
    }
}

@Composable
private fun LegendBottomSheetContent(
    filters: Map<String, Boolean>,
    onFilterChanged: (String, Boolean) -> Unit
) {
    val reportTypes = getReportTypesWithEmojis()

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            Text("Map Legend & Filters", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 16.dp))
        }
        items(reportTypes) { (name, emoji) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(emoji, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(name, style = MaterialTheme.typography.bodyLarge)
                }
                Switch(
                    checked = filters[name] ?: true,
                    onCheckedChange = { onFilterChanged(name, it) }
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
            if (!report.comments.isNullOrBlank()) {
                Text(report.comments, style = MaterialTheme.typography.bodyMedium)
            }
        }
        // NWS Status Section
        item { NwsStatusSection(report = report) }
        // Comments Section
        item {
            Divider()
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
            item { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        } else if (comments.isEmpty()) {
            item { Text("No comments yet.", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, modifier = Modifier.padding(vertical = 8.dp)) }
        } else {
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

private fun getReportTypesWithEmojis(): List<Pair<String, String>> {
    return listOf(
        "Tornado" to "🌪️", "Funnel Cloud" to "🌥️", "Wall Cloud" to "☁️", "Shelf Cloud" to "🌬️", "Waterspout" to "🌀", "Wind Damage" to "💨",
        "Hail" to "☄️", "Frequent Lightning" to "⚡", "Flooding" to "💧", "Coastal Flooding" to "🌊", "River Flooding" to "🏞️",
        "Freezing Rain / Ice" to "🧊", "Sleet" to "🌨️", "Snow" to "❄️", "Dense Fog" to "🌫️", "Wildfire Smoke / Haze" to "🔥",
        "Dust Storm" to "🏜️", "Severe Weather" to "⛈️", "Other" to "❓"
    )
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
        FloatingActionButton(onClick = { /* TODO: Navigate to User Settings Screen */ }, containerColor = MaterialTheme.colorScheme.secondary, contentColor = Color.White) {
            Icon(Icons.Default.Settings, "User Settings")
        }
        FloatingActionButton(onClick = { navController.navigate(Screen.MakeReport.route) }, containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White) {
            Icon(Icons.Default.AddAPhoto, "Make Report")
        }
    }
}

class MarkerIconFactory(private val context: Context) {
    private val iconCache = mutableMapOf<String, BitmapDescriptor>()

    fun createMarkerIcon(report: MapReport): BitmapDescriptor? {
        val cacheKey = "${report.reportType}_${report.direction.toInt()}"
        if (iconCache.containsKey(cacheKey)) {
            return iconCache[cacheKey]
        }

        val baseBitmap = ContextCompat.getDrawable(context, R.drawable.weatherpin)?.let {
            val bmp = Bitmap.createBitmap(120, 150, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            it.setBounds(0, 0, canvas.width, canvas.height)
            it.draw(canvas)
            bmp
        } ?: return null

        val rotatedBitmap = baseBitmap.rotate(report.direction)
        val emoji = getReportTypesWithEmojis().find { it.first == report.reportType }?.second ?: "❓"

        val finalBitmap = rotatedBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(finalBitmap)
        val paint = Paint().apply {
            textSize = 60f
            color = Color.Black.toArgb()
            textAlign = Paint.Align.CENTER
        }
        val x = canvas.width / 2f
        val y = (canvas.height / 2f) - ((paint.descent() + paint.ascent()) / 2) - 15f
        canvas.drawText(emoji, x, y, paint)

        val descriptor = BitmapDescriptorFactory.fromBitmap(finalBitmap)
        iconCache[cacheKey] = descriptor
        return descriptor
    }

    private fun Bitmap.rotate(degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }
}

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
