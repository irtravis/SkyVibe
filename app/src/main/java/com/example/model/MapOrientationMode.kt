package com.example.model

/**
 * Tactical Radar Map Orientation Modes:
 * 1. REGULAR: Standard stationary radar orientation (000° North at top).
 * 2. FOLLOW_NORTH: Compass mode using phone's sensors to follow real-world Earth North.
 * 3. FOLLOW_AIRCRAFT: Heading-up / Track-up mode aligned with the selected aircraft's flight vector.
 */
enum class MapOrientationMode(
    val title: String,
    val shortLabel: String,
    val description: String
) {
    REGULAR("REGULAR", "NORTH UP", "Fixed 000° North at top"),
    FOLLOW_NORTH("FOLLOW NORTH", "COMPASS", "Follows phone orientation to North"),
    FOLLOW_AIRCRAFT("FOLLOW AIRCRAFT", "TRACK UP", "Oriented to aircraft flight direction")
}
