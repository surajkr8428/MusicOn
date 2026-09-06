package com.example.musicon.data.remote

import kotlinx.coroutines.*
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

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var clearJob: Job? = null

    fun updateStatus(newStatus: SyncStatus) {
        _status.value = newStatus
        
        if (newStatus is SyncStatus.Success || newStatus is SyncStatus.Error) {
            clearJob?.cancel()
            clearJob = scope.launch {
                delay(4000) // Show for 4 seconds
                _status.value = SyncStatus.Idle
            }
        }
    }

    fun clearStatus() {
        clearJob?.cancel()
        _status.value = SyncStatus.Idle
    }
}
