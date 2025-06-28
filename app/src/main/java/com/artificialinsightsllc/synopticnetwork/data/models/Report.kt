package com.artificialinsightsllc.synopticnetwork.data.models

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Data class representing a single weather report in Firestore.
 *
 * @param reportId The unique ID for the report document.
 * @param userId The ID of the user who created the report.
 * @param location The geographic location of the report, stored as a GeoPoint.
 * @param imageUrl The public download URL of the report's image in Firebase Storage.
 * @param direction The direction in degrees the camera was facing.
 * @param reportType The type of weather phenomenon reported (e.g., "Tornado").
 * @param comments Any additional comments from the user.
 * @param sendToNws A flag indicating if the user wants this report forwarded to the NWS.
 * @param nwsAcknowledged A flag indicating if the NWS has acknowledged receipt of the report.
 * @param phoneNumber The user's phone number (optional, for NWS callback).
 * @param wfo The NWS Weather Forecast Office ID for the report's location (e.g., "TBW").
 * @param zone The NWS forecast zone ID for the report's location (e.g., "FLZ151").
 * @param timestamp The time the report was created, automatically set by the server.
 */
data class Report(
    val reportId: String = "",
    val userId: String = "",
    val location: GeoPoint? = null,
    val imageUrl: String = "",
    val direction: Float = 0f,
    val reportType: String = "",
    val comments: String? = null,
    val sendToNws: Boolean = false,
    val nwsAcknowledged: Boolean = false, // New field
    val phoneNumber: String? = null,
    val wfo: String? = null,
    val zone: String? = null,
    @ServerTimestamp
    val timestamp: Date? = null
)
