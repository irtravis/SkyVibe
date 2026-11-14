package com.example.model

import java.util.Locale

/**
 * Live verified flight route / itinerary resolved from aviation route databases
 * (adsbdb.com / hexdb.io). Never fabricated or guessed from compass headings.
 */
data class FlightPlanData(
    val flightNumber: String,
    val originIcao: String,
    val originName: String,
    val originCity: String,
    val destinationIcao: String,
    val destinationName: String,
    val destinationCity: String,
    val airlineName: String = "",
    val sourceName: String = "ADSBDB Live Route",
    val isLiveVerified: Boolean = true
)

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
    val isNearStorm: Boolean = false,
    // Craft Category & Verified Metadata
    val craftCategory: CraftCategory = CraftCategory.AIRPLANE,
    val emitterCategory: String = "",
    val sourceFeed: String = "",
    val publicRegistry: PublicRegistryData? = null,
    val flightPlan: FlightPlanData? = null
) {
    val isHelicopter: Boolean
        get() = craftCategory == CraftCategory.HELICOPTER ||
                emitterCategory.equals("A7", ignoreCase = true) ||
                aircraftTypeCode.uppercase() in ROTORCRAFT_ICAO_TYPES ||
                aircraftTypeName.contains("Helicopter", ignoreCase = true) ||
                aircraftTypeName.contains("Rotorcraft", ignoreCase = true)

    val formattedAltitude: String
        get() = if (onGround) "GROUND" else if (altitudeFt >= 18000) "FL${altitudeFt / 100}" else "$altitudeFt ft"

    val displayId: String
        get() = when {
            callsign.isNotBlank() -> callsign
            tailNumber.isNotBlank() && !tailNumber.equals(icao24, ignoreCase = true) -> tailNumber
            else -> "HEX #${icao24.uppercase()}"
        }

    val stormAdvisorySummary: String?
        get() = when {
            isInsideStorm && nearestStormName != null ->
                "⚠️ INSIDE $nearestStormName (${nearestStormDbz ?: 55} dBZ)"
            isNearStorm && nearestStormDistanceNm != null && nearestStormName != null ->
                "⚡ ${String.format(Locale.US, "%.1f", nearestStormDistanceNm)} NM to $nearestStormName (${nearestStormDbz ?: 45} dBZ)"
            else -> null
        }

    val resolvedFlightPlan: FlightPlanData?
        get() = flightPlan ?: com.example.data.FlightRouteDatabase.getCachedRoute(callsign)

    companion object {
        val ROTORCRAFT_ICAO_TYPES = setOf(
            "B06", "B06T", "B206", "B407", "B412", "B429", "B430", "B47G", "B505", "B212", "B222",
            "UH1", "UH60", "H60", "S70", "S76", "S92", "S61", "S58", "S300", "H269",
            "EC20", "EC25", "EC30", "EC35", "EC45", "EC55", "EC75",
            "H125", "H130", "H135", "H145", "H155", "H160", "H175", "H215", "H225",
            "AS50", "AS55", "AS65", "AS32", "BK17", "BO105", "EXPL",
            "R22", "R44", "R66", "MD52", "MD60", "MD90", "H500", "OH58",
            "A109", "A119", "A139", "A149", "A169", "A189", "AW39",
            "CH47", "CH53", "V22", "B07", "B12", "G2CA", "EN48", "F28", "EXEC"
        )
    }
}
