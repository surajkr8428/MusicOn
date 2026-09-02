package com.example.musicon.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class SyncStatus {
    object Idle : SyncStatus()
    data class Loading(val message: String, val progress: Float = -1f) : SyncStatus()
    data class Success(val message: String) : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

object CloudSyncManager {
    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status = _status.asStateFlow()

    fun updateStatus(newStatus: SyncStatus) {
        _status.value = newStatus
    }

    fun clearStatus() {
        _status.value = SyncStatus.Idle
    }
}
