package com.example.model

enum class CraftCategory(val label: String) {
    AIRPLANE("Airplane / Fixed-Wing"),
    HELICOPTER("Helicopter / Rotorcraft"),
    JET("Jet Transport"),
    MILITARY("Military Combat / Tactical"),
    GENERAL_AVIATION("General Aviation")
}

data class AircraftTypeInfo(
    val typeCode: String,
    val modelName: String,
    val manufacturer: String,
    val category: String,
    val maxSpeedKts: Int,
    val cruiseAltitudeFt: Int,
    val rangeNm: Int,
    val wingspanMeters: Double,
    val passengerCapacity: String,
    val engines: String,
    val description: String,
    val drawableResName: String? = null,
    val craftCategory: CraftCategory = CraftCategory.AIRPLANE
) {
    val isHelicopter: Boolean
        get() = craftCategory == CraftCategory.HELICOPTER
}
