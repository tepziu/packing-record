package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PackingApplication
import com.example.domain.model.PackingSession
import com.example.domain.model.UploadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryFilter(
    val searchQuery: String = "",
    val statusFilter: UploadStatus? = null,
    val selectedWarehouse: String? = null
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as PackingApplication
    private val repo = app.packingRepository

    private val _filter = MutableStateFlow(HistoryFilter())
    val filter: StateFlow<HistoryFilter> = _filter

    val allSessions: StateFlow<List<PackingSession>> = combine(
        repo.allSessions,
        _filter
    ) { sessions, filter ->
        sessions.filter { item ->
            val matchQuery = filter.searchQuery.isBlank() ||
                    item.orderCode.contains(filter.searchQuery, ignoreCase = true) ||
                    item.employeeId.contains(filter.searchQuery, ignoreCase = true) ||
                    item.videoHash.contains(filter.searchQuery, ignoreCase = true)

            val matchStatus = filter.statusFilter == null || item.uploadStatus == filter.statusFilter
            val matchWarehouse = filter.selectedWarehouse == null || item.warehouseId == filter.selectedWarehouse

            matchQuery && matchStatus && matchWarehouse
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateSearchQuery(query: String) {
        _filter.value = _filter.value.copy(searchQuery = query)
    }

    fun updateStatusFilter(status: UploadStatus?) {
        _filter.value = _filter.value.copy(statusFilter = status)
    }

    fun deleteSession(session: PackingSession) {
        viewModelScope.launch {
            repo.deleteSessionSafely(session)
        }
    }
}
