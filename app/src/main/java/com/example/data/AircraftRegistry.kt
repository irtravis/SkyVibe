package com.example.data

import com.example.model.AircraftTypeInfo

object AircraftRegistry {

    // Airline & Operator ICAO prefixes
    private val AIRLINE_OPERATORS = mapOf(
        "AAL" to "American Airlines",
        "DAL" to "Delta Air Lines",
        "UAL" to "United Airlines",
        "SWA" to "Southwest Airlines",
        "JBU" to "JetBlue Airways",
        "ASA" to "Alaska Airlines",
        "SKW" to "SkyWest Airlines",
        "ENY" to "Envoy Air",
        "FDX" to "FedEx Express",
        "UPS" to "UPS Airlines",
        "GTI" to "Atlas Air",
        "ABX" to "ABX Air",
        "EJA" to "NetJets Aviation",
        "LXJ" to "Flexjet",
        "VJT" to "VistaJet",
        "WJA" to "WestJet",
        "ACA" to "Air Canada",
        "BAW" to "British Airways",
        "DLH" to "Lufthansa",
        "AFR" to "Air France",
        "KLM" to "KLM Royal Dutch Airlines",
        "IBE" to "Iberia",
        "SAS" to "Scandinavian Airlines",
        "SWR" to "Swiss International Air Lines",
        "AZA" to "ITA Airways",
        "VIR" to "Virgin Atlantic",
        "UAE" to "Emirates",
        "QTR" to "Qatar Airways",
        "ETD" to "Etihad Airways",
        "ANA" to "All Nippon Airways",
        "JAL" to "Japan Airlines",
        "CPA" to "Cathay Pacific",
        "SIA" to "Singapore Airlines",
        "QFA" to "Qantas Airways",
        "ANZ" to "Air New Zealand",
        "KQA" to "Kenya Airways",
        "ETH" to "Ethiopian Airlines",
        "RCH" to "US Air Force Air Mobility Command",
        "PAT" to "US Army Priority Air Transport",
        "CNV" to "US Navy Fleet Logistics",
        "NASA" to "NASA Flight Operations",
        "JUS" to "US Department of Justice / JPATS"
    )

    // Known Aircraft Type Specifications Catalog
    val AIRCRAFT_TYPES: Map<String, AircraftTypeInfo> = mapOf(
        "B738" to AircraftTypeInfo(
            typeCode = "B738",
            modelName = "Boeing 737-800",
            manufacturer = "The Boeing Company",
            category = "Narrow-Body Jet Airliner",
            maxSpeedKts = 492,
            cruiseAltitudeFt = 41000,
            rangeNm = 2935,
            wingspanMeters = 35.8,
            passengerCapacity = "162 - 189 Passengers",
            engines = "2x CFM International CFM56-7B (27,300 lbf)",
            description = "The Boeing 737-800 is one of the world's most successful and ubiquitous short-to-medium range twin-engine narrow-body airliners, trusted by commercial fleets worldwide.",
            drawableResName = "plane_boeing_737"
        ),
        "A320" to AircraftTypeInfo(
            typeCode = "A320",
            modelName = "Airbus A320-200",
            manufacturer = "Airbus SE",
            category = "Narrow-Body Jet Airliner",
            maxSpeedKts = 470,
            cruiseAltitudeFt = 39800,
            rangeNm = 3300,
            wingspanMeters = 35.8,
            passengerCapacity = "150 - 180 Passengers",
            engines = "2x CFM56-5B or IAE V2500 turbofans",
            description = "The pioneering digital fly-by-wire airliner featuring sidestick controls and composite materials, renowned for exceptional fuel efficiency and cabin comfort.",
            drawableResName = "plane_airbus_a320"
        ),
        "GLF6" to AircraftTypeInfo(
            typeCode = "GLF6",
            modelName = "Gulfstream G650ER",
            manufacturer = "Gulfstream Aerospace",
            category = "Ultra Long-Range Business Jet",
            maxSpeedKts = 516,
            cruiseAltitudeFt = 51000,
            rangeNm = 7500,
            wingspanMeters = 30.36,
            passengerCapacity = "11 - 19 Passengers (VIP)",
            engines = "2x Rolls-Royce BR725 A1-12 (16,900 lbf)",
            description = "Flagship ultra-long-range executive jet capable of flying nonstop from New York to Tokyo at near supersonic speeds (Mach 0.925) in pressurized bespoke luxury.",
            drawableResName = "plane_gulfstream_g650"
        ),
        "C172" to AircraftTypeInfo(
            typeCode = "C172",
            modelName = "Cessna 172 Skyhawk",
            manufacturer = "Textron Aviation",
            category = "Single-Engine General Aviation",
            maxSpeedKts = 140,
            cruiseAltitudeFt = 14000,
            rangeNm = 640,
            wingspanMeters = 11.0,
            passengerCapacity = "1 Pilot + 3 Passengers",
            engines = "Lycoming IO-360-L2A (180 hp)",
            description = "The most produced aircraft in aviation history. A high-wing, four-seat, single-engine piston aircraft beloved by flight schools and private pilots globally.",
            drawableResName = "plane_boeing_737"
        ),
        "B77W" to AircraftTypeInfo(
            typeCode = "B77W",
            modelName = "Boeing 777-300ER",
            manufacturer = "The Boeing Company",
            category = "Wide-Body Long-Range Airliner",
            maxSpeedKts = 502,
            cruiseAltitudeFt = 43100,
            rangeNm = 7370,
            wingspanMeters = 64.8,
            passengerCapacity = "396 - 451 Passengers",
            engines = "2x General Electric GE90-115B (115,300 lbf)",
            description = "The world's largest twinjet airliner equipped with the colossal GE90 engines, serving as the backbone for intercontinental flights across all major oceans.",
            drawableResName = "plane_boeing_737"
        ),
        "F16" to AircraftTypeInfo(
            typeCode = "F16",
            modelName = "General Dynamics F-16 Fighting Falcon",
            manufacturer = "Lockheed Martin",
            category = "Supersonic Multirole Military Fighter",
            maxSpeedKts = 1147,
            cruiseAltitudeFt = 50000,
            rangeNm = 2277,
            wingspanMeters = 9.96,
            passengerCapacity = "1 Pilot",
            engines = "Pratt & Whitney F100-PW-229 afterburning turbofan",
            description = "An agile supersonic multirole fighter aircraft featuring bubble canopy for unobstructed 360-degree visibility and 9-g maneuverability.",
            drawableResName = "plane_gulfstream_g650"
        )
    )

    /**
     * Resolves registered company or operator name based on callsign, tail number, and icao hex.
     */
    fun resolveRegisteredOperator(callsign: String, tailNumber: String, icao24: String): String {
        val cleanCallsign = callsign.trim().uppercase()
        if (cleanCallsign.length >= 3) {
            val prefix = cleanCallsign.substring(0, 3)
            AIRLINE_OPERATORS[prefix]?.let { return it }
        }

        val cleanTail = tailNumber.trim().uppercase()
        val cleanHex = icao24.trim().lowercase()

        // Known private jet fleets and state operators
        return when {
            cleanCallsign.startsWith("EJA") || cleanTail.endsWith("QS") -> "NetJets Aviation"
            cleanCallsign.startsWith("LXJ") -> "Flexjet LLC"
            cleanCallsign.startsWith("VJT") -> "VistaJet Operations"
            cleanCallsign.startsWith("WUP") -> "Wheels Up Aviation"
            cleanCallsign.startsWith("XOJ") -> "XOJET Private Charters"
            cleanCallsign.startsWith("SPX") -> "SpaceX Flight Operations"
            cleanCallsign.startsWith("RCH") || cleanCallsign.startsWith("MC") -> "United States Air Force"
            cleanCallsign.startsWith("NAVY") || cleanCallsign.startsWith("CNV") -> "United States Navy"
            cleanCallsign.startsWith("CG") -> "United States Coast Guard"
            cleanCallsign.startsWith("SAM") -> "Special Air Mission (USAF VIP)"
            cleanTail.startsWith("N") && cleanHex.startsWith("a") -> "US Civil Aircraft / Private Owner"
            cleanTail.startsWith("G-") -> "UK Civil Aviation Registry"
            cleanTail.startsWith("C-") -> "Transport Canada Civil Aviation"
            cleanTail.startsWith("D-") -> "German Federal Aviation Office"
            cleanTail.startsWith("F-") -> "French Civil Aviation Authority"
            cleanTail.startsWith("VH-") -> "CASA Australia Aircraft Registry"
            cleanTail.startsWith("JA") -> "Japan Civil Aviation Bureau"
            else -> "Private / General Aviation"
        }
    }

    /**
     * Converts a US ICAO hex address (A00001 - ADF7C7) into an FAA N-Number registration.
     */
    fun convertIcao24ToTailNumber(icao24: String): String {
        val hex = icao24.trim().lowercase()
        val hexInt = hex.toIntOrNull(16) ?: return hex.uppercase()

        // US FAA Range: A00001 - ADF7C7
        if (hexInt in 0xA00001..0xADF7C7) {
            val nInt = hexInt - 0xA00001
            val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ" // FAA omits 'I' and 'O'
            
            val d1 = nInt / 101711 + 1
            var rem = nInt % 101711
            if (rem <= 600) {
                // 1 digit
                return if (rem == 0) "N$d1" else decodeRemainder(d1.toString(), rem, alphabet)
            }
            rem -= 601
            val d2 = rem / 10111
            rem %= 10111
            if (rem <= 600) {
                return decodeRemainder("$d1$d2", rem, alphabet)
            }
            rem -= 601
            val d3 = rem / 1011
            rem %= 1011
            if (rem <= 600) {
                return decodeRemainder("$d1$d2$d3", rem, alphabet)
            }
            rem -= 601
            val d4 = rem / 101
            rem %= 101
            if (rem <= 600) {
                return decodeRemainder("$d1$d2$d3$d4", rem, alphabet)
            }
            rem -= 601
            val d5 = rem / 10
            return "N$d1$d2$d3$d4$d5"
        }

        // Country prefix heuristics for international craft
        return when {
            hex.startsWith("40") || hex.startsWith("41") || hex.startsWith("42") || hex.startsWith("43") -> "G-" + hex.takeLast(4).uppercase()
            hex.startsWith("c0") || hex.startsWith("c1") || hex.startsWith("c2") || hex.startsWith("c3") -> "C-" + hex.takeLast(4).uppercase()
            hex.startsWith("3c") || hex.startsWith("3d") || hex.startsWith("3e") || hex.startsWith("3f") -> "D-" + hex.takeLast(4).uppercase()
            hex.startsWith("38") || hex.startsWith("39") || hex.startsWith("3a") || hex.startsWith("3b") -> "F-" + hex.takeLast(4).uppercase()
            hex.startsWith("7c") || hex.startsWith("7d") -> "VH-" + hex.takeLast(3).uppercase()
            hex.startsWith("84") || hex.startsWith("85") || hex.startsWith("86") -> "JA" + hex.takeLast(4).uppercase()
            else -> hex.uppercase()
        }
    }

    private fun decodeRemainder(prefix: String, rem: Int, alphabet: String): String {
        return if (rem == 0) {
            "N$prefix"
        } else if (rem <= 24) {
            "N$prefix${alphabet[rem - 1]}"
        } else {
            val r = rem - 25
            val c1 = alphabet[r / 25]
            val c2 = alphabet[r % 25]
            "N$prefix$c1$c2"
        }
    }

    fun getAircraftType(code: String): AircraftTypeInfo {
        return AIRCRAFT_TYPES[code] ?: AIRCRAFT_TYPES["B738"]!!
    }
}
