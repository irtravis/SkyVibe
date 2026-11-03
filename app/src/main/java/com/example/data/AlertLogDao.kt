package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.AlertLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertLogDao {
    @Query("SELECT * FROM alert_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentAlerts(): Flow<List<AlertLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: AlertLogEntity)

    @Query("DELETE FROM alert_logs")
    suspend fun clearAll()
}
