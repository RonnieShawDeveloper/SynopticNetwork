package com.artificialinsightsllc.synopticnetwork.data.models

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Data class representing a single comment on a weather report.
 *
 * @param commentId The unique ID for the comment document.
 * @param reportId The ID of the report this comment belongs to.
 * @param userId The ID of the user who created the comment.
 * @param screenName The public screen name of the user who commented.
 * @param text The content of the comment.
 * @param timestamp The time the comment was created, automatically set by the server.
 */
data class Comment(
    val commentId: String = "",
    val reportId: String = "",
    val userId: String = "",
    val screenName: String = "",
    val text: String = "",
    @ServerTimestamp
    val timestamp: Date? = null
)
