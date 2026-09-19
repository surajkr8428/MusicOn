package com.example.musicon.ui.viewmodel

import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.FileProvider
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

    private val _isOnline = MutableStateFlow(true)
    val isOnline = _isOnline.asStateFlow()

    private val _isWifi = MutableStateFlow(false)
    val isWifi = _isWifi.asStateFlow()

    fun updateOnlineStatus(online: Boolean, isWifi: Boolean = false) {
        _isOnline.value = online
        _isWifi.value = isWifi
        if (online && isUserSignedIn.value) syncCloudTracks()
    }

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SPOTIFY_DARK)

    val libraryViewMode: StateFlow<LibraryViewMode> = settingsRepository.libraryViewModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryViewMode.LIST)

    val playerImageMode: StateFlow<com.example.musicon.data.PlayerImageMode> = settingsRepository.playerImageModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.musicon.data.PlayerImageMode.SQUARE)

    val allTracks: StateFlow<List<TrackEntity>> = musicRepository.allTracks
        .map { tracks ->
            // Filter only local files for the main library
            val localTracks = tracks.filter { it.localPath != null }
            localTracks.distinctBy { 
                val cleanTitle = (it.customTitle ?: it.title).lowercase().removeSuffix(".mp3").trim().replace(" ", "")
                val cleanArtist = (it.customArtist ?: it.artist).lowercase().trim().replace(" ", "")
                "${cleanTitle}_${cleanArtist}"
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<Playlist>> = musicRepository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val localTracksCount: StateFlow<Int> = allTracks.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val cloudTracksCount: StateFlow<Int> = musicRepository.allTracks
        .map { tracks -> tracks.count { it.gDriveId != null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

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

    val backgroundMode: StateFlow<String> = settingsRepository.backgroundModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DYNAMIC")

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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1L)

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

    private val _sessionRecentlyPlayed = MutableStateFlow<List<TrackEntity>>(emptyList())
    val sessionRecentlyPlayed: StateFlow<List<TrackEntity>> = _sessionRecentlyPlayed.asStateFlow()

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

    private var contentObserver: ContentObserver? = null

    fun startRealTimeSync() {
        if (contentObserver != null) return
        
        contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                android.util.Log.d("MainViewModel", "Storage change detected, scanning...")
                scanLocalStorage()
            }
        }
        
        settingsRepository.context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver!!
        )
        
        // Initial scan
        scanLocalStorage()
    }

    fun openFileLocation(track: TrackEntity) {
        val path = track.localPath ?: return
        val file = java.io.File(path)
        val parentDir = file.parentFile ?: return
        
        val context = settingsRepository.context
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            parentDir
        )
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "resource/folder")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback for file managers that don't support "resource/folder"
            val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                android.util.Log.e("MainViewModel", "Could not open folder", e2)
            }
        }
    }

    // Sleep Timer
    private var sleepTimerJob: Job? = null
    private val _sleepTimerRemaining = MutableStateFlow<Long?>(null)
    val sleepTimerRemaining: StateFlow<Long?> = _sleepTimerRemaining.asStateFlow()

    private val _isSleepTimerPaused = MutableStateFlow(false)
    val isSleepTimerPaused: StateFlow<Boolean> = _isSleepTimerPaused.asStateFlow()

    fun setSleepTimer(hours: Int, minutes: Int, seconds: Int = 0) {
        sleepTimerJob?.cancel()
        _isSleepTimerPaused.value = false
        if (hours == 0 && minutes == 0 && seconds == 0) {
            _sleepTimerRemaining.value = null
            return
        }
        _sleepTimerRemaining.value = (hours * 3600 + minutes * 60 + seconds) * 1000L
        startSleepTimerJob()
    }

    fun toggleSleepTimerPause() {
        if (_sleepTimerRemaining.value != null) {
            _isSleepTimerPaused.value = !_isSleepTimerPaused.value
        }
    }

    fun resetSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerRemaining.value = null
        _isSleepTimerPaused.value = false
    }

    private fun startSleepTimerJob() {
        sleepTimerJob = viewModelScope.launch {
            while ((_sleepTimerRemaining.value ?: 0) > 0) {
                if (!_isSleepTimerPaused.value) {
                    delay(1000)
                    _sleepTimerRemaining.value = (_sleepTimerRemaining.value ?: 0) - 1000
                } else {
                    delay(500)
                }
            }
            if (_sleepTimerRemaining.value != null) {
                _playbackCommand.emit(PlaybackCommand.STOP_PLAYBACK)
                delay(500)
                _playbackCommand.emit(PlaybackCommand.CLOSE_APP)
                _sleepTimerRemaining.value = null
            }
        }
    }

    private val _playbackCommand = MutableSharedFlow<PlaybackCommand>()
    val playbackCommand = _playbackCommand.asSharedFlow()

    enum class PlaybackCommand { PAUSE, CLOSE_APP, STOP_PLAYBACK }

    val songSortOrder: StateFlow<String> = settingsRepository.songSortOrderFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "NAME")

    fun updateSongSortOrder(order: String) = viewModelScope.launch { settingsRepository.updateSongSortOrder(order) }

    // Search & Filter
    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery

    val filteredTracks = combine(
        allTracks, 
        _searchQuery.asFlow(), 
        songSortOrder, 
        isUserSignedIn.asFlow()
    ) { tracks, query, sort, signedIn ->
        // Only show local songs in the main list
        val localOnly = tracks.filter { it.localPath != null }
        
        val localFiltered = if (query.isEmpty()) localOnly else {
            localOnly.filter { 
                it.displayName.contains(query, ignoreCase = true) ||
                it.displayArtist.contains(query, ignoreCase = true) ||
                it.displayAlbum.contains(query, ignoreCase = true)
            }
        }
        
        var list = localFiltered
        // If searching and signed in, we can still show cloud results if they match query but aren't local
        if (query.isNotEmpty() && isOnline.value && signedIn) {
            try {
                val cloudFiles = musicRepository.cloudStorageManager.listAudioFiles(null)
                val cloudTracks = cloudFiles.filter { 
                    it.name.contains(query, ignoreCase = true) 
                }.map { file ->
                    TrackEntity(
                        id = file.id, title = file.name, artist = "Cloud", album = "Google Drive",
                        duration = 0, gDriveId = file.id, isDownloaded = false
                    )
                }
                // Merge and remove duplicates (prefer local)
                val cloudOnlyMatches = cloudTracks.filter { ct -> 
                    list.none { it.title.equals(ct.title, true) || it.gDriveId == ct.gDriveId } 
                }
                list = list + cloudOnlyMatches
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Cloud search failed", e)
            }
        }

        // Final filtering to hide any unsynced cloud tracks if signed out
        if (!signedIn) {
            list = list.filter { it.localPath != null || it.isDownloaded }
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
            _sessionRecentlyPlayed.value = (listOf(track) + _sessionRecentlyPlayed.value).distinctBy { it.id }
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
            _sessionRecentlyPlayed.value = (listOf(startTrack) + _sessionRecentlyPlayed.value).distinctBy { it.id }
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

    fun updateCurrentTrackById(trackId: String) {
        _currentPlayingTrackId.value = trackId
        viewModelScope.launch {
            musicRepository.recordTrackPlayed(trackId)
            // Immediately save track change to persistence
            settingsRepository.updateLastPlaybackState(trackId, 0L)
            refreshStats()
        }
    }

    fun shareTrack(track: TrackEntity) {
        val path = track.localPath ?: return
        val file = java.io.File(path)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(settingsRepository.context, "${settingsRepository.context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        settingsRepository.context.startActivity(Intent.createChooser(intent, "Share Song").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun shareTracks(tracks: List<TrackEntity>) {
        if (tracks.isEmpty()) return
        val uris = ArrayList<Uri>()
        tracks.forEach { track ->
            track.localPath?.let { path ->
                val file = java.io.File(path)
                if (file.exists()) {
                    uris.add(FileProvider.getUriForFile(settingsRepository.context, "${settingsRepository.context.packageName}.fileprovider", file))
                }
            }
        }
        if (uris.isEmpty()) return
        
        val intent = Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
            type = "audio/*"
            if (uris.size == 1) putExtra(Intent.EXTRA_STREAM, uris[0])
            else putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        settingsRepository.context.startActivity(Intent.createChooser(intent, "Share Songs").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        viewModelScope.launch {
            val playlists = allPlaylists.value
            val playlist = playlists.find { it.id == playlistId } ?: return@launch
            musicRepository.updatePlaylist(playlist.copy(name = newName))
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            musicRepository.deletePlaylist(playlist)
        }
    }

    fun sharePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            val tracks = musicRepository.getTracksForPlaylist(playlist.id).first()
            if (tracks.isEmpty()) return@launch
            
            val uris = ArrayList<Uri>()
            tracks.forEach { track ->
                track.localPath?.let { path ->
                    val file = java.io.File(path)
                    if (file.exists()) {
                        uris.add(FileProvider.getUriForFile(settingsRepository.context, "${settingsRepository.context.packageName}.fileprovider", file))
                    }
                }
            }
            
            if (uris.isEmpty()) return@launch
            
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "audio/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            settingsRepository.context.startActivity(Intent.createChooser(intent, "Share Playlist").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
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
            val deletedIds = tracks.map { it.id }.toSet()
            if (_currentPlayingTrackId.value in deletedIds) {
                _playbackCommand.emit(PlaybackCommand.STOP_PLAYBACK)
                _currentPlayingTrackId.value = null
            }
            tracks.forEach { musicRepository.removeTrack(it) }
            refreshStats()
        }
    }

    fun bulkUpload(tracks: List<TrackEntity>) {
        val trackIds = tracks.map { it.id }.toTypedArray()
        val data = Data.Builder()
            .putString("sync_type", "bulk_upload")
            .putStringArray("track_ids", trackIds as Array<String?>)
            .build()
        WorkManager.getInstance(settingsRepository.context).enqueue(
            OneTimeWorkRequestBuilder<com.example.musicon.service.SyncWorker>()
                .setInputData(data)
                .build()
        )
    }

    fun removeFromLibrary(tracks: List<TrackEntity>) {
        viewModelScope.launch {
            val removedIds = tracks.map { it.id }.toSet()
            if (_currentPlayingTrackId.value in removedIds) {
                _playbackCommand.emit(PlaybackCommand.STOP_PLAYBACK)
                _currentPlayingTrackId.value = null
            }
            tracks.forEach { musicRepository.removeTrack(it) }
            refreshStats()
        }
    }

    fun syncAllLocalToCloud() {
        viewModelScope.launch {
            // 1. Refresh cloud state first to ensure we have latest GDrive IDs
            try { musicRepository.syncCloudTracks() } catch (e: Exception) {}
            
            // 2. Fetch latest local tracks
            val allLocal = musicRepository.allTracks.first()
            
            // 3. Optimized Sync: Only upload tracks that DON'T have a GDrive ID yet
            val unsyncedTracks = allLocal.filter { it.localPath != null && it.gDriveId == null }
            
            if (unsyncedTracks.isNotEmpty()) {
                android.util.Log.d("MainViewModel", "Smart Sync: Uploading ${unsyncedTracks.size} new tracks")
                bulkUpload(unsyncedTracks)
            } else {
                android.util.Log.d("MainViewModel", "Smart Sync: All tracks are already synchronized.")
            }
        }
    }

    fun bulkDownload(tracks: List<TrackEntity>) {
        tracks.forEach { downloadTrack(it) }
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

    fun playExternalFile(uri: Uri) {
        viewModelScope.launch {
            try {
                val track = com.example.musicon.logic.MediaMetadataUtils.extractMetadata(
                    settingsRepository.context, uri, "external_${System.currentTimeMillis()}"
                )
                if (track != null) {
                    playTrack(track)
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Failed to play external file", e)
            }
        }
    }

    init {
        // Initialize default "Favorite" playlist
        viewModelScope.launch {
            musicRepository.allPlaylists.first().let { playlists ->
                if (playlists.none { it.name == "Favorite" }) {
                    musicRepository.createPlaylist("Favorite")
                }
            }
            
            // Wait for tracks to be available (useful if scanning is ongoing)
            allTracks.filter { it.isNotEmpty() }.first().let { tracks ->
                val lastTrackId = settingsRepository.lastTrackIdFlow.first()
                val lastPos = settingsRepository.lastPositionFlow.first()
                val startTrack = tracks.find { it.id == lastTrackId } ?: tracks.first()
                
                _currentPlayingTrackId.value = startTrack.id
                _playbackQueue.value = tracks
                android.util.Log.d("MainViewModel", "Startup: Loaded ${tracks.size} tracks into queue. Default: ${startTrack.displayName}, position: $lastPos")
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

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch { musicRepository.removeTrackFromPlaylist(playlistId, trackId) }
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
    fun updateBackgroundMode(mode: String) = viewModelScope.launch { settingsRepository.updateBackgroundMode(mode) }
    fun updateShakeToSkip(enabled: Boolean) = viewModelScope.launch { settingsRepository.updateShakeToSkip(enabled) }
    fun updateCustomBackground(uri: String?) = viewModelScope.launch { settingsRepository.updateCustomBgUri(uri) }

    fun updateEqEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.updateEqEnabled(enabled) }
    fun updateEqBands(bands: String) = viewModelScope.launch { settingsRepository.updateEqBands(bands) }
    fun updateBassBoost(level: Int) = viewModelScope.launch { settingsRepository.updateBassBoost(level) }
    fun updateVirtualizer(level: Int) = viewModelScope.launch { settingsRepository.updateVirtualizer(level) }

    override fun onCleared() {
        contentObserver?.let {
            settingsRepository.context.contentResolver.unregisterContentObserver(it)
        }
        super.onCleared()
    }

    private fun <T> State<T>.asFlow(): Flow<T> = snapshotFlow { value }
}
