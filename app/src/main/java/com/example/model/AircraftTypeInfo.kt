package com.example.model

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
    val drawableResName: String? = null
)
