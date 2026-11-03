package com.example.data

import com.example.model.Aircraft
import com.example.model.RadarCenter
import com.example.model.StormCell
import com.example.model.StormSeverity
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object StormRadarManager {

    private var activeStormCells = listOf<StormCell>()
    private var lastCenterId: String? = null
    private var lastRadiusNm: Double = 0.0

    /**
     * Retrieves or generates storm cells for the given radar airspace.
     */
    @Synchronized
    fun getStormCells(center: RadarCenter, radiusNm: Double): List<StormCell> {
        if (activeStormCells.isEmpty() || lastCenterId != center.id || Math.abs(lastRadiusNm - radiusNm) > 5.0) {
            activeStormCells = generateStormCellsForAirspace(center, radiusNm)
            lastCenterId = center.id
            lastRadiusNm = radiusNm
        }
        return activeStormCells
    }

    /**
     * Slowly drifts storm cells along their headings to simulate live weather radar evolution.
     */
    @Synchronized
    fun tickStormMovement() {
        if (activeStormCells.isEmpty()) return
        activeStormCells = activeStormCells.map { cell ->
            // Advance storm position based on heading & speed (drifting ~0.0003 deg per tick)
            val rad = Math.toRadians(cell.headingDeg.toDouble())
            val deltaLat = cos(rad) * 0.0004
            val deltaLon = sin(rad) * 0.0004
            cell.copy(
                latitude = cell.latitude + deltaLat,
                longitude = cell.longitude + deltaLon,
                // Random lightning strike discharge toggle
                hasLightning = if (cell.severity == StormSeverity.EXTREME || cell.severity == StormSeverity.SEVERE) {
                    Random.nextFloat() > 0.35f
                } else false
            )
        }
    }

    /**
     * Computes proximity of every aircraft to storm cells and decorates the aircraft with weather metrics.
     */
    fun enrichAircraftWithStormProximity(
        aircraftList: List<Aircraft>,
        stormCells: List<StormCell>
    ): List<Aircraft> {
        if (stormCells.isEmpty()) return aircraftList

        return aircraftList.map { aircraft ->
            var closestCell: StormCell? = null
            var minEdgeDistance = Double.MAX_VALUE

            for (cell in stormCells) {
                val edgeDist = cell.distanceToEdgeNm(aircraft.latitude, aircraft.longitude)
                if (edgeDist < minEdgeDistance) {
                    minEdgeDistance = edgeDist
                    closestCell = cell
                }
            }

            if (closestCell != null) {
                val isInside = minEdgeDistance <= 0.0
                val isNear = minEdgeDistance <= 15.0 // within 15 NM convective advisory
                val displayDistance = if (isInside) 0.0 else minEdgeDistance

                aircraft.copy(
                    nearestStormName = closestCell.name,
                    nearestStormDistanceNm = displayDistance,
                    nearestStormDbz = closestCell.peakDbz,
                    nearestStormSeverity = closestCell.severity,
                    isInsideStorm = isInside,
                    isNearStorm = isNear
                )
            } else {
                aircraft
            }
        }
    }

    private fun generateStormCellsForAirspace(center: RadarCenter, radiusNm: Double): List<StormCell> {
        val latScale = 1.0 / 60.0 // 1 deg lat ~ 60 NM
        val lonScale = 1.0 / (60.0 * cos(Math.toRadians(center.latitude)).coerceAtLeast(0.2))

        val cells = mutableListOf<StormCell>()

        // 1. Primary Convective Supercell (Severe / Extreme with Lightning Core)
        // Positioned approx 25-45% of scan radius to the North-East
        val dist1 = radiusNm * 0.38
        val angle1 = Math.toRadians(48.0)
        val lat1 = center.latitude + (dist1 * cos(angle1) * latScale)
        val lon1 = center.longitude + (dist1 * sin(angle1) * lonScale)
        cells.add(
            StormCell(
                id = "cell_alpha",
                name = "CELL ECHO ALPHA",
                latitude = lat1,
                longitude = lon1,
                radiusNm = (radiusNm * 0.16).coerceIn(6.0, 18.0),
                peakDbz = 62,
                topAltitudeFt = 44000,
                headingDeg = 75f,
                speedKts = 26,
                hasLightning = true,
                severity = StormSeverity.EXTREME
            )
        )

        // 2. Secondary Convective Thunderstorm (Severe 52 dBZ)
        // Positioned to the West-Southwest
        val dist2 = radiusNm * 0.48
        val angle2 = Math.toRadians(235.0)
        val lat2 = center.latitude + (dist2 * cos(angle2) * latScale)
        val lon2 = center.longitude + (dist2 * sin(angle2) * lonScale)
        cells.add(
            StormCell(
                id = "cell_bravo",
                name = "CELL ECHO BRAVO",
                latitude = lat2,
                longitude = lon2,
                radiusNm = (radiusNm * 0.20).coerceIn(7.0, 22.0),
                peakDbz = 54,
                topAltitudeFt = 39000,
                headingDeg = 82f,
                speedKts = 22,
                hasLightning = true,
                severity = StormSeverity.SEVERE
            )
        )

        // 3. Convective Rain Band / Squall Line Fragment (Heavy 44 dBZ)
        // Positioned to the South-Southeast
        val dist3 = radiusNm * 0.62
        val angle3 = Math.toRadians(145.0)
        val lat3 = center.latitude + (dist3 * cos(angle3) * latScale)
        val lon3 = center.longitude + (dist3 * sin(angle3) * lonScale)
        cells.add(
            StormCell(
                id = "cell_charlie",
                name = "CONVECTIVE BAND CHARLIE",
                latitude = lat3,
                longitude = lon3,
                radiusNm = (radiusNm * 0.24).coerceIn(9.0, 28.0),
                peakDbz = 45,
                topAltitudeFt = 32000,
                headingDeg = 68f,
                speedKts = 28,
                hasLightning = false,
                severity = StormSeverity.HEAVY
            )
        )

        // 4. Developing Shower Cell (Moderate 36 dBZ)
        val dist4 = radiusNm * 0.72
        val angle4 = Math.toRadians(310.0)
        val lat4 = center.latitude + (dist4 * cos(angle4) * latScale)
        val lon4 = center.longitude + (dist4 * sin(angle4) * lonScale)
        cells.add(
            StormCell(
                id = "cell_delta",
                name = "SHOWER CELL DELTA",
                latitude = lat4,
                longitude = lon4,
                radiusNm = (radiusNm * 0.14).coerceIn(5.0, 14.0),
                peakDbz = 35,
                topAltitudeFt = 24000,
                headingDeg = 90f,
                speedKts = 18,
                hasLightning = false,
                severity = StormSeverity.MODERATE
            )
        )

        return cells
    }
}
