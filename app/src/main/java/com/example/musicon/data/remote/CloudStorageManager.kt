package com.example.musicon.data.remote

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.InputStreamContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class CloudStorageManager(private val context: Context) {

    suspend fun getOrCreateAppFolder(): String? = withContext(Dispatchers.IO) {
        val service = getDriveService() ?: return@withContext null
        try {
            val query = "name = 'MusicOn' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
            val result = service.files().list().setQ(query).setFields("files(id)").execute()
            val existing = result.files?.firstOrNull()?.id
            if (existing != null) return@withContext existing
            
            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = "MusicOn"
                mimeType = "application/vnd.google-apps.folder"
            }
            val folder = service.files().create(fileMetadata).setFields("id").execute()
            folder.id
        } catch (e: Exception) {
            android.util.Log.e("CloudStorageManager", "Folder creation failed", e)
            null
        }
    }

    suspend fun getAccessTokenAsync(): String? = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext null
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_READONLY)
        )
        credential.selectedAccount = account.account
        try {
            credential.getToken()
        } catch (e: Exception) {
            android.util.Log.e("CloudStorageManager", "Failed to get token", e)
            null
        }
    }

    private fun getDriveService(): Drive? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_READONLY)
        )
        credential.selectedAccount = account.account
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("MusicOn").build()
    }

    suspend fun listAudioFiles(folderId: String?): List<com.google.api.services.drive.model.File> = withContext(Dispatchers.IO) {
        val service = getDriveService() ?: return@withContext emptyList()
        val query = if (folderId != null) {
            "mimeType contains 'audio/' and '$folderId' in parents and trashed = false"
        } else {
            "mimeType contains 'audio/' and trashed = false"
        }
        
        try {
            val result = service.files().list()
                .setQ(query)
                .setFields("files(id, name, mimeType, size, thumbnailLink, hasThumbnail)")
                .execute()
            result.files ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("CloudStorageManager", "List files failed", e)
            emptyList()
        }
    }

    suspend fun downloadFile(fileId: String, destPath: String) = withContext(Dispatchers.IO) {
        val service = getDriveService() ?: return@withContext
        FileOutputStream(destPath).use { outputStream ->
            service.files().get(fileId).executeMediaAndDownloadTo(outputStream)
        }
    }

    suspend fun findFileByName(name: String): com.google.api.services.drive.model.File? = withContext(Dispatchers.IO) {
        val service = getDriveService() ?: return@withContext null
        val folderId = getOrCreateAppFolder() ?: return@withContext null
        val query = "name = '$name' and '$folderId' in parents and mimeType contains 'audio/' and trashed = false"
        try {
            val result = service.files().list()
                .setQ(query)
                .setFields("files(id, name)")
                .execute()
            result.files?.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteFile(fileId: String) = withContext(Dispatchers.IO) {
        val service = getDriveService() ?: return@withContext
        try {
            service.files().delete(fileId).execute()
        } catch (e: Exception) {
            android.util.Log.e("CloudStorageManager", "Delete failed", e)
        }
    }

    suspend fun renameFile(fileId: String, newName: String) = withContext(Dispatchers.IO) {
        val service = getDriveService() ?: return@withContext
        try {
            val fileMetadata = com.google.api.services.drive.model.File().apply {
                this.name = newName
            }
            service.files().update(fileId, fileMetadata).execute()
        } catch (e: Exception) {
            android.util.Log.e("CloudStorageManager", "Rename failed", e)
        }
    }

    suspend fun uploadData(name: String, data: String): String? = withContext(Dispatchers.IO) {
        val folderId = getOrCreateAppFolder()
        val service = getDriveService() ?: return@withContext null
        try {
            val fileMetadata = com.google.api.services.drive.model.File().apply {
                this.name = name
                if (folderId != null) this.parents = listOf(folderId)
            }
            val content = InputStreamContent("application/json", data.byteInputStream())
            val result = service.files().create(fileMetadata, content).setFields("id").execute()
            result.id
        } catch (e: Exception) {
            Log.e("CloudStorageManager", "Data upload failed", e)
            null
        }
    }

    suspend fun downloadData(fileId: String): String? = withContext(Dispatchers.IO) {
        val service = getDriveService() ?: return@withContext null
        try {
            val outputStream = ByteArrayOutputStream()
            service.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            outputStream.toString("UTF-8")
        } catch (e: Exception) {
            Log.e("CloudStorageManager", "Data download failed", e)
            null
        }
    }

    suspend fun deleteFileByName(name: String) = withContext(Dispatchers.IO) {
        val folderId = getOrCreateAppFolder()
        val service = getDriveService() ?: return@withContext
        try {
            val query = "name = '$name' and '$folderId' in parents and trashed = false"
            val result = service.files().list().setQ(query).setFields("files(id)").execute()
            result.files?.forEach { deleteFile(it.id) }
        } catch (e: Exception) {}
    }

    suspend fun uploadFile(filePath: String, name: String): String? = withContext(Dispatchers.IO) {
        val folderId = getOrCreateAppFolder()
        android.util.Log.d("CloudStorageManager", "Starting upload: $name from $filePath to $folderId")
        val service = getDriveService() ?: run {
            android.util.Log.e("CloudStorageManager", "Failed to get Drive service")
            return@withContext null
        }
        try {
            val fileMetadata = com.google.api.services.drive.model.File().apply {
                this.name = name
                if (folderId != null) {
                    this.parents = listOf(folderId)
                }
            }
            
            val mediaContent = if (filePath.startsWith("content://")) {
                val uri = android.net.Uri.parse(filePath)
                // Copy to temp file to avoid permission issues in background worker
                val tempFile = File(context.cacheDir, "upload_temp_${System.currentTimeMillis()}.mp3")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (!tempFile.exists() || tempFile.length() == 0L) {
                    android.util.Log.e("CloudStorageManager", "Temp file creation failed")
                    return@withContext null
                }
                val content = FileContent("audio/mpeg", tempFile)
                val result = service.files().create(fileMetadata, content).setFields("id").execute()
                tempFile.delete() // Cleanup
                result.id
            } else {
                val localFile = File(filePath)
                if (!localFile.exists()) {
                    android.util.Log.e("CloudStorageManager", "Local file does not exist: $filePath")
                    return@withContext null
                }
                val content = FileContent("audio/mpeg", localFile)
                val result = service.files().create(fileMetadata, content).setFields("id").execute()
                result.id
            }
            
            android.util.Log.d("CloudStorageManager", "Upload successful, ID: $mediaContent")
            mediaContent
        } catch (e: Exception) {
            android.util.Log.e("CloudStorageManager", "Upload failed", e)
            null
        }
    }
}
