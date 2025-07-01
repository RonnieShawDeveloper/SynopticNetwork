package com.artificialinsightsllc.synopticnetwork.data.models

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint

/**
 * Represents the parsed content of a NEXRAD Level 3 Attributes placefile.
 * This is the top-level data model that will hold all the information extracted from the placefile.
 *
 * @param refreshInterval The refresh rate in seconds (from "Refresh: X").
 * @param title The title of the placefile (from "Title: ...").
 * @param iconFileUrl The URL for the storm attribute icon (from "IconFile: ...").
 * @param fontDetails Details about the font specified in the placefile (from "Font: ...").
 * @param stormCells A list of all detected storm cells, each containing its own details, track, and forecast.
 * @param lastUpdatedTimestamp The timestamp when this placefile data was last fetched and parsed.
 */
data class NexradL3AttributePlacefile(
    val refreshInterval: Int = 300, // Default to 5 minutes if not specified or parsed
    val title: String = "NEXRAD Level 3 Attributes",
    val iconFileUrl: String? = null,
    val fontDetails: FontDetails? = null,
    val stormCells: List<StormCell> = emptyList(),
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)

/**
 * Represents the font details specified in a placefile's "Font:" line.
 *
 * @param type The font type (e.g., 1).
 * @param size The font size.
 * @param weight The font weight (e.g., 1).
 * @param family The font family name (e.g., "Courier New").
 */
data class FontDetails(
    val type: Int,
    val size: Int,
    val weight: Int,
    val family: String
)

/**
 * Represents a single storm cell object parsed from a placefile's "Object:" block.
 *
 * @param initialLocation The geographic coordinates (latitude, longitude) of the storm cell's origin.
 * @param mainIconText The multi-line text associated with the main storm cell icon.
 * This will be used for the marker's info window.
 * @param trackLine A list of LatLng points representing the storm cell's projected track.
 * @param forecastIcons A list of icons representing future forecast positions (e.g., +15 min).
 * @param hasTVS A boolean indicating if the storm cell has a Tornado Vortex Signature (TVS).
 * @param hasMeso A boolean indicating if the storm cell has a Mesocyclone (MESO).
 */
data class StormCell(
    val initialLocation: LatLng,
    val mainIconText: String,
    val trackLine: List<LatLng> = emptyList(),
    val forecastIcons: List<ForecastIcon> = emptyList(),
    val hasTVS: Boolean = false, // Derived from mainIconText
    val hasMeso: Boolean = false // Derived from mainIconText
)

/**
 * Represents a forecast icon (e.g., +15 min, +30 min) associated with a storm cell.
 *
 * @param location The geographic coordinates of the forecast icon.
 * @param rotation The rotation angle in degrees for the icon.
 * @param label The text label for the forecast icon (e.g., "+15 min").
 */
data class ForecastIcon(
    val location: LatLng,
    val rotation: Float,
    val label: String
)
