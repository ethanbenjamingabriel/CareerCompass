package dev.hungrymonkey.careercompass.network

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import dev.hungrymonkey.careercompass.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class CoverLetterRepository {
    
    private val apiService = NetworkClient.coverLetterApiService
    
    suspend fun generateAndDownloadCoverLetter(
        userId: String,
        coverLetterId: String,
        context: Context
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("CoverLetterRepository", "Generating cover letter for user: $userId, cover letter: $coverLetterId")
                
                val request = GenerateCoverLetterRequest(
                    user_id = userId,
                    cover_letter_id = coverLetterId
                )
                
                val response = apiService.generateCoverLetter(request)
                
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        val contentDisposition = response.headers()["Content-Disposition"]
                        val fileName = if (contentDisposition != null && contentDisposition.contains("filename=")) {
                            val startIndex = contentDisposition.indexOf("filename=") + 9
                            val endIndex = contentDisposition.indexOf(";", startIndex).let { 
                                if (it == -1) contentDisposition.length else it 
                            }
                            contentDisposition.substring(startIndex, endIndex).trim('"')
                        } else {
                            "cover_letter_${System.currentTimeMillis()}.pdf"
                        }
                        
                        val file = savePdfToStorage(context, responseBody.bytes(), fileName)
                        
                        if (file != null) {
                            Log.d("CoverLetterRepository", "PDF saved successfully: ${file.absolutePath}")
                            Result.success(file.absolutePath)
                        } else {
                            Log.e("CoverLetterRepository", "Failed to save PDF file")
                            Result.failure(Exception("Failed to save PDF file"))
                        }
                    } ?: Result.failure(Exception("Empty response body"))
                } else {
                    Log.e("CoverLetterRepository", "API call failed: ${response.code()} - ${response.message()}")
                    Result.failure(Exception("Failed to generate cover letter: ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e("CoverLetterRepository", "Error generating cover letter", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun generateAndPreviewCoverLetter(
        userId: String,
        coverLetterId: String,
        context: Context
    ): Result<Uri> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("CoverLetterRepository", "Generating cover letter preview for user: $userId, cover letter: $coverLetterId")
                
                val request = GenerateCoverLetterRequest(
                    user_id = userId,
                    cover_letter_id = coverLetterId
                )
                
                val response = apiService.generateCoverLetter(request)
                
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        val contentDisposition = response.headers()["Content-Disposition"]
                        val fileName = if (contentDisposition != null && contentDisposition.contains("filename=")) {
                            val startIndex = contentDisposition.indexOf("filename=") + 9
                            val endIndex = contentDisposition.indexOf(";", startIndex).let { 
                                if (it == -1) contentDisposition.length else it 
                            }
                            contentDisposition.substring(startIndex, endIndex).trim('"')
                        } else {
                            "cover_letter_${System.currentTimeMillis()}.pdf"
                        }
                        
                        val file = savePdfToTempStorage(context, responseBody.bytes(), fileName)
                        
                        if (file != null) {
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}${AppConfig.Files.FILE_PROVIDER_AUTHORITY_SUFFIX}",
                                file
                            )
                            Log.d("CoverLetterRepository", "PDF preview ready: ${file.absolutePath}")
                            Result.success(uri)
                        } else {
                            Log.e("CoverLetterRepository", "Failed to save PDF file for preview")
                            Result.failure(Exception("Failed to save PDF file for preview"))
                        }
                    } ?: Result.failure(Exception("Empty response body"))
                } else {
                    Log.e("CoverLetterRepository", "API call failed: ${response.code()} - ${response.message()}")
                    Result.failure(Exception("Failed to generate cover letter preview: ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e("CoverLetterRepository", "Error generating cover letter preview", e)
                Result.failure(e)
            }
        }
    }
    
    private fun savePdfToTempStorage(context: Context, pdfBytes: ByteArray, fileName: String): File? {
        return try {
            val cacheDir = File(context.cacheDir, "cover_letter_previews")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            cleanupOldPreviewFiles(cacheDir)
            
            val tempFile = File(cacheDir, fileName)
            tempFile.writeBytes(pdfBytes)
            
            Log.d("CoverLetterRepository", "PDF preview saved to temp location: ${tempFile.absolutePath}")
            tempFile
        } catch (e: Exception) {
            Log.e("CoverLetterRepository", "Error saving PDF to temp storage", e)
            null
        }
    }
    
    private fun cleanupOldPreviewFiles(cacheDir: File) {
        try {
            val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000)
            cacheDir.listFiles()?.forEach { file ->
                if (file.lastModified() < oneHourAgo) {
                    file.delete()
                    Log.d("CoverLetterRepository", "Cleaned up old preview file: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e("CoverLetterRepository", "Error cleaning up old preview files", e)
        }
    }

    private fun savePdfToStorage(context: Context, pdfBytes: ByteArray, fileName: String): File? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                savePdfToDownloadsWithMediaStore(context, pdfBytes, fileName)
            } else {
                savePdfToDownloadsLegacy(context, pdfBytes, fileName)
            }
        } catch (e: Exception) {
            Log.e(AppConfig.Logging.LOG_TAG, "Error saving PDF file", e)
            null
        }
    }
    
    private fun savePdfToDownloadsWithMediaStore(context: Context, pdfBytes: ByteArray, fileName: String): File? {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        
        return uri?.let { insertUri ->
            resolver.openOutputStream(insertUri)?.use { outputStream ->
                outputStream.write(pdfBytes)
                outputStream.flush()
            }
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
        }
    }
    
    private fun savePdfToDownloadsLegacy(context: Context, pdfBytes: ByteArray, fileName: String): File? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        
        val file = File(downloadsDir, fileName)
        
        FileOutputStream(file).use { fos ->
            fos.write(pdfBytes)
            fos.flush()
        }
        
        return file
    }
}
