package com.artificialinsightsllc.synopticnetwork.data.services

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import kotlinx.coroutines.tasks.await

/**
 * A service class to encapsulate all Firebase Remote Config operations.
 */
class RemoteConfigService {

    private val remoteConfig = Firebase.remoteConfig

    init {
        // Set configuration settings for Remote Config.
        // During development, a low minimum fetch interval is useful for testing.
        // For a production app, this should be set to a higher value like 12 hours.
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (com.artificialinsightsllc.synopticnetwork.BuildConfig.DEBUG) 60 else 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
    }

    /**
     * Fetches the latest values from the Remote Config server and activates them.
     */
    suspend fun fetchAndActivate() {
        try {
            remoteConfig.fetchAndActivate().await()
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle exceptions, e.g., network issues
        }
    }

    /**
     * Retrieves the Google Maps API key from Remote Config.
     *
     * @return The API key as a String, or an empty string if not found.
     */
    fun getGoogleMapsApiKey(): String {
        return remoteConfig.getString("google_map_api")
    }
}
