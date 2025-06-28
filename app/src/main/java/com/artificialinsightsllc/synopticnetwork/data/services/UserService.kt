package com.artificialinsightsllc.synopticnetwork.data.services

import com.artificialinsightsllc.synopticnetwork.data.models.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * A service class to encapsulate all Firestore operations related to user profiles.
 */
class UserService {

    private val db: FirebaseFirestore = Firebase.firestore

    /**
     * Creates a new user document in the 'users' collection in Firestore.
     *
     * @param user The User object containing all the profile data.
     * @return True if the document was created successfully, false otherwise.
     */
    suspend fun createUserProfile(user: User): Boolean {
        return try {
            // The document ID will be the same as the user's Firebase Auth UID
            db.collection("users").document(user.userId).set(user).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Checks if a given screen name already exists in the 'users' collection.
     * This is crucial for ensuring screen names are unique.
     *
     * @param screenName The screen name to check.
     * @return True if the screen name is already taken, false otherwise.
     */
    suspend fun isScreenNameTaken(screenName: String): Boolean {
        return try {
            val query = db.collection("users")
                .whereEqualTo("screenName", screenName)
                .limit(1)
                .get()
                .await()
            !query.isEmpty
        } catch (e: Exception) {
            e.printStackTrace()
            // If there's an error, we assume the name is taken to be safe.
            true
        }
    }
}
