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

        // Exact FAA Mode-S algorithm test vectors
        assertEquals("N1", AircraftRegistry.convertIcao24ToTailNumber("a00001"))
        assertEquals("N1A", AircraftRegistry.convertIcao24ToTailNumber("a00002"))
        assertEquals("N1AA", AircraftRegistry.convertIcao24ToTailNumber("a00003"))
        assertEquals("N12345", AircraftRegistry.convertIcao24ToTailNumber("a061d9"))
        assertEquals("N86QU", AircraftRegistry.convertIcao24ToTailNumber("abcdef"))
        // Frontier Airlines Airbus A321 (Flight F9 4018) Mode-S hex
        assertEquals("N705FR", AircraftRegistry.convertIcao24ToTailNumber("a967ae"))
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

    @Test
    fun testHelicopterDifferentiationAndPhotos() {
        val bell206 = AircraftRegistry.getAircraftType("B06")
        assertEquals(com.example.model.CraftCategory.HELICOPTER, bell206.craftCategory)
        assertTrue(bell206.isHelicopter)
        assertEquals("plane_bell_206", bell206.drawableResName)

        val blackHawk = AircraftRegistry.getAircraftType("UH60")
        assertEquals(com.example.model.CraftCategory.HELICOPTER, blackHawk.craftCategory)
        assertTrue(blackHawk.isHelicopter)
        assertEquals("plane_black_hawk", blackHawk.drawableResName)

        val c172 = AircraftRegistry.getAircraftType("C172")
        assertEquals(com.example.model.CraftCategory.GENERAL_AVIATION, c172.craftCategory)
        assertEquals("plane_cessna_172", c172.drawableResName)

        val b77w = AircraftRegistry.getAircraftType("B77W")
        assertEquals(com.example.model.CraftCategory.JET, b77w.craftCategory)
        assertEquals("plane_boeing_777", b77w.drawableResName)

        // Aircraft model helper
        val heliCraft = com.example.model.Aircraft(
            icao24 = "a206b1",
            callsign = "HELI04",
            tailNumber = "N206JR",
            registeredOperator = "Appalachian Rotorcraft Charters",
            aircraftTypeCode = "B06",
            aircraftTypeName = "Bell 206 JetRanger",
            latitude = 36.5,
            longitude = -82.4,
            altitudeFt = 2200,
            velocityKts = 115,
            headingDeg = 120f,
            verticalRateFpm = 0,
            squawk = "1200",
            onGround = false,
            lastContactTimestamp = System.currentTimeMillis(),
            craftCategory = com.example.model.CraftCategory.HELICOPTER
        )
        assertTrue(heliCraft.isHelicopter)
    }

    @Test
    fun testPublicRegistryDataResolution() {
        val deltaReg = AircraftRegistry.resolvePublicRegistry("N356DN", "DAL1420", "A3568F", "Delta Air Lines")
        assertEquals("DELTA AIR LINES INC", deltaReg.registeredOwner)
        assertTrue(deltaReg.registeredParty.contains("CORPORATION SERVICE COMPANY"))
        assertTrue(deltaReg.certificateStatus.contains("Valid"))
        assertEquals("Federal Aviation Administration (FAA)", deltaReg.registryAuthority)

        val heliReg = AircraftRegistry.resolvePublicRegistry("N206JR", "HELI04", "A206B1", "Appalachian Rotorcraft Charters")
        assertEquals("APPALACHIAN ROTORCRAFT CHARTERS LLC", heliReg.registeredOwner)
        assertTrue(heliReg.registeredParty.contains("PILOT") || heliReg.registeredParty.contains("Representative"))
        assertEquals("Tennessee", heliReg.registryStateOrProvince)
    }

    @Test
    fun testKmlExportGeneration() {
        val sampleTrack = listOf(
            com.example.model.FlightTrackPointEntity(
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

        val kml = com.example.util.KmlExportManager.generateWatchlistTracksKml(sampleTrack, "N856UA")
        assertTrue(kml.contains("<kml"))
        assertTrue(kml.contains("<Document>"))
        assertTrue(kml.contains("<LineString>"))
        assertTrue(kml.contains("<altitudeMode>absolute</altitudeMode>"))
        assertTrue(kml.contains("N856UA"))

        val streamKml = com.example.util.KmlExportManager.generateLiveStreamingNetworkLinkKml()
        assertTrue(streamKml.contains("<NetworkLink>"))
        assertTrue(streamKml.contains("<refreshMode>onInterval</refreshMode>"))
        assertTrue(streamKml.contains("<refreshInterval>5</refreshInterval>"))
    }

    @Test
    fun testDefaultRadarCenterPresetIsTri() {
        val defaultPreset = com.example.model.RadarCenter.PRESETS.first()
        assertEquals("tri", defaultPreset.id)
        assertTrue(defaultPreset.name.contains("Tri-Cities"))
        assertEquals(36.4752, defaultPreset.latitude, 0.001)
        assertEquals(-82.4074, defaultPreset.longitude, 0.001)
    }

    @Test
    fun testVirginiaStatePoliceHelicopterResolution() {
        // Test tail ending in VA (e.g. N71VA, N28VA, N39VA)
        val opN71 = AircraftRegistry.resolveRegisteredOperator("TROOPER2", "N71VA", "a710va")
        assertTrue(opN71.contains("Virginia State Police"))

        val opN28 = AircraftRegistry.resolveRegisteredOperator("", "N28VA", "a280va")
        assertTrue(opN28.contains("Virginia State Police"))

        // Public Registry must be Commonwealth of Virginia, NOT Bradley Thomas
        val reg = AircraftRegistry.resolvePublicRegistry("N71VA", "TROOPER2", "a710va", opN71)
        assertEquals("COMMONWEALTH OF VIRGINIA - DEPARTMENT OF STATE POLICE", reg.registeredOwner)
        assertTrue(reg.registeredParty.contains("VIRGINIA STATE POLICE AVIATION"))
        assertTrue(!reg.registeredOwner.contains("BRADLEY", ignoreCase = true))

        // Helicopter specs & type
        val b407 = AircraftRegistry.getAircraftType("B407")
        assertEquals(com.example.model.CraftCategory.HELICOPTER, b407.craftCategory)
        assertTrue(b407.isHelicopter)

        // Aircraft model validation
        val vspAircraft = com.example.model.Aircraft(
            icao24 = "a710va",
            callsign = "TROOPER2",
            tailNumber = "N71VA",
            registeredOperator = opN71,
            aircraftTypeCode = "B407",
            aircraftTypeName = "Bell 407GXi",
            latitude = 36.60,
            longitude = -82.10,
            altitudeFt = 2400,
            velocityKts = 135,
            headingDeg = 75f,
            verticalRateFpm = 0,
            squawk = "1200",
            onGround = false,
            lastContactTimestamp = System.currentTimeMillis(),
            craftCategory = com.example.model.CraftCategory.HELICOPTER,
            publicRegistry = reg
        )
        assertTrue(vspAircraft.isHelicopter)

        // Flight plan should reflect VSP Med-Flight / Public safety mission from Abingdon KVJI
        val plan = com.example.model.Aircraft.generateDefaultFlightPlan(vspAircraft)
        assertEquals("KVJI", plan.originIcao)
        assertTrue(plan.originName.contains("Virginia Highlands"))
        assertTrue(plan.originCity.contains("Abingdon"))
        assertTrue(!plan.destinationCity.contains("Asheville"))
        assertTrue(plan.flightRules.contains("PUBLIC SAFETY") || plan.flightRules.contains("MEDEVAC"))
    }

    @Test
    fun testFrontierFlightF94018Resolution() {
        // Hex a967ae must decode to N705FR (Frontier Airlines Airbus A321)
        val tail = AircraftRegistry.convertIcao24ToTailNumber("a967ae")
        assertEquals("N705FR", tail)

        // Operator resolution
        val op = AircraftRegistry.resolveRegisteredOperator("FFT4018", tail, "a967ae")
        assertTrue(op.contains("Frontier", ignoreCase = true))

        // Public registry resolution: Must be Frontier Airlines, NOT a fictitious individual owner
        val reg = AircraftRegistry.resolvePublicRegistry(tail, "FFT4018", "a967ae", op)
        assertEquals("FRONTIER AIRLINES INC", reg.registeredOwner)
        assertTrue(!reg.registeredOwner.contains("BRADLEY", ignoreCase = true))

        // Aircraft model validation: must be an airliner (A320/A321 family), NOT a single-engine Rotax or C172
        val frontierCraft = com.example.model.Aircraft(
            icao24 = "a967ae",
            callsign = "FFT4018",
            tailNumber = tail,
            registeredOperator = op,
            aircraftTypeCode = "A320",
            aircraftTypeName = "Airbus A321neo",
            latitude = 36.4,
            longitude = -82.4,
            altitudeFt = 36000,
            velocityKts = 450,
            headingDeg = 15f,
            verticalRateFpm = 0,
            squawk = "2341",
            onGround = false,
            lastContactTimestamp = System.currentTimeMillis(),
            craftCategory = com.example.model.CraftCategory.JET,
            publicRegistry = reg
        )
        assertEquals(com.example.model.CraftCategory.JET, frontierCraft.craftCategory)

        // Flight plan validation: Must be Orlando (KMCO) to Cleveland (KCLE), NOT Abingdon to Asheville!
        val plan = com.example.model.Aircraft.generateDefaultFlightPlan(frontierCraft)
        assertEquals("KMCO", plan.originIcao)
        assertTrue(plan.originCity.contains("Orlando"))
        assertEquals("KCLE", plan.destinationIcao)
        assertTrue(plan.destinationCity.contains("Cleveland"))
        assertTrue(!plan.originIcao.equals("KVJI", ignoreCase = true))
        assertTrue(!plan.destinationIcao.equals("KAVL", ignoreCase = true))

        // Delay status and gate details requested by user
        assertEquals("Arriving late / Delayed", plan.flightStatus)
        assertTrue(plan.delayReason?.contains("F9 4018") == true)
        assertEquals("Terminal A · Gate 15", plan.originTerminalGate)
        assertEquals("Terminal A · Gate A10", plan.destinationTerminalGate)
        assertEquals("8:30 PM", plan.scheduledDepartureTime)
        assertEquals("9:06 PM (Delayed)", plan.actualDepartureTime)
        assertEquals("11:06 PM", plan.scheduledArrivalTime)
        assertEquals("11:35 PM (Delayed)", plan.estimatedArrivalTime)
    }

    @Test
    fun testEliminationOfFictitiousOwners() {
        // Any general US aircraft without explicit airline operator should be identified as FAA US Civil Registry
        val reg = AircraftRegistry.resolvePublicRegistry("N12345", "N12345", "a061d9", "")
        assertEquals("FAA US CIVIL AIRCRAFT REGISTRY", reg.registeredOwner)
        assertTrue(reg.registeredParty.contains("N12345"))
        assertTrue(!reg.registeredOwner.contains("BRADLEY", ignoreCase = true))
        assertTrue(!reg.registeredOwner.contains("WILMINGTON", ignoreCase = true))
    }

    @Test
    fun testAmericanAirlinesFlight1081RouteAndType() {
        // AAL1081 must be Boeing 737-800, NOT Airbus A320
        val aalCraft = com.example.model.Aircraft(
            icao24 = "a95123",
            callsign = "AAL1081",
            tailNumber = "N951AA",
            registeredOperator = "American Airlines",
            aircraftTypeCode = "B738",
            aircraftTypeName = "Boeing 737-800",
            latitude = 36.3619,
            longitude = -82.4043,
            altitudeFt = 34000,
            velocityKts = 421,
            headingDeg = 315f, // Heading Northwest towards Indiana
            verticalRateFpm = 0,
            squawk = "2566",
            onGround = false,
            lastContactTimestamp = System.currentTimeMillis()
        )

        // Route must be Charlotte (KCLT) to Indianapolis (KIND), NOT Dallas (KDFW)
        val plan = com.example.data.FlightRouteDatabase.resolveFlightPlan(aalCraft)
        assertEquals("KCLT", plan.originIcao)
        assertTrue(plan.originCity.contains("Charlotte"))
        assertEquals("KIND", plan.destinationIcao)
        assertTrue(plan.destinationCity.contains("Indianapolis") || plan.destinationName.contains("Indianapolis"))
        assertTrue(!plan.destinationIcao.equals("KDFW", ignoreCase = true))
        assertTrue(!plan.destinationCity.contains("Dallas"))

        // Airway must reflect Indianapolis corridor
        assertTrue(plan.routeAirway.contains("KIND") || plan.routeAirway.contains("LINNC"))
    }

    @Test
    fun testEnvoyAirFlight4246RouteAndType() {
        // Mode-S hex aaa342 must decode to authentic Envoy Air N785BR (Embraer E170), NOT Beechcraft
        val tail = AircraftRegistry.convertIcao24ToTailNumber("aaa342")
        assertEquals("N785BR", tail)

        // Operator resolution
        val op = AircraftRegistry.resolveRegisteredOperator("ENY4246", tail, "aaa342")
        assertEquals("Envoy Air", op)

        // Public registry resolution
        val reg = AircraftRegistry.resolvePublicRegistry(tail, "ENY4246", "aaa342", op)
        assertEquals("ENVOY AIR INC", reg.registeredOwner)

        val enyCraft = com.example.model.Aircraft(
            icao24 = "aaa342",
            callsign = "ENY4246",
            tailNumber = tail,
            registeredOperator = op,
            aircraftTypeCode = "E170",
            aircraftTypeName = "Embraer ERJ-170 / E175",
            latitude = 37.02,
            longitude = -83.05,
            altitudeFt = 14250,
            velocityKts = 390,
            headingDeg = 135f, // Descending Southeast towards KTRI
            verticalRateFpm = -1200,
            squawk = "4125",
            onGround = false,
            lastContactTimestamp = System.currentTimeMillis(),
            publicRegistry = reg
        )

        // Route must be Chicago O'Hare (KORD) to Tri-Cities (KTRI), NOT Abingdon to Asheville!
        val plan = com.example.data.FlightRouteDatabase.resolveFlightPlan(enyCraft)
        assertEquals("KORD", plan.originIcao)
        assertTrue(plan.originCity.contains("Chicago"))
        assertEquals("KTRI", plan.destinationIcao)
        assertTrue(plan.destinationName.contains("Tri-Cities") || plan.destinationCity.contains("Blountville"))
        assertTrue(!plan.originIcao.equals("KVJI", ignoreCase = true))
        assertTrue(!plan.destinationIcao.equals("KAVL", ignoreCase = true))
    }

    @Test
    fun testDynamicHeadingRoutingAmericanAirlines() {
        // Northwest track departing Charlotte must head towards Midwest/Indiana (KIND), NOT DFW
        val nwCraft = com.example.model.Aircraft(
            icao24 = "a12345",
            callsign = "AAL492",
            tailNumber = "N123AA",
            registeredOperator = "American Airlines",
            aircraftTypeCode = "B738",
            aircraftTypeName = "Boeing 737-800",
            latitude = 36.4,
            longitude = -82.4,
            altitudeFt = 33000,
            velocityKts = 430,
            headingDeg = 320f, // Northwest
            verticalRateFpm = 0,
            squawk = "1200",
            onGround = false,
            lastContactTimestamp = System.currentTimeMillis()
        )

        val planNw = com.example.data.FlightRouteDatabase.resolveFlightPlan(nwCraft)
        assertEquals("KCLT", planNw.originIcao)
        assertEquals("KIND", planNw.destinationIcao)
        assertTrue(!planNw.destinationIcao.equals("KDFW", ignoreCase = true))

        // West/Southwest track departing Charlotte heading towards Texas
        val swCraft = nwCraft.copy(headingDeg = 245f)
        val planSw = com.example.data.FlightRouteDatabase.resolveFlightPlan(swCraft)
        assertEquals("KCLT", planSw.originIcao)
        assertEquals("KDFW", planSw.destinationIcao)
    }

    @Test
    fun testDisplayIdWithoutTailNumber() {
        val planeWithCallsign = com.example.model.Aircraft(
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
        // Must show pure callsign, never appending tail numbers in parentheses
        assertEquals("AAL1081", planeWithCallsign.displayId)
        assertTrue(!planeWithCallsign.displayId.contains("("))
        assertTrue(!planeWithCallsign.displayId.contains("N951AA"))

        val planeBlankCallsign = planeWithCallsign.copy(callsign = "")
        assertEquals("HEX #A95123", planeBlankCallsign.displayId)
    }

    @Test
    fun testFlightAwareTailInfoResolution() {
        // 1. American Airlines AAL1081 -> Verified Boeing 737-800, tail N951AA, photo plane_boeing_737
        val aalCraft = com.example.model.Aircraft(
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
        val aalInfo = com.example.data.FlightAwareRegistry.resolveFlightAwareTailInfo(aalCraft)
        assertEquals("N951AA", aalInfo.tailNumber)
        assertEquals("Boeing 737-800", aalInfo.airframeModel)
        assertEquals("plane_boeing_737", aalInfo.photoDrawableResName)
        assertTrue(aalInfo.photoCorrelationSummary.contains("Boeing 737-800"))
        assertTrue(aalInfo.flightAwareRegistrationUrl.contains("N951AA"))
        assertTrue(aalInfo.flightAwarePhotosUrl.contains("N951AA"))

        // 2. Frontier Airlines FFT4018 -> Verified Airbus A321neo, tail N705FR, photo plane_airbus_a320
        val fftCraft = aalCraft.copy(
            icao24 = "a967ae",
            callsign = "FFT4018",
            tailNumber = "N705FR",
            registeredOperator = "Frontier Airlines",
            aircraftTypeCode = "A321",
            aircraftTypeName = "Airbus A321-271NX (neo)"
        )
        val fftInfo = com.example.data.FlightAwareRegistry.resolveFlightAwareTailInfo(fftCraft)
        assertEquals("N705FR", fftInfo.tailNumber)
        assertEquals("Airbus A321-271NX (neo)", fftInfo.airframeModel)
        assertEquals("plane_airbus_a320", fftInfo.photoDrawableResName)
        assertTrue(fftInfo.photoCorrelationSummary.contains("Airbus"))
        assertTrue(fftInfo.flightAwareRegistrationUrl.contains("N705FR"))

        // 3. Envoy Air ENY4246 (American Eagle) -> Verified Embraer ERJ-170, tail N785BR, twin jet photo
        val enyCraft = aalCraft.copy(
            icao24 = "aaa579",
            callsign = "ENY4246",
            tailNumber = "N785BR",
            registeredOperator = "Envoy Air (American Eagle)",
            aircraftTypeCode = "E170",
            aircraftTypeName = "Embraer ERJ-170 / E175"
        )
        val enyInfo = com.example.data.FlightAwareRegistry.resolveFlightAwareTailInfo(enyCraft)
        assertEquals("N785BR", enyInfo.tailNumber)
        assertEquals("Embraer ERJ-170 / E175", enyInfo.airframeModel)
        assertEquals("plane_boeing_737", enyInfo.photoDrawableResName) // Twin jet photo, NOT prop or Cessna

        // 4. Virginia State Police Helicopter TROOPER2 -> Verified Bell 407, tail N71VA, photo plane_bell_206
        val vspCraft = aalCraft.copy(
            icao24 = "a71000",
            callsign = "TROOPER2",
            tailNumber = "N71VA",
            registeredOperator = "Virginia State Police",
            aircraftTypeCode = "B407",
            aircraftTypeName = "Bell 407GXi",
            altitudeFt = 2500,
            velocityKts = 135,
            craftCategory = com.example.model.CraftCategory.HELICOPTER
        )
        val vspInfo = com.example.data.FlightAwareRegistry.resolveFlightAwareTailInfo(vspCraft)
        assertEquals("N71VA", vspInfo.tailNumber)
        assertEquals("Bell 407GXi", vspInfo.airframeModel)
        assertEquals("plane_bell_206", vspInfo.photoDrawableResName) // Helicopter photo, NEVER Cessna 172
        assertTrue(vspInfo.operatorName.contains("Virginia State Police"))
    }

    @Test
    fun testPhotoNeverMismatchesAirframeCategory() {
        // Any helicopter must NEVER correlate to a Cessna or airplane photo
        val heliGeneric = com.example.model.Aircraft(
            icao24 = "a00111",
            callsign = "HELI01",
            tailNumber = "N206JR",
            registeredOperator = "Appalachian Rotorcraft Charters",
            aircraftTypeCode = "B06",
            aircraftTypeName = "Bell 206L LongRanger",
            latitude = 36.5,
            longitude = -82.5,
            altitudeFt = 1500,
            velocityKts = 110,
            headingDeg = 180f,
            verticalRateFpm = 0,
            squawk = "1200",
            onGround = false,
            lastContactTimestamp = System.currentTimeMillis(),
            craftCategory = com.example.model.CraftCategory.HELICOPTER
        )
        val heliInfo = com.example.data.FlightAwareRegistry.resolveFlightAwareTailInfo(heliGeneric)
        assertTrue(heliInfo.photoDrawableResName.contains("bell") || heliInfo.photoDrawableResName.contains("hawk"))
        assertTrue(!heliInfo.photoDrawableResName.contains("cessna"))
        assertTrue(!heliInfo.photoDrawableResName.contains("boeing"))
        assertTrue(!heliInfo.photoDrawableResName.contains("airbus"))
    }

    @Test
    fun testDeltaN359DNIsAirbusNeverBoeing() {
        // Delta flight DAL2472 / Tail N359DN must be accurately identified as Airbus A321, NEVER Boeing 737!
        val deltaCraft = com.example.model.Aircraft(
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

        // 1. FlightAware tail resolver
        val tailInfo = com.example.data.FlightAwareRegistry.resolveFlightAwareTailInfo(deltaCraft)
        assertEquals("N359DN", tailInfo.tailNumber)
        assertTrue(tailInfo.airframeModel.contains("Airbus") && tailInfo.airframeModel.contains("A321"))
        assertTrue(!tailInfo.airframeModel.contains("Boeing"))
        assertEquals("plane_airbus_a320", tailInfo.photoDrawableResName)
        assertTrue(!tailInfo.photoDrawableResName.contains("boeing"))

        // 2. AircraftDatabaseService fleet resolution
        val meta = com.example.data.AircraftDatabaseService.getCachedOrFleet("N359DN", "A40694", "Delta Air Lines")
        org.junit.Assert.assertNotNull(meta)
        assertEquals("Airbus", meta?.manufacturer)
        assertTrue(meta?.model?.contains("Airbus") == true && meta?.model?.contains("A321") == true)
        assertEquals("A321", meta?.icaoTypeCode)
        assertTrue(meta?.isAirbus == true)
        assertTrue(meta?.isBoeing == false)
        assertEquals("plane_airbus_a320", meta?.correlatedPhotoDrawable)
    }
}



