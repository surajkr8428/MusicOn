package com.example.musicon.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.musicon.data.local.MusicDatabase
import com.example.musicon.data.remote.CloudStorageManager
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
                val fileName = inputData.getString("file_name") ?: "temp.mp3"
                val destFile = File(applicationContext.filesDir, fileName)
                try {
                    cloudManager.downloadFile(fileId, destFile.absolutePath)
                    val existingTrack = database.trackDao().getTrackById(fileId)
                    if (existingTrack != null) {
                        database.trackDao().updateTrack(
                            existingTrack.copy(localPath = destFile.absolutePath, isDownloaded = true)
                        )
                    }
                    Result.success()
                } catch (e: Exception) {
                    Result.failure()
                }
            }
            "upload" -> {
                val filePath = inputData.getString("file_path") ?: return Result.failure()
                val fileName = inputData.getString("file_name") ?: "Uploaded Song"
                val trackId = inputData.getString("track_id") ?: return Result.failure()
                try {
                    val gDriveId = cloudManager.uploadFile(filePath, fileName, "14W_7EbfeM4oTwyXxS1FL7jt5Sf_6siCg")
                    if (gDriveId != null) {
                        val existingTrack = database.trackDao().getTrackById(trackId)
                        if (existingTrack != null) {
                            database.trackDao().updateTrack(existingTrack.copy(gDriveId = gDriveId))
                        }
                    }
                    Result.success()
                } catch (e: Exception) {
                    Result.failure()
                }
            }
            else -> Result.failure()
        }
    }
}
