@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.artificialinsightsllc.synopticnetwork.data.models

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName // Import SerialName
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

// NEW: Data models for NWS Products API

/**
 * Represents a single product entry in the list of available NWS products.
 * This is the object contained within the @graph array.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class NwsProductListItem( // Renamed to NwsProductListItem for clarity
    val productCode: String, // The WMO product code (e.g., "AFD", "CF6")
    val productName: String // The human-readable name of the product (e.g., "Area Forecast Discussion")
)

/**
 * Represents the top-level response for a list of available NWS products.
 * Endpoint: https://api.weather.gov/products/locations/{WFO}/types
 * This object contains the "@graph" array.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class NwsProductListResponse(
    @SerialName("@graph") // Maps the JSON field "@graph" to this property
    val graph: List<NwsProductListItem> = emptyList() // Now a list of NwsProductListItem
)

/**
 * Represents the top-level response for a specific NWS product's content.
 * Endpoint: https://api.weather.gov/products/types/{productCode}/locations/{WFO}/latest
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class NwsProductDetailResponse(
    val id: String, // Unique ID for this specific product instance
    val wmoCollectiveId: String? = null, // WMO Collective ID (e.g., "FXUS62")
    val issuingOffice: String? = null, // Issuing office (e.g., "KTBW")
    val issuanceTime: String? = null, // Timestamp when the product was issued (ISO 8601)
    val productCode: String, // The product code (e.g., "AFD")
    val productName: String, // The human-readable name of the product
    val productText: String // The actual text content of the product
)
