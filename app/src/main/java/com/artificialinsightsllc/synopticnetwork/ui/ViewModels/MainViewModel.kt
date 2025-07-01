package com.artificialinsightsllc.synopticnetwork.ui.viewmodels

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artificialinsightsllc.synopticnetwork.data.models.AlertFeature
import com.artificialinsightsllc.synopticnetwork.data.models.AlertSeverity
import com.artificialinsightsllc.synopticnetwork.data.models.Comment
import com.artificialinsightsllc.synopticnetwork.data.models.MapReport
import com.artificialinsightsllc.synopticnetwork.data.models.Report
import com.artificialinsightsllc.synopticnetwork.data.services.AuthService
import com.artificialinsightsllc.synopticnetwork.data.services.NwsApiService
import com.artificialinsightsllc.synopticnetwork.data.services.ReportService
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat // Import SimpleDateFormat
import java.util.Date // Import Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.cos
import kotlin.math.sin
import ch.hsr.geohash.GeoHash
import com.artificialinsightsllc.synopticnetwork.BuildConfig
import com.artificialinsightsllc.synopticnetwork.data.services.UserService

// Define minimum and maximum zoom levels for our custom behavior
private const val MIN_SPREAD_ZOOM = 16f
private const val MAX_SPREAD_ZOOM = 18f
private const val GEOHASH_PRECISION_FOR_GROUPING = 7 // Street-level precision (35 bits)
private const val GEOHASH_PRECISION_FOR_AREA_LOADING = 3 // Area-level precision (15 bits)
private const val RADAR_POLLING_INTERVAL_SECONDS = 300L // Poll every 5 minutes (300 seconds)

// Sealed class to represent different types of markers displayed on the map
sealed class DisplayMarker {
    // Represents an individual report marker, with an optional groupCenterLatLng if it was spread
    data class IndividualReport(val report: MapReport, val displayLatLng: LatLng, val groupCenterLatLng: LatLng? = null) : DisplayMarker()
    // Represents a group of reports at a specific geohash location
    data class GroupMarker(val geohash: String, val count: Int, val centerLatLng: LatLng) : DisplayMarker()
    // Represents the central marker for a spread group
    data class SpreadCenter(val centerLatLng: LatLng, val geohash: String) : DisplayMarker()
}

// Wrapper data class for alerts to include local status
data class DisplayAlert(
    val alert: AlertFeature,
    val isLocal: Boolean // True if this alert affects the user's current zone
)

/**
 * Data class to hold the entire state of the map screen, including alert data.
 */
data class MapState(
    val isLoading: Boolean = true,
    val currentLocation: LatLng? = null,
    val mapProperties: MapProperties = MapProperties(mapType = MapType.NORMAL),
    val rawReports: List<MapReport> = emptyList(), // Store the raw, unfiltered reports from Firestore
    val displayedMarkers: List<DisplayMarker> = emptyList(), // The list of markers actually shown on map
    val selectedReport: Report? = null, // Used for the individual report bottom sheet
    val selectedGroupedFullReports: List<Report> = emptyList(), // Used for the grouped reports dialog
    val comments: List<Comment> = emptyList(),
    val isLoadingComments: Boolean = false,
    val currentUserScreenName: String? = null,
    val reportTypeFilters: Map<String, Boolean> = initialFilters,
    val activeAlerts: List<DisplayAlert> = emptyList(),
    val alertsLoading: Boolean = false,
    val highestSeverity: AlertSeverity = AlertSeverity.NONE,
    val radarWfo: String? = null,
    val currentMapZoom: Float = 10f,
    val userGeohash3Char: String? = null, // User's 3-char geohash for local report loading
    val userStateCode: String? = null, // User's 2-letter state code (e.g., "FL")
    val userForecastZone: String? = null, // User's NWS forecast zone (e.g., "FLZ151")
    val latestRadarTimestamp: String? = null, // Latest available radar timestamp from GetCapabilities
    val isReflectivityRadarActive: Boolean = false, // State for reflectivity radar overlay
    val isVelocityRadarActive: Boolean = false, // State for velocity radar overlay
    val lastRadarUpdateTimeString: String? = null // NEW: Formatted string for last radar update time
)

// Initialize the filters with all types set to true (visible)
private val initialFilters: Map<String, Boolean> = mapOf(
    "Tornado" to true, "Funnel Cloud" to true, "Wall Cloud" to true,
    "Shelf Cloud" to true, "Waterspout" to true, "Wind Damage" to true,
    "Hail" to true, "Frequent Lightning" to true, "Flooding" to true,
    "Coastal Flooding" to true, "River Flooding" to true, "Freezing Rain / Ice" to true,
    "Sleet" to true, "Snow" to true, "Dense Fog" to true,
    "Wildfire Smoke / Haze" to true, "Dust Storm" to true, "Severe Weather" to true,
    "Other" to true
)

/**
 * ViewModel for the MainScreen.
 */
class MainViewModel : ViewModel() {

    private val _mapState = MutableStateFlow(MapState())
    val uiState = _mapState.asStateFlow()

    private val reportService = ReportService()
    private val authService = AuthService()
    private val userService = UserService()
    private val nwsApiService = NwsApiService()

    private var commentsListenerJob: Job? = null
    private var alertsPollingJob: Job? = null
    private var reportsListenerJob: Job? = null // To manage the reports listener
    private var radarPollingJob: Job? = null // To manage the radar polling listener

    init {
        // Fetch user profile (only screen name for now)
        fetchCurrentUserProfile()
        // The reports listener will be started once currentLocation is available via onMapReady
    }

    private fun fetchCurrentUserProfile() {
        viewModelScope.launch {
            val userId = authService.getCurrentUserId()
            if (userId != null) {
                val user = userService.getUserProfile(userId)
                _mapState.update {
                    it.copy(
                        currentUserScreenName = user?.screenName,
                        userForecastZone = user?.zone
                    )
                }
            } else {
                _mapState.update { it.copy(isLoading = false) } // Ensure loading state is false if no user
            }
        }
    }


    /**
     * Called when the map is ready and location permissions are granted.
     * Fetches current location and starts alert polling.
     */
    fun onMapReady(context: Context) {
        viewModelScope.launch {
            val location = getCurrentLocation(context)
            if (location == null) {
                Log.e("MainViewModel", "Could not get current location.")
                _mapState.update { it.copy(isLoading = false) }
                return@launch
            }

            // Calculate 3-char geohash from current location
            val userGeohash3Char = GeoHash.withBitPrecision(location.latitude, location.longitude, GEOHASH_PRECISION_FOR_AREA_LOADING * 5).toBase32()

            // Determine user's state code and WFO/Zone
            val pointData = nwsApiService.getNwsPointData(location.latitude, location.longitude)
            val userStateCode = pointData?.properties?.forecastZone
                ?.substringAfterLast("/")
                ?.substring(0, 2)
                ?.uppercase(Locale.US)

            val userForecastZone = pointData?.properties?.forecastZone?.substringAfterLast("/")

            // Fetch radar WFO (still based on user's current location for relevant radar)
            val radarWfo = location.let {
                nwsApiService.getRadarWfo(it.latitude, it.longitude)
            }

            _mapState.update {
                it.copy(
                    currentLocation = location,
                    isLoading = false,
                    currentMapZoom = 10f,
                    userGeohash3Char = userGeohash3Char,
                    userStateCode = userStateCode,
                    userForecastZone = userForecastZone,
                    radarWfo = radarWfo // Set the radar WFO
                )
            }

            // Start listening for reports based on current location's geohash
            listenForMapReports(userGeohash3Char)
            // Start polling for alerts once we have a state code
            if (userStateCode != null) {
                startAlertsPolling(userStateCode, userForecastZone)
            } else {
                Log.e("MainViewModel", "Could not determine user's state code for alerts.")
                _mapState.update { it.copy(alertsLoading = false) }
            }

            // Initial fetch of radar timestamp (without starting polling yet)
            // This ensures that if a radar overlay is turned on immediately,
            // it has some initial data.
            if (radarWfo != null) {
                val reflectivityLayerName = "${radarWfo.lowercase(Locale.US)}_sr_bref"
                val initialTimestamp = nwsApiService.getLatestRadarTimestamp(radarWfo.lowercase(Locale.US), reflectivityLayerName)
                _mapState.update {
                    it.copy(
                        latestRadarTimestamp = initialTimestamp,
                        lastRadarUpdateTimeString = initialTimestamp?.let { ts -> formatTimestampForDisplay(ts) }
                    )
                }
            }
            // Polling will be managed by onReflectivityRadarToggled/onVelocityRadarToggled
        }
    }

    /**
     * Listens for real-time updates to reports, filtered by the user's 3-char Geohash.
     */
    private fun listenForMapReports(geohash3Char: String?) {
        reportsListenerJob?.cancel() // Cancel any existing listener
        reportsListenerJob = viewModelScope.launch {
            reportService.listenForMapReports(geohash3Char)
                .catch { e -> e.printStackTrace() }
                .collect { mapReports ->
                    _mapState.update { currentState ->
                        currentState.copy(rawReports = mapReports)
                    }
                    // Trigger update of displayed markers whenever raw reports change
                    this@MainViewModel.updateDisplayedMarkers()
                }
        }
    }


    /**
     * Called when the map's zoom level changes.
     * This will trigger re-evaluation of marker positions for spreading/grouping.
     */
    fun onMapZoomChanged(newZoom: Float) {
        // Only update if the zoom level has actually changed to avoid unnecessary re-renders
        if (_mapState.value.currentMapZoom != newZoom) {
            _mapState.update { it.copy(currentMapZoom = newZoom) }
            // Trigger update of displayed markers whenever zoom changes
            this@MainViewModel.updateDisplayedMarkers()
        }
    }

    /**
     * Orchestrates the grouping and spreading logic based on the current zoom level
     * and updates the `displayedMarkers` in `MapState`.
     */
    private fun updateDisplayedMarkers(): List<DisplayMarker> {
        val currentState = _mapState.value
        // Filter reports based on the user's selected report type filters
        val filteredReports = currentState.rawReports.filter { report ->
            currentState.reportTypeFilters[report.reportType] ?: true // Default to true if filter not set
        }

        // Group by Geohash to manage proximity
        val reportsByGeohash = filteredReports.groupBy { it.geohash }

        val displayedMarkers = mutableListOf<DisplayMarker>()

        // Logic based on zoom level:
        when {
            // High zoom: Spread out markers that share the same Geohash
            currentState.currentMapZoom >= MIN_SPREAD_ZOOM -> {
                reportsByGeohash.forEach { (geohash, geohashReports) ->
                    if (geohashReports.size > 1) {
                        // More than one report in this Geohash, apply spreading algorithm
                        val centerLatLng = calculateCenter(geohashReports)
                        if (centerLatLng != null) {
                            val spreadReports = spreadMarkersAroundCenter(geohashReports, centerLatLng)

                            // Add the central marker for this group
                            displayedMarkers.add(DisplayMarker.SpreadCenter(centerLatLng, geohash))

                            // Add individual spread reports, linking them to their center
                            displayedMarkers.addAll(spreadReports.map {
                                DisplayMarker.IndividualReport(it, it.location!!.toLatLng(), centerLatLng)
                            })
                        } else {
                            // Fallback: if centerLatLng is null (e.g., no valid locations), display individual reports
                            geohashReports.forEach { report ->
                                report.location?.toLatLng()?.let { latLng ->
                                    displayedMarkers.add(DisplayMarker.IndividualReport(report, latLng, null))
                                }
                            }
                        }
                    } else {
                        // Only one report in this Geohash, display as is
                        geohashReports.first().let { report ->
                            report.location?.toLatLng()?.let { latLng ->
                                displayedMarkers.add(DisplayMarker.IndividualReport(report, latLng, null)) // No group center
                            }
                        }
                    }
                }
            }
            // Low zoom: Group markers by Geohash
            currentState.currentMapZoom < MIN_SPREAD_ZOOM -> {
                reportsByGeohash.forEach { (geohash, geohashReports) ->
                    val centerLatLng = calculateCenter(geohashReports)
                    if (centerLatLng != null) {
                        displayedMarkers.add(
                            DisplayMarker.GroupMarker(
                                geohash = geohash,
                                count = geohashReports.size,
                                centerLatLng = centerLatLng
                            )
                        )
                    }
                }
            }
        }
        return displayedMarkers
    }

    /**
     * Calculates the geometric center of a list of MapReports.
     */
    private fun calculateCenter(reports: List<MapReport>): LatLng? {
        if (reports.isEmpty()) return null
        var sumLat = 0.0
        var sumLon = 0.0
        var validCount = 0
        reports.forEach { report ->
            report.location?.let {
                sumLat += it.latitude
                sumLon += it.longitude
                validCount++
            }
        }
        return if (validCount > 0) LatLng(sumLat / validCount, sumLon / validCount) else null
    }

    /**
     * Converts GeoPoint to LatLng. This helper might already exist, but adding it here for clarity.
     */
    private fun GeoPoint.toLatLng(): LatLng {
        return LatLng(this.latitude, this.longitude)
    }

    /**
     * Calculates new LatLng positions to spread out a list of reports around their geometric center.
     * This uses a simple circular spreading algorithm.
     */
    private fun spreadMarkersAroundCenter(reports: List<MapReport>, centerLatLon: LatLng): List<MapReport> {
        if (reports.isEmpty()) return emptyList()
        if (reports.size == 1) return reports // No need to spread a single marker

        val spreadReports = mutableListOf<MapReport>()
        val baseRadiusDegrees = 0.0005 // A small degree offset (approx 50m at equator), adjust as needed
        val angleIncrement = 360.0 / reports.size

        reports.forEachIndexed { index, report ->
            val angleRad = Math.toRadians(index * angleIncrement)
            // Calculate offset based on current marker count to make sure radius is dynamic
            val currentRadius = baseRadiusDegrees * (1 + (reports.size * 0.1).toFloat()) // Slight increase with more markers

            // Simple trig to find new lat/lon
            val newLat = centerLatLon.latitude + currentRadius * sin(angleRad)
            val newLon = centerLatLon.longitude + currentRadius * cos(angleRad)

            // Create a new MapReport with the adjusted location (GeoPoint)
            report.copy(location = GeoPoint(newLat, newLon)).also { spreadReports.add(it) }
        }
        return spreadReports
    }


    /**
     * Starts a coroutine job to periodically fetch active NWS alerts and radar WFO.
     */
    private fun startAlertsPolling(stateCode: String, userForecastZone: String?) {
        alertsPollingJob?.cancel() // Cancel any existing polling job
        alertsPollingJob = viewModelScope.launch {
            while (true) {
                _mapState.update { it.copy(alertsLoading = true) }
                try {
                    // Fetch active alerts for the entire state
                    val alertsResponse = nwsApiService.getActiveAlerts(stateCode)
                    val rawAlerts = alertsResponse?.features ?: emptyList()

                    // Process alerts to determine if they are local
                    val processedAlerts = rawAlerts.map { alertFeature ->
                        val isLocal = userForecastZone != null && alertFeature.properties.UGC?.contains(userForecastZone) == true
                        DisplayAlert(alertFeature, isLocal)
                    }

                    // Determine the highest severity among *all* fetched alerts
                    val highestSeverity = processedAlerts
                        .sortedByDescending { AlertSeverity.fromString(it.alert.properties.severity).level }
                        .firstOrNull()?.let { AlertSeverity.fromString(it.alert.properties.severity) }
                        ?: AlertSeverity.NONE

                    _mapState.update {
                        it.copy(
                            activeAlerts = processedAlerts,
                            highestSeverity = highestSeverity,
                            alertsLoading = false
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _mapState.update { it.copy(alertsLoading = false) }
                }
                delay(60 * 1000L) // Poll every minute (60 seconds)
            }
        }
    }

    /**
     * Manages the lifecycle of the radar polling job based on whether any radar overlay is active.
     */
    private fun manageRadarPollingJob() {
        val currentState = _mapState.value
        val shouldPoll = currentState.isReflectivityRadarActive || currentState.isVelocityRadarActive
        val radarWfo = currentState.radarWfo

        if (shouldPoll && radarWfo != null) {
            // Start polling if it's not already running and a radar WFO is available
            if (radarPollingJob == null || radarPollingJob?.isActive == false) {
                Log.d("MainViewModel", "Starting radar polling.")
                startRadarPolling(radarWfo)
            }
        } else {
            // Stop polling if no radar overlay is active or WFO is null
            if (radarPollingJob?.isActive == true) {
                Log.d("MainViewModel", "Stopping radar polling.")
                radarPollingJob?.cancel()
                radarPollingJob = null // Clear the job
                // Clear the last update time string when polling stops
                _mapState.update { it.copy(lastRadarUpdateTimeString = null) }
            }
        }
    }

    /**
     * The actual coroutine for periodically fetching the latest radar timestamp.
     * This will trigger map tile overlay updates if the timestamp changes.
     */
    private fun startRadarPolling(radarWfo: String) {
        radarPollingJob = viewModelScope.launch {
            while (true) {
                try {
                    // Fetch latest radar timestamp for reflectivity layer
                    val reflectivityLayerName = "${radarWfo.lowercase(Locale.US)}_sr_bref"
                    val newReflectivityTimestamp = nwsApiService.getLatestRadarTimestamp(radarWfo.lowercase(Locale.US), reflectivityLayerName)

                    // Fetch latest radar timestamp for velocity layer (assuming a similar naming convention)
                    val velocityLayerName = "${radarWfo.lowercase(Locale.US)}_sr_bvel"
                    val newVelocityTimestamp = nwsApiService.getLatestRadarTimestamp(radarWfo.lowercase(Locale.US), velocityLayerName)

                    // Only update the state if the timestamp is newer or different
                    _mapState.update { currentState ->
                        var updatedTimestamp = currentState.latestRadarTimestamp
                        var updatedTimeString: String? = currentState.lastRadarUpdateTimeString

                        // For simplicity, we'll just use the reflectivity timestamp as the primary one
                        // for the general 'latestRadarTimestamp' in MapState.
                        // The RadarTileProvider will use the specific layer's timestamp internally.
                        if (newReflectivityTimestamp != null && newReflectivityTimestamp != currentState.latestRadarTimestamp) {
                            updatedTimestamp = newReflectivityTimestamp
                            updatedTimeString = formatTimestampForDisplay(newReflectivityTimestamp)
                            Log.d("MainViewModel", "New radar timestamp detected: $newReflectivityTimestamp, formatted: $updatedTimeString")
                        }
                        currentState.copy(
                            latestRadarTimestamp = updatedTimestamp,
                            lastRadarUpdateTimeString = updatedTimeString
                        )
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error polling for radar timestamp: ${e.message}", e)
                }
                delay(RADAR_POLLING_INTERVAL_SECONDS * 1000L) // Poll every X seconds
            }
        }
    }

    /**
     * Helper function to format ISO 8601 timestamps into a readable "Last Updated: HH:MM AM/PM" format.
     */
    private fun formatTimestampForDisplay(isoTimestamp: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
            val formatter = SimpleDateFormat("hh:mm a", Locale.US) // hh:MM AM/PM
            "Last Updated: ${formatter.format(parser.parse(isoTimestamp) ?: Date())}"
        } catch (e: Exception) {
            e.printStackTrace()
            "Last Updated: N/A"
        }
    }

    /**
     * Toggles the state of the reflectivity radar overlay and manages polling.
     */
    fun onReflectivityRadarToggled(newValue: Boolean) {
        _mapState.update { it.copy(isReflectivityRadarActive = newValue) }
        manageRadarPollingJob()
    }

    /**
     * Toggles the state of the velocity radar overlay and manages polling.
     */
    fun onVelocityRadarToggled(newValue: Boolean) {
        _mapState.update { it.copy(isVelocityRadarActive = newValue) }
        manageRadarPollingJob()
    }


    /**
     * Handles a click on an individual report marker.
     */
    fun onMarkerClicked(report: MapReport?) {
        commentsListenerJob?.cancel()
        if (report == null) {
            _mapState.update { it.copy(selectedReport = null, comments = emptyList()) }
            return
        }
        _mapState.update { it.copy(isLoadingComments = true, selectedReport = null) }
        viewModelScope.launch {
            val fullReport = reportService.getReportDetails(report.reportId)
            _mapState.update { it.copy(selectedReport = fullReport) }
            commentsListenerJob = launch {
                reportService.listenForComments(report.reportId)
                    .catch { e -> e.printStackTrace() }
                    .collect { comments ->
                        _mapState.update { it.copy(comments = comments, isLoadingComments = false) }
                    }
            }
        }
    }

    /**
     * Handles a click on a group marker. Fetches all reports belonging to that geohash
     * and provides them via a callback after fetching their full details.
     */
    fun onGroupMarkerClicked(geohash: String) {
        viewModelScope.launch {
            // Clear previous grouped reports instantly for better UX
            _mapState.update { it.copy(selectedGroupedFullReports = emptyList()) }

            val reportsInGroupMapReports = _mapState.value.rawReports.filter { it.geohash == geohash }
            val fullReports = reportsInGroupMapReports.mapNotNull { mapReport ->
                reportService.getReportDetails(mapReport.reportId)
            }
            _mapState.update { it.copy(selectedGroupedFullReports = fullReports) }
        }
    }


    fun addComment(reportId: String, commentText: String) {
        viewModelScope.launch {
            val userId = authService.getCurrentUserId()
            val screenName = _mapState.value.currentUserScreenName
            if (userId != null && screenName != null) {
                val comment = Comment(
                    reportId = reportId, userId = userId,
                    screenName = screenName, text = commentText
                )
                reportService.addComment(reportId, comment)
            }
        }
    }

    fun onFilterChanged(reportType: String, isVisible: Boolean) {
        _mapState.update { state ->
            val updatedFilters = state.reportTypeFilters.toMutableMap()
            updatedFilters[reportType] = isVisible
            state.copy(reportTypeFilters = updatedFilters)
        }
        // Re-process markers when filters change
        this@MainViewModel.updateDisplayedMarkers()
    }

    fun onMapTypeChanged(mapType: MapType) {
        _mapState.update { it.copy(mapProperties = it.mapProperties.copy(mapType = mapType)) }
    }

    /**
     * Suspends until the current location is fetched or an error occurs.
     * @param context The application context.
     * @return The LatLng of the current location, or null if fetching fails.
     */
    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(context: Context): LatLng? =
        suspendCancellableCoroutine { continuation ->
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        continuation.resume(LatLng(location.latitude, location.longitude))
                    } else {
                        Log.w("MainViewModel", "Last location is null.")
                        continuation.resume(null)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MainViewModel", "Error fetching current location", e)
                    continuation.resumeWithException(e)
                }
        }

    override fun onCleared() {
        super.onCleared()
        alertsPollingJob?.cancel()
        commentsListenerJob?.cancel()
        reportsListenerJob?.cancel()
        radarPollingJob?.cancel() // Ensure radar polling job is cancelled
    }
}
