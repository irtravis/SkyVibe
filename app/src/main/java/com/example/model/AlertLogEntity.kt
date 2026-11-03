package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alert_logs")
data class AlertLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tailNumber: String,
    val callsign: String,
    val registeredOperator: String,
    val aircraftType: String,
    val distanceNm: Double,
    val altitudeFt: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val radarLocationName: String,
    val latitude: Double,
    val longitude: Double
)
