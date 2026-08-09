package com.example.musicon.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String, // Can be GDrive ID or Local Path hash
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val bitrate: String? = null,
    val localPath: String? = null,
    val gDriveId: String? = null,
    val isDownloaded: Boolean = false,
    val isFavorite: Boolean = false,
    val lastPlayed: Long = 0,
    // Custom metadata overrides
    val customTitle: String? = null,
    val customArtist: String? = null,
    val customAlbum: String? = null,
    val customCoverPath: String? = null
) {
    val displayName: String get() = customTitle ?: title
    val displayArtist: String get() = customArtist ?: artist
    val displayAlbum: String get() = customAlbum ?: album
}
