package com.example.data.repository

import android.content.Context
import com.example.data.local.PackingDatabase
import com.example.domain.model.PackingSession
import com.example.domain.model.UploadStatus
import com.example.sync.UploadWorker
import kotlinx.coroutines.flow.Flow
import java.io.File

class PackingRepository(private val context: Context) {
    private val dao = PackingDatabase.getDatabase(context).packingSessionDao()

    val allSessions: Flow<List<PackingSession>> = dao.getAllSessions()
    val unsyncedSessions: Flow<List<PackingSession>> = dao.getUnsyncedSessionsFlow()
    val completedCount: Flow<Int> = dao.getCompletedCount()
    val pendingCount: Flow<Int> = dao.getPendingCount()

    fun getSessionById(id: Long): Flow<PackingSession?> = dao.getSessionById(id)

    fun searchSessions(query: String): Flow<List<PackingSession>> = dao.searchSessions(query)

    fun getSessionsByOrderCode(orderCode: String): Flow<List<PackingSession>> = dao.getSessionsByOrderCode(orderCode)

    suspend fun saveCompletedSession(session: PackingSession): Long {
        val id = dao.insertSession(session)
        // Automatically enqueue upload worker
        UploadWorker.enqueueUploadWork(context)
        return id
    }

    suspend fun updateSession(session: PackingSession) {
        dao.updateSession(session)
    }

    suspend fun deleteSessionSafely(session: PackingSession): Boolean {
        // Enforce safety rule: Only allow deletion if backend confirmed completed upload or user explicitly force purges
        if (session.uploadStatus == UploadStatus.COMPLETED) {
            val file = File(session.videoFilePath)
            if (file.exists()) {
                file.delete()
            }
        }
        dao.deleteSession(session)
        return true
    }

    fun triggerSyncNow(requireWifi: Boolean = false) {
        UploadWorker.enqueueUploadWork(context, requireWifi)
    }
}
