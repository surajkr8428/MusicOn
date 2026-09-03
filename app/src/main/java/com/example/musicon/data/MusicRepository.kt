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
    private val cloudStorageManager: com.example.musicon.data.remote.CloudStorageManager
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
            val existingNames = existingTracks.map { it.title.lowercase() }.toSet()
            val existingDisplayNames = existingTracks.map { it.displayName.lowercase() }.toSet()
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
                val name = cursor.getString(nameColumn)
                val path = cursor.getString(dataColumn)
                
                // Duplicate Protection: Check if a track with this name or path already exists
                if (existingNames.contains(name.lowercase()) || 
                    existingDisplayNames.contains(name.lowercase()) ||
                    existingPaths.contains(path.lowercase())) {
                    android.util.Log.d("MusicRepository", "Skipping duplicate: $name")
                    continue
                }

                // Explicit check for 'call' in path to exclude more recordings
                if (path.lowercase().contains("call")) {
                    android.util.Log.d("MusicRepository", "Filtering out potential recording: $path")
                    continue
                }

                val contentUri = android.content.ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                val track = MediaMetadataUtils.extractMetadata(context, contentUri, id.toString())
                if (track != null) {
                    trackDao.insertTrack(track)
                }
            }
        }
    }

    suspend fun syncCloudTracks(folderId: String? = "14W_7EbfeM4oTwyXxS1FL7jt5Sf_6siCg") {
        try {
            val cloudFiles = cloudStorageManager.listAudioFiles(folderId)
            val allLocalTracks = trackDao.getAllTracks().first()
            
            cloudFiles.forEach { file ->
                // Duplicate Protection: Check if a track with same title already exists locally
                val alreadyExists = allLocalTracks.any { 
                    it.title.equals(file.name, ignoreCase = true) || 
                    it.displayName.equals(file.name, ignoreCase = true) ||
                    it.gDriveId == file.id
                }
                
                if (!alreadyExists) {
                    trackDao.insertTrack(
                        TrackEntity(
                            id = file.id,
                            title = file.name,
                            artist = "Cloud Artist",
                            album = "Cloud Album",
                            duration = 0,
                            gDriveId = file.id,
                            isDownloaded = false
                        )
                    )
                } else {
                    android.util.Log.d("MusicRepository", "Cloud sync: skipping duplicate or already synced file: ${file.name}")
                    // Optionally update the GDriveId if it was missing
                    val local = allLocalTracks.find { 
                        it.title.equals(file.name, ignoreCase = true) || it.displayName.equals(file.name, ignoreCase = true)
                    }
                    if (local != null && local.gDriveId == null) {
                        trackDao.updateTrack(local.copy(gDriveId = file.id))
                    }
                }
            }
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

    fun getTracksForPlaylist(playlistId: String): Flow<List<TrackEntity>> {
        return playlistDao.getTracksForPlaylist(playlistId)
    }

    suspend fun removeTrack(track: TrackEntity) {
        trackDao.deleteTrack(track)
        // Clean up files? 
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
        lyrics: String?
    ) {
        val track = trackDao.getTrackById(trackId) ?: return
        val updatedTrack = track.copy(
            customTitle = title,
            customArtist = artist,
            customAlbum = album,
            customCoverPath = coverPath,
            lyrics = lyrics
        )
        trackDao.updateTrack(updatedTrack)
    }
}
