package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey
    val tailNumber: String,
    val label: String = "",
    val alertRadiusNm: Float = 25.0f,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
