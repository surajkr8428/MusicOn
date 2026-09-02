package com.example.musicon.ui.viewmodel

import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class PlaybackEvent {
    data class PlayTrackList(val tracks: List<TrackEntity>, val startIndex: Int) : PlaybackEvent()
}

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

    val playerImageMode: StateFlow<com.example.musicon.data.PlayerImageMode> = settingsRepository.playerImageModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.musicon.data.PlayerImageMode.SQUARE)

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
    val accentColor: StateFlow<Int> = settingsRepository.accentColorFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0xFFBB86FC.toInt())
    val shakeToSkip: StateFlow<Boolean> = settingsRepository.shakeToSkipFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val customBgUri: StateFlow<String?> = settingsRepository.customBgUriFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val autoTheme: StateFlow<Boolean> = settingsRepository.autoThemeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _extractedAccentColor = MutableStateFlow<Int?>(null)
    val extractedAccentColor = _extractedAccentColor.asStateFlow()

    fun updateExtractedColor(color: Int?) {
        _extractedAccentColor.value = color
    }

    private val _customFolders = MutableStateFlow<List<String>>(emptyList())
    val customFolders = _customFolders.asStateFlow()

    fun addCustomFolder(path: String) {
        _customFolders.value = _customFolders.value + path
    }

    val lastPosition: StateFlow<Long> = settingsRepository.lastPositionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Equalizer & FX
    val eqEnabled: StateFlow<Boolean> = settingsRepository.eqEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val eqBands: StateFlow<String> = settingsRepository.eqBandsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0,0,0,0,0")
    val bassBoost: StateFlow<Int> = settingsRepository.bassBoostFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val virtualizer: StateFlow<Int> = settingsRepository.virtualizerFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Smart Playlists
    private val _recentlyPlayed = MutableStateFlow<List<TrackEntity>>(emptyList())
    val recentlyPlayed: StateFlow<List<TrackEntity>> = _recentlyPlayed.asStateFlow()

    private val _mostPlayed = MutableStateFlow<List<TrackEntity>>(emptyList())
    val mostPlayed: StateFlow<List<TrackEntity>> = _mostPlayed.asStateFlow()

    private val _currentPlayingTrackId = MutableStateFlow<String?>(null)
    val currentPlayingTrack: StateFlow<TrackEntity?> = combine(
        allTracks,
        _currentPlayingTrackId
    ) { tracks, id ->
        tracks.find { it.id == id }
    }.distinctUntilChanged { old, new -> old?.id == new?.id && old?.isFavorite == new?.isFavorite }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _playbackQueue = MutableStateFlow<List<TrackEntity>>(emptyList())
    val playbackQueue: StateFlow<List<TrackEntity>> = _playbackQueue.asStateFlow()

    private val _playbackEvents = MutableSharedFlow<PlaybackEvent>()
    val playbackEvents = _playbackEvents.asSharedFlow()

    // Sleep Timer
    private var sleepTimerJob: Job? = null
    private val _sleepTimerRemaining = MutableStateFlow<Long?>(null)
    val sleepTimerRemaining: StateFlow<Long?> = _sleepTimerRemaining.asStateFlow()

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes == 0) {
            _sleepTimerRemaining.value = null
            return
        }
        _sleepTimerRemaining.value = minutes * 60 * 1000L
        sleepTimerJob = viewModelScope.launch {
            while ((_sleepTimerRemaining.value ?: 0) > 0) {
                delay(1000)
                _sleepTimerRemaining.value = (_sleepTimerRemaining.value ?: 0) - 1000
            }
            _playbackCommand.emit(PlaybackCommand.PAUSE)
            _sleepTimerRemaining.value = null
        }
    }

    private val _playbackCommand = MutableSharedFlow<PlaybackCommand>()
    val playbackCommand = _playbackCommand.asSharedFlow()

    enum class PlaybackCommand { PAUSE }

    val songSortOrder: StateFlow<String> = settingsRepository.songSortOrderFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "NAME")

    fun updateSongSortOrder(order: String) = viewModelScope.launch { settingsRepository.updateSongSortOrder(order) }

    // Search & Filter
    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery

    val filteredTracks = combine(allTracks, _searchQuery.asFlow(), songSortOrder) { tracks, query, sort ->
        val list = if (query.isEmpty()) tracks else {
            tracks.filter { 
                it.displayName.contains(query, ignoreCase = true) ||
                it.displayArtist.contains(query, ignoreCase = true) ||
                it.displayAlbum.contains(query, ignoreCase = true)
            }
        }
        when(sort) {
            "NAME_ASC" -> list.sortedBy { it.displayName }
            "NAME_DESC" -> list.sortedByDescending { it.displayName }
            "ARTIST_ASC" -> list.sortedBy { it.displayArtist }
            "ARTIST_DESC" -> list.sortedByDescending { it.displayArtist }
            "DURATION_ASC" -> list.sortedBy { it.duration }
            "DURATION_DESC" -> list.sortedByDescending { it.duration }
            "RECENT_ASC" -> list.sortedBy { it.id }
            "RECENT_DESC" -> list.sortedByDescending { it.id }
            else -> list.sortedBy { it.displayName }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun playTrack(track: TrackEntity) {
        val tracks = allTracks.value
        val index = tracks.indexOfFirst { it.id == track.id }
        if (index != -1) {
            _playbackQueue.value = tracks
            viewModelScope.launch { 
                _playbackEvents.emit(PlaybackEvent.PlayTrackList(tracks, index)) 
            }
        } else {
            val list = listOf(track)
            _playbackQueue.value = list
            viewModelScope.launch { 
                _playbackEvents.emit(PlaybackEvent.PlayTrackList(list, 0)) 
            }
        }
        _currentPlayingTrackId.value = track.id
        viewModelScope.launch {
            musicRepository.recordTrackPlayed(track.id)
            refreshStats()
        }
    }

    fun playTrackList(tracks: List<TrackEntity>, startTrack: TrackEntity) {
        val index = tracks.indexOfFirst { it.id == startTrack.id }.coerceAtLeast(0)
        _playbackQueue.value = tracks
        _currentPlayingTrackId.value = startTrack.id
        viewModelScope.launch {
            _playbackEvents.emit(PlaybackEvent.PlayTrackList(tracks, index))
            musicRepository.recordTrackPlayed(startTrack.id)
            refreshStats()
        }
    }

    fun updateCurrentTrack(track: TrackEntity) {
        _currentPlayingTrackId.value = track.id
        viewModelScope.launch {
            musicRepository.recordTrackPlayed(track.id)
            refreshStats()
        }
    }

    private suspend fun refreshStats() {
        _recentlyPlayed.value = musicRepository.getRecentlyPlayed(20)
        _mostPlayed.value = musicRepository.getMostPlayed(20)
    }

    // Bulk Actions
    fun playSelected(tracks: List<TrackEntity>) {
        if (tracks.isNotEmpty()) {
            _playbackQueue.value = tracks
            _currentPlayingTrackId.value = tracks.first().id
            viewModelScope.launch {
                _playbackEvents.emit(PlaybackEvent.PlayTrackList(tracks, 0))
                musicRepository.recordTrackPlayed(tracks.first().id)
                refreshStats()
            }
        }
    }

    fun addToQueueNext(tracks: List<TrackEntity>) {
        val currentQueue = _playbackQueue.value.toMutableList()
        val currentIndex = currentQueue.indexOfFirst { it.id == _currentPlayingTrackId.value }
        if (currentIndex != -1) {
            currentQueue.addAll(currentIndex + 1, tracks)
        } else {
            currentQueue.addAll(tracks)
        }
        _playbackQueue.value = currentQueue
        // For simple queue additions, we don't necessarily restart playback, 
        // so we don't emit PlaybackEvent.PlayTrackList here unless the UI logic requires a total reset.
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
            tracks.forEach { musicRepository.removeTrack(it) }
        }
    }

    fun syncAllLocalToCloud() {
        bulkUpload(allTracks.value)
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
            
            // Resume last played track
            val lastTrackId = settingsRepository.lastTrackIdFlow.first()
            if (lastTrackId != null) {
                val track = musicRepository.getTrackById(lastTrackId)
                if (track != null) {
                    _currentPlayingTrackId.value = track.id
                    // Pre-fill queue with all songs starting from this one
                    val tracks = musicRepository.allTracks.first()
                    val index = tracks.indexOfFirst { it.id == track.id }
                    if (index != -1) {
                        _playbackQueue.value = tracks.drop(index)
                    } else {
                        _playbackQueue.value = listOf(track)
                    }
                }
            }
            
            refreshStats()
        }
    }

    fun savePlaybackState(position: Long) {
        viewModelScope.launch {
            settingsRepository.updateLastPlaybackState(_currentPlayingTrackId.value, position)
        }
    }

    fun toggleFavorite(track: TrackEntity) {
        viewModelScope.launch {
            val updatedIsFav = !track.isFavorite
            musicRepository.toggleFavorite(track)
            
            val playlists = musicRepository.allPlaylists.first()
            val favPlaylist = playlists.find { it.name == "Favorite" }
            if (favPlaylist != null) {
                if (updatedIsFav) { 
                    musicRepository.addTrackToPlaylist(favPlaylist.id, track.id)
                } else {
                    musicRepository.removeTrackFromPlaylist(favPlaylist.id, track.id)
                }
            }
        }
    }

    fun createPlaylist(name: String, tracksToAdd: List<String> = emptyList()) {
        viewModelScope.launch {
            val id = musicRepository.createPlaylist(name)
            tracksToAdd.forEach { musicRepository.addTrackToPlaylist(id, it) }
        }
    }

    fun addTrackToPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch { musicRepository.addTrackToPlaylist(playlistId, trackId) }
    }

    fun getTracksForPlaylist(playlistId: String): Flow<List<TrackEntity>> {
        return musicRepository.getTracksForPlaylist(playlistId)
    }

    // Presets
    fun setPreset(name: String) {
        val bands = when(name) {
            "Rock" -> "300,200,0,-100,200"
            "Pop" -> "-100,100,300,100,-100"
            "Jazz" -> "200,100,0,100,200"
            "Classical" -> "300,200,0,0,0"
            "Bass Boost" -> "500,300,0,0,0"
            else -> "0,0,0,0,0" // Flat
        }
        updateEqBands(bands)
    }

    fun updateTrackMetadata(
        trackId: String,
        title: String?,
        artist: String?,
        album: String?,
        coverPath: String?,
        lyrics: String?
    ) {
        viewModelScope.launch {
            musicRepository.updateTrackMetadata(trackId, title, artist, album, coverPath, lyrics)
            
            // If it's a cloud track, rename on Drive too
            val track = allTracks.value.find { it.id == trackId }
            if (track?.gDriveId != null && title != null) {
                try {
                    val cloudManager = com.example.musicon.data.remote.CloudStorageManager(settingsRepository.context)
                    cloudManager.renameFile(track.gDriveId, title)
                } catch (e: Exception) {
                    android.util.Log.e("MainViewModel", "Cloud rename failed", e)
                }
            }
        }
    }
    fun updateThemeMode(themeMode: ThemeMode) = viewModelScope.launch { settingsRepository.updateThemeMode(themeMode) }
    fun updateLibraryViewMode(mode: LibraryViewMode) = viewModelScope.launch { settingsRepository.updateLibraryViewMode(mode) }
    fun updatePlayerImageMode(mode: com.example.musicon.data.PlayerImageMode) = viewModelScope.launch { settingsRepository.updatePlayerImageMode(mode) }
    fun updatePauseOnDetach(enabled: Boolean) = viewModelScope.launch { settingsRepository.updatePauseOnDetach(enabled) }
    fun updateKeepScreenOn(enabled: Boolean) = viewModelScope.launch { settingsRepository.updateKeepScreenOn(enabled) }
    fun updateShowNotifications(enabled: Boolean) = viewModelScope.launch { settingsRepository.updateShowNotifications(enabled) }
    fun updateCrossfade(enabled: Boolean) = viewModelScope.launch { settingsRepository.updateCrossfade(enabled) }
    fun updateAccentColor(color: Int) = viewModelScope.launch { settingsRepository.updateAccentColor(color) }
    fun updateAutoTheme(enabled: Boolean) = viewModelScope.launch { settingsRepository.updateAutoTheme(enabled) }
    fun updateShakeToSkip(enabled: Boolean) = viewModelScope.launch { settingsRepository.updateShakeToSkip(enabled) }
    fun updateCustomBackground(uri: String?) = viewModelScope.launch { settingsRepository.updateCustomBgUri(uri) }

    fun updateEqEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.updateEqEnabled(enabled) }
    fun updateEqBands(bands: String) = viewModelScope.launch { settingsRepository.updateEqBands(bands) }
    fun updateBassBoost(level: Int) = viewModelScope.launch { settingsRepository.updateBassBoost(level) }
    fun updateVirtualizer(level: Int) = viewModelScope.launch { settingsRepository.updateVirtualizer(level) }

    private fun <T> State<T>.asFlow(): Flow<T> = snapshotFlow { value }
}
