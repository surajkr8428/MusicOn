package com.example.musicon.logic

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.musicon.data.local.TrackEntity
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object MediaMetadataUtils {
    private const val TAG = "MediaMetadataUtils"

    fun extractMetadata(context: Context, uri: Uri, id: String? = null): TrackEntity? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "Unknown Title"
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown Album"
            val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE) ?: "Unknown Genre"
            val duration = try {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            } catch (e: Exception) { 0L }
            
            val bitrateRaw = try {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toInt() ?: 0
            } catch (e: Exception) { 0 }
            val bitrate = if (bitrateRaw > 0) "${bitrateRaw / 1000}k" else "320k"
            
            val artBytes = retriever.embeddedPicture
            var artPath: String? = null
            if (artBytes != null) {
                try {
                    val coversDir = File(context.filesDir, "covers")
                    if (!coversDir.exists()) coversDir.mkdirs()
                    
                    val fileName = "cover_${id ?: UUID.randomUUID()}.jpg"
                    val file = File(coversDir, fileName)
                    FileOutputStream(file).use { it.write(artBytes) }
                    artPath = file.absolutePath
                    android.util.Log.d(TAG, "Saved cover to: $artPath")
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Failed to save cover art", e)
                }
            }

            TrackEntity(
                id = id ?: UUID.randomUUID().toString(),
                title = title,
                artist = artist,
                album = album,
                genre = genre,
                duration = duration,
                bitrate = bitrate,
                localPath = uri.toString(),
                isDownloaded = true,
                customCoverPath = artPath
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Metadata extraction failed for $uri", e)
            null
        } finally {
            retriever.release()
        }
    }
}
