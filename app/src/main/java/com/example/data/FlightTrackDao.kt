package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.FlightTrackPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlightTrackDao {
    @Query("SELECT * FROM flight_tracks ORDER BY timestamp DESC LIMIT 500")
    fun getAllTracks(): Flow<List<FlightTrackPointEntity>>

    @Query("SELECT * FROM flight_tracks WHERE tailNumber = :tailNumber ORDER BY timestamp ASC")
    fun getTracksForTail(tailNumber: String): Flow<List<FlightTrackPointEntity>>

    @Query("SELECT DISTINCT tailNumber FROM flight_tracks ORDER BY tailNumber ASC")
    fun getDistinctTrackedTails(): Flow<List<String>>

    @Query("SELECT * FROM flight_tracks WHERE tailNumber = :tailNumber ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestPointForTail(tailNumber: String): FlightTrackPointEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoint(point: FlightTrackPointEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(points: List<FlightTrackPointEntity>)

    @Query("DELETE FROM flight_tracks WHERE tailNumber = :tailNumber")
    suspend fun deleteTracksForTail(tailNumber: String)

    @Query("DELETE FROM flight_tracks")
    suspend fun clearAllTracks()
}
