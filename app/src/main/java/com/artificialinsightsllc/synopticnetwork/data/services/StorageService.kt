package com.artificialinsightsllc.synopticnetwork.data.services

import android.net.Uri
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * A service class to encapsulate all Firebase Storage operations.
 */
class StorageService {

    private val storage = Firebase.storage

    /**
     * Uploads an image to Firebase Storage and returns its download URL.
     *
     * @param userId The ID of the user uploading the image, used to organize storage.
     * @param uri The local URI of the image file to upload.
     * @return The public download URL of the uploaded image, or null on failure.
     */
    suspend fun uploadReportImage(userId: String, uri: Uri): String? {
        return try {
            // Create a unique file name for the image
            val fileName = "${UUID.randomUUID()}.jpg"
            val ref = storage.reference.child("reports/$userId/$fileName")

            // Upload the file and wait for the result
            ref.putFile(uri).await()

            // Get the public download URL and return it
            ref.downloadUrl.await()?.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
