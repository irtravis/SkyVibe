package com.example.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "flight_tracks",
    indices = [Index(value = ["tailNumber"]), Index(value = ["timestamp"])]
)
data class FlightTrackPointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tailNumber: String,
    val callsign: String,
    val registeredOperator: String,
    val aircraftType: String,
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double,
    val longitude: Double,
    val altitudeFt: Int,
    val velocityKts: Int,
    val headingDeg: Float,
    val verticalRateFpm: Int = 0
)
