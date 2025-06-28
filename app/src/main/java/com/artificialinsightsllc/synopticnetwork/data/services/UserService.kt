package com.artificialinsightsllc.synopticnetwork.data.services

import com.artificialinsightsllc.synopticnetwork.data.models.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * A service class to encapsulate all Firestore operations related to user profiles.
 */
class UserService {

    private val db: FirebaseFirestore = Firebase.firestore
    private val usersCollection = db.collection("users")

    /**
     * Creates a new user document in the 'users' collection in Firestore.
     */
    suspend fun createUserProfile(user: User): Boolean {
        return try {
            usersCollection.document(user.userId).set(user).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Fetches a user's profile from Firestore.
     *
     * @param userId The ID of the user to fetch.
     * @return The User object, or null if not found or an error occurs.
     */
    suspend fun getUserProfile(userId: String): User? {
        return try {
            usersCollection.document(userId).get().await()
                .toObject<User>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    /**
     * Checks if a given screen name already exists in the 'users' collection.
     */
    suspend fun isScreenNameTaken(screenName: String): Boolean {
        return try {
            val query = usersCollection
                .whereEqualTo("screenName", screenName)
                .limit(1)
                .get()
                .await()
            !query.isEmpty
        } catch (e: Exception) {
            e.printStackTrace()
            true
        }
    }
}
