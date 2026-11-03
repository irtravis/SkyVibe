package com.example.model

/**
 * Real-time ADS-B transponder target data with weather proximity telemetry.
 */
data class Aircraft(
    val icao24: String,
    val callsign: String,
    val tailNumber: String,
    val registeredOperator: String,
    val aircraftTypeCode: String,
    val aircraftTypeName: String,
    val latitude: Double,
    val longitude: Double,
    val altitudeFt: Int,
    val velocityKts: Int,
    val headingDeg: Float,
    val verticalRateFpm: Int,
    val squawk: String,
    val onGround: Boolean,
    val lastContactTimestamp: Long,
    val distanceNm: Double = 0.0,
    val isWatchlisted: Boolean = false,
    val watchlistLabel: String? = null,
    // Weather Storm Proximity Metrics
    val nearestStormName: String? = null,
    val nearestStormDistanceNm: Double? = null,
    val nearestStormDbz: Int? = null,
    val nearestStormSeverity: StormSeverity? = null,
    val isInsideStorm: Boolean = false,
    val isNearStorm: Boolean = false
) {
    val formattedAltitude: String
        get() = if (onGround) "GROUND" else if (altitudeFt >= 18000) "FL${altitudeFt / 100}" else "${altitudeFt} ft"

    val displayId: String
        get() = when {
            tailNumber.isNotBlank() && callsign.isNotBlank() && tailNumber != callsign -> "$callsign ($tailNumber)"
            tailNumber.isNotBlank() -> tailNumber
            callsign.isNotBlank() -> callsign
            else -> icao24.uppercase()
        }

    val stormAdvisorySummary: String?
        get() = when {
            isInsideStorm && nearestStormName != null ->
                "⚠️ INSIDE $nearestStormName (${nearestStormDbz ?: 55} dBZ)"
            isNearStorm && nearestStormDistanceNm != null && nearestStormName != null ->
                "⚡ ${String.format(java.util.Locale.US, "%.1f", nearestStormDistanceNm)} NM to $nearestStormName (${nearestStormDbz ?: 45} dBZ)"
            else -> null
        }
}
