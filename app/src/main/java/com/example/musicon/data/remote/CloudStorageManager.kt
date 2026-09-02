package com.example.musicon.data.remote

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class CloudStorageManager(private val context: Context) {

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

    suspend fun listAudioFiles(folderId: String? = null): List<com.google.api.services.drive.model.File> = withContext(Dispatchers.IO) {
        val service = getDriveService() ?: return@withContext emptyList()
        
        val query = if (folderId != null) {
            "mimeType contains 'audio/' and '$folderId' in parents"
        } else {
            "mimeType contains 'audio/'"
        }
        
        val result = service.files().list()
            .setQ(query)
            .setFields("files(id, name, mimeType, size)")
            .execute()
        result.files ?: emptyList()
    }

    suspend fun downloadFile(fileId: String, destPath: String) = withContext(Dispatchers.IO) {
        val service = getDriveService() ?: return@withContext
        val outputStream = FileOutputStream(destPath)
        service.files().get(fileId).executeMediaAndDownloadTo(outputStream)
        outputStream.close()
    }

    suspend fun findFileByName(name: String): com.google.api.services.drive.model.File? = withContext(Dispatchers.IO) {
        val service = getDriveService() ?: return@withContext null
        val query = "name = '$name' and mimeType contains 'audio/' and trashed = false"
        val result = service.files().list()
            .setQ(query)
            .setFields("files(id, name)")
            .execute()
        result.files?.firstOrNull()
    }

    suspend fun renameFile(fileId: String, newName: String) = withContext(Dispatchers.IO) {
        val service = getDriveService() ?: return@withContext
        val fileMetadata = com.google.api.services.drive.model.File().apply {
            this.name = newName
        }
        service.files().update(fileId, fileMetadata).execute()
    }

    suspend fun uploadFile(filePath: String, name: String, folderId: String? = null): String? = withContext(Dispatchers.IO) {
        android.util.Log.d("CloudStorageManager", "Starting upload: $name from $filePath")
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
            val localFile = File(filePath)
            if (!localFile.exists()) {
                android.util.Log.e("CloudStorageManager", "Local file does not exist: $filePath")
                return@withContext null
            }
            val mediaContent = FileContent("audio/mpeg", localFile)
            
            val result = service.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()
            android.util.Log.d("CloudStorageManager", "Upload successful, ID: ${result.id}")
            result.id
        } catch (e: Exception) {
            android.util.Log.e("CloudStorageManager", "Upload failed", e)
            null
        }
    }
}
