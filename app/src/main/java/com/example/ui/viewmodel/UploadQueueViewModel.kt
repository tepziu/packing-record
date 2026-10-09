package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PackingApplication
import com.example.domain.model.PackingSession
import com.example.domain.model.UploadStatus
import com.example.sync.UploadWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UploadQueueViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as PackingApplication
    private val repo = app.packingRepository

    val queueItems: StateFlow<List<PackingSession>> = repo.unsyncedSessions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun syncNow() {
        repo.triggerSyncNow(requireWifi = false)
    }

    fun retryItem(session: PackingSession) {
        viewModelScope.launch {
            repo.updateSession(
                session.copy(
                    uploadStatus = UploadStatus.PENDING,
                    errorMessage = null
                )
            )
            repo.triggerSyncNow(requireWifi = false)
        }
    }
}
