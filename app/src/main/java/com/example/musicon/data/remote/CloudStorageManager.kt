package com.example.musicon.data.remote

import android.content.Context
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
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class CloudStorageManager(private val context: Context) {

    private val FOLDER_ID = "14W_7EbfeM4oTwyXxS1FL7jt5Sf_6siCg"

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

    suspend fun listAudioFiles(folderId: String? = FOLDER_ID): List<com.google.api.services.drive.model.File> = withContext(Dispatchers.IO) {
        val service = getDriveService() ?: return@withContext emptyList()
        val query = if (folderId != null) {
            "mimeType contains 'audio/' and '$folderId' in parents and trashed = false"
        } else {
            "mimeType contains 'audio/' and trashed = false"
        }
        
        try {
            val result = service.files().list()
                .setQ(query)
                .setFields("files(id, name, mimeType, size)")
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
        val query = "name = '$name' and '$FOLDER_ID' in parents and mimeType contains 'audio/' and trashed = false"
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

    suspend fun uploadFile(filePath: String, name: String, folderId: String? = FOLDER_ID): String? = withContext(Dispatchers.IO) {
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
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    android.util.Log.e("CloudStorageManager", "Could not open InputStream for URI: $filePath")
                    return@withContext null
                }
                InputStreamContent("audio/mpeg", inputStream)
            } else {
                val localFile = File(filePath)
                if (!localFile.exists()) {
                    android.util.Log.e("CloudStorageManager", "Local file does not exist: $filePath")
                    return@withContext null
                }
                FileContent("audio/mpeg", localFile)
            }
            
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
