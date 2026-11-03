package com.example

import com.example.data.AircraftRegistry
import com.example.data.AirportDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testOperatorResolution() {
        val deltaOperator = AircraftRegistry.resolveRegisteredOperator("DAL1420", "N356DN", "a3568f")
        assertEquals("Delta Air Lines", deltaOperator)

        val ualOperator = AircraftRegistry.resolveRegisteredOperator("UAL452", "N856UA", "a835af")
        assertEquals("United Airlines", ualOperator)

        val netJetsOperator = AircraftRegistry.resolveRegisteredOperator("EJA650", "N990QS", "a099e3")
        assertEquals("NetJets Aviation", netJetsOperator)

        val usafOperator = AircraftRegistry.resolveRegisteredOperator("RCH101", "88-0435", "a6783a")
        assertTrue(usafOperator.contains("Air Force"))
    }

    @Test
    fun testIcaoToTailConversion() {
        val tailUk = AircraftRegistry.convertIcao24ToTailNumber("40098F")
        assertTrue(tailUk.startsWith("G-"))

        val tailCanada = AircraftRegistry.convertIcao24ToTailNumber("c012ab")
        assertTrue(tailCanada.startsWith("C-"))
    }

    @Test
    fun testAircraftTypeSpecs() {
        val b738 = AircraftRegistry.getAircraftType("B738")
        assertNotNull(b738)
        assertEquals("Boeing 737-800", b738.modelName)
        assertEquals(41000, b738.cruiseAltitudeFt)
        assertEquals("plane_boeing_737", b738.drawableResName)

        val g650 = AircraftRegistry.getAircraftType("GLF6")
        assertNotNull(g650)
        assertEquals("Gulfstream G650ER", g650.modelName)
        assertEquals("Ultra Long-Range Business Jet", g650.category)
    }

    @Test
    fun testAirportCodeLookupTriAndOthers() {
        // Test user-specified TRI code
        val triAirport = AirportDatabase.findAirport("TRI")
        assertNotNull(triAirport)
        assertEquals("tri", triAirport?.id)
        assertTrue(triAirport?.name?.contains("Tri-Cities") == true)
        assertEquals(36.4752, triAirport?.latitude ?: 0.0, 0.001)
        assertEquals(-82.4074, triAirport?.longitude ?: 0.0, 0.001)

        // Test lowercase 3-digit code
        val jfkAirport = AirportDatabase.findAirport("jfk")
        assertNotNull(jfkAirport)
        assertEquals("jfk", jfkAirport?.id)

        // Test 4-letter ICAO
        val ktriAirport = AirportDatabase.findAirport("KTRI")
        assertNotNull(ktriAirport)
        assertEquals("tri", ktriAirport?.id)

        // Test custom coordinate pair
        val customPoint = AirportDatabase.findAirport("36.50, -82.35")
        assertNotNull(customPoint)
        assertEquals("custom", customPoint?.id)
    }

    @Test
    fun testFlightTrackProfile() {
        val samplePoints = listOf(
            com.example.model.FlightTrackPointEntity(
                tailNumber = "N856UA",
                callsign = "UAL452",
                registeredOperator = "United Airlines",
                aircraftType = "Boeing 777-300ER",
                timestamp = 1000L,
                latitude = 40.64,
                longitude = -73.78,
                altitudeFt = 4000,
                velocityKts = 250,
                headingDeg = 75f
            ),
            com.example.model.FlightTrackPointEntity(
                tailNumber = "N856UA",
                callsign = "UAL452",
                registeredOperator = "United Airlines",
                aircraftType = "Boeing 777-300ER",
                timestamp = 2000L,
                latitude = 41.20,
                longitude = -72.90,
                altitudeFt = 37000,
                velocityKts = 510,
                headingDeg = 78f
            )
        )

        assertEquals("N856UA", samplePoints.first().tailNumber)
        val maxAlt = samplePoints.maxOf { it.altitudeFt }
        val maxSpd = samplePoints.maxOf { it.velocityKts }

        assertEquals(37000, maxAlt)
        assertEquals(510, maxSpd)
    }

    @Test
    fun testStormRadarAndProximityCalculations() {
        val triCenter = AirportDatabase.findAirport("TRI")!!
        val storms = com.example.data.StormRadarManager.getStormCells(triCenter, 50.0)
        assertTrue(storms.isNotEmpty())

        val extremeCell = storms.find { it.severity == com.example.model.StormSeverity.EXTREME }
        assertNotNull(extremeCell)
        assertTrue(extremeCell!!.peakDbz >= 60)

        // Aircraft directly on storm cell center
        val insidePlane = com.example.model.Aircraft(
            icao24 = "a11111",
            callsign = "TEST1",
            tailNumber = "N111XX",
            registeredOperator = "SkyWatch Test",
            aircraftTypeCode = "B738",
            aircraftTypeName = "Boeing 737-800",
            latitude = extremeCell.latitude,
            longitude = extremeCell.longitude,
            altitudeFt = 35000,
            velocityKts = 450,
            headingDeg = 90f,
            verticalRateFpm = 0,
            squawk = "1200",
            onGround = false,
            lastContactTimestamp = System.currentTimeMillis()
        )

        val enriched = com.example.data.StormRadarManager.enrichAircraftWithStormProximity(listOf(insidePlane), storms)
        val resultPlane = enriched.first()
        assertTrue(resultPlane.isInsideStorm)
        assertEquals(0.0, resultPlane.nearestStormDistanceNm ?: 100.0, 0.01)
        assertEquals(extremeCell.name, resultPlane.nearestStormName)
    }
}

