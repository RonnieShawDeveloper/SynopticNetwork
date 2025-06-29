package com.artificialinsightsllc.synopticnetwork.ui.viewmodels

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artificialinsightsllc.synopticnetwork.data.models.AlertFeature
import com.artificialinsightsllc.synopticnetwork.data.models.AlertSeverity
import com.artificialinsightsllc.synopticnetwork.data.models.Comment
import com.artificialinsightsllc.synopticnetwork.data.models.MapReport
import com.artificialinsightsllc.synopticnetwork.data.models.Report
// Removed import for ReportClusterItem as it's no longer used
import com.artificialinsightsllc.synopticnetwork.data.services.AuthService
import com.artificialinsightsllc.synopticnetwork.data.services.NwsApiService
import com.artificialinsightsllc.synopticnetwork.data.services.ReportService
import com.artificialinsightsllc.synopticnetwork.data.services.UserService
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
// Removed combine flow import as dynamic jittering is removed
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Data class to hold the entire state of the map screen, including alert data.
 */
data class MapState(
    val isLoading: Boolean = true,
    val currentLocation: LatLng? = null,
    val mapProperties: MapProperties = MapProperties(mapType = MapType.NORMAL),
    val reports: List<MapReport> = emptyList(), // Changed from List<ReportClusterItem> to List<MapReport>
    val selectedReport: Report? = null,
    val comments: List<Comment> = emptyList(),
    val isLoadingComments: Boolean = false,
    val currentUserScreenName: String? = null,
    val reportTypeFilters: Map<String, Boolean> = initialFilters,
    val activeAlerts: List<AlertFeature> = emptyList(),
    val alertsLoading: Boolean = false,
    val highestSeverity: AlertSeverity = AlertSeverity.NONE,
    val radarWfo: String? = null
    // Removed currentMapZoom and mapMaxZoomLevel as dynamic jittering is no longer needed
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

    // Removed _rawReports as it's no longer needed for jittering.

    private val reportService = ReportService()
    private val authService = AuthService()
    private val userService = UserService()
    private val nwsApiService = NwsApiService()

    private var commentsListenerJob: Job? = null
    private var alertsPollingJob: Job? = null

    init {
        // Now listening directly for MapReports, no intermediate _rawReports needed for jittering.
        listenForMapReports()
        fetchCurrentUser()

        // Removed the combine flow that handled dynamic jittering based on zoom.
    }

    private fun fetchCurrentUser() {
        viewModelScope.launch {
            val userId = authService.getCurrentUserId()
            if (userId != null) {
                val user = userService.getUserProfile(userId)
                _mapState.update { it.copy(currentUserScreenName = user?.screenName) }
            }
        }
    }

    /**
     * Called when the map is ready and location permissions are granted.
     * Fetches current location and starts alert polling.
     */
    fun onMapReady(context: Context) {
        viewModelScope.launch {
            getCurrentLocation(context) { location ->
                _mapState.update { it.copy(currentLocation = location, isLoading = false) }
                // Start polling for alerts once we have a location
                startAlertsPolling(location)
            }
        }
    }

    /**
     * Listens for real-time updates to reports directly for display.
     * No jittering applied here.
     */
    private fun listenForMapReports() {
        viewModelScope.launch {
            reportService.listenForMapReports()
                .catch { e -> e.printStackTrace() }
                .collect { mapReports ->
                    // Directly update the 'reports' list in MapState with MapReport objects.
                    _mapState.update { currentState ->
                        currentState.copy(reports = mapReports)
                    }
                }
        }
    }

    // Removed onMapZoomChanged and onMapMaxZoomLevelChanged as dynamic jittering is removed.


    /**
     * Starts a coroutine job to periodically fetch active NWS alerts and radar WFO.
     */
    private fun startAlertsPolling(location: LatLng) {
        alertsPollingJob?.cancel() // Cancel any existing polling job
        alertsPollingJob = viewModelScope.launch {
            while (true) {
                _mapState.update { it.copy(alertsLoading = true) }
                try {
                    // Fetch active alerts
                    val alertsResponse = nwsApiService.getActiveAlerts(location.latitude, location.longitude)
                    val activeAlerts = alertsResponse?.features ?: emptyList()

                    // Determine the highest severity
                    val highestSeverity = activeAlerts.maxOfOrNull {
                        AlertSeverity.fromString(it.properties.severity)
                    } ?: AlertSeverity.NONE

                    // Fetch radar WFO
                    val radarWfo = nwsApiService.getRadarWfo(location.latitude, location.longitude)

                    _mapState.update {
                        it.copy(
                            activeAlerts = activeAlerts,
                            highestSeverity = highestSeverity,
                            radarWfo = radarWfo,
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

    fun onMarkerClicked(report: MapReport?) {
        commentsListenerJob?.cancel()
        if (report == null) {
            _mapState.update { it.copy(selectedReport = null, comments = emptyList()) }
            return
        }
        _mapState.update { it.copy(isLoadingComments = true, selectedReport = null) }
        viewModelScope.launch {
            // report.reportId is always available in MapReport
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
    }

    fun onMapTypeChanged(mapType: MapType) {
        _mapState.update { it.copy(mapProperties = it.mapProperties.copy(mapType = mapType)) }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation(context: Context, onLocationFetched: (LatLng) -> Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let { onLocationFetched(LatLng(it.latitude, it.longitude)) }
                    ?: run { _mapState.update { it.copy(isLoading = false) } }
            }
            .addOnFailureListener { _mapState.update { it.copy(isLoading = false) } }
    }

    // Removed applyJitterToOverlappingReports function as clustering is removed.

    override fun onCleared() {
        super.onCleared()
        alertsPollingJob?.cancel()
        commentsListenerJob?.cancel()
    }
}
