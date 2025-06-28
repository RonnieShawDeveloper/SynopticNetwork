package com.artificialinsightsllc.synopticnetwork.ui.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artificialinsightsllc.synopticnetwork.data.services.AuthResult
import com.artificialinsightsllc.synopticnetwork.data.services.AuthService
import kotlinx.coroutines.launch

// Represents the overall state of the login process
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel(
    private val authService: AuthService = AuthService()
) : ViewModel() {

    private val _loginState = mutableStateOf<LoginState>(LoginState.Idle)
    val loginState: State<LoginState> = _loginState

    /**
     * This function is called from the UI once the permission dialog flow is complete.
     * It then checks if the user is already signed in and their account is valid on the server.
     */
    fun onPermissionFlowFinished() {
        viewModelScope.launch {
            if (authService.isUserValidAndAuthenticated()) {
                _loginState.value = LoginState.Success
            }
        }
    }

    /**
     * Handles the login button click event.
     *
     * @param email The email entered by the user.
     * @param password The password entered by the user.
     */
    fun onLoginClicked(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            // Basic validation
            if (email.isBlank() || password.isBlank()) {
                _loginState.value = LoginState.Error("Email and password cannot be empty.")
                return@launch
            }

            when (val result = authService.signIn(email, password)) {
                is AuthResult.Success -> {
                    _loginState.value = LoginState.Success
                }
                is AuthResult.Error -> {
                    _loginState.value = LoginState.Error(result.message)
                }
            }
        }
    }

    /**
     * Resets the login state back to Idle, for example, after an error dialog is dismissed.
     */
    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}
