package com.example.musicon.data.remote

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class SyncStatus {
    object Idle : SyncStatus()
    data class Loading(
        val message: String, 
        val progress: Float = -1f,
        val current: Int = 0,
        val total: Int = 0
    ) : SyncStatus()
    data class Success(
        val message: String,
        val uploaded: Int = 0,
        val failed: Int = 0
    ) : SyncStatus()
    data class Paused(val current: Int, val total: Int) : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

object CloudSyncManager {
    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status = _status.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused = _isPaused.asStateFlow()

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

    fun setPaused(paused: Boolean) {
        _isPaused.value = paused
        val current = _status.value
        if (paused && current is SyncStatus.Loading) {
            _status.value = SyncStatus.Paused(current.current, current.total)
        } else if (!paused && current is SyncStatus.Paused) {
            _status.value = SyncStatus.Loading("Resuming sync...", current.current.toFloat() / current.total, current.current, current.total)
        }
    }

    fun clearStatus() {
        clearJob?.cancel()
        _status.value = SyncStatus.Idle
    }
}
