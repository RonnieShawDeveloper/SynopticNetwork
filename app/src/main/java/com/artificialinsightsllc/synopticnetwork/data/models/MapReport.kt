package com.artificialinsightsllc.synopticnetwork.data.models

import com.google.firebase.firestore.GeoPoint

/**
 * A lightweight data class representing the minimal information
 * needed to display a report marker on the map. This is used to
 * efficiently load all markers without fetching unnecessary data like
 * image URLs or comments.
 *
 * @param reportId The unique ID for the report document.
 * @param location The geographic location of the report.
 * @param direction The direction in degrees the camera was facing.
 * @param reportType The type of weather phenomenon reported.
 */
data class MapReport(
    val reportId: String = "",
    val location: GeoPoint? = null,
    val direction: Float = 0f,
    val reportType: String = ""
)
