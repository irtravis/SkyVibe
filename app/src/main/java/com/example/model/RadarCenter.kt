package com.example.model

data class RadarCenter(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val isGps: Boolean = false
) {
    companion object {
        val PRESETS = listOf(
            RadarCenter("tri", "TRI · Tri-Cities Airport", 36.4752, -82.4074),
            RadarCenter("jfk", "New York (JFK)", 40.6413, -73.7781),
            RadarCenter("lax", "Los Angeles (LAX)", 33.9416, -118.4085),
            RadarCenter("ord", "Chicago (ORD)", 41.9742, -87.9073),
            RadarCenter("lhr", "London Heathrow (LHR)", 51.4700, -0.4543),
            RadarCenter("sfo", "San Francisco (SFO)", 37.6213, -122.3790),
            RadarCenter("mia", "Miami (MIA)", 25.7959, -80.2870),
            RadarCenter("dfw", "Dallas Fort Worth (DFW)", 32.8998, -97.0403),
            RadarCenter("hnd", "Tokyo Haneda (HND)", 35.5494, 139.7798)
        )
    }
}
