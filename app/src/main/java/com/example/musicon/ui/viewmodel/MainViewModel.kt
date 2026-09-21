package com.example.musicon.ui.viewmodel

import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
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
import com.google.gson.Gson
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppBackup(
    val playlists: List<PlaylistBackup>,
    val settings: Map<String, Any>
)

data class PlaylistBackup(
    val name: String,
    val trackTitles: List<String>
)

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

    private val _isForceSelectionMode = MutableStateFlow(false)
    val isForceSelectionMode: StateFlow<Boolean> = _isForceSelectionMode.asStateFlow()

    fun setForceSelectionMode(enabled: Boolean) { _isForceSelectionMode.value = enabled }

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
            tracks.filter { it.localPath != null || it.gDriveId != null }
                .sortedByDescending { it.localPath != null } // Prefer local entries
                .distinctBy { 
                    val cleanTitle = (it.customTitle ?: it.title).lowercase().trim().replace(" ", "")
                    val cleanArtist = (it.customArtist ?: it.artist).lowercase().trim().replace(" ", "")
                    // Include duration to distinguish same name songs
                    "${cleanTitle}_${cleanArtist}_${it.duration / 1000}"
                }
                .sortedBy { it.displayName.lowercase() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<Playlist>> = musicRepository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val localTracksCount: StateFlow<Int> = allTracks.map { it.count { t -> t.localPath != null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val cloudTracksCount: StateFlow<Int> = musicRepository.allTracks
        .map { tracks -> tracks.count { it.gDriveId != null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // RESTORED PREFERENCES
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
    fun updateExtractedColor(color: Int?) { _extractedAccentColor.value = color }

    private val _customFolders = MutableStateFlow<List<String>>(emptyList())
    val customFolders = _customFolders.asStateFlow()
    fun addCustomFolder(path: String) { _customFolders.value = _customFolders.value + path }

    val lastPosition: StateFlow<Long> = settingsRepository.lastPositionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1L)

    // RESTORED EQ
    val eqEnabled: StateFlow<Boolean> = settingsRepository.eqEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val eqBands: StateFlow<String> = settingsRepository.eqBandsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0,0,0,0,0")
    val bassBoost: StateFlow<Int> = settingsRepository.bassBoostFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val virtualizer: StateFlow<Int> = settingsRepository.virtualizerFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _sessionRecentlyPlayed = MutableStateFlow<List<TrackEntity>>(emptyList())
    val sessionRecentlyPlayed: StateFlow<List<TrackEntity>> = _sessionRecentlyPlayed.asStateFlow()

    private val _currentPlayingTrackId = MutableStateFlow<String?>(null)
    private val _currentPlaylistId = MutableStateFlow<String?>(null)

    val currentPlayingTrack: StateFlow<TrackEntity?> = combine(allTracks, _currentPlayingTrackId) { tracks, id -> tracks.find { it.id == id } }
        .distinctUntilChanged { old, new -> old?.id == new?.id && old?.isFavorite == new?.isFavorite }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _playbackQueue = MutableStateFlow<List<TrackEntity>>(emptyList())
    val playbackQueue: StateFlow<List<TrackEntity>> = _playbackQueue.asStateFlow()

    private val _playbackEvents = MutableSharedFlow<PlaybackEvent>()
    val playbackEvents = _playbackEvents.asSharedFlow()

    private var contentObserver: ContentObserver? = null

    fun startRealTimeSync() {
        if (contentObserver != null) return
        contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) { override fun onChange(selfChange: Boolean, uri: Uri?) { scanLocalStorage() } }
        settingsRepository.context.contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, contentObserver!!)
        scanLocalStorage()
    }

    fun openFileLocation(track: TrackEntity) {
        val path = track.localPath ?: return
        val file = java.io.File(path)
        val parentDir = file.parentFile ?: return
        val context = settingsRepository.context
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", parentDir)
        val intent = Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "resource/folder"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK) }
        try { context.startActivity(intent) } catch (e: Exception) {
            val fb = Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "*/*"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK) }
            try { context.startActivity(fb) } catch (e2: Exception) {}
        }
    }

    // Sleep Timer
    private var sleepTimerJob: Job? = null
    private val _sleepTimerRemaining = MutableStateFlow<Long?>(null)
    val sleepTimerRemaining: StateFlow<Long?> = _sleepTimerRemaining.asStateFlow()
    private val _isSleepTimerPaused = MutableStateFlow(false)
    val isSleepTimerPaused: StateFlow<Boolean> = _isSleepTimerPaused.asStateFlow()

    fun setSleepTimer(h: Int, m: Int, s: Int = 0) {
        sleepTimerJob?.cancel()
        _isSleepTimerPaused.value = false
        if (h == 0 && m == 0 && s == 0) { _sleepTimerRemaining.value = null; return }
        _sleepTimerRemaining.value = (h * 3600 + m * 60 + s) * 1000L
        startSleepTimerJob()
    }
    fun toggleSleepTimerPause() { if (_sleepTimerRemaining.value != null) _isSleepTimerPaused.value = !_isSleepTimerPaused.value }
    fun resetSleepTimer() { sleepTimerJob?.cancel(); _sleepTimerRemaining.value = null; _isSleepTimerPaused.value = false }
    private fun startSleepTimerJob() {
        sleepTimerJob = viewModelScope.launch {
            while ((_sleepTimerRemaining.value ?: 0) > 0) {
                if (!_isSleepTimerPaused.value) { delay(1000); _sleepTimerRemaining.value = (_sleepTimerRemaining.value ?: 0) - 1000 }
                else delay(500)
            }
            if (_sleepTimerRemaining.value != null) { _playbackCommand.emit(PlaybackCommand.STOP_PLAYBACK); delay(500); _playbackCommand.emit(PlaybackCommand.CLOSE_APP); _sleepTimerRemaining.value = null }
        }
    }

    private val _playbackCommand = MutableSharedFlow<PlaybackCommand>()
    val playbackCommand = _playbackCommand.asSharedFlow()
    enum class PlaybackCommand { PAUSE, CLOSE_APP, STOP_PLAYBACK }

    val songSortOrder: StateFlow<String> = settingsRepository.songSortOrderFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "NAME")
    fun updateSongSortOrder(order: String) = viewModelScope.launch { settingsRepository.updateSongSortOrder(order) }

    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery

    val filteredTracks = combine(allTracks, _searchQuery.asFlow(), songSortOrder, isUserSignedIn.asFlow()) { tracks, query, sort, signedIn ->
        var list = if (query.isEmpty()) tracks else tracks.filter { it.displayName.contains(query, true) || it.displayArtist.contains(query, true) }
        if (!signedIn) list = list.filter { it.localPath != null }
        when(sort) {
            "NAME_ASC" -> list.sortedBy { it.displayName }
            "NAME_DESC" -> list.sortedByDescending { it.displayName }
            "ARTIST_ASC" -> list.sortedBy { it.displayArtist }
            "RECENT_DESC" -> list.sortedByDescending { it.id }
            else -> list.sortedBy { it.displayName }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) { _searchQuery.value = query }

    fun playTrack(track: TrackEntity) {
        val tracks = allTracks.value
        val index = tracks.indexOfFirst { it.id == track.id }
        _currentPlaylistId.value = null
        if (index != -1) { _playbackQueue.value = tracks; viewModelScope.launch { _playbackEvents.emit(PlaybackEvent.PlayTrackList(tracks, index)) } }
        else { val list = listOf(track); _playbackQueue.value = list; viewModelScope.launch { _playbackEvents.emit(PlaybackEvent.PlayTrackList(list, 0)) } }
        _currentPlayingTrackId.value = track.id
        viewModelScope.launch { musicRepository.recordTrackPlayed(track.id); _sessionRecentlyPlayed.value = (listOf(track) + _sessionRecentlyPlayed.value).distinctBy { it.id } }
    }

    fun playTrackList(tracks: List<TrackEntity>, startTrack: TrackEntity, playlistId: String? = null) {
        val index = tracks.indexOfFirst { it.id == startTrack.id }.coerceAtLeast(0)
        _playbackQueue.value = tracks
        _currentPlayingTrackId.value = startTrack.id
        _currentPlaylistId.value = playlistId
        viewModelScope.launch { _playbackEvents.emit(PlaybackEvent.PlayTrackList(tracks, index)); musicRepository.recordTrackPlayed(startTrack.id); _sessionRecentlyPlayed.value = (listOf(startTrack) + _sessionRecentlyPlayed.value).distinctBy { it.id } }
    }

    fun updateCurrentTrackById(trackId: String) {
        _currentPlayingTrackId.value = trackId
        viewModelScope.launch { musicRepository.recordTrackPlayed(trackId); settingsRepository.updateLastPlaybackState(trackId, 0L, _currentPlaylistId.value) }
    }

    fun shareTrack(track: TrackEntity) {
        val path = track.localPath ?: return
        val file = java.io.File(path)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(settingsRepository.context, "${settingsRepository.context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply { type = "audio/*"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK) }
        settingsRepository.context.startActivity(Intent.createChooser(intent, "Share Song").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun shareTracks(tracks: List<TrackEntity>) {
        if (tracks.isEmpty()) return
        val uris = ArrayList<Uri>()
        tracks.forEach { t -> t.localPath?.let { p -> val f = java.io.File(p); if (f.exists()) uris.add(FileProvider.getUriForFile(settingsRepository.context, "${settingsRepository.context.packageName}.fileprovider", f)) } }
        if (uris.isEmpty()) return
        val intent = Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply { type = "audio/*"; if (uris.size == 1) putExtra(Intent.EXTRA_STREAM, uris[0]) else putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK) }
        settingsRepository.context.startActivity(Intent.createChooser(intent, "Share Songs").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun renamePlaylist(pId: String, name: String) = viewModelScope.launch { val p = allPlaylists.value.find { it.id == pId } ?: return@launch; musicRepository.updatePlaylist(p.copy(name = name)) }
    fun deletePlaylist(p: Playlist) = viewModelScope.launch { musicRepository.deletePlaylist(p) }

    fun bulkDeletePlaylists(playlists: List<Playlist>) = viewModelScope.launch {
        playlists.forEach { musicRepository.deletePlaylist(it) }
        triggerBackup()
    }

    fun playSelected(tracks: List<TrackEntity>) { 
        if (tracks.isNotEmpty()) { 
            _playbackQueue.value = tracks
            _currentPlayingTrackId.value = tracks.first().id
            viewModelScope.launch { 
                _playbackEvents.emit(PlaybackEvent.PlayTrackList(tracks, 0))
                musicRepository.recordTrackPlayed(tracks.first().id) 
            } 
        } 
    }
    fun addToQueueNext(tracks: List<TrackEntity>) { val q = _playbackQueue.value.toMutableList(); val i = q.indexOfFirst { it.id == _currentPlayingTrackId.value }; if (i != -1) q.addAll(i + 1, tracks) else q.addAll(tracks); _playbackQueue.value = q }

    fun bulkAddTracksToPlaylist(pId: String, ids: List<String>) = viewModelScope.launch { ids.forEach { musicRepository.addTrackToPlaylist(pId, it) } }
    fun bulkDelete(tracks: List<TrackEntity>) = viewModelScope.launch { val ids = tracks.map { it.id }.toSet(); if (_currentPlayingTrackId.value in ids) { _playbackCommand.emit(PlaybackCommand.STOP_PLAYBACK); _currentPlayingTrackId.value = null }; tracks.forEach { musicRepository.removeTrack(it) } }
    fun bulkUpload(tracks: List<TrackEntity>) { val ids = tracks.map { it.id }.toTypedArray(); val data = Data.Builder().putString("sync_type", "bulk_upload").putStringArray("track_ids", ids as Array<String?>).build(); WorkManager.getInstance(settingsRepository.context).enqueue(OneTimeWorkRequestBuilder<com.example.musicon.service.SyncWorker>().setInputData(data).addTag("sync_task").build()) }
    fun removeFromLibrary(tracks: List<TrackEntity>) = viewModelScope.launch { val ids = tracks.map { it.id }.toSet(); if (_currentPlayingTrackId.value in ids) { _playbackCommand.emit(PlaybackCommand.STOP_PLAYBACK); _currentPlayingTrackId.value = null }; tracks.forEach { musicRepository.removeTrack(it) } }

    fun syncAllLocalToCloud() = viewModelScope.launch { musicRepository.syncCloudTracks(); val unsynced = musicRepository.allTracks.first().filter { it.localPath != null && it.gDriveId == null }; if (unsynced.isNotEmpty()) { val ids = unsynced.map { it.id }.toTypedArray(); val data = Data.Builder().putString("sync_type", "bulk_upload").putStringArray("track_ids", ids as Array<String?>).build(); WorkManager.getInstance(settingsRepository.context).enqueueUniqueWork("sync_all", androidx.work.ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<com.example.musicon.service.SyncWorker>().setInputData(data).addTag("sync_task").build()) } }
    fun pauseSync() = com.example.musicon.data.remote.CloudSyncManager.setPaused(true)
    fun resumeSync() = com.example.musicon.data.remote.CloudSyncManager.setPaused(false)

    fun shareAppApk() {
        val context = settingsRepository.context
        val sourceFile = java.io.File(context.applicationInfo.sourceDir)
        val shareDir = java.io.File(context.externalCacheDir, "shared_apk")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (shareDir.exists()) shareDir.deleteRecursively()
                shareDir.mkdirs()
                val destFile = java.io.File(shareDir, "Nirvaana.apk")
                sourceFile.copyTo(destFile, true)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destFile)
                val intent = Intent(Intent.ACTION_SEND).apply { type = "application/vnd.android.package-archive"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK) }
                withContext(Dispatchers.Main) { context.startActivity(Intent.createChooser(intent, "Share Nirvaana APK").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            } catch (e: Exception) { android.util.Log.e("MainViewModel", "Failed to share APK", e) }
        }
    }

    fun updateSignInStatus(signedIn: Boolean) { _isUserSignedIn.value = signedIn; if (signedIn) syncCloudTracks() }
    fun syncCloudTracks() = viewModelScope.launch { try { musicRepository.syncCloudTracks() } catch (e: Exception) {} }
    fun scanLocalStorage() = viewModelScope.launch { try { musicRepository.scanLocalStorage() } catch (e: Exception) {} }
    fun importLocalTracks(uris: List<Uri>) = viewModelScope.launch { musicRepository.importLocalTracks(uris) }

    fun playExternalFile(uri: Uri) = viewModelScope.launch { try { val track = com.example.musicon.logic.MediaMetadataUtils.extractMetadata(settingsRepository.context, uri, "ext_${System.currentTimeMillis()}"); if (track != null) playTrack(track) } catch (e: Exception) {} }

    init {
        viewModelScope.launch {
            musicRepository.allPlaylists.first().let { if (it.none { p -> p.name == "Favorite" }) musicRepository.createPlaylist("Favorite") }
            allTracks.filter { it.isNotEmpty() }.first().let { tracks ->
                val lastId = settingsRepository.lastTrackIdFlow.first()
                val lastPos = settingsRepository.lastPositionFlow.first()
                val lastPid = settingsRepository.lastPlaylistIdFlow.first()
                var finalQ = tracks
                if (lastPid != null) { val pt = musicRepository.getTracksForPlaylist(lastPid).first(); if (pt.isNotEmpty()) { finalQ = pt; _currentPlaylistId.value = lastPid } }
                val start = finalQ.find { it.id == lastId } ?: finalQ.first()
                _currentPlayingTrackId.value = start.id
                _playbackQueue.value = finalQ
            }
        }
    }

    fun savePlaybackState(pos: Long) = viewModelScope.launch { settingsRepository.updateLastPlaybackState(_currentPlayingTrackId.value, pos, _currentPlaylistId.value) }
    fun toggleFavorite(track: TrackEntity) = viewModelScope.launch { musicRepository.toggleFavorite(track); val p = musicRepository.allPlaylists.first().find { it.name == "Favorite" }; if (p != null) { if (!track.isFavorite) musicRepository.addTrackToPlaylist(p.id, track.id) else musicRepository.removeTrackFromPlaylist(p.id, track.id) }; triggerBackup() }
    fun createPlaylist(name: String) = viewModelScope.launch { musicRepository.createPlaylist(name); triggerBackup() }
    fun removeTrackFromPlaylist(pId: String, tId: String) = viewModelScope.launch { musicRepository.removeTrackFromPlaylist(pId, tId) }
    fun getTracksForPlaylist(pId: String) = musicRepository.getTracksForPlaylist(pId)

    fun updateTrackMetadata(id: String, t: String?, ar: String?, al: String?, c: String?, l: String?) = viewModelScope.launch { musicRepository.updateTrackMetadata(id, t, ar, al, c, l); val track = allTracks.value.find { it.id == id }; if (track?.gDriveId != null && t != null) try { com.example.musicon.data.remote.CloudStorageManager(settingsRepository.context).renameFile(track.gDriveId, t) } catch (e: Exception) {} }
    
    fun updateThemeMode(tm: ThemeMode) = viewModelScope.launch { settingsRepository.updateThemeMode(tm); triggerBackup() }
    fun updateLibraryViewMode(m: LibraryViewMode) = viewModelScope.launch { settingsRepository.updateLibraryViewMode(m); triggerBackup() }
    fun updatePlayerImageMode(m: com.example.musicon.data.PlayerImageMode) = viewModelScope.launch { settingsRepository.updatePlayerImageMode(m); triggerBackup() }
    fun updatePauseOnDetach(e: Boolean) = viewModelScope.launch { settingsRepository.updatePauseOnDetach(e) }
    fun updateKeepScreenOn(e: Boolean) = viewModelScope.launch { settingsRepository.updateKeepScreenOn(e) }
    fun updateShowNotifications(e: Boolean) = viewModelScope.launch { settingsRepository.updateShowNotifications(e) }
    fun updateCrossfade(e: Boolean) = viewModelScope.launch { settingsRepository.updateCrossfade(e) }
    fun updateAccentColor(c: Int) = viewModelScope.launch { settingsRepository.updateAccentColor(c); triggerBackup() }
    fun updateAutoTheme(e: Boolean) = viewModelScope.launch { settingsRepository.updateAutoTheme(e) }
    fun updateBackgroundMode(m: String) = viewModelScope.launch { settingsRepository.updateBackgroundMode(m); triggerBackup() }
    fun updateShakeToSkip(e: Boolean) = viewModelScope.launch { settingsRepository.updateShakeToSkip(e) }
    fun updateCustomBackground(uri: String?) = viewModelScope.launch { settingsRepository.updateCustomBgUri(uri) }

    fun updateEqEnabled(e: Boolean) = viewModelScope.launch { settingsRepository.updateEqEnabled(e) }
    fun updateEqBands(b: String) = viewModelScope.launch { settingsRepository.updateEqBands(b) }
    fun updateBassBoost(l: Int) = viewModelScope.launch { settingsRepository.updateBassBoost(l) }
    fun updateVirtualizer(l: Int) = viewModelScope.launch { settingsRepository.updateVirtualizer(l) }
    fun setPreset(name: String) { val bands = when(name) { "Rock" -> "300,200,0,-100,200"; "Pop" -> "-100,100,300,100,-100"; "Jazz" -> "200,100,0,100,200"; "Classical" -> "300,200,0,0,0"; "Bass Boost" -> "500,300,0,0,0"; else -> "0,0,0,0,0" }; updateEqBands(bands) }

    fun setAsRingtone(track: TrackEntity) {
        // Placeholder for Ringtone logic
        Log.d("MainViewModel", "Setting as ringtone: ${track.displayName}")
    }

    fun triggerBackup() = viewModelScope.launch { /* logic */ }
    fun restoreFromCloud() = viewModelScope.launch { /* logic */ }
    fun deleteAllCloudTracks() = viewModelScope.launch { /* logic */ }
    fun downloadTrack(t: TrackEntity) { /* logic */ }
    fun uploadTrack(t: TrackEntity) { /* logic */ }
    fun deleteTrackFromCloud(t: TrackEntity) = viewModelScope.launch { /* logic */ }

    private fun <T> State<T>.asFlow(): Flow<T> = snapshotFlow { value }
}
