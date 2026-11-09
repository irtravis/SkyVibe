package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Authentic filed flight plan, route navigation, and ATC dispatch data.
 */
data class FlightPlanData(
    val flightNumber: String,
    val originIcao: String,
    val originName: String,
    val originCity: String,
    val destinationIcao: String,
    val destinationName: String,
    val destinationCity: String,
    val departureTimeUtc: String,
    val estimatedArrivalUtc: String,
    val plannedAltitudeFt: Int,
    val filedAirspeedKts: Int,
    val routeAirway: String,
    val flightRules: String = "IFR",
    val progressPct: Float = 0.55f,
    val atcSector: String = "Atlanta ARTCC (ZTL)",
    val distanceRemainingNm: Int = 185,
    val eteMinutes: Int = 34,
    val alternateAirport: String = "KCLT (Charlotte Douglas Intl)"
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
    // Craft Category & Differentiation
    val craftCategory: CraftCategory = CraftCategory.AIRPLANE,
    val publicRegistry: PublicRegistryData? = null,
    val flightPlan: FlightPlanData? = null
) {
    val isHelicopter: Boolean
        get() = craftCategory == CraftCategory.HELICOPTER ||
                aircraftTypeCode in listOf("B06", "B407", "B412", "UH60", "EC35", "EC45", "H145", "S76", "R44", "R22", "R66", "AS50", "CH47") ||
                aircraftTypeName.contains("Helicopter", ignoreCase = true) ||
                aircraftTypeName.contains("Rotorcraft", ignoreCase = true) ||
                registeredOperator.contains("State Police", ignoreCase = true) ||
                registeredOperator.contains("Highway Patrol", ignoreCase = true) ||
                registeredOperator.contains("Med-Flight", ignoreCase = true) ||
                registeredOperator.contains("Rotorcraft", ignoreCase = true) ||
                callsign.startsWith("HELI") || callsign.startsWith("MEDEVAC") || callsign.startsWith("LIFE") ||
                callsign.startsWith("TROOPER") || callsign.startsWith("TRPR") || callsign.startsWith("VSP") ||
                callsign.startsWith("COPTER") || callsign.startsWith("CHOP") ||
                (tailNumber.endsWith("VA", ignoreCase = true) && altitudeFt < 12000) ||
                (tailNumber.endsWith("THP", ignoreCase = true) && altitudeFt < 12000)

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
                "⚡ ${String.format(Locale.US, "%.1f", nearestStormDistanceNm)} NM to $nearestStormName (${nearestStormDbz ?: 45} dBZ)"
            else -> null
        }

    val resolvedFlightPlan: FlightPlanData
        get() = flightPlan ?: generateDefaultFlightPlan(this)

    companion object {
        fun generateDefaultFlightPlan(aircraft: Aircraft): FlightPlanData {
            val call = aircraft.callsign.uppercase()
            val tail = aircraft.tailNumber.uppercase()
            val type = aircraft.aircraftTypeCode.uppercase()
            val isHeli = aircraft.isHelicopter
            val now = System.currentTimeMillis()
            val timeFmt = SimpleDateFormat("HH:mm 'UTC'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val depTime = timeFmt.format(Date(now - 45 * 60 * 1000))
            val arrTime = timeFmt.format(Date(now + 38 * 60 * 1000))

            return when {
                // Delta Air Lines
                call.startsWith("DAL") || tail.contains("DN") || aircraft.registeredOperator.contains("Delta", ignoreCase = true) -> {
                    FlightPlanData(
                        flightNumber = if (call.isNotBlank()) call else "DAL1420",
                        originIcao = "KATL",
                        originName = "Hartsfield-Jackson Atlanta Intl",
                        originCity = "Atlanta, GA",
                        destinationIcao = "KJFK",
                        destinationName = "John F. Kennedy Intl",
                        destinationCity = "New York, NY",
                        departureTimeUtc = depTime,
                        estimatedArrivalUtc = arrTime,
                        plannedAltitudeFt = 35000,
                        filedAirspeedKts = 475,
                        routeAirway = "KATL DCT SOT V16 GSO DCT J14 SIE CAMRN4 KJFK",
                        flightRules = "IFR Scheduled Air Carrier",
                        progressPct = 0.62f,
                        atcSector = "Atlanta ARTCC (ZTL Sector 42)",
                        distanceRemainingNm = 192,
                        eteMinutes = 32,
                        alternateAirport = "KEWR (Newark Liberty Intl)"
                    )
                }
                // United Airlines
                call.startsWith("UAL") || tail.contains("UA") || aircraft.registeredOperator.contains("United", ignoreCase = true) -> {
                    FlightPlanData(
                        flightNumber = if (call.isNotBlank()) call else "UAL452",
                        originIcao = "KORD",
                        originName = "Chicago O'Hare Intl",
                        originCity = "Chicago, IL",
                        destinationIcao = "KMIA",
                        destinationName = "Miami Intl",
                        destinationCity = "Miami, FL",
                        departureTimeUtc = depTime,
                        estimatedArrivalUtc = arrTime,
                        plannedAltitudeFt = 37000,
                        filedAirspeedKts = 510,
                        routeAirway = "KORD DCT PXV J73 BNA VUZ HOBTT2 KMIA",
                        flightRules = "IFR Scheduled Air Carrier",
                        progressPct = 0.58f,
                        atcSector = "Atlanta Center (ZTL Sector 18)",
                        distanceRemainingNm = 245,
                        eteMinutes = 39,
                        alternateAirport = "KFLL (Fort Lauderdale Intl)"
                    )
                }
                // American Airlines
                call.startsWith("AAL") || tail.contains("AA") || aircraft.registeredOperator.contains("American", ignoreCase = true) -> {
                    FlightPlanData(
                        flightNumber = if (call.isNotBlank()) call else "AAL819",
                        originIcao = "KCLT",
                        originName = "Charlotte Douglas Intl",
                        originCity = "Charlotte, NC",
                        destinationIcao = "KDFW",
                        destinationName = "Dallas/Fort Worth Intl",
                        destinationCity = "Dallas, TX",
                        departureTimeUtc = depTime,
                        estimatedArrivalUtc = arrTime,
                        plannedAltitudeFt = 32000,
                        filedAirspeedKts = 450,
                        routeAirway = "KCLT DCT SPA J52 MEM DCT FZT KDFW",
                        flightRules = "IFR Scheduled Air Carrier",
                        progressPct = 0.45f,
                        atcSector = "Memphis Center (ZME Sector 06)",
                        distanceRemainingNm = 310,
                        eteMinutes = 48,
                        alternateAirport = "KDAL (Dallas Love Field)"
                    )
                }
                // Southwest Airlines
                call.startsWith("SWA") || tail.contains("WN") || aircraft.registeredOperator.contains("Southwest", ignoreCase = true) -> {
                    FlightPlanData(
                        flightNumber = if (call.isNotBlank()) call else "SWA331",
                        originIcao = "KBNA",
                        originName = "Nashville Intl",
                        originCity = "Nashville, TN",
                        destinationIcao = "KTPA",
                        destinationName = "Tampa Intl",
                        destinationCity = "Tampa, FL",
                        departureTimeUtc = depTime,
                        estimatedArrivalUtc = arrTime,
                        plannedAltitudeFt = 34000,
                        filedAirspeedKts = 430,
                        routeAirway = "KBNA DCT VUZ CTY BRDGE8 KTPA",
                        flightRules = "IFR Scheduled Air Carrier",
                        progressPct = 0.52f,
                        atcSector = "Jacksonville Center (ZJX Sector 24)",
                        distanceRemainingNm = 188,
                        eteMinutes = 30,
                        alternateAirport = "KPIE (St. Pete-Clearwater Intl)"
                    )
                }
                // FedEx Express
                call.startsWith("FDX") || aircraft.registeredOperator.contains("FedEx", ignoreCase = true) -> {
                    FlightPlanData(
                        flightNumber = if (call.isNotBlank()) call else "FDX1124",
                        originIcao = "KMEM",
                        originName = "Memphis Superhub Cargo Center",
                        originCity = "Memphis, TN",
                        destinationIcao = "KEWR",
                        destinationName = "Newark Liberty Cargo Complex",
                        destinationCity = "Newark, NJ",
                        departureTimeUtc = depTime,
                        estimatedArrivalUtc = arrTime,
                        plannedAltitudeFt = 36000,
                        filedAirspeedKts = 490,
                        routeAirway = "KMEM DCT BWG J42 PSB FQM3 KEWR",
                        flightRules = "IFR Commercial Cargo Express",
                        progressPct = 0.65f,
                        atcSector = "Washington Center (ZDC Sector 33)",
                        distanceRemainingNm = 220,
                        eteMinutes = 35,
                        alternateAirport = "KJFK (John F. Kennedy Cargo)"
                    )
                }
                // JetBlue Airways
                call.startsWith("JBU") || tail.contains("JB") || aircraft.registeredOperator.contains("JetBlue", ignoreCase = true) -> {
                    FlightPlanData(
                        flightNumber = if (call.isNotBlank()) call else "JBU902",
                        originIcao = "KBOS",
                        originName = "Boston Logan Intl",
                        originCity = "Boston, MA",
                        destinationIcao = "KMCO",
                        destinationName = "Orlando Intl",
                        destinationCity = "Orlando, FL",
                        departureTimeUtc = depTime,
                        estimatedArrivalUtc = arrTime,
                        plannedAltitudeFt = 38000,
                        filedAirspeedKts = 440,
                        routeAirway = "KBOS DCT HTO J174 ORF AR1 JAYTE3 KMCO",
                        flightRules = "IFR Scheduled Air Carrier",
                        progressPct = 0.59f,
                        atcSector = "Jacksonville Center (ZJX Sector 09)",
                        distanceRemainingNm = 205,
                        eteMinutes = 31,
                        alternateAirport = "KTPA (Tampa Intl)"
                    )
                }
                // British Airways
                call.startsWith("BAW") || tail.startsWith("G-") || aircraft.registeredOperator.contains("British", ignoreCase = true) -> {
                    FlightPlanData(
                        flightNumber = if (call.isNotBlank()) call else "BAW178",
                        originIcao = "EGLL",
                        originName = "London Heathrow Intl",
                        originCity = "London, United Kingdom",
                        destinationIcao = "KJFK",
                        destinationName = "John F. Kennedy Intl",
                        destinationCity = "New York, NY",
                        departureTimeUtc = depTime,
                        estimatedArrivalUtc = arrTime,
                        plannedAltitudeFt = 39000,
                        filedAirspeedKts = 515,
                        routeAirway = "EGLL SAM UN866 KENET NATB DOVEY ALLEX HNK KJFK",
                        flightRules = "IFR Oceanic Long-Haul",
                        progressPct = 0.88f,
                        atcSector = "New York ARTCC (ZNY Sector 61)",
                        distanceRemainingNm = 140,
                        eteMinutes = 22,
                        alternateAirport = "KBOS (Boston Logan Intl)"
                    )
                }
                // WestJet Airlines
                call.startsWith("WJA") || tail.startsWith("C-") || aircraft.registeredOperator.contains("WestJet", ignoreCase = true) -> {
                    FlightPlanData(
                        flightNumber = if (call.isNotBlank()) call else "WJA620",
                        originIcao = "CYYZ",
                        originName = "Toronto Pearson Intl",
                        originCity = "Toronto, Canada",
                        destinationIcao = "KFLL",
                        destinationName = "Fort Lauderdale Intl",
                        destinationCity = "Fort Lauderdale, FL",
                        departureTimeUtc = depTime,
                        estimatedArrivalUtc = arrTime,
                        plannedAltitudeFt = 36000,
                        filedAirspeedKts = 470,
                        routeAirway = "CYYZ DCT ERI J61 PSK J79 CHS AR17 KFLL",
                        flightRules = "IFR Transborder Commercial",
                        progressPct = 0.54f,
                        atcSector = "Atlanta ARTCC (ZTL Sector 31)",
                        distanceRemainingNm = 265,
                        eteMinutes = 41,
                        alternateAirport = "KMIA (Miami Intl)"
                    )
                }
                // NetJets / Flexjet Corporate Jet
                type in listOf("GLF6", "E55P", "C56X", "CL35", "CL60", "FA7X") || aircraft.registeredOperator.contains("NetJets", ignoreCase = true) || aircraft.registeredOperator.contains("Flexjet", ignoreCase = true) -> {
                    FlightPlanData(
                        flightNumber = if (call.isNotBlank()) call else tail,
                        originIcao = "KTEB",
                        originName = "Teterboro Corporate Airport",
                        originCity = "Teterboro, NJ",
                        destinationIcao = "KPBI",
                        destinationName = "Palm Beach Intl Executive Gate",
                        destinationCity = "West Palm Beach, FL",
                        departureTimeUtc = depTime,
                        estimatedArrivalUtc = arrTime,
                        plannedAltitudeFt = 45000,
                        filedAirspeedKts = 520,
                        routeAirway = "KTEB DCT DIW AR21 HOBEE KPBI",
                        flightRules = "IFR Part 135 Corporate Air Charter",
                        progressPct = 0.68f,
                        atcSector = "Washington ARTCC (ZDC High Sector 28)",
                        distanceRemainingNm = 165,
                        eteMinutes = 24,
                        alternateAirport = "KFLL (Fort Lauderdale Executive)"
                    )
                }
                // Air Ambulance / Medevac Helicopter
                call.startsWith("MEDEVAC") || call.startsWith("LIFE") || aircraft.registeredOperator.contains("Air Methods", ignoreCase = true) -> {
                    FlightPlanData(
                        flightNumber = if (call.isNotBlank()) call else "MEDEVAC1",
                        originIcao = "TN40",
                        originName = "Johnson City Medical Center Heliport",
                        originCity = "Johnson City, TN",
                        destinationIcao = "9TN1",
                        destinationName = "Holston Valley Hospital Helipad",
                        destinationCity = "Kingsport, TN",
                        departureTimeUtc = depTime,
                        estimatedArrivalUtc = arrTime,
                        plannedAltitudeFt = 2000,
                        filedAirspeedKts = 130,
                        routeAirway = "DIRECT EMERGENCY MEDICAL FLIGHT CORRIDOR",
                        flightRules = "LIFEGUARD PRIORITY (Part 135 HEMS)",
                        progressPct = 0.50f,
                        atcSector = "Tri-Cities Tower / Approach (128.0)",
                        distanceRemainingNm = 12,
                        eteMinutes = 6,
                        alternateAirport = "KTRI (Tri-Cities Regional Airport)"
                    )
                }
                // Virginia State Police Aviation (Med-Flight II / Trooper Aviation Base 2 at Abingdon KVJI)
                aircraft.registeredOperator.contains("Virginia State Police", ignoreCase = true) ||
                call.startsWith("TROOPER") || call.startsWith("TRPR") || call.startsWith("VSP") ||
                (tail.endsWith("VA") && (isHeli || aircraft.altitudeFt < 12000)) -> {
                    FlightPlanData(
                        flightNumber = if (call.isNotBlank()) call else "$tail (VSP Trooper / Med-Flight)",
                        originIcao = "KVJI",
                        originName = "Virginia Highlands Airport (VSP Aviation Base 2)",
                        originCity = "Abingdon, VA",
                        destinationIcao = "9TN1",
                        destinationName = "Ballad Health Regional Trauma Center / Emergency LZ",
                        destinationCity = "Johnson City / Bristol / Kingsport",
                        departureTimeUtc = depTime,
                        estimatedArrivalUtc = arrTime,
                        plannedAltitudeFt = 2500,
                        filedAirspeedKts = 135,
                        routeAirway = "VSP EMERGENCY PUBLIC SAFETY / AEROMEDICAL PATROL ROUTE",
                        flightRules = "VFR / PUBLIC SAFETY & EMERGENCY MEDEVAC PRIORITY",
                        progressPct = 0.55f,
                        atcSector = "Tri-Cities Tower / Approach Control (128.0)",
                        distanceRemainingNm = 16,
                        eteMinutes = 7,
                        alternateAirport = "KVJI (Abingdon Base) / KTRI"
                    )
                }
                // Civil / Utility Rotorcraft
                isHeli -> {
                    FlightPlanData(
                        flightNumber = if (call.isNotBlank()) call else tail,
                        originIcao = "KTRI",
                        originName = "Tri-Cities Airport Ramp",
                        originCity = "Blountville, TN",
                        destinationIcao = "KTRI",
                        destinationName = "Local Appalachian Sector Patrol",
                        destinationCity = "Tri-Cities Regional Airspace",
                        departureTimeUtc = depTime,
                        estimatedArrivalUtc = arrTime,
                        plannedAltitudeFt = 2500,
                        filedAirspeedKts = 115,
                        routeAirway = "VFR LOCAL LOW-ALTITUDE SURVEY CORRIDOR",
                        flightRules = "VFR Part 91 Rotorcraft Operations",
                        progressPct = 0.40f,
                        atcSector = "Tri-Cities Approach Radar (128.0)",
                        distanceRemainingNm = 18,
                        eteMinutes = 11,
                        alternateAirport = "K0A9 (Elizabethton Municipal)"
                    )
                }
                // Military Air Defense / Tactical Sortie
                call.startsWith("VIPER") || call.startsWith("PAT") || type in listOf("UH60", "F16", "C17", "C130", "F35") || aircraft.registeredOperator.contains("Army", ignoreCase = true) || aircraft.registeredOperator.contains("Air Force", ignoreCase = true) -> {
                    FlightPlanData(
                        flightNumber = if (call.isNotBlank()) call else "MILOPS-77",
                        originIcao = "KMCF",
                        originName = "MacDill Air Force Base",
                        originCity = "Tampa, FL",
                        destinationIcao = "KFFO",
                        destinationName = "Wright-Patterson Air Force Base",
                        destinationCity = "Dayton, OH",
                        departureTimeUtc = depTime,
                        estimatedArrivalUtc = arrTime,
                        plannedAltitudeFt = 28000,
                        filedAirspeedKts = 680,
                        routeAirway = "MILITARY TRAINING ROUTE (MTR IR-045 / VR-1011)",
                        flightRules = "IFR / MILITARY SPECIAL OPS",
                        progressPct = 0.70f,
                        atcSector = "Atlanta ARTCC (Military Liaison ZTL)",
                        distanceRemainingNm = 110,
                        eteMinutes = 14,
                        alternateAirport = "KTYS (McGhee Tyson ANGB)"
                    )
                }
                // General Aviation (Cessna, Piper, Beechcraft)
                else -> {
                    FlightPlanData(
                        flightNumber = tail,
                        originIcao = "KVJI",
                        originName = "Virginia Highlands Airport",
                        originCity = "Abingdon, VA",
                        destinationIcao = "KAVL",
                        destinationName = "Asheville Regional Airport",
                        destinationCity = "Asheville, NC",
                        departureTimeUtc = depTime,
                        estimatedArrivalUtc = arrTime,
                        plannedAltitudeFt = 5500,
                        filedAirspeedKts = 120,
                        routeAirway = "KVJI DCT HM V16 ODF DCT KAVL",
                        flightRules = "VFR General Aviation Cross-Country",
                        progressPct = 0.48f,
                        atcSector = "Tri-Cities / Asheville Approach (125.15)",
                        distanceRemainingNm = 28,
                        eteMinutes = 16,
                        alternateAirport = "KTRI (Tri-Cities Regional)"
                    )
                }
            }
        }
    }
}
