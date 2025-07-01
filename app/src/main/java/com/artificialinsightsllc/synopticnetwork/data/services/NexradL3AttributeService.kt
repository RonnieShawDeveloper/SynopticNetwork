package com.artificialinsightsllc.synopticnetwork.data.services

import android.util.Log
import com.artificialinsightsllc.synopticnetwork.data.models.FontDetails
import com.artificialinsightsllc.synopticnetwork.data.models.ForecastIcon
import com.artificialinsightsllc.synopticnetwork.data.models.NexradL3AttributePlacefile
import com.artificialinsightsllc.synopticnetwork.data.models.StormCell
import com.google.android.gms.maps.model.LatLng
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.Locale

/**
 * Service class responsible for fetching and parsing NEXRAD Level 3 Attributes placefile data.
 */
class NexradL3AttributeService(
    private val httpClient: OkHttpClient // Inject OkHttpClient for network requests
) {
    private val TAG = "NexradL3AttributeService"
    private val BASE_PLACEFILE_URL = "https://mesonet.agron.iastate.edu/request/grx/l3attr.txt"

    /**
     * Fetches the raw text content of the NEXRAD Level 3 Attributes placefile.
     *
     * @param radarSiteId The 3-letter identifier of the NEXRAD radar site (e.g., "MOB", "TBW").
     * @return The raw placefile content as a String, or null if fetching fails.
     */
    suspend fun fetchPlacefile(radarSiteId: String): String? {
        val url = "$BASE_PLACEFILE_URL?nexrad=${radarSiteId.uppercase(Locale.US)}"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "SynopticNetwork (rdspromo@gmail.com)") // Required for some NWS/NOAA services
            .build()

        return try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string().also {
                    Log.d(TAG, "Successfully fetched placefile for $radarSiteId")
                }
            } else {
                Log.e(TAG, "Failed to fetch placefile for $radarSiteId: ${response.code} - ${response.message}")
                null
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error fetching placefile for $radarSiteId: ${e.message}", e)
            null
        }
    }

    /**
     * Parses the raw text content of a NEXRAD Level 3 Attributes placefile into structured data models.
     *
     * @param rawText The raw string content of the placefile.
     * @return A [NexradL3AttributePlacefile] object if parsing is successful, or null if an error occurs.
     */
    fun parsePlacefile(rawText: String): NexradL3AttributePlacefile? {
        val lines = rawText.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            Log.w(TAG, "Empty or blank placefile content provided.")
            return null
        }

        var refreshInterval: Int? = null
        var title: String? = null
        var iconFileUrl: String? = null
        var fontDetails: FontDetails? = null
        val stormCells = mutableListOf<StormCell>()

        var currentStormCellInitialLocation: LatLng? = null
        var currentStormCellMainIconText: String? = null
        val currentStormCellTrackLine = mutableListOf<LatLng>()
        val currentStormCellForecastIcons = mutableListOf<ForecastIcon>()
        var inObjectBlock = false

        lines.forEachIndexed { index, line ->
            try {
                when {
                    line.startsWith("Refresh:") -> {
                        refreshInterval = line.substringAfter("Refresh:").trim().toIntOrNull()
                    }
                    line.startsWith("Title:") -> {
                        title = line.substringAfter("Title:").trim()
                    }
                    line.startsWith("IconFile:") -> {
                        val parts = line.substringAfter("IconFile:").split(",").map { it.trim() }
                        if (parts.size >= 6) {
                            iconFileUrl = parts[5].trim('"') // URL is the 6th part (index 5)
                        }
                    }
                    line.startsWith("Font:") -> {
                        val parts = line.substringAfter("Font:").split(",").map { it.trim() }
                        if (parts.size >= 4) {
                            fontDetails = FontDetails(
                                type = parts[0].toIntOrNull() ?: 0,
                                size = parts[1].toIntOrNull() ?: 0,
                                weight = parts[2].toIntOrNull() ?: 0,
                                family = parts[3].trim('"')
                            )
                        }
                    }
                    line.startsWith("Object:") -> {
                        // If we were already in an object block, it means the previous one implicitly ended
                        // without an explicit "END:", so we finalize it here.
                        if (inObjectBlock && currentStormCellInitialLocation != null && currentStormCellMainIconText != null) {
                            val hasTVS = currentStormCellMainIconText?.contains("TVS:", ignoreCase = true) == true
                            val hasMeso = currentStormCellMainIconText?.contains("MESO:", ignoreCase = true) == true
                            stormCells.add(
                                StormCell(
                                    initialLocation = currentStormCellInitialLocation!!,
                                    mainIconText = currentStormCellMainIconText!!,
                                    trackLine = currentStormCellTrackLine.toList(),
                                    forecastIcons = currentStormCellForecastIcons.toList(),
                                    hasTVS = hasTVS,
                                    hasMeso = hasMeso
                                )
                            )
                        }
                        // Reset for the new object block
                        inObjectBlock = true
                        currentStormCellTrackLine.clear()
                        currentStormCellForecastIcons.clear()
                        currentStormCellMainIconText = null // Reset main icon text for new object

                        val coords = line.substringAfter("Object:").trim().split(",")
                        if (coords.size == 2) {
                            currentStormCellInitialLocation = LatLng(coords[0].toDouble(), coords[1].toDouble())
                        } else {
                            Log.w(TAG, "Malformed Object coordinates on line ${index + 1}: $line")
                            currentStormCellInitialLocation = null // Invalidate current object
                        }
                    }
                    line.startsWith("Icon:") -> {
                        val iconData = line.substringAfter("Icon:").trim()
                        if (inObjectBlock) {
                            // This is either the main storm cell icon or a forecast icon
                            val parts = iconData.split(",")
                            if (parts.size >= 5) { // Minimum parts for a basic icon
                                // Check if it's the main icon (usually starts with 0,0,0,1,X where X is icon type)
                                // and contains multi-line text (indicated by \n)
                                val isMainIcon = parts[0].trim() == "0" && parts[1].trim() == "0" && parts[2].trim() == "0"
                                if (isMainIcon && iconData.contains("\"")) {
                                    // Extract the multi-line text between the quotes
                                    val textStart = iconData.indexOf("\"") + 1
                                    val textEnd = iconData.lastIndexOf("\"")
                                    if (textStart > 0 && textEnd > textStart) {
                                        currentStormCellMainIconText = iconData.substring(textStart, textEnd).replace("\\n", "\n")
                                    }
                                } else {
                                    // This is a forecast icon
                                    val coordsAndLabel = iconData.split(",").map { it.trim() }
                                    if (coordsAndLabel.size >= 6) { // Expecting lat, lon, rotation, ?, ?, label
                                        val lat = coordsAndLabel[0].toDoubleOrNull()
                                        val lon = coordsAndLabel[1].toDoubleOrNull()
                                        val rotation = coordsAndLabel[2].toFloatOrNull()
                                        val label = coordsAndLabel.drop(5).joinToString(",").trim('"') // Label is the 6th part (index 5) onwards

                                        if (lat != null && lon != null && rotation != null && label.isNotBlank()) {
                                            currentStormCellForecastIcons.add(ForecastIcon(LatLng(lat, lon), rotation, label))
                                        } else {
                                            Log.w(TAG, "Malformed Forecast Icon data on line ${index + 1}: $line")
                                        }
                                    }
                                }
                            }
                        } else {
                            Log.w(TAG, "Icon outside of Object block on line ${index + 1}: $line")
                        }
                    }
                    line.startsWith("Line:") -> {
                        if (inObjectBlock) {
                            // The first line of a "Line:" block is metadata, subsequent lines are coordinates
                            // We expect the very next lines to be coordinates until "END:" or another keyword
                            // This simple parser assumes coordinates immediately follow.
                            // The actual line parsing logic will be in the loop below.
                        } else {
                            Log.w(TAG, "Line outside of Object block on line ${index + 1}: $line")
                        }
                    }
                    line.matches(Regex("^-?\\d+\\.\\d+,-?\\d+\\.\\d+$")) -> { // Matches "lat,lon" format
                        if (inObjectBlock) {
                            val coords = line.split(",")
                            if (coords.size == 2) {
                                currentStormCellTrackLine.add(LatLng(coords[0].toDouble(), coords[1].toDouble()))
                            } else {
                                Log.w(TAG, "Malformed Line coordinate on line ${index + 1}: $line")
                            }
                        }
                    }
                    line.startsWith("END:") -> {
                        if (inObjectBlock) {
                            if (currentStormCellInitialLocation != null && currentStormCellMainIconText != null) {
                                val hasTVS = currentStormCellMainIconText?.contains("TVS:", ignoreCase = true) == true
                                val hasMeso = currentStormCellMainIconText?.contains("MESO:", ignoreCase = true) == true
                                stormCells.add(
                                    StormCell(
                                        initialLocation = currentStormCellInitialLocation!!,
                                        mainIconText = currentStormCellMainIconText!!,
                                        trackLine = currentStormCellTrackLine.toList(),
                                        forecastIcons = currentStormCellForecastIcons.toList(),
                                        hasTVS = hasTVS,
                                        hasMeso = hasMeso
                                    )
                                )
                            } else {
                                Log.w(TAG, "Skipping incomplete StormCell before END on line ${index + 1}")
                            }
                            inObjectBlock = false // End of object block
                            currentStormCellTrackLine.clear()
                            currentStormCellForecastIcons.clear()
                            currentStormCellInitialLocation = null
                            currentStormCellMainIconText = null
                        } else {
                            Log.w(TAG, "Unexpected END outside of Object block on line ${index + 1}")
                        }
                    }
                    // Ignore Threshold lines as they are not directly mapped to our model
                    line.startsWith("Threshold:") -> { /* Do nothing */ }
                    else -> {
                        // If we are in an object block and the line is not a recognized keyword,
                        // it might be part of the "Line:" coordinates.
                        // The regex above handles this, so this 'else' is for truly unhandled lines.
                        Log.d(TAG, "Unhandled line in placefile on line ${index + 1}: $line")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing placefile line ${index + 1}: '$line'", e)
                // Decide whether to continue parsing or return null on error
                // For now, we'll log and continue, allowing partial data.
            }
        }

        // After the loop, check if there's an un-ended object block
        if (inObjectBlock && currentStormCellInitialLocation != null && currentStormCellMainIconText != null) {
            val hasTVS = currentStormCellMainIconText?.contains("TVS:", ignoreCase = true) == true
            val hasMeso = currentStormCellMainIconText?.contains("MESO:", ignoreCase = true) == true
            stormCells.add(
                StormCell(
                    initialLocation = currentStormCellInitialLocation!!,
                    mainIconText = currentStormCellMainIconText!!,
                    trackLine = currentStormCellTrackLine.toList(),
                    forecastIcons = currentStormCellForecastIcons.toList(),
                    hasTVS = hasTVS,
                    hasMeso = hasMeso
                )
            )
        }

        if (stormCells.isEmpty()) {
            Log.w(TAG, "No storm cells parsed from placefile.")
        }

        return NexradL3AttributePlacefile(
            refreshInterval = refreshInterval ?: 300,
            title = title ?: "NEXRAD Level 3 Attributes",
            iconFileUrl = iconFileUrl,
            fontDetails = fontDetails,
            stormCells = stormCells.toList(), // Convert mutable list to immutable
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
    }
}
