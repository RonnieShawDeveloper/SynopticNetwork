package com.artificialinsightsllc.synopticnetwork.data.models

/**
 * Data class representing a user profile in the Firestore database.
 *
 * @param userId The unique ID from Firebase Authentication, linking the auth record to this profile.
 * @param email The user's email address.
 * @param screenName The user's unique, public-facing screen name.
 * @param firstName The user's first name.
 * @param middleName The user's middle name (optional).
 * @param lastName The user's last name.
 * @param zipCode The user's 5-digit zip code, used for local alert targeting.
 * @param nwsSpotterId The user's National Weather Service Spotter ID (optional).
 * @param hamRadioCallSign The user's amateur radio call sign (optional).
 * @param experienceLevel The user's self-identified experience level.
 * @param memberType The user's role or affiliation.
 * @param profilePicUrl The URL to the user's profile picture in Firebase Storage (optional).
 * @param wfo The user's home Weather Forecast Office (e.g., "TBW"), determined later.
 * @param zone The user's NWS forecast zone (e.g., "FLZ151"), determined later.
 * @param verified A flag indicating if the user is a verified spotter or official.
 */
data class User(
    val userId: String = "",
    val email: String = "",
    val screenName: String = "",
    val firstName: String = "",
    val middleName: String? = null,
    val lastName: String = "",
    val zipCode: String = "",
    val nwsSpotterId: String? = null,
    val hamRadioCallSign: String? = null,
    val experienceLevel: String = ExperienceLevel.ENTHUSIAST.name,
    val memberType: String = MemberType.STANDARD.name,
    val profilePicUrl: String? = null,
    val wfo: String? = null,
    val zone: String? = null,
    val verified: Boolean = false
)

// Enum for Experience Level to ensure data consistency
enum class ExperienceLevel {
    ENTHUSIAST,
    CHASER_SPOTTER,
    METEOROLOGIST
}

// Enum for Member Type to ensure data consistency
enum class MemberType {
    STANDARD,
    NWS_EMPLOYEE,
    NWS_WFO,
    COUNTY_EOS,
    PUBLIC_SAFETY_OTHER
}
