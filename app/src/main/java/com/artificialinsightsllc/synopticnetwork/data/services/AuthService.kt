package com.artificialinsightsllc.synopticnetwork.data.services

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * A sealed class to represent the result of an authentication operation.
 * This allows the UI to easily handle success, error, and loading states.
 */
sealed class AuthResult {
    data class Success(val uid: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

/**
 * A service class to encapsulate all Firebase Authentication operations.
 * This provides a clean API for ViewModels to use for auth-related tasks.
 */
class AuthService {

    internal val firebaseAuth: FirebaseAuth = Firebase.auth

    /**
     * Creates a new user with email and password in Firebase Authentication.
     *
     * @param email The user's email.
     * @param password The user's password.
     * @return An [AuthResult] indicating success (with UID) or failure (with a friendly error message).
     */
    suspend fun createUser(email: String, password: String): AuthResult {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return AuthResult.Error("An unknown error occurred.")
            AuthResult.Success(uid)
        } catch (e: FirebaseAuthException) {
            AuthResult.Error(getFriendlyAuthErrorMessage(e.errorCode))
        } catch (e: Exception) {
            AuthResult.Error("A network error occurred. Please check your connection.")
        }
    }

    /**
     * Signs in a user with email and password.
     *
     * @param email The user's email.
     * @param password The user's password.
     * @return An [AuthResult] indicating success (with UID) or failure (with a friendly error message).
     */
    suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return AuthResult.Error("An unknown error occurred.")
            AuthResult.Success(uid)
        } catch (e: FirebaseAuthException) {
            AuthResult.Error(getFriendlyAuthErrorMessage(e.errorCode))
        } catch (e: Exception) {
            AuthResult.Error("A network error occurred. Please check your connection.")
        }
    }

    /**
     * Signs out the current user.
     */
    fun signOut() {
        firebaseAuth.signOut()
    }

    /**
     * Sends a password reset email to the given address.
     *
     * @param email The user's email address.
     * @return True if the email was sent successfully, false otherwise.
     */
    suspend fun sendPasswordResetEmail(email: String): Boolean {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Deletes the currently authenticated user. This is a sensitive operation
     * used for cleanup if the sign-up process fails after auth creation.
     */
    suspend fun deleteCurrentUser() {
        try {
            firebaseAuth.currentUser?.delete()?.await()
        } catch (e: Exception) {
            e.printStackTrace()
            // Log this critical failure. Manual cleanup in Firebase may be required.
        }
    }

    /**
     * Checks if a user is currently signed in and their account is still valid on the server.
     * It does this by forcing a token refresh. If the refresh fails, it signs the user out.
     *
     * @return True if a user is signed in and valid, false otherwise.
     */
    suspend fun isUserValidAndAuthenticated(): Boolean {
        val user = firebaseAuth.currentUser ?: return false
        return try {
            user.reload().await()
            true // If reload succeeds, the user account is still active and valid.
        } catch (e: Exception) {
            // Reload will fail if the user has been deleted or disabled from the Firebase console.
            e.printStackTrace()
            // Clean up the invalid local session by signing the user out.
            signOut()
            false
        }
    }

    /**
     * Gets the current user's UID.
     * @return The UID of the current user, or null if no user is signed in.
     */
    fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    /**
     * Translates Firebase's auth error codes into user-friendly strings.
     *
     * @param errorCode The error code from a [FirebaseAuthException].
     * @return A human-readable error message.
     */
    private fun getFriendlyAuthErrorMessage(errorCode: String): String {
        return when (errorCode) {
            "ERROR_INVALID_EMAIL" -> "The email address you entered is not valid."
            "ERROR_WRONG_PASSWORD" -> "Incorrect password. Please try again."
            "ERROR_USER_NOT_FOUND" -> "No account found with this email. Please sign up."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "This email address is already registered. Please log in."
            "ERROR_WEAK_PASSWORD" -> "Your password is too weak. It must be at least 6 characters long."
            "ERROR_USER_DISABLED" -> "This account has been disabled. Please contact support."
            "ERROR_TOO_MANY_REQUESTS" -> "Access to this account has been temporarily disabled due to too many failed login attempts. You can reset your password or try again later."
            "ERROR_NETWORK_REQUEST_FAILED" -> "A network error occurred. Please check your internet connection and try again."
            else -> "An unexpected error occurred. Please try again."
        }
    }
}
