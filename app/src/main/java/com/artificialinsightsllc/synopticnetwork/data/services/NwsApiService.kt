package com.artificialinsightsllc.synopticnetwork.data.services

import android.util.Log
import com.artificialinsightsllc.synopticnetwork.BuildConfig
import com.artificialinsightsllc.synopticnetwork.data.models.GeocodingResponse
import com.artificialinsightsllc.synopticnetwork.data.models.NwsAlertsResponse // Import the new alerts response model
import com.artificialinsightsllc.synopticnetwork.data.models.NwsPointResponse
import com.artificialinsightsllc.synopticnetwork.data.models.NwsProductListResponse // NEW: Import NwsProductListResponse
import com.artificialinsightsllc.synopticnetwork.data.models.NwsProductDetailResponse // NEW: Import NwsProductDetailResponse
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory // NEW: Import ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// Base URLs for the APIs
private const val NWS_API_BASE_URL = "https://api.weather.gov/"
private const val GOOGLE_GEOCODING_API_BASE_URL = "https://maps.googleapis.com/"
private const val NCEP_GEOSERVER_BASE_URL = "https://opengeo.ncep.noaa.gov/geoserver/" // NEW: Base URL for NCEP GeoServer
private const val TAG = "NwsApiService"

/**
 * A Retrofit interface defining the API endpoints we will connect to.
 */
interface NwsApi {
    @GET("points/{lat},{lon}")
    suspend fun getPointData(
        @Path("lat") latitude: Double,
        @Path("lon") longitude: Double
    ): NwsPointResponse

    @GET("maps/api/geocode/json")
    suspend fun getCoordsFromZip(
        @Query("address") zipCode: String,
        @Query("key") apiKey: String
    ): GeocodingResponse

    // MODIFIED: Removed 'limit' parameter as it's not recognized by the NWS API
    @GET("alerts/active")
    suspend fun getActiveAlerts(
        @Query("status") status: String = "actual",
        @Query("message_type") messageType: String = "alert,update,cancel",
        @Query("area") area: String, // MODIFIED: Now takes a state code (e.g., "FL")
        @Query("urgency") urgency: String = "Immediate,Expected,Future,Past,Unknown", // MODIFIED: All urgency types
        @Query("severity") severity: String = "Extreme,Severe,Moderate,Minor,Unknown",
        @Query("certainty") certainty: String = "Observed,Likely" // Changed to Observed,Likely as per NWS best practices
    ): NwsAlertsResponse

    // MODIFIED: Changed return type from List<NwsProductListResponse> to NwsProductListResponse
    @GET("products/locations/{wfo}/types")
    suspend fun getProductTypes(
        @Path("wfo") wfo: String
    ): NwsProductListResponse // MODIFIED: Now returns the wrapper object

    // NEW: Endpoint to get the latest product text
    @GET("products/types/{productCode}/locations/{wfo}/latest")
    suspend fun getLatestProduct(
        @Path("productCode") productCode: String,
        @Path("wfo") wfo: String
    ): NwsProductDetailResponse

    // NEW: Endpoint for WMS GetCapabilities
    // The path parameter should match the WFO ID as it appears in the URL (e.g., "ktbw")
    @GET("{officeCode}/ows")
    suspend fun getWmsCapabilities(
        @Path("officeCode") officeCode: String, // This will be the 4-letter WFO, e.g., "ktbw"
        @Query("service") service: String = "WMS",
        @Query("version") version: String = "1.3.0",
        @Query("request") request: String = "GetCapabilities"
    ): String // Return raw XML string
}

/**
 * Service class that provides a clean way to interact with the NWS and Geocoding APIs.
 */
class NwsApiService {

    // Configure Json to ignore unknown keys to make our app more robust.
    private val json = Json { ignoreUnknownKeys = true }

    // Create a logging interceptor to see network traffic in Logcat for debugging.
    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d(TAG, message)
    }.apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

    // Create an OkHttpClient and add the logging interceptor.
    // Also add an Interceptor for the User-Agent header required by NWS for radar images/some data.
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "SynopticNetwork (rdspromo@gmail.com)") // NWS requires this
                .build()
            chain.proceed(request)
        }
        .build()

    // Create a Retrofit instance for the NWS API
    private val nwsRetrofit = Retrofit.Builder()
        .baseUrl(NWS_API_BASE_URL)
        .client(httpClient) // Add the OkHttpClient
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    // Create a Retrofit instance for the Google Geocoding API
    private val googleRetrofit = Retrofit.Builder()
        .baseUrl(GOOGLE_GEOCODING_API_BASE_URL)
        .client(httpClient) // Add the OkHttpClient
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    // NEW: Create a Retrofit instance for the NCEP GeoServer WMS
    private val ncepGeoServerRetrofit = Retrofit.Builder()
        .baseUrl(NCEP_GEOSERVER_BASE_URL)
        .client(httpClient)
        .addConverterFactory(ScalarsConverterFactory.create()) // NEW: Add ScalarsConverterFactory for String responses
        .build()

    private val nwsApi: NwsApi = nwsRetrofit.create(NwsApi::class.java)
    private val googleApi: NwsApi = googleRetrofit.create(NwsApi::class.java)
    private val ncepGeoServerApi: NwsApi = ncepGeoServerRetrofit.create(NwsApi::class.java) // NEW: Instance for GeoServer

    /**
     * Fetches the NWS point data (WFO and Zone) for a given latitude and longitude.
     */
    suspend fun getNwsPointData(latitude: Double, longitude: Double): NwsPointResponse? {
        Log.i(TAG, "Fetching NWS point data for lat: $latitude, lon: $longitude")
        return try {
            val response = nwsApi.getPointData(latitude, longitude)
            Log.d(TAG, "NWS API Point Data Response: $response")
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching NWS point data", e)
            null
        }
    }

    /**
     * Fetches latitude and longitude for a given zip code using the Google Geocoding API.
     */
    suspend fun getCoordinatesFromZipCode(zipCode: String, apiKey: String): GeocodingResponse? {
        Log.i(TAG, "Attempting to geocode zip code: $zipCode")
        if (apiKey.isBlank()) {
            Log.e(TAG, "Google Maps API Key is BLANK. Geocoding will fail.")
        } else {
            Log.d(TAG, "Using Google Maps API Key ending in: ...${apiKey.takeLast(4)}")
        }

        return try {
            val response = googleApi.getCoordsFromZip(zipCode, apiKey)
            Log.d(TAG, "Geocoding API Raw Response: $response")
            if (response.status != "OK") {
                Log.w(TAG, "Geocoding API returned non-OK status: ${response.status}")
            }
            if (response.results.isEmpty()) {
                Log.w(TAG, "Geocoding API returned zero results for zip code: $zipCode")
            }
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching coordinates from zip code", e)
            null
        }
    }

    /**
     * Fetches active NWS alerts for a given state.
     *
     * @param stateCode The 2-letter state code (e.g., "FL").
     * @return An [NwsAlertsResponse] object containing active alerts, or null on error.
     */
    suspend fun getActiveAlerts(stateCode: String): NwsAlertsResponse? {
        Log.i(TAG, "Fetching active alerts for state: $stateCode")
        return try {
            val response = nwsApi.getActiveAlerts(area = stateCode)
            Log.d(TAG, "NWS Active Alerts Response: $response")
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching active alerts for state $stateCode", e)
            null
        }
    }

    /**
     * Fetches the 4-letter WFO (Weather Forecast Office) identifier for a given location,
     * prepending "K" as required for radar image URLs.
     *
     * @param latitude The latitude of the point.
     * @param longitude The longitude of the point.
     * @return The 4-letter WFO identifier (e.g., "KTBW"), or null if not found.
     */
    suspend fun getRadarWfo(latitude: Double, longitude: Double): String? {
        Log.i(TAG, "Fetching WFO for radar for lat: $latitude, lon: $longitude")
        return try {
            val pointData = nwsApi.getPointData(latitude, longitude)
            val gridId = pointData.properties?.gridId
            if (gridId != null) {
                val radarWfo = "K$gridId"
                Log.d(TAG, "Found Radar WFO: $radarWfo")
                radarWfo
            } else {
                Log.w(TAG, "Grid ID (WFO) not found for lat: $latitude, lon: $longitude")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching WFO for radar for lat: $latitude, lon: $longitude", e)
            null
        }
    }

    /**
     * Fetches a list of available weather product types for a given WFO.
     *
     * @param officeCode The 3-letter WFO identifier (e.g., "TBW").
     * @return An [NwsProductListResponse] object containing the list of products, or an empty object on error.
     */
    suspend fun getAvailableProductTypes(officeCode: String): NwsProductListResponse { // MODIFIED parameter name
        Log.i(TAG, "Fetching available product types for WFO: $officeCode")
        return try {
            val response = nwsApi.getProductTypes(officeCode)
            Log.d(TAG, "Available Product Types Response: $response")
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching available product types for WFO $officeCode", e)
            NwsProductListResponse(emptyList()) // Return an empty response on error
        }
    }

    /**
     * Fetches the latest content for a specific NWS product.
     *
     * @param productCode The product code (e.g., "AFD").
     * @param officeCode The 3-letter WFO identifier.
     * @return An [NwsProductDetailResponse] object, or null if not found or an error occurs.
     */
    suspend fun getLatestProduct(productCode: String, officeCode: String): NwsProductDetailResponse? { // MODIFIED parameter name
        Log.i(TAG, "Fetching latest product: $productCode for WFO: $officeCode")
        return try {
            val response = nwsApi.getLatestProduct(productCode, officeCode)
            Log.d(TAG, "Latest Product Response: $response")
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching latest product $productCode for WFO $officeCode", e)
            null
        }
    }

    /**
     * Fetches the raw XML GetCapabilities response from the NCEP GeoServer for a given WFO.
     *
     * @param officeCode The 4-letter WFO identifier (e.g., "ktbw"). This should be lowercase.
     * @return The raw XML string, or null on failure.
     */
    suspend fun getCapabilitiesXml(officeCode: String): String? {
        Log.i(TAG, "Fetching WMS GetCapabilities for office: $officeCode")
        return try {
            // Pass the officeCode directly to the Retrofit endpoint,
            // as it should already be in the correct lowercase 'ktbw' format from MainViewModel.
            val response = ncepGeoServerApi.getWmsCapabilities(officeCode)
            Log.d(TAG, "WMS GetCapabilities Response (partial): ${response.take(200)}...") // Log first 200 chars
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching WMS GetCapabilities for office $officeCode", e)
            null
        }
    }

    /**
     * Parses the GetCapabilities XML to find the latest default timestamp for a specific radar layer.
     *
     * @param officeCode The 4-letter WFO identifier (e.g., "ktbw"). This should be lowercase.
     * @param layerName The full layer name (e.g., "ktbw_sr_bref").
     * @return The latest ISO 8601 timestamp string, or null if not found.
     */
    suspend fun getLatestRadarTimestamp(officeCode: String, layerName: String): String? {
        // Pass the officeCode directly to getCapabilitiesXml without further lowercasing.
        // It is assumed that officeCode is already in the correct lowercase 4-letter format (e.g., "ktbw")
        // when this function is called from MainViewModel.
        val xmlString = getCapabilitiesXml(officeCode)
        if (xmlString == null) {
            Log.w(TAG, "Could not get GetCapabilities XML for $officeCode.")
            return null
        }

        // Simple string parsing to find the default timestamp for the specific layer
        // This is a brittle approach and assumes a very specific XML structure.
        // A proper XML parser (e.g., XmlPullParser) would be more robust for complex XML.
        try {
            val layerTag = "<Name>$layerName</Name>"
            val layerStartIndex = xmlString.indexOf(layerTag)
            if (layerStartIndex == -1) {
                Log.w(TAG, "Layer '$layerName' not found in GetCapabilities for $officeCode.")
                return null
            }

            // Search within the found layer block for the time dimension
            val layerEndIndex = xmlString.indexOf("</Layer>", layerStartIndex)
            if (layerEndIndex == -1) {
                Log.w(TAG, "Malformed Layer tag for '$layerName' in GetCapabilities.")
                return null
            }

            val layerContent = xmlString.substring(layerStartIndex, layerEndIndex)
            val dimensionTag = "<Dimension name=\"time\""
            val dimensionStartIndex = layerContent.indexOf(dimensionTag)
            if (dimensionStartIndex == -1) {
                Log.w(TAG, "Time dimension not found for layer '$layerName' in GetCapabilities.")
                return null
            }

            val defaultAttr = "default=\""
            val defaultStartIndex = layerContent.indexOf(defaultAttr, dimensionStartIndex)
            if (defaultStartIndex == -1) {
                Log.w(TAG, "Default time attribute not found for layer '$layerName'.")
                return null
            }

            val valueStartIndex = defaultStartIndex + defaultAttr.length
            val valueEndIndex = layerContent.indexOf("\"", valueStartIndex)
            if (valueEndIndex == -1) {
                Log.w(TAG, "Malformed default time attribute for layer '$layerName'.")
                return null
            }

            val timestamp = layerContent.substring(valueStartIndex, valueEndIndex)
            Log.d(TAG, "Found latest radar timestamp for $layerName: $timestamp")
            return timestamp
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing GetCapabilities XML for timestamp: ${e.message}", e)
            return null
        }
    }
}
