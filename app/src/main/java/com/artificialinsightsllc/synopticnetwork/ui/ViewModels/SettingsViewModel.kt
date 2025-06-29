package com.artificialinsightsllc.synopticnetwork.ui.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artificialinsightsllc.synopticnetwork.BuildConfig
import com.artificialinsightsllc.synopticnetwork.data.models.ExperienceLevel
import com.artificialinsightsllc.synopticnetwork.data.models.MemberType
import com.artificialinsightsllc.synopticnetwork.data.models.User
import com.artificialinsightsllc.synopticnetwork.data.services.AuthService
import com.artificialinsightsllc.synopticnetwork.data.services.NwsApiService
import com.artificialinsightsllc.synopticnetwork.data.services.UserService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Data class to hold all the mutable state for the Settings screen.
 */
data class SettingsUiState(
    val currentUser: User? = null,
    val screenName: String = "",
    val zipCode: String = "",
    val nwsSpotterId: String = "",
    val hamRadioCallSign: String = "",
    val experienceLevel: String = ExperienceLevel.ENTHUSIAST.name,
    val memberType: String = MemberType.STANDARD.name,
    val isLoading: Boolean = false,
    val showLogoutConfirmation: Boolean = false,
    val showPasswordResetConfirmation: Boolean = false,
    val showSuccessMessage: String? = null,
    val errorMessage: String? = null,
    val screenNameAvailability: ScreenNameState = ScreenNameState.IDLE,
    val isSaving: Boolean = false
)

/**
 * ViewModel for the SettingsScreen, handling user profile management,
 * password reset, and logout functionality.
 */
class SettingsViewModel(
    private val authService: AuthService = AuthService(),
    private val userService: UserService = UserService(),
    private val nwsApiService: NwsApiService = NwsApiService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private var screenNameCheckJob: Job? = null

    init {
        loadUserProfile()
    }

    /**
     * Loads the current user's profile data into the UI state.
     */
    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val userId = authService.getCurrentUserId()
            if (userId != null) {
                val user = userService.getUserProfile(userId)
                if (user != null) {
                    _uiState.update {
                        it.copy(
                            currentUser = user,
                            screenName = user.screenName,
                            zipCode = user.zipCode,
                            nwsSpotterId = user.nwsSpotterId ?: "",
                            hamRadioCallSign = user.hamRadioCallSign ?: "",
                            experienceLevel = user.experienceLevel,
                            memberType = user.memberType,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load user profile.") }
                }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "User not authenticated.") }
            }
        }
    }

    /**
     * Handles the change of the screen name and triggers an availability check with debounce.
     */
    fun onScreenNameChanged(newScreenName: String) {
        _uiState.update { it.copy(screenName = newScreenName) }
        screenNameCheckJob?.cancel()
        screenNameCheckJob = viewModelScope.launch {
            delay(500) // Debounce
            if (newScreenName.length >= 3) {
                // Only check if screen name is different from current user's original screen name
                if (newScreenName != _uiState.value.currentUser?.screenName) {
                    _uiState.update { it.copy(screenNameAvailability = ScreenNameState.CHECKING) }
                    val isTaken = userService.isScreenNameTaken(newScreenName)
                    _uiState.update { it.copy(screenNameAvailability = if (isTaken) ScreenNameState.TAKEN else ScreenNameState.AVAILABLE) }
                } else {
                    _uiState.update { it.copy(screenNameAvailability = ScreenNameState.IDLE) }
                }
            } else {
                _uiState.update { it.copy(screenNameAvailability = ScreenNameState.IDLE) }
            }
        }
    }

    fun onZipCodeChanged(newZipCode: String) {
        _uiState.update { it.copy(zipCode = newZipCode) }
    }

    fun onNwsSpotterIdChanged(newId: String) {
        _uiState.update { it.copy(nwsSpotterId = newId) }
    }

    fun onHamRadioCallSignChanged(newSign: String) {
        _uiState.update { it.copy(hamRadioCallSign = newSign) }
    }

    fun onExperienceLevelChanged(newLevel: String) {
        _uiState.update { it.copy(experienceLevel = newLevel) }
    }

    fun onMemberTypeChanged(newType: String) {
        _uiState.update { it.copy(memberType = newType) }
    }

    /**
     * Saves the updated user profile to Firestore.
     */
    fun saveProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, showSuccessMessage = null) }

            val currentUser = _uiState.value.currentUser
            if (currentUser == null) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "User not found. Please log in again.") }
                return@launch
            }

            // Perform validation
            if (_uiState.value.screenName.isBlank()) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Screen Name cannot be empty.") }
                return@launch
            }
            if (_uiState.value.screenNameAvailability == ScreenNameState.TAKEN) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Screen Name is already taken.") }
                return@launch
            }
            if (_uiState.value.zipCode.isBlank()) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Zip Code cannot be empty.") }
                return@launch
            }
            // Basic zip code length check
            if (_uiState.value.zipCode.length != 5) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Zip Code must be 5 digits.") }
                return@launch
            }


            // Re-fetch WFO/Zone if zip code has changed
            val updatedWfo: String?
            val updatedZone: String?

            if (_uiState.value.zipCode != currentUser.zipCode) {
                val apiKey = BuildConfig.GOOGLE_MAPS_API_KEY
                if (apiKey.isBlank()) {
                    _uiState.update { it.copy(isSaving = false, errorMessage = "API key missing for location update.") }
                    return@launch
                }
                val geocodingResponse = nwsApiService.getCoordinatesFromZipCode(_uiState.value.zipCode, apiKey)
                val location = geocodingResponse?.results?.firstOrNull()?.geometry?.location
                if (location == null) {
                    _uiState.update { it.copy(isSaving = false, errorMessage = "Could not validate new Zip Code. Please check it and try again.") }
                    return@launch
                }
                val pointData = nwsApiService.getNwsPointData(location.lat, location.lng)
                updatedWfo = pointData?.properties?.gridId
                updatedZone = pointData?.properties?.forecastZone?.substringAfterLast("/")

                if (updatedWfo == null || updatedZone == null) {
                    _uiState.update { it.copy(isSaving = false, errorMessage = "Could not determine NWS forecast zone for the new Zip Code.") }
                    return@launch
                }
            } else {
                updatedWfo = currentUser.wfo
                updatedZone = currentUser.zone
            }

            val updatedUser = currentUser.copy(
                screenName = _uiState.value.screenName,
                zipCode = _uiState.value.zipCode,
                nwsSpotterId = _uiState.value.nwsSpotterId.takeIf { it.isNotBlank() },
                hamRadioCallSign = _uiState.value.hamRadioCallSign.takeIf { it.isNotBlank() },
                experienceLevel = _uiState.value.experienceLevel,
                memberType = _uiState.value.memberType,
                wfo = updatedWfo,
                zone = updatedZone
            )

            val success = userService.createUserProfile(updatedUser) // createUserProfile also handles updates if doc exists
            if (success) {
                _uiState.update { it.copy(isSaving = false, showSuccessMessage = "Profile updated successfully!", currentUser = updatedUser) }
            } else {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Failed to save profile. Please try again.") }
            }
        }
    }

    /**
     * Confirms the user wants to log out and then performs the logout.
     */
    fun confirmLogout() {
        _uiState.update { it.copy(showLogoutConfirmation = true) }
    }

    fun performLogout() {
        authService.signOut()
        _uiState.update { it.copy(showLogoutConfirmation = false) } // Reset state, navigation will handle the rest
        // The app's navigation will observe the authentication state change and redirect to LoginScreen
    }

    fun dismissLogoutConfirmation() {
        _uiState.update { it.copy(showLogoutConfirmation = false) }
    }

    /**
     * Initiates the password reset process by sending an email.
     */
    fun sendPasswordReset() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, showSuccessMessage = null) }
            val email = authService.firebaseAuth.currentUser?.email
            if (email != null) {
                val success = authService.sendPasswordResetEmail(email)
                if (success) {
                    _uiState.update { it.copy(isSaving = false, showPasswordResetConfirmation = true) }
                } else {
                    _uiState.update { it.copy(isSaving = false, errorMessage = "Failed to send password reset email. Please try again.") }
                }
            } else {
                _uiState.update { it.copy(isSaving = false, errorMessage = "No email associated with current user.") }
            }
        }
    }

    fun dismissPasswordResetConfirmation() {
        _uiState.update { it.copy(showPasswordResetConfirmation = false) }
    }

    fun dismissErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun dismissSuccessMessage() {
        _uiState.update { it.copy(showSuccessMessage = null) }
    }
}
