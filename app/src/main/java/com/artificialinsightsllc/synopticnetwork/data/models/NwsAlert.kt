package com.artificialinsightsllc.synopticnetwork.data.models

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable
import com.google.firebase.firestore.GeoPoint // For potential future use, though NWS alerts use coordinates directly
import java.util.Date // For parsing timestamps

/**
 * Top-level data class for the NWS Alerts API response.
 * Example URL: https://api.weather.gov/alerts/active?status=actual&message_type=alert,update,cancel&point={lat}-{lon}&urgency=Immediate&severity=Extreme,Severe,Moderate,Minor,Unknown&certainty=Observed,Likely,Possible,Unlikely,Unknown
 */
@SuppressLint("UnsafeOptInUsageError") // Suppress warning for ExperimentalSerializationApi
@Serializable
data class NwsAlertsResponse(
    val type: String, // e.g., "FeatureCollection"
    val features: List<AlertFeature> = emptyList(), // List of individual alert features
    val title: String? = null,
    val updated: String? = null
)

/**
 * Represents a single alert feature within the NWS Alerts response.
 */
@SuppressLint("UnsafeOptInUsageError") // Suppress warning for ExperimentalSerializationApi
@Serializable
data class AlertFeature(
    val id: String, // Unique ID for the alert
    val type: String, // e.g., "Feature"
    val geometry: AlertGeometry? = null, // Geographic area of the alert
    val properties: AlertProperties // Contains the main details of the alert
)

/**
 * Represents the geometric information of an alert.
 * This can be a Polygon, Point, etc. For simplicity, we only parse Polygon coordinates.
 */
@SuppressLint("UnsafeOptInUsageError") // Suppress warning for ExperimentalSerializationApi
@Serializable
data class AlertGeometry(
    val type: String, // e.g., "Polygon", "Point"
    val coordinates: List<List<List<Double>>>? = null // For Polygon: List of rings, each ring a list of [lon, lat] pairs
)

/**
 * Contains the detailed properties of an NWS alert.
 * Uses default values for nullable fields to prevent crashes if data is missing.
 */
@SuppressLint("UnsafeOptInUsageError") // Suppress warning for ExperimentalSerializationApi
@Serializable
data class AlertProperties(
    val id: String,
    val areaDesc: String? = null, // Description of the affected area
    val sent: String? = null, // Timestamp when the alert was sent (ISO 8601)
    val effective: String? = null, // Timestamp when the alert becomes effective (ISO 8601)
    val onset: String? = null, // Timestamp when the event is expected to begin (ISO 8601)
    val expires: String? = null, // Timestamp when the alert is expected to expire (ISO 8601)
    val ends: String? = null, // Timestamp when the event is expected to end (ISO 8601)
    val status: String? = null, // e.g., "Actual"
    val messageType: String? = null, // e.g., "Alert", "Update", "Cancel"
    val category: String? = null, // e.g., "Met" (Meteorological)
    val severity: String? = null, // "Extreme", "Severe", "Moderate", "Minor", "Unknown"
    val certainty: String? = null, // e.g., "Likely", "Observed"
    val urgency: String? = null, // e.g., "Immediate", "Expected"
    val event: String? = null, // Name of the event (e.g., "Special Marine Warning", "Tornado Warning")
    val senderName: String? = null, // Name of the issuing office (e.g., "NWS Tampa Bay Ruskin FL")
    val headline: String? = null, // Short, summarizing text
    val description: String? = null, // Detailed description of the alert
    val instruction: String? = null, // Recommended actions for the public
    val response: String? = null, // Recommended response (e.g., "Evacuate", "Shelter", "Avoid")
    val UGC: List<String>? = null // NEW: Added for User Geocodes (zones)
    // Note: 'parameters' field is complex and often contains varied structures,
    // so it's omitted here for simplicity unless specifically needed.
)

/**
 * Defines the possible severity levels for NWS alerts, in hierarchical order.
 * This enum can be used for sorting and determining the highest severity.
 */
enum class AlertSeverity(val level: Int) {
    EXTREME(5),
    SEVERE(4),
    MODERATE(3),
    MINOR(2),
    UNKNOWN(1),
    NONE(0); // Custom level for no alerts

    companion object {
        /**
         * Converts a string severity into an AlertSeverity enum, defaulting to UNKNOWN.
         */
        fun fromString(severityString: String?): AlertSeverity {
            return entries.find { it.name.equals(severityString, ignoreCase = true) } ?: UNKNOWN
        }
    }
}
