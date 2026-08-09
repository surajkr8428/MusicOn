package com.example.musicon.data

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.musicon.data.local.*
import com.example.musicon.logic.MediaMetadataUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
            MediaStore.Audio.Media.DATA
        )
        
        context.contentResolver.query(collection, projection, null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            
            while (cursor.moveToNext()) {
                val path = cursor.getString(dataColumn)
                val file = java.io.File(path)
                if (file.exists()) {
                    val track = MediaMetadataUtils.extractMetadata(context, Uri.fromFile(file))
                    if (track != null) {
                        trackDao.insertTrack(track.copy(localPath = file.absolutePath))
                    }
                }
            }
        }
    }

    suspend fun syncCloudTracks(folderId: String? = null) {
        val cloudFiles = cloudStorageManager.listAudioFiles(folderId)
        cloudFiles.forEach { file ->
            val existing = trackDao.getTrackById(file.id)
            if (existing == null) {
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
            }
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

    suspend fun createPlaylist(name: String) {
        val playlist = Playlist(id = java.util.UUID.randomUUID().toString(), name = name)
        playlistDao.insertPlaylist(playlist)
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

    suspend fun insertTrack(track: TrackEntity) = trackDao.insertTrack(track)
    suspend fun updateTrack(track: TrackEntity) = trackDao.updateTrack(track)
    suspend fun deleteTrack(track: TrackEntity) = trackDao.deleteTrack(track)
    suspend fun getTrackById(id: String) = trackDao.getTrackById(id)

    suspend fun updateTrackMetadata(
        trackId: String,
        title: String?,
        artist: String?,
        album: String?,
        coverPath: String?
    ) {
        val track = trackDao.getTrackById(trackId) ?: return
        val updatedTrack = track.copy(
            customTitle = title,
            customArtist = artist,
            customAlbum = album,
            customCoverPath = coverPath
        )
        trackDao.updateTrack(updatedTrack)
    }
}
