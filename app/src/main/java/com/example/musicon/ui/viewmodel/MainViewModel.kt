package com.example.musicon.ui.viewmodel

import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.musicon.data.LibraryViewMode
import com.example.musicon.data.SettingsRepository
import com.example.musicon.data.local.Playlist
import com.example.musicon.data.local.TrackEntity
import com.example.musicon.ui.theme.ThemeMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val settingsRepository: SettingsRepository,
    private val musicRepository: com.example.musicon.data.MusicRepository
) : ViewModel() {

    private val _isUserSignedIn = mutableStateOf(false)
    val isUserSignedIn: State<Boolean> = _isUserSignedIn

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SPOTIFY_DARK)

    val libraryViewMode: StateFlow<LibraryViewMode> = settingsRepository.libraryViewModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryViewMode.LIST)

    val allTracks: StateFlow<List<TrackEntity>> = musicRepository.allTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<Playlist>> = musicRepository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Preferences
    val pauseOnDetach: StateFlow<Boolean> = settingsRepository.pauseOnDetachFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val keepScreenOn: StateFlow<Boolean> = settingsRepository.keepScreenOnFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val showNotifications: StateFlow<Boolean> = settingsRepository.showNotificationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val crossfade: StateFlow<Boolean> = settingsRepository.crossfadeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _currentPlayingTrack = mutableStateOf<TrackEntity?>(null)
    val currentPlayingTrack: State<TrackEntity?> = _currentPlayingTrack

    private val _playbackQueue = mutableStateOf<List<TrackEntity>>(emptyList())
    val playbackQueue: State<List<TrackEntity>> = _playbackQueue

    fun playTrack(track: TrackEntity) {
        _playbackQueue.value = listOf(track)
        _currentPlayingTrack.value = track
    }

    // Bulk Actions
    fun playSelected(tracks: List<TrackEntity>) {
        if (tracks.isNotEmpty()) {
            _playbackQueue.value = tracks
            _currentPlayingTrack.value = tracks.first()
        }
    }

    fun addToQueueNext(tracks: List<TrackEntity>) {
        val currentQueue = _playbackQueue.value.toMutableList()
        val currentIndex = currentQueue.indexOf(_currentPlayingTrack.value)
        if (currentIndex != -1) {
            currentQueue.addAll(currentIndex + 1, tracks)
        } else {
            currentQueue.addAll(tracks)
        }
        _playbackQueue.value = currentQueue
    }

    fun bulkAddTracksToPlaylist(playlistId: String, trackIds: List<String>) {
        viewModelScope.launch {
            trackIds.forEach { trackId ->
                musicRepository.addTrackToPlaylist(playlistId, trackId)
            }
        }
    }

    fun bulkDelete(tracks: List<TrackEntity>) {
        viewModelScope.launch {
            tracks.forEach { musicRepository.deleteTrack(it) }
        }
    }

    fun bulkUpload(tracks: List<TrackEntity>) {
        tracks.filter { it.localPath != null && it.gDriveId == null }.forEach { uploadTrack(it) }
    }

    fun removeFromLibrary(tracks: List<TrackEntity>) {
        viewModelScope.launch {
            tracks.forEach { musicRepository.deleteTrack(it) }
            // Note: In a real app, "remove from library" might just hide it, 
            // but for simplicity here we delete the record.
        }
    }

    fun downloadTrack(track: TrackEntity) {
        if (track.gDriveId == null) return
        val data = Data.Builder()
            .putString("sync_type", "download")
            .putString("file_id", track.gDriveId)
            .putString("file_name", "${track.title}.mp3")
            .build()
        WorkManager.getInstance(settingsRepository.context).enqueue(OneTimeWorkRequestBuilder<com.example.musicon.service.SyncWorker>().setInputData(data).build())
    }

    fun uploadTrack(track: TrackEntity) {
        if (track.localPath == null) return
        val data = Data.Builder()
            .putString("sync_type", "upload")
            .putString("file_path", track.localPath)
            .putString("file_name", track.displayName)
            .putString("track_id", track.id)
            .build()
        WorkManager.getInstance(settingsRepository.context).enqueue(OneTimeWorkRequestBuilder<com.example.musicon.service.SyncWorker>().setInputData(data).build())
    }

    fun updateSignInStatus(signedIn: Boolean) {
        _isUserSignedIn.value = signedIn
        if (signedIn) syncCloudTracks()
    }

    fun syncCloudTracks() {
        viewModelScope.launch {
            try { musicRepository.syncCloudTracks() } catch (e: Exception) {}
        }
    }

    fun scanLocalStorage() {
        viewModelScope.launch {
            try { musicRepository.scanLocalStorage() } catch (e: Exception) {}
        }
    }

    fun importLocalTracks(uris: List<Uri>) {
        viewModelScope.launch { musicRepository.importLocalTracks(uris) }
    }

    fun addTrackFromUrl(url: String, title: String) {
        viewModelScope.launch { musicRepository.addTrackFromUrl(url, title) }
    }

    init {
        // Initialize default "Favorite" playlist
        viewModelScope.launch {
            musicRepository.allPlaylists.first().let { playlists ->
                if (playlists.none { it.name == "Favorite" }) {
                    musicRepository.createPlaylist("Favorite")
                }
            }
        }
    }

    fun toggleFavorite(track: TrackEntity) {
        viewModelScope.launch {
            musicRepository.toggleFavorite(track)
            
            // Sync with "Favorite" playlist
            val playlists = musicRepository.allPlaylists.first()
            val favPlaylist = playlists.find { it.name == "Favorite" }
            if (favPlaylist != null) {
                if (!track.isFavorite) { 
                    musicRepository.addTrackToPlaylist(favPlaylist.id, track.id)
                } else {
                    musicRepository.removeTrackFromPlaylist(favPlaylist.id, track.id)
                }
            }
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch { musicRepository.createPlaylist(name) }
    }

    fun addTrackToPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch { musicRepository.addTrackToPlaylist(playlistId, trackId) }
    }

    // Preference Updates
    fun updateThemeMode(themeMode: ThemeMode) = viewModelScope.launch { settingsRepository.updateThemeMode(themeMode) }
    fun updateLibraryViewMode(mode: LibraryViewMode) = viewModelScope.launch { settingsRepository.updateLibraryViewMode(mode) }
    fun updatePauseOnDetach(enabled: Boolean) = viewModelScope.launch { settingsRepository.updatePauseOnDetach(enabled) }
    fun updateKeepScreenOn(enabled: Boolean) = viewModelScope.launch { settingsRepository.updateKeepScreenOn(enabled) }
    fun updateShowNotifications(enabled: Boolean) = viewModelScope.launch { settingsRepository.updateShowNotifications(enabled) }
    fun updateCrossfade(enabled: Boolean) = viewModelScope.launch { settingsRepository.updateCrossfade(enabled) }
}
