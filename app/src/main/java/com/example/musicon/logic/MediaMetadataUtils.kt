package com.example.musicon.logic

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.musicon.data.local.TrackEntity
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object MediaMetadataUtils {
    fun extractMetadata(context: Context, uri: Uri): TrackEntity? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "Unknown Title"
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown Album"
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            val bitrateRaw = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toInt() ?: 0
            val bitrate = if (bitrateRaw > 0) "${bitrateRaw / 1000}k" else "320k"
            
            // Extract and save album art
            val artBytes = retriever.embeddedPicture
            var artPath: String? = null
            if (artBytes != null) {
                val fileName = "thumb_${UUID.randomUUID()}.jpg"
                val file = File(context.cacheDir, fileName)
                FileOutputStream(file).use { it.write(artBytes) }
                artPath = file.absolutePath
            }

            TrackEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                artist = artist,
                album = album,
                duration = duration,
                bitrate = bitrate,
                localPath = uri.path, // Store the absolute path
                isDownloaded = true,
                customCoverPath = artPath
            )
        } catch (e: Exception) {
            android.util.Log.e("MediaMetadataUtils", "Metadata extraction failed", e)
            null
        } finally {
            retriever.release()
        }
    }
}
