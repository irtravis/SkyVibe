package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.WatchlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY createdAt DESC")
    fun getAllWatchlist(): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist WHERE isActive = 1")
    suspend fun getActiveWatchlistSync(): List<WatchlistEntity>

    @Query("SELECT * FROM watchlist WHERE tailNumber = :tailNumber LIMIT 1")
    suspend fun getByTailNumber(tailNumber: String): WatchlistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entry: WatchlistEntity)

    @Update
    suspend fun update(entry: WatchlistEntity)

    @Delete
    suspend fun delete(entry: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE tailNumber = :tailNumber")
    suspend fun deleteByTailNumber(tailNumber: String)
}
