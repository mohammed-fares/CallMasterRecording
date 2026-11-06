package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings WHERE isArchived = 0 ORDER BY startedAt DESC")
    fun getAllRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE isArchived = 0 ORDER BY startedAt DESC LIMIT :limit")
    fun getRecentRecordings(limit: Int = 5): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getRecordingById(id: String): RecordingEntity?

    @Query("SELECT * FROM recordings WHERE isFavorite = 1 AND isArchived = 0 ORDER BY startedAt DESC")
    fun getFavoriteRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE sourceType = :source AND isArchived = 0 ORDER BY startedAt DESC")
    fun getRecordingsBySource(source: RecordingSource): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE (title LIKE '%' || :query || '%' OR contactName LIKE '%' || :query || '%' OR contactNumber LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' OR transcript LIKE '%' || :query || '%') AND isArchived = 0 ORDER BY startedAt DESC")
    fun searchRecordings(query: String): Flow<List<RecordingEntity>>

    @Query("SELECT COUNT(*) FROM recordings WHERE isArchived = 0")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM recordings WHERE callDirection = 'INCOMING' AND isArchived = 0")
    fun getIncomingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM recordings WHERE callDirection = 'OUTGOING' AND isArchived = 0")
    fun getOutgoingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM recordings WHERE sourceType != 'CELLULAR' AND sourceType != 'MIC' AND isArchived = 0")
    fun getVoipCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(fileSize), 0) FROM recordings WHERE isArchived = 0")
    fun getTotalStorageBytes(): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: RecordingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recordings: List<RecordingEntity>)

    @Update
    suspend fun updateRecording(recording: RecordingEntity)

    @Delete
    suspend fun deleteRecording(recording: RecordingEntity)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteById(id: String)
}
