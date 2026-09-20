package com.example.musicon.data

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.musicon.data.local.*
import com.example.musicon.logic.MediaMetadataUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class MusicRepository(
    private val context: Context,
    private val trackDao: TrackDao,
    private val playlistDao: PlaylistDao,
    val cloudStorageManager: com.example.musicon.data.remote.CloudStorageManager
) {

    val allTracks: Flow<List<TrackEntity>> = trackDao.getAllTracks()
    val downloadedTracks: Flow<List<TrackEntity>> = trackDao.getDownloadedTracks()
    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists()

    suspend fun scanLocalStorage() = withContext(Dispatchers.IO) {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATA
        )
        
        // Filter for MP3 only and exclude recordings aggressively
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.MIME_TYPE} = ? AND ${MediaStore.Audio.Media.DATA} NOT LIKE ? AND ${MediaStore.Audio.Media.DATA} NOT LIKE ? AND ${MediaStore.Audio.Media.DATA} NOT LIKE ? AND ${MediaStore.Audio.Media.DATA} NOT LIKE ?"
        val selectionArgs = arrayOf("audio/mpeg", "%Recordings%", "%voice%", "%CallRecordings%", "%Recorder%")
        
        context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            
            val existingTracks = trackDao.getAllTracks().first()
            
            fun String.normalize() = this.lowercase().removeSuffix(".mp3").trim()
            
            val existingFingerprints = existingTracks.map { 
                "${it.displayName.normalize()}_${it.displayArtist.normalize()}" 
            }.toSet()
            val existingPaths = existingTracks.mapNotNull { it.localPath?.lowercase() }.toSet()

            // Cleanup: Check if local files in DB still exist on device
            existingTracks.forEach { existingTrack ->
                if (existingTrack.localPath != null && !existingTrack.localPath.startsWith("content://")) {
                    val file = java.io.File(existingTrack.localPath)
                    if (!file.exists()) {
                        android.util.Log.d("MusicRepository", "Removing missing track: ${existingTrack.displayName}")
                        trackDao.deleteTrack(existingTrack)
                    }
                }
            }

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val path = cursor.getString(dataColumn)
                
                if (existingPaths.contains(path.lowercase())) continue

                val contentUri = android.content.ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                val track = MediaMetadataUtils.extractMetadata(context, contentUri, id.toString())
                
                if (track != null) {
                    fun String.normalizeFingerprint() = this.lowercase().removeSuffix(".mp3").trim().replace(" ", "")
                    val fingerprint = "${track.displayName.normalizeFingerprint()}_${track.displayArtist.normalizeFingerprint()}"
                    
                    if (existingFingerprints.contains(fingerprint)) {
                        android.util.Log.d("MusicRepository", "Skipping duplicate local: ${track.displayName}")
                        continue
                    }
                    trackDao.insertTrack(track)
                }
            }
        }
    }

    suspend fun syncCloudTracks() {
        try {
            android.util.Log.d("MusicRepository", "Starting cloud sync...")
            val folderId = cloudStorageManager.getOrCreateAppFolder()
            android.util.Log.d("MusicRepository", "App folder ID: $folderId")
            
            val cloudFiles = cloudStorageManager.listAudioFiles(folderId)
            android.util.Log.d("MusicRepository", "Found ${cloudFiles.size} audio files on Drive")
            
            val allLocalTracks = trackDao.getAllTracks().first()
            
            cloudFiles.forEach { file ->
                fun String.normalizeFingerprint() = this.lowercase().removeSuffix(".mp3").trim().replace(" ", "")
                val cleanCloudName = file.name.normalizeFingerprint()
                
                val local = allLocalTracks.find { 
                    it.displayName.normalizeFingerprint() == cleanCloudName ||
                    it.title.normalizeFingerprint() == cleanCloudName ||
                    it.gDriveId == file.id
                }
                
                if (local == null) {
                    android.util.Log.d("MusicRepository", "Inserting new cloud track: ${file.name}")
                    trackDao.insertTrack(
                        TrackEntity(
                            id = file.id,
                            title = file.name.removeSuffix(".mp3"),
                            artist = "Cloud Artist",
                            album = "Google Drive",
                            duration = 0,
                            gDriveId = file.id,
                            isDownloaded = false,
                            customCoverPath = file.thumbnailLink // Store thumbnail
                        )
                    )
                } else {
                    // Merging: Update existing entry with cloud link
                    android.util.Log.d("MusicRepository", "Linking cloud file to existing track: ${local.displayName}")
                    val updatedLocal = local.copy(
                        gDriveId = file.id,
                        customCoverPath = local.customCoverPath ?: file.thumbnailLink
                    )
                    trackDao.updateTrack(updatedLocal)
                }
            }
            android.util.Log.d("MusicRepository", "Cloud sync completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("MusicRepository", "Cloud sync failed", e)
            throw e
        }
    }

    suspend fun importLocalTracks(uris: List<Uri>) {
        uris.forEach { uri ->
            try {
                // Copy file to internal storage
                val fileName = "local_${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}.mp3"
                val destFile = java.io.File(context.filesDir, "music/$fileName")
                destFile.parentFile?.mkdirs()
                
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val track = MediaMetadataUtils.extractMetadata(context, Uri.fromFile(destFile))
                if (track != null) {
                    trackDao.insertTrack(track.copy(localPath = destFile.absolutePath))
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicRepository", "Import failed for $uri", e)
            }
        }
    }

    suspend fun addTrackFromUrl(url: String, title: String) {
        val track = TrackEntity(
            id = "url_${url.hashCode()}",
            title = title,
            artist = "Web Stream",
            album = "Online",
            duration = 0,
            gDriveId = url,
            isDownloaded = false
        )
        trackDao.insertTrack(track)
    }

    suspend fun toggleFavorite(track: TrackEntity) {
        trackDao.updateTrack(track.copy(isFavorite = !track.isFavorite))
    }

    suspend fun createPlaylist(name: String): String {
        val id = java.util.UUID.randomUUID().toString()
        val playlist = Playlist(id = id, name = name)
        playlistDao.insertPlaylist(playlist)
        return id
    }

    suspend fun addTrackToPlaylist(playlistId: String, trackId: String) {
        playlistDao.addTrackToPlaylist(PlaylistTrack(playlistId, trackId))
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    suspend fun updatePlaylist(playlist: Playlist) {
        playlistDao.updatePlaylist(playlist)
    }

    suspend fun deletePlaylist(playlist: Playlist) {
        playlistDao.deletePlaylist(playlist)
    }

    fun getTracksForPlaylist(playlistId: String): Flow<List<TrackEntity>> {
        return playlistDao.getTracksForPlaylist(playlistId)
    }

    suspend fun removeTrack(track: TrackEntity) {
        trackDao.deleteTrack(track)
        // Physically delete file if it's a local file
        if (track.localPath != null && !track.localPath.startsWith("content://")) {
            try {
                val file = java.io.File(track.localPath)
                if (file.exists()) {
                    file.delete()
                    android.util.Log.d("MusicRepository", "Physically deleted file: ${track.localPath}")
                    
                    // Trigger MediaScanner refresh
                    val intent = android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    intent.data = android.net.Uri.fromFile(file)
                    context.sendBroadcast(intent)
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicRepository", "Failed to delete file: ${track.localPath}", e)
            }
        }
    }

    suspend fun recordTrackPlayed(trackId: String) {
        val track = trackDao.getTrackById(trackId) ?: return
        val updated = track.copy(
            playCount = track.playCount + 1,
            lastPlayed = System.currentTimeMillis()
        )
        trackDao.updateTrack(updated)
    }

    suspend fun getRecentlyPlayed(limit: Int): List<TrackEntity> {
        return trackDao.getRecentlyPlayed(limit)
    }

    suspend fun getMostPlayed(limit: Int): List<TrackEntity> {
        return trackDao.getMostPlayed(limit)
    }
    suspend fun insertTrack(track: TrackEntity) = trackDao.insertTrack(track)
    suspend fun updateTrack(track: TrackEntity) = trackDao.updateTrack(track)
    suspend fun deleteTrack(track: TrackEntity) = trackDao.deleteTrack(track)
    suspend fun getTrackById(id: String) = trackDao.getTrackById(id)

    suspend fun updateTrackMetadata(
        trackId: String,
        title: String?,
        artist: String?,
        album: String?,
        coverPath: String?,
        lyrics: String?,
        cloudId: String? = null
    ) {
        val track = trackDao.getTrackById(trackId) ?: return
        val updatedTrack = track.copy(
            customTitle = title ?: track.customTitle,
            customArtist = artist ?: track.customArtist,
            customAlbum = album ?: track.customAlbum,
            customCoverPath = coverPath ?: track.customCoverPath,
            lyrics = lyrics ?: track.lyrics,
            gDriveId = if (cloudId != null) (if (cloudId.isEmpty()) null else cloudId) else track.gDriveId
        )
        trackDao.updateTrack(updatedTrack)
    }
}
