package com.example.data

import com.example.model.RadarCenter

object AirportDatabase {

    data class AirportInfo(
        val code: String, // 3-letter IATA e.g. TRI, JFK
        val icao: String, // 4-letter ICAO e.g. KTRI, KJFK
        val name: String,
        val city: String,
        val stateOrCountry: String,
        val latitude: Double,
        val longitude: Double
    )

    private val AIRPORTS = listOf(
        // User-requested TRI and regional airports
        AirportInfo("TRI", "KTRI", "Tri-Cities Airport", "Bristol / Kingsport / Johnson City", "TN, USA", 36.4752, -82.4074),
        AirportInfo("TYS", "KTYS", "McGhee Tyson Airport", "Knoxville", "TN, USA", 35.8110, -83.9940),
        AirportInfo("BNA", "KBNA", "Nashville International Airport", "Nashville", "TN, USA", 36.1245, -86.6782),
        AirportInfo("CHA", "KCHA", "Lovell Field Airport", "Chattanooga", "TN, USA", 35.0353, -85.2038),
        AirportInfo("MEM", "KMEM", "Memphis International Airport", "Memphis", "TN, USA", 35.0424, -89.9767),
        AirportInfo("AVL", "KAVL", "Asheville Regional Airport", "Asheville", "NC, USA", 35.4362, -82.5418),
        AirportInfo("CLT", "KCLT", "Charlotte Douglas International", "Charlotte", "NC, USA", 35.2140, -80.9431),
        AirportInfo("RDU", "KRDU", "Raleigh-Durham International", "Raleigh", "NC, USA", 35.8776, -78.7875),
        AirportInfo("GSO", "KGSO", "Piedmont Triad International", "Greensboro", "NC, USA", 36.0978, -79.9373),
        AirportInfo("ROA", "KROA", "Roanoke-Blacksburg Regional", "Roanoke", "VA, USA", 37.3255, -79.9754),
        AirportInfo("GSP", "KGSP", "Greenville-Spartanburg International", "Greenville", "SC, USA", 34.9456, -82.2189),
        AirportInfo("ATL", "KATL", "Hartsfield-Jackson Atlanta International", "Atlanta", "GA, USA", 33.6407, -84.4277),

        // Major US Hubs
        AirportInfo("JFK", "KJFK", "John F. Kennedy International", "New York", "NY, USA", 40.6413, -73.7781),
        AirportInfo("EWR", "KEWR", "Newark Liberty International", "Newark", "NJ, USA", 40.6895, -74.1745),
        AirportInfo("LGA", "KLGA", "LaGuardia Airport", "New York", "NY, USA", 40.7769, -73.8740),
        AirportInfo("LAX", "KLAX", "Los Angeles International", "Los Angeles", "CA, USA", 33.9416, -118.4085),
        AirportInfo("ORD", "KORD", "O'Hare International Airport", "Chicago", "IL, USA", 41.9742, -87.9073),
        AirportInfo("MDW", "KMDW", "Chicago Midway International", "Chicago", "IL, USA", 41.7868, -87.7522),
        AirportInfo("DFW", "KDFW", "Dallas/Fort Worth International", "Dallas", "TX, USA", 32.8998, -97.0403),
        AirportInfo("DAL", "KDAL", "Dallas Love Field", "Dallas", "TX, USA", 32.8471, -96.8518),
        AirportInfo("DEN", "KDEN", "Denver International Airport", "Denver", "CO, USA", 39.8561, -104.6737),
        AirportInfo("SFO", "KSFO", "San Francisco International", "San Francisco", "CA, USA", 37.6213, -122.3790),
        AirportInfo("OAK", "KOAK", "Oakland International Airport", "Oakland", "CA, USA", 37.7213, -122.2208),
        AirportInfo("SJC", "KSJC", "San Jose International", "San Jose", "CA, USA", 37.3639, -121.9289),
        AirportInfo("SEA", "KSEA", "Seattle-Tacoma International", "Seattle", "WA, USA", 47.4502, -122.3088),
        AirportInfo("LAS", "KLAS", "Harry Reid International Airport", "Las Vegas", "NV, USA", 36.0840, -115.1537),
        AirportInfo("MCO", "KMCO", "Orlando International Airport", "Orlando", "FL, USA", 28.4312, -81.3081),
        AirportInfo("MIA", "KMIA", "Miami International Airport", "Miami", "FL, USA", 25.7959, -80.2870),
        AirportInfo("FLL", "KFLL", "Fort Lauderdale-Hollywood International", "Fort Lauderdale", "FL, USA", 26.0742, -80.1506),
        AirportInfo("TPA", "KTPA", "Tampa International Airport", "Tampa", "FL, USA", 27.9755, -82.5332),
        AirportInfo("RSW", "KRSW", "Southwest Florida International", "Fort Myers", "FL, USA", 26.5362, -81.7552),
        AirportInfo("PHX", "KPHX", "Phoenix Sky Harbor International", "Phoenix", "AZ, USA", 33.4352, -112.0101),
        AirportInfo("IAH", "KIAH", "George Bush Intercontinental", "Houston", "TX, USA", 29.9902, -95.3368),
        AirportInfo("HOU", "KHOU", "William P. Hobby Airport", "Houston", "TX, USA", 29.6454, -95.2789),
        AirportInfo("BOS", "KBOS", "Boston Logan International", "Boston", "MA, USA", 42.3656, -71.0096),
        AirportInfo("MSP", "KMSP", "Minneapolis-St. Paul International", "Minneapolis", "MN, USA", 44.8848, -93.2223),
        AirportInfo("DTW", "KDTW", "Detroit Metropolitan Wayne County", "Detroit", "MI, USA", 42.2162, -83.3554),
        AirportInfo("PHL", "KPHL", "Philadelphia International", "Philadelphia", "PA, USA", 39.8729, -75.2437),
        AirportInfo("BWI", "KBWI", "Baltimore/Washington International", "Baltimore", "MD, USA", 39.1774, -76.6684),
        AirportInfo("DCA", "KDCA", "Ronald Reagan Washington National", "Washington", "DC, USA", 38.8512, -77.0402),
        AirportInfo("IAD", "KIAD", "Washington Dulles International", "Washington", "DC, USA", 38.9531, -77.4565),
        AirportInfo("SLC", "KSLC", "Salt Lake City International", "Salt Lake City", "UT, USA", 40.7899, -111.9791),
        AirportInfo("SAN", "KSAN", "San Diego International", "San Diego", "CA, USA", 32.7338, -117.1933),
        AirportInfo("PDX", "KPDX", "Portland International Airport", "Portland", "OR, USA", 45.5898, -122.5951),
        AirportInfo("HNL", "PHNL", "Daniel K. Inouye International", "Honolulu", "HI, USA", 21.3187, -157.9224),
        AirportInfo("ANC", "PANC", "Ted Stevens Anchorage International", "Anchorage", "AK, USA", 61.1760, -149.9984),
        AirportInfo("AUS", "KAUS", "Austin-Bergstrom International", "Austin", "TX, USA", 30.1975, -97.6664),
        AirportInfo("SAT", "KSAT", "San Antonio International", "San Antonio", "TX, USA", 29.5337, -98.4698),
        AirportInfo("STL", "KSTL", "St. Louis Lambert International", "St. Louis", "MO, USA", 38.7472, -90.3599),
        AirportInfo("MCI", "KMCI", "Kansas City International", "Kansas City", "MO, USA", 39.2976, -94.7139),
        AirportInfo("IND", "KIND", "Indianapolis International", "Indianapolis", "IN, USA", 39.7173, -86.2944),
        AirportInfo("CLE", "KCLE", "Cleveland Hopkins International", "Cleveland", "OH, USA", 41.4058, -81.8539),
        AirportInfo("CMH", "KCMH", "John Glenn Columbus International", "Columbus", "OH, USA", 39.9980, -82.8919),
        AirportInfo("CVG", "KCVG", "Cincinnati/Northern Kentucky", "Cincinnati", "KY, USA", 39.0461, -84.6622),
        AirportInfo("PIT", "KPIT", "Pittsburgh International", "Pittsburgh", "PA, USA", 40.4915, -80.2329),
        AirportInfo("MSY", "KMSY", "Louis Armstrong New Orleans", "New Orleans", "LA, USA", 29.9934, -90.2580),
        AirportInfo("SMF", "KSMF", "Sacramento International", "Sacramento", "CA, USA", 38.6954, -121.5908),
        AirportInfo("BHM", "KBHM", "Birmingham-Shuttlesworth International", "Birmingham", "AL, USA", 33.5629, -86.7535),
        AirportInfo("HSV", "KHSV", "Huntsville International Airport", "Huntsville", "AL, USA", 34.6404, -86.7731),
        AirportInfo("SAV", "KSAV", "Savannah/Hilton Head International", "Savannah", "GA, USA", 32.1276, -81.2021),
        AirportInfo("CHS", "KCHS", "Charleston International", "Charleston", "SC, USA", 32.8986, -80.0405),
        AirportInfo("MYR", "KMYR", "Myrtle Beach International", "Myrtle Beach", "SC, USA", 33.6797, -78.9283),
        AirportInfo("JAX", "KJAX", "Jacksonville International", "Jacksonville", "FL, USA", 30.4941, -81.6879),
        AirportInfo("PBI", "KPBI", "Palm Beach International", "West Palm Beach", "FL, USA", 26.6832, -80.0956),
        AirportInfo("BOI", "KBOI", "Boise Airport", "Boise", "ID, USA", 43.5644, -116.2228),
        AirportInfo("BZN", "KBZN", "Bozeman Yellowstone International", "Bozeman", "MT, USA", 45.7769, -111.1530),
        AirportInfo("JAC", "KJAC", "Jackson Hole Airport", "Jackson", "WY, USA", 43.6073, -110.7377),
        AirportInfo("RNO", "KRNO", "Reno-Tahoe International", "Reno", "NV, USA", 39.4986, -119.7681),
        AirportInfo("ABQ", "KABQ", "Albuquerque International Sunport", "Albuquerque", "NM, USA", 35.0402, -106.6092),
        AirportInfo("OMA", "KOMA", "Eppley Airfield", "Omaha", "NE, USA", 41.3025, -95.8941),
        AirportInfo("DSM", "KDSM", "Des Moines International", "Des Moines", "IA, USA", 41.5340, -93.6631),
        AirportInfo("ICT", "KICT", "Wichita Dwight D. Eisenhower", "Wichita", "KS, USA", 37.6499, -97.4331),
        AirportInfo("LIT", "KLIT", "Bill and Hillary Clinton National", "Little Rock", "AR, USA", 34.7294, -92.2243),
        AirportInfo("XNA", "KXNA", "Northwest Arkansas National", "Bentonville/Fayetteville", "AR, USA", 36.2819, -94.3068),
        AirportInfo("CRW", "KCRW", "West Virginia International Yeager", "Charleston", "WV, USA", 38.3731, -81.5932),

        // Key International Hubs
        AirportInfo("LHR", "EGLL", "London Heathrow Airport", "London", "United Kingdom", 51.4700, -0.4543),
        AirportInfo("LGW", "EGKK", "London Gatwick Airport", "London", "United Kingdom", 51.1537, -0.1821),
        AirportInfo("CDG", "LFPG", "Paris Charles de Gaulle", "Paris", "France", 49.0097, 2.5479),
        AirportInfo("FRA", "EDDF", "Frankfurt Airport", "Frankfurt", "Germany", 50.0379, 8.5622),
        AirportInfo("AMS", "EHAM", "Amsterdam Airport Schiphol", "Amsterdam", "Netherlands", 52.3105, 4.7683),
        AirportInfo("MAD", "LEMD", "Adolfo Suárez Madrid-Barajas", "Madrid", "Spain", 40.4839, -3.5680),
        AirportInfo("FCO", "LIRF", "Rome Leonardo da Vinci-Fiumicino", "Rome", "Italy", 41.8003, 12.2389),
        AirportInfo("ZRH", "LSZH", "Zurich Airport", "Zurich", "Switzerland", 47.4582, 8.5555),
        AirportInfo("DXB", "OMDB", "Dubai International Airport", "Dubai", "United Arab Emirates", 25.2532, 55.3657),
        AirportInfo("DOH", "OTHH", "Hamad International Airport", "Doha", "Qatar", 25.2731, 51.6081),
        AirportInfo("HND", "RJTT", "Tokyo Haneda Airport", "Tokyo", "Japan", 35.5494, 139.7798),
        AirportInfo("NRT", "RJAA", "Narita International Airport", "Tokyo", "Japan", 35.7720, 140.3929),
        AirportInfo("ICN", "RKSI", "Incheon International Airport", "Seoul", "South Korea", 37.4602, 126.4407),
        AirportInfo("SIN", "WSSS", "Singapore Changi Airport", "Singapore", "Singapore", 1.3644, 103.9915),
        AirportInfo("HKG", "VHHH", "Hong Kong International Airport", "Hong Kong", "Hong Kong", 22.3080, 113.9185),
        AirportInfo("SYD", "YSSY", "Sydney Kingsford Smith Airport", "Sydney", "Australia", -33.9399, 151.1753),
        AirportInfo("MEL", "YMML", "Melbourne Airport", "Melbourne", "Australia", -37.6690, 144.8410),
        AirportInfo("YYZ", "CYYZ", "Toronto Pearson International", "Toronto", "Canada", 43.6777, -79.6248),
        AirportInfo("YVR", "CYVR", "Vancouver International Airport", "Vancouver", "Canada", 49.1967, -123.1815),
        AirportInfo("MEX", "MMMX", "Mexico City International", "Mexico City", "Mexico", 19.4361, -99.0719),
        AirportInfo("GRU", "SBGR", "São Paulo/Guarulhos International", "São Paulo", "Brazil", -23.4356, -46.4731)
    )

    private val codeMap: Map<String, AirportInfo> = AIRPORTS.associateBy { it.code.uppercase() }
    private val icaoMap: Map<String, AirportInfo> = AIRPORTS.associateBy { it.icao.uppercase() }

    /**
     * Resolves a radar center by 3-letter IATA code, 4-letter ICAO code, or partial city/airport name.
     */
    fun findAirport(input: String): RadarCenter? {
        val clean = input.trim().uppercase()
        if (clean.isEmpty()) return null

        // 1. Direct match by 3-letter IATA code (e.g. TRI, JFK, LAX)
        codeMap[clean]?.let {
            return RadarCenter(
                id = it.code.lowercase(),
                name = "${it.code} · ${it.name}",
                latitude = it.latitude,
                longitude = it.longitude
            )
        }

        // 2. Direct match by 4-letter ICAO code (e.g. KTRI, KJFK)
        icaoMap[clean]?.let {
            return RadarCenter(
                id = it.code.lowercase(),
                name = "${it.code} (${it.icao}) · ${it.name}",
                latitude = it.latitude,
                longitude = it.longitude
            )
        }

        // 3. Search by city name or airport name
        val query = clean.lowercase()
        val match = AIRPORTS.firstOrNull {
            it.name.lowercase().contains(query) ||
                    it.city.lowercase().contains(query) ||
                    it.stateOrCountry.lowercase().contains(query)
        }

        if (match != null) {
            return RadarCenter(
                id = match.code.lowercase(),
                name = "${match.code} · ${match.name}",
                latitude = match.latitude,
                longitude = match.longitude
            )
        }

        // 4. Coordinates fallback if user entered e.g. "36.47, -82.40"
        if (clean.contains(",") || clean.contains(" ")) {
            val parts = clean.split(Regex("[,\\s]+")).filter { it.isNotBlank() }
            if (parts.size >= 2) {
                val lat = parts[0].toDoubleOrNull()
                val lon = parts[1].toDoubleOrNull()
                if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                    return RadarCenter(
                        id = "custom",
                        name = "Custom Point (${String.format(java.util.Locale.US, "%.3f, %.3f", lat, lon)})",
                        latitude = lat,
                        longitude = lon
                    )
                }
            }
        }

        return null
    }

    fun findAirportInfo(code: String): AirportInfo? {
        val clean = code.trim().uppercase()
        if (clean.isEmpty()) return null
        return codeMap[clean] ?: icaoMap[clean]
    }

    /**
     * Returns matching airports for autocomplete/suggestion list.
     */
    fun searchAirports(query: String): List<AirportInfo> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return AIRPORTS.take(10)
        return AIRPORTS.filter {
            it.code.lowercase().startsWith(q) ||
                    it.icao.lowercase().startsWith(q) ||
                    it.name.lowercase().contains(q) ||
                    it.city.lowercase().contains(q)
        }.take(8)
    }

    fun getAllAirports(): List<AirportInfo> = AIRPORTS
}
