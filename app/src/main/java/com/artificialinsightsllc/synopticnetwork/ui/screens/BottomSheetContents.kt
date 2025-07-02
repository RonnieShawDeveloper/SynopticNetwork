package com.artificialinsightsllc.synopticnetwork.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.artificialinsightsllc.synopticnetwork.R
import com.artificialinsightsllc.synopticnetwork.data.models.AlertSeverity
import com.artificialinsightsllc.synopticnetwork.data.models.Comment
import com.artificialinsightsllc.synopticnetwork.data.models.Report
import com.artificialinsightsllc.synopticnetwork.data.models.StormCell
import com.artificialinsightsllc.synopticnetwork.data.models.getReportTypesWithEmojis
import com.artificialinsightsllc.synopticnetwork.ui.viewmodels.DisplayAlert
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Composable for the content of the Report Details Bottom Sheet.
 * Displays detailed information about a user-submitted report.
 *
 * @param report The [Report] object to display.
 * @param comments The list of [Comment]s associated with the report.
 * @param isLoadingComments True if comments are currently being loaded.
 * @param onAddCommentClicked Callback to trigger the "Add Comment" dialog.
 */
@Composable
fun ReportBottomSheetContent(
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

/**
 * Composable for displaying the NWS status section of a report.
 *
 * @param report The [Report] object to display the NWS status for.
 */
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

/**
 * Composable for displaying a single comment item.
 *
 * @param comment The [Comment] object to display.
 */
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

/**
 * Composable for the content of the Alerts Bottom Sheet.
 * Displays a radar map and a list of active NWS alerts.
 *
 * @param activeAlerts The list of [DisplayAlert]s to show.
 * @param isLoadingAlerts True if alerts are currently loading.
 * @param radarWfo The WFO code for displaying the local radar image.
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
 * Composable for displaying a single NWS alert item within the Alerts Bottom Sheet.
 *
 * @param displayAlert The [DisplayAlert] object to show.
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
 * Composable for the content of the Map Legend & Filters Bottom Sheet.
 *
 * @param filters A map of report type names to their visibility status.
 * @param onFilterChanged Callback to update the visibility status of a report type.
 */
@Composable
fun LegendBottomSheetContent(
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

/**
 * Composable for displaying detailed storm cell information in a ModalBottomSheet.
 *
 * @param stormCell The [StormCell] object to display details for.
 */
@Composable
fun StormCellDetailsBottomSheetContent(stormCell: StormCell) {
    val details = parseStormCellDetails(stormCell.mainIconText)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .verticalScroll(rememberScrollState()), // Enable scrolling for long content
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stormCell.mainIconText.substringBefore("\n").trim(), // Use the first line as main title
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        details.forEach { detailLine ->
            Text(
                text = detailLine,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }

        // You can add more structured display here if needed, e.g.,
        // if (stormCell.hasTVS) Text("TVS Detected!", color = GraphicsColor.RED)
        // if (stormCell.hasMeso) Text("Mesocyclone Detected!", color = GraphicsColor.YELLOW)
    }
}

/**
 * Helper function to parse the mainIconText from a StormCell and extract relevant details for display.
 *
 * @param mainIconText The raw multi-line text from the placefile's main icon.
 * @return A list of strings, where each string is a detail line (excluding the main title line).
 */
@Composable
private fun parseStormCellDetails(mainIconText: String): List<String> {
    return remember(mainIconText) {
        val lines = mainIconText.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        val details = mutableListOf<String>()

        // Add all lines except the first (which is used for the title of the marker)
        if (lines.size > 1) {
            details.addAll(lines.subList(1, lines.size))
        }
        details
    }
}

/**
 * Helper function to map AlertSeverity to a Color for display.
 *
 * @param severity The [AlertSeverity] enum value.
 * @return The corresponding [Color] for the severity.
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
 *
 * @param isoTimestamp The ISO 8601 timestamp string.
 * @return A formatted date/time string.
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
 *
 * @param isoTimestamp The ISO 8601 timestamp string for expiration.
 * @return A human-readable string (e.g., "1 hour", "2 days", "5 minutes"), or null if parsing fails.
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

/**
 * Helper function to get relative time string from a timestamp.
 *
 * @param timestamp The timestamp in milliseconds.
 * @return A human-readable relative time string (e.g., "Just now", "5 minutes ago").
 */
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
