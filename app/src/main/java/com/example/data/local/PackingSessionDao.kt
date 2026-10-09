package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.domain.model.PackingSession
import com.example.domain.model.UploadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface PackingSessionDao {
    @Query("SELECT * FROM packing_sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<PackingSession>>

    @Query("SELECT * FROM packing_sessions WHERE uploadStatus IN ('PENDING', 'UPLOADING', 'FAILED') ORDER BY createdAt DESC")
    fun getUnsyncedSessionsFlow(): Flow<List<PackingSession>>

    @Query("SELECT * FROM packing_sessions WHERE uploadStatus IN ('PENDING', 'UPLOADING', 'FAILED') ORDER BY createdAt ASC")
    suspend fun getUnsyncedSessionsList(): List<PackingSession>

    @Query("SELECT * FROM packing_sessions WHERE id = :id")
    fun getSessionById(id: Long): Flow<PackingSession?>

    @Query("SELECT * FROM packing_sessions WHERE id = :id")
    suspend fun getSessionDirect(id: Long): PackingSession?

    @Query("SELECT * FROM packing_sessions WHERE orderCode LIKE '%' || :query || '%' OR employeeId LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchSessions(query: String): Flow<List<PackingSession>>

    @Query("SELECT * FROM packing_sessions WHERE orderCode = :orderCode ORDER BY createdAt DESC")
    fun getSessionsByOrderCode(orderCode: String): Flow<List<PackingSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: PackingSession): Long

    @Update
    suspend fun updateSession(session: PackingSession)

    @Query("UPDATE packing_sessions SET uploadStatus = :status, uploadProgress = :progress, driveFileId = :driveFileId, driveViewUrl = :driveViewUrl, errorMessage = :error WHERE id = :id")
    suspend fun updateUploadState(
        id: Long,
        status: UploadStatus,
        progress: Int,
        driveFileId: String?,
        driveViewUrl: String?,
        error: String?
    )

    @Query("UPDATE packing_sessions SET uploadProgress = :progress WHERE id = :id")
    suspend fun updateProgress(id: Long, progress: Int)

    @Delete
    suspend fun deleteSession(session: PackingSession)

    @Query("SELECT COUNT(*) FROM packing_sessions WHERE uploadStatus = 'COMPLETED'")
    fun getCompletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM packing_sessions WHERE uploadStatus IN ('PENDING', 'UPLOADING', 'FAILED')")
    fun getPendingCount(): Flow<Int>
}
