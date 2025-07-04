package com.artificialinsightsllc.synopticnetwork.data.services

import com.artificialinsightsllc.synopticnetwork.data.models.Comment
import com.artificialinsightsllc.synopticnetwork.data.models.MapReport
import com.artificialinsightsllc.synopticnetwork.data.models.Report
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * A service class to encapsulate all Firestore operations related to weather reports.
 */
class ReportService {

    private val db: FirebaseFirestore = Firebase.firestore
    private val reportsCollection = db.collection("reports")

    /**
     * Creates a new weather report document in the 'reports' collection in Firestore.
     */
    suspend fun createReport(report: Report): Boolean {
        return try {
            reportsCollection.add(report).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Listens for real-time updates to the 'reports' collection, fetching only the
     * lightweight data needed for map markers. This method now allows filtering by a
     * 3-character Geohash to load reports relevant to the user's general area.
     *
     * @param geohash3Char The 3-character Geohash to filter reports by. If empty or null,
     * it will attempt to fetch all reports (though this is not recommended
     * for production with large datasets without further pagination).
     */
    fun listenForMapReports(geohash3Char: String?): Flow<List<MapReport>> {
        return callbackFlow {
            val query = if (!geohash3Char.isNullOrBlank()) {
                reportsCollection.whereEqualTo("geohash3Char", geohash3Char)
            } else {
                reportsCollection // Fallback to fetching all if no geohash is provided
            }

            val listenerRegistration = query
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val reports = snapshot.documents.mapNotNull {
                            it.toObject(MapReport::class.java)?.copy(reportId = it.id)
                        }
                        trySend(reports).isSuccess
                    }
                }
            awaitClose { listenerRegistration.remove() }
        }
    }

    /**
     * Fetches the full details for a single report document.
     */
    suspend fun getReportDetails(reportId: String): Report? {
        return try {
            reportsCollection.document(reportId).get().await()
                .toObject<Report>()?.copy(reportId = reportId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    /**
     * Adds a new comment to the 'comments' subcollection of a specific report.
     */
    suspend fun addComment(reportId: String, comment: Comment): Boolean {
        return try {
            reportsCollection.document(reportId).collection("comments").add(comment).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Listens for real-time updates to the comments of a specific report.
     */
    fun listenForComments(reportId: String): Flow<List<Comment>> {
        return callbackFlow {
            val commentsListener = reportsCollection.document(reportId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val comments = snapshot.documents.mapNotNull {
                            it.toObject(Comment::class.java)?.copy(commentId = it.id)
                        }
                        trySend(comments).isSuccess
                    }
                }
            awaitClose { commentsListener.remove() }
        }
    }
}
