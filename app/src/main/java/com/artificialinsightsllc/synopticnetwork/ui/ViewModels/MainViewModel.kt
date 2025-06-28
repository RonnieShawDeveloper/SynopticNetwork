package com.artificialinsightsllc.synopticnetwork.ui.viewmodels

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artificialinsightsllc.synopticnetwork.data.models.Comment
import com.artificialinsightsllc.synopticnetwork.data.models.MapReport
import com.artificialinsightsllc.synopticnetwork.data.models.Report
import com.artificialinsightsllc.synopticnetwork.data.services.AuthService
import com.artificialinsightsllc.synopticnetwork.data.services.ReportService
import com.artificialinsightsllc.synopticnetwork.data.services.UserService
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Data class to hold the entire state of the map screen.
 */
data class MapState(
    val isLoading: Boolean = true,
    val currentLocation: LatLng? = null,
    val mapProperties: MapProperties = MapProperties(mapType = MapType.NORMAL),
    val reports: List<MapReport> = emptyList(),
    val selectedReport: Report? = null,
    val comments: List<Comment> = emptyList(),
    val isLoadingComments: Boolean = false,
    val currentUserScreenName: String? = null,
    val reportTypeFilters: Map<String, Boolean> = initialFilters
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
    val mapState = _mapState.asStateFlow()

    private val reportService = ReportService()
    private val authService = AuthService()
    private val userService = UserService()

    private var commentsListenerJob: Job? = null

    init {
        listenForMapReports()
        fetchCurrentUser()
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

    fun onMapReady(context: Context) {
        viewModelScope.launch {
            getCurrentLocation(context) { location ->
                _mapState.update { it.copy(currentLocation = location, isLoading = false) }
            }
        }
    }

    private fun listenForMapReports() {
        viewModelScope.launch {
            reportService.listenForMapReports()
                .catch { e -> e.printStackTrace() }
                .collect { reports ->
                    _mapState.update { it.copy(reports = reports) }
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
}
