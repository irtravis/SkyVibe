package com.example

import com.example.data.AircraftDatabaseService
import com.example.data.AircraftRegistry
import com.example.data.AirportDatabase
import com.example.data.FlightAwareRegistry
import com.example.data.FlightRouteDatabase
import com.example.data.StormRadarManager
import com.example.model.Aircraft
import com.example.model.CraftCategory
import com.example.model.FlightTrackPointEntity
import com.example.model.RadarCenter
import com.example.model.StormSeverity
import com.example.util.KmlExportManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testOperatorResolutionFromAirlineCallsigns() {
        assertEquals("Delta Air Lines", AircraftRegistry.resolveRegisteredOperator("DAL1420", "N356DN", "a3568f"))
        assertEquals("United Airlines", AircraftRegistry.resolveRegisteredOperator("UAL452", "N856UA", "a835af"))
        assertEquals("NetJets Aviation", AircraftRegistry.resolveRegisteredOperator("EJA650", "N990QS", "a099e3"))
        assertTrue(AircraftRegistry.resolveRegisteredOperator("RCH101", "88-0435", "a6783a").contains("Air Force"))
        assertTrue(AircraftRegistry.resolveRegisteredOperator("N12345", "N12345", "a061d9").contains("US Civil"))
    }

    @Test
    fun testIcaoToTailConversion() {
        val tailUk = AircraftRegistry.convertIcao24ToTailNumber("40098F")
        assertTrue(tailUk.startsWith("G-"))

        val tailCanada = AircraftRegistry.convertIcao24ToTailNumber("c012ab")
        assertTrue(tailCanada.startsWith("C-"))

        assertEquals("N1", AircraftRegistry.convertIcao24ToTailNumber("a00001"))
        assertEquals("N1A", AircraftRegistry.convertIcao24ToTailNumber("a00002"))
        assertEquals("N1AA", AircraftRegistry.convertIcao24ToTailNumber("a00003"))
        assertEquals("N12345", AircraftRegistry.convertIcao24ToTailNumber("a061d9"))
        assertEquals("N86QU", AircraftRegistry.convertIcao24ToTailNumber("abcdef"))
        assertEquals("N705FR", AircraftRegistry.convertIcao24ToTailNumber("a967ae"))
    }

    @Test
    fun testAircraftTypeSpecs() {
        val b738 = AircraftRegistry.getAircraftType("B738")
        assertEquals("Boeing 737-800", b738.modelName)
        assertEquals(41000, b738.cruiseAltitudeFt)
        assertEquals("plane_boeing_737", b738.drawableResName)

        val g650 = AircraftRegistry.getAircraftType("GLF6")
        assertEquals("Gulfstream G650ER", g650.modelName)
        assertEquals("Ultra Long-Range Business Jet", g650.category)
    }

    @Test
    fun testUnknownTypeNeverHallucinatesBoeingOrCessna() {
        val unknown = AircraftRegistry.getAircraftType("ZZZZ")
        assertFalse(unknown.modelName.contains("Boeing", ignoreCase = true))
        assertFalse(unknown.modelName.contains("Cessna", ignoreCase = true))
        assertNull(unknown.drawableResName)
        assertEquals(0, unknown.maxSpeedKts)

        val unlistedRotor = AircraftRegistry.getAircraftType("", isHelicopterHint = true)
        assertTrue(unlistedRotor.isHelicopter)
        assertEquals(CraftCategory.HELICOPTER, unlistedRotor.craftCategory)
        assertEquals("plane_bell_206", unlistedRotor.drawableResName)
    }

    @Test
    fun testAirportCodeLookupTriAndOthers() {
        val triAirport = AirportDatabase.findAirport("TRI")
        assertNotNull(triAirport)
        assertEquals("tri", triAirport?.id)
        assertTrue(triAirport?.name?.contains("Tri-Cities") == true)
        assertEquals(36.4752, triAirport?.latitude ?: 0.0, 0.001)
        assertEquals(-82.4074, triAirport?.longitude ?: 0.0, 0.001)

        val jfkAirport = AirportDatabase.findAirport("jfk")
        assertNotNull(jfkAirport)
        assertEquals("jfk", jfkAirport?.id)

        val ktriAirport = AirportDatabase.findAirport("KTRI")
        assertNotNull(ktriAirport)
        assertEquals("tri", ktriAirport?.id)

        val customPoint = AirportDatabase.findAirport("36.50, -82.35")
        assertNotNull(customPoint)
        assertEquals("custom", customPoint?.id)
    }

    @Test
    fun testFlightTrackProfile() {
        val samplePoints = listOf(
            FlightTrackPointEntity(
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
            FlightTrackPointEntity(
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
        assertEquals(37000, samplePoints.maxOf { it.altitudeFt })
        assertEquals(510, samplePoints.maxOf { it.velocityKts })
    }

    @Test
    fun testStormRadarAndProximityCalculations() {
        val triCenter = AirportDatabase.findAirport("TRI")!!
        val storms = StormRadarManager.getStormCells(triCenter, 50.0)
        assertTrue(storms.isNotEmpty())

        val extremeCell = storms.find { it.severity == StormSeverity.EXTREME }
        assertNotNull(extremeCell)
        assertTrue(extremeCell!!.peakDbz >= 60)

        val insidePlane = Aircraft(
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

        val enriched = StormRadarManager.enrichAircraftWithStormProximity(listOf(insidePlane), storms)
        val resultPlane = enriched.first()
        assertTrue(resultPlane.isInsideStorm)
        assertEquals(0.0, resultPlane.nearestStormDistanceNm ?: 100.0, 0.01)
        assertEquals(extremeCell.name, resultPlane.nearestStormName)
    }

    @Test
    fun testHelicopterDifferentiationAndNoFalseTailSuffix() {
        val bell206 = AircraftRegistry.getAircraftType("B06")
        assertEquals(CraftCategory.HELICOPTER, bell206.craftCategory)
        assertTrue(bell206.isHelicopter)
        assertEquals("plane_bell_206", bell206.drawableResName)

        val blackHawk = AircraftRegistry.getAircraftType("UH60")
        assertEquals(CraftCategory.HELICOPTER, blackHawk.craftCategory)
        assertTrue(blackHawk.isHelicopter)
        assertEquals("plane_black_hawk", blackHawk.drawableResName)

        // Authentic helicopter via A7 emitter category even if model is pending
        val realHeli = Aircraft(
            icao24 = "a206b1",
            callsign = "MED1",
            tailNumber = "N206JR",
            registeredOperator = "MEDEVAC",
            aircraftTypeCode = "",
            aircraftTypeName = "Helicopter",
            latitude = 36.5,
            longitude = -82.4,
            altitudeFt = 2200,
            velocityKts = 115,
            headingDeg = 120f,
            verticalRateFpm = 0,
            squawk = "1200",
            onGround = false,
            lastContactTimestamp = System.currentTimeMillis(),
            craftCategory = CraftCategory.HELICOPTER,
            emitterCategory = "A7"
        )
        assertTrue(realHeli.isHelicopter)

        // Fixed-wing airplane ending in VA must NEVER be misclassified as a helicopter
        val fixedWingEndingInVa = Aircraft(
            icao24 = "a98111",
            callsign = "N70VA",
            tailNumber = "N70VA",
            registeredOperator = "US Civil Registration",
            aircraftTypeCode = "B738",
            aircraftTypeName = "Boeing 737-800",
            latitude = 36.5,
            longitude = -82.4,
            altitudeFt = 32000,
            velocityKts = 440,
            headingDeg = 90f,
            verticalRateFpm = 0,
            squawk = "1200",
            onGround = false,
            lastContactTimestamp = System.currentTimeMillis(),
            craftCategory = CraftCategory.JET,
            emitterCategory = "A3"
        )
        assertFalse(fixedWingEndingInVa.isHelicopter)
    }

    @Test
    fun testPublicRegistryEliminatesFakeOwnersAndDates() {
        val reg = AircraftRegistry.resolvePublicRegistry("N12345", "N12345", "a061d9", "")
        assertTrue(reg.registeredOwner.contains("US Civil Registration"))
        assertFalse(reg.registeredOwner.contains("BRADLEY", ignoreCase = true))
        assertFalse(reg.registeredOwner.contains("WILMINGTON", ignoreCase = true))
        assertEquals("", reg.serialNumber)
        assertEquals("", reg.issueDate)
        assertEquals("", reg.expirationDate)
    }

    @Test
    fun testUncachedRouteReturnsNullInsteadOfGuessing() {
        val plane = Aircraft(
            icao24 = "a12345",
            callsign = "AAL9999",
            tailNumber = "N999AA",
            registeredOperator = "American Airlines",
            aircraftTypeCode = "B738",
            aircraftTypeName = "Boeing 737-800",
            latitude = 36.4,
            longitude = -82.4,
            altitudeFt = 33000,
            velocityKts = 430,
            headingDeg = 320f,
            verticalRateFpm = 0,
            squawk = "1200",
            onGround = false,
            lastContactTimestamp = System.currentTimeMillis()
        )
        assertNull(FlightRouteDatabase.resolveFlightPlan(plane))
        assertNull(plane.resolvedFlightPlan)
    }

    @Test
    fun testDisplayIdPriority() {
        val planeWithCallsign = Aircraft(
            icao24 = "a95123",
            callsign = "AAL1081",
            tailNumber = "N951AA",
            registeredOperator = "American Airlines",
            aircraftTypeCode = "B738",
            aircraftTypeName = "Boeing 737-800",
            latitude = 36.36,
            longitude = -82.40,
            altitudeFt = 34000,
            velocityKts = 421,
            headingDeg = 315f,
            verticalRateFpm = 0,
            squawk = "2566",
            onGround = false,
            lastContactTimestamp = System.currentTimeMillis()
        )
        assertEquals("AAL1081", planeWithCallsign.displayId)

        val planeBlankCallsign = planeWithCallsign.copy(callsign = "")
        assertEquals("N951AA", planeBlankCallsign.displayId)

        val planeHexOnly = planeWithCallsign.copy(callsign = "", tailNumber = "")
        assertEquals("HEX #A95123", planeHexOnly.displayId)
    }

    @Test
    fun testDeltaN359DNIsAirbusAndAmbiguousTailReturnsNull() {
        val meta = AircraftDatabaseService.resolveOfflineFleet("N359DN", "A40694", "Delta Air Lines")
        assertNotNull(meta)
        assertEquals("Airbus", meta?.manufacturer)
        assertTrue(meta?.model?.contains("Airbus") == true && meta?.model?.contains("A321") == true)
        assertEquals("A321", meta?.icaoTypeCode)
        assertTrue(meta?.isAirbus == true)
        assertFalse(meta?.isBoeing == true)
        assertEquals("plane_airbus_a320", meta?.correlatedPhotoDrawable)

        val deltaCraft = Aircraft(
            icao24 = "a40694",
            callsign = "DAL2472",
            tailNumber = "N359DN",
            registeredOperator = "Delta Air Lines",
            aircraftTypeCode = "A321",
            aircraftTypeName = "Airbus A321-211",
            latitude = 36.36,
            longitude = -82.40,
            altitudeFt = 33000,
            velocityKts = 465,
            headingDeg = 220f,
            verticalRateFpm = 0,
            squawk = "1200",
            onGround = false,
            lastContactTimestamp = System.currentTimeMillis()
        )
        val tailInfo = FlightAwareRegistry.resolveFlightAwareTailInfo(deltaCraft)
        assertEquals("N359DN", tailInfo.tailNumber)
        assertTrue(tailInfo.airframeModel.contains("Airbus") && tailInfo.airframeModel.contains("A321"))
        assertEquals("plane_airbus_a320", tailInfo.photoDrawableResName)

        // Ambiguous generic tail must not be guessed as Boeing or Airbus
        assertNull(AircraftDatabaseService.resolveOfflineFleet("N123XY", "A061AA"))
    }

    @Test
    fun testKmlExportGeneration() {
        val sampleTrack = listOf(
            FlightTrackPointEntity(
                tailNumber = "N856UA",
                callsign = "UAL452",
                registeredOperator = "United Airlines",
                aircraftType = "Boeing 777-300ER",
                timestamp = System.currentTimeMillis(),
                latitude = 40.64,
                longitude = -73.78,
                altitudeFt = 12000,
                velocityKts = 380,
                headingDeg = 85f
            )
        )

        val kml = KmlExportManager.generateWatchlistTracksKml(sampleTrack, "N856UA")
        assertTrue(kml.contains("<kml"))
        assertTrue(kml.contains("<Document>"))
        assertTrue(kml.contains("<LineString>"))
        assertTrue(kml.contains("<altitudeMode>absolute</altitudeMode>"))
        assertTrue(kml.contains("N856UA"))

        val streamKml = KmlExportManager.generateLiveStreamingNetworkLinkKml()
        assertTrue(streamKml.contains("<NetworkLink>"))
        assertTrue(streamKml.contains("<refreshMode>onInterval</refreshMode>"))
        assertTrue(streamKml.contains("<refreshInterval>5</refreshInterval>"))
    }

    @Test
    fun testDefaultRadarCenterPresetIsTri() {
        val defaultPreset = RadarCenter.PRESETS.first()
        assertEquals("tri", defaultPreset.id)
        assertTrue(defaultPreset.name.contains("Tri-Cities"))
        assertEquals(36.4752, defaultPreset.latitude, 0.001)
        assertEquals(-82.4074, defaultPreset.longitude, 0.001)
    }
}
