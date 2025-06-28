package com.artificialinsightsllc.synopticnetwork.ui.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artificialinsightsllc.synopticnetwork.BuildConfig
import com.artificialinsightsllc.synopticnetwork.data.models.User
import com.artificialinsightsllc.synopticnetwork.data.services.AuthResult
import com.artificialinsightsllc.synopticnetwork.data.services.AuthService
import com.artificialinsightsllc.synopticnetwork.data.services.NwsApiService
import com.artificialinsightsllc.synopticnetwork.data.services.UserService
import kotlinx.coroutines.*

// Represents the availability state of a screen name
enum class ScreenNameState {
    IDLE, // Not yet checked
    CHECKING, // Currently checking
    AVAILABLE,
    TAKEN
}

// Represents the overall state of the registration process
sealed class SignUpState {
    object Idle : SignUpState()
    object Loading : SignUpState()
    object Success : SignUpState()
    data class Error(val message: String) : SignUpState()
}

class SignUpViewModel(
    private val authService: AuthService = AuthService(),
    private val userService: UserService = UserService(),
    private val nwsApiService: NwsApiService = NwsApiService()
    // RemoteConfigService is no longer needed here
) : ViewModel() {

    private val _signUpState = mutableStateOf<SignUpState>(SignUpState.Idle)
    val signUpState: State<SignUpState> = _signUpState

    private val _screenNameState = mutableStateOf(ScreenNameState.IDLE)
    val screenNameState: State<ScreenNameState> = _screenNameState

    private var screenNameJob: Job? = null

    /**
     * Checks the availability of a screen name with a debounce to prevent excessive checks.
     */
    fun onScreenNameChanged(screenName: String) {
        _screenNameState.value = ScreenNameState.CHECKING
        screenNameJob?.cancel() // Cancel any previous check
        screenNameJob = viewModelScope.launch {
            delay(500) // Debounce for 500ms
            if (screenName.length >= 3) {
                val isTaken = userService.isScreenNameTaken(screenName)
                _screenNameState.value = if (isTaken) ScreenNameState.TAKEN else ScreenNameState.AVAILABLE
            } else {
                _screenNameState.value = ScreenNameState.IDLE
            }
        }
    }

    /**
     * The main function to orchestrate the entire user registration process.
     */
    fun onSignUpClicked(user: User, password: String) {
        viewModelScope.launch {
            _signUpState.value = SignUpState.Loading

            // 1. Create the user in Firebase Auth
            when (val authResult = authService.createUser(user.email, password)) {
                is AuthResult.Success -> {
                    // 2. Auth creation successful, now enrich and save the profile
                    val userWithId = user.copy(userId = authResult.uid)
                    enrichAndSaveUserProfile(userWithId)
                }
                is AuthResult.Error -> {
                    // Auth creation failed. Show the friendly error message.
                    _signUpState.value = SignUpState.Error(authResult.message)
                }
            }
        }
    }

    private suspend fun enrichAndSaveUserProfile(user: User) {
        // Get the API key securely from the auto-generated BuildConfig file.
        val apiKey = BuildConfig.GOOGLE_MAPS_API_KEY
        if (apiKey.isBlank()) {
            _signUpState.value = SignUpState.Error("Could not retrieve necessary API keys. Please try again later.")
            return
        }

        // 1. Get Lat/Lon from Zip Code
        val geocodingResponse = nwsApiService.getCoordinatesFromZipCode(user.zipCode, apiKey)
        val location = geocodingResponse?.results?.firstOrNull()?.geometry?.location
        if (location == null) {
            authService.deleteCurrentUser() // Clean up auth user
            _signUpState.value = SignUpState.Error("Could not validate the provided Zip Code. Please check it and try again.")
            return
        }

        // 2. Get WFO/Zone from Lat/Lon
        val pointData = nwsApiService.getNwsPointData(location.lat, location.lng)
        val wfo = pointData?.properties?.gridId
        val zoneUrl = pointData?.properties?.forecastZone
        val zone = zoneUrl?.substringAfterLast("/") // Extract zone ID from URL

        if (wfo == null || zone == null) {
            authService.deleteCurrentUser() // Clean up auth user
            _signUpState.value = SignUpState.Error("Could not determine the NWS forecast zone for your location. Please try again later.")
            return
        }

        // 3. Create the final user object and save to Firestore
        val finalUser = user.copy(wfo = wfo, zone = zone)
        val profileCreated = userService.createUserProfile(finalUser)

        if (profileCreated) {
            _signUpState.value = SignUpState.Success
        } else {
            authService.deleteCurrentUser() // Clean up auth user
            _signUpState.value = SignUpState.Error("A database error occurred while creating your profile. Please try again.")
        }
    }

    /**
     * Resets the sign-up state back to Idle, for example, after an error dialog is dismissed.
     */
    fun resetState() {
        _signUpState.value = SignUpState.Idle
    }
}
