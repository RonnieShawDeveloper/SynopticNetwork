@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.artificialinsightsllc.synopticnetwork.data.models

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

/**
 * Represents the top-level response from the NWS /points/{lat},{lon} endpoint.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class NwsPointResponse(
    val properties: NwsPointProperties? = null
)

/**
 * Contains the specific properties we need from the NWS point response.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class NwsPointProperties(
    // The three-letter Weather Forecast Office ID (e.g., "TBW")
    val gridId: String? = null,
    // The full URL for the forecast zone (e.g., "https://api.weather.gov/zones/forecast/FLZ151")
    val forecastZone: String? = null
)

/**
 * Represents the top-level response from the Google Geocoding API.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class GeocodingResponse(
    val results: List<GeocodingResult> = emptyList(),
    val status: String
)

/**
 * Represents a single result from the geocoding response.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class GeocodingResult(
    val geometry: Geometry
)

/**
 * Contains the location data (latitude and longitude).
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Geometry(
    val location: Location
)

/**
 * The precise latitude and longitude coordinates.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Location(
    val lat: Double,
    val lng: Double
)
