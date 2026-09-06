package com.example.musicon.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.musicon.data.local.MusicDatabase
import com.example.musicon.data.remote.CloudStorageManager
import com.example.musicon.data.remote.CloudSyncManager
import com.example.musicon.data.remote.SyncStatus
import java.io.File

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val type = inputData.getString("sync_type") ?: "download"
        val cloudManager = CloudStorageManager(applicationContext)
        val database = MusicDatabase.getDatabase(applicationContext)

        return when (type) {
            "download" -> {
                val fileId = inputData.getString("file_id") ?: return Result.failure()
                val fileName = inputData.getString("file_name") ?: "downloaded_${System.currentTimeMillis()}.mp3"
                val musicDir = File(applicationContext.filesDir, "music")
                if (!musicDir.exists()) musicDir.mkdirs()
                val destFile = File(musicDir, fileName)
                try {
                    CloudSyncManager.updateStatus(SyncStatus.Loading("Downloading...", 0.1f))
                    cloudManager.downloadFile(fileId, destFile.absolutePath)
                    
                    val existingTrack = database.trackDao().getTrackById(fileId)
                    if (existingTrack != null) {
                        database.trackDao().updateTrack(
                            existingTrack.copy(
                                localPath = destFile.absolutePath, 
                                isDownloaded = true
                            )
                        )
                    }
                    CloudSyncManager.updateStatus(SyncStatus.Success("Download Complete"))
                    Result.success()
                } catch (e: Exception) {
                    android.util.Log.e("SyncWorker", "Download failed", e)
                    CloudSyncManager.updateStatus(SyncStatus.Error("Download failed: ${e.message}"))
                    Result.failure()
                }
            }
            "upload" -> {
                val filePath = inputData.getString("file_path") ?: return Result.failure()
                val fileName = inputData.getString("file_name") ?: "Uploaded Song"
                val trackId = inputData.getString("track_id") ?: return Result.failure()
                try {
                    CloudSyncManager.updateStatus(SyncStatus.Loading("Checking cloud for $fileName...", 0.05f))
                    
                    // Duplicate Protection: Check by name
                    val existingFile = cloudManager.findFileByName(fileName)
                    val gDriveId = if (existingFile != null) {
                        android.util.Log.d("SyncWorker", "Duplicate found: $fileName, using existing ID: ${existingFile.id}")
                        existingFile.id
                    } else {
                        CloudSyncManager.updateStatus(SyncStatus.Loading("Uploading $fileName...", 0.1f))
                        cloudManager.uploadFile(filePath, fileName)
                    }

                    if (gDriveId != null) {
                        val existingTrack = database.trackDao().getTrackById(trackId)
                        if (existingTrack != null) {
                            database.trackDao().updateTrack(existingTrack.copy(gDriveId = gDriveId))
                        }
                        CloudSyncManager.updateStatus(SyncStatus.Success("Upload Complete"))
                        Result.success()
                    } else {
                        CloudSyncManager.updateStatus(SyncStatus.Error("Upload failed: Storage or connection issue"))
                        Result.failure()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SyncWorker", "Upload error", e)
                    CloudSyncManager.updateStatus(SyncStatus.Error("Sync failed: ${e.localizedMessage ?: "Unknown error"}"))
                    Result.failure()
                }
            }
            else -> Result.failure()
        }
    }
}
