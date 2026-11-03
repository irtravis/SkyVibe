package com.example.model

import androidx.compose.ui.graphics.Color
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class StormSeverity(
    val label: String,
    val color: Color,
    val minDbz: Int,
    val description: String
) {
    MODERATE(
        label = "MODERATE",
        color = Color(0xFF00E676),
        minDbz = 30,
        description = "Light to moderate rain showers"
    ),
    HEAVY(
        label = "HEAVY",
        color = Color(0xFFFFD600),
        minDbz = 40,
        description = "Heavy rain & convective activity"
    ),
    SEVERE(
        label = "SEVERE",
        color = Color(0xFFFF1744),
        minDbz = 50,
        description = "Severe thunderstorm with wind shear"
    ),
    EXTREME(
        label = "EXTREME",
        color = Color(0xFFD500F9),
        minDbz = 60,
        description = "Severe hail core & tornadic potential"
    )
}

data class StormCell(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusNm: Double,
    val peakDbz: Int,
    val topAltitudeFt: Int,
    val headingDeg: Float,
    val speedKts: Int,
    val hasLightning: Boolean,
    val severity: StormSeverity
) {
    /**
     * Calculates distance in Nautical Miles from this storm cell center to a coordinate.
     */
    fun distanceToCenterNm(targetLat: Double, targetLon: Double): Double {
        val lat1Rad = Math.toRadians(latitude)
        val lat2Rad = Math.toRadians(targetLat)
        val deltaLat = Math.toRadians(targetLat - latitude)
        val deltaLon = Math.toRadians(targetLon - longitude)

        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLon / 2) * sin(deltaLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distanceKm = 6371.0 * c
        return distanceKm / 1.852
    }

    /**
     * Distance to the outer edge of this storm cell.
     * Negative values indicate the coordinate is inside the storm cell.
     */
    fun distanceToEdgeNm(targetLat: Double, targetLon: Double): Double {
        return distanceToCenterNm(targetLat, targetLon) - radiusNm
    }

    /**
     * Bearing from target coordinate towards this storm cell center in degrees (0-360).
     */
    fun bearingFrom(targetLat: Double, targetLon: Double): Double {
        val lat1 = Math.toRadians(targetLat)
        val lat2 = Math.toRadians(latitude)
        val dLon = Math.toRadians(longitude - targetLon)

        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        val brng = Math.toDegrees(atan2(y, x))
        return (brng + 360.0) % 360.0
    }
}
