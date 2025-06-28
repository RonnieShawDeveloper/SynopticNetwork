package com.artificialinsightsllc.synopticnetwork.data.services

import android.util.Log
import com.artificialinsightsllc.synopticnetwork.BuildConfig
import com.artificialinsightsllc.synopticnetwork.data.models.GeocodingResponse
import com.artificialinsightsllc.synopticnetwork.data.models.NwsPointResponse
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
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
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
            Log.d(TAG, "NWS API Response: $response")
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
}
