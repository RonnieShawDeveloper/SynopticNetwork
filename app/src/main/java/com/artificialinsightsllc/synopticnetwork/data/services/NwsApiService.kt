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
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// Base URLs for the APIs
private const val NWS_API_BASE_URL = "https://api.weather.gov/"
private const val GOOGLE_GEOCODING_API_BASE_URL = "https://maps.googleapis.com/"
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

    // MODIFIED: Changed from 'point' to 'area' and updated urgency parameter
    @GET("alerts/active")
    suspend fun getActiveAlerts(
        @Query("status") status: String = "actual",
        @Query("message_type") messageType: String = "alert,update,cancel",
        @Query("area") area: String, // MODIFIED: Now takes a state code (e.g., "FL")
        @Query("urgency") urgency: String = "Immediate,Expected,Future,Past,Unknown", // MODIFIED: All urgency types
        @Query("severity") severity: String = "Extreme,Severe,Moderate,Minor,Unknown",
        @Query("certainty") certainty: String = "Observed,Likely,Possible,Unlikely,Unknown"
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
                .header("User-Agent", "SynopticNetwork (rdspromo@gmail.00)") // NWS requires this
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

    private val nwsApi: NwsApi = nwsRetrofit.create(NwsApi::class.java)
    private val googleApi: NwsApi = googleRetrofit.create(NwsApi::class.java)

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

    // NEW: Functions for NWS Products

    /**
     * Fetches a list of available weather product types for a given WFO.
     *
     * @param wfo The 3-letter WFO identifier (e.g., "TBW").
     * @return An [NwsProductListResponse] object containing the list of products, or an empty object on error.
     */
    suspend fun getAvailableProductTypes(wfo: String): NwsProductListResponse { // MODIFIED return type
        Log.i(TAG, "Fetching available product types for WFO: $wfo")
        return try {
            val response = nwsApi.getProductTypes(wfo)
            Log.d(TAG, "Available Product Types Response: $response")
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching available product types for WFO $wfo", e)
            NwsProductListResponse(emptyList()) // Return an empty response on error
        }
    }

    /**
     * Fetches the latest content for a specific NWS product.
     *
     * @param productCode The product code (e.g., "AFD").
     * @param wfo The 3-letter WFO identifier.
     * @return An [NwsProductDetailResponse] object, or null if not found or an error occurs.
     */
    suspend fun getLatestProduct(productCode: String, wfo: String): NwsProductDetailResponse? {
        Log.i(TAG, "Fetching latest product: $productCode for WFO: $wfo")
        return try {
            val response = nwsApi.getLatestProduct(productCode, wfo)
            Log.d(TAG, "Latest Product Response: $response")
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching latest product $productCode for WFO $wfo", e)
            null
        }
    }
}
