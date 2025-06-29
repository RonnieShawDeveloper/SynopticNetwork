package com.artificialinsightsllc.synopticnetwork.data.models

/**
 * Provides constant data related to weather report types and their associated emojis.
 */
fun getReportTypesWithEmojis(): List<Pair<String, String>> {
    return listOf(
        "Tornado" to "🌪️", "Funnel Cloud" to "🌪️", "Wall Cloud" to "☁️", "Shelf Cloud" to "💨", "Waterspout" to "🌀", "Wind Damage" to "🌬️",
        "Hail" to "☄️", "Frequent Lightning" to "⚡", "Flooding" to "💧", "Coastal Flooding" to "🌊", "River Flooding" to "🏞️",
        "Freezing Rain / Ice" to "🧊", "Sleet" to "🌨️", "Snow" to "❄️", "Dense Fog" to "🌫️", "Wildfire Smoke / Haze" to "🔥",
        "Dust Storm" to "🏜️", "Severe Weather" to "⛈️", "Other" to "❓"
    )
}
