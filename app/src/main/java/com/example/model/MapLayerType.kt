package com.example.model

enum class MapLayerType(val displayName: String, val subtitle: String) {
    RADAR("RADAR", "CRT Avionics Sweep"),
    SATELLITE("SATELLITE", "Nocturnal Earth Orbit"),
    TERRAIN("TERRAIN", "Sectional Topography")
}

data class CastDevice(
    val id: String,
    val name: String,
    val deviceType: String,
    val resolution: String = "1080p 60fps",
    val latencyMs: Int = 14,
    val isConnected: Boolean = false
) {
    companion object {
        val DEFAULT_DEVICES = listOf(
            CastDevice("dev_1", "Living Room TV (Chromecast Ultra)", "Google Cast", "4K 60fps", 12),
            CastDevice("dev_2", "Cockpit Primary MFD Screen", "Wi-Fi Display / Miracast", "1080p 60fps", 8),
            CastDevice("dev_3", "ATC Operations Wall Monitor", "Google Cast 4K", "4K 60fps", 15),
            CastDevice("dev_4", "Flight Sim Overhead Display", "Wireless Display", "1440p 60fps", 11)
        )
    }
}
