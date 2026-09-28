package com.superfastscan.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.superfastscan.domain.service.OcrResult
import com.superfastscan.domain.service.OcrService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class PerformOcrUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ocrService: OcrService
) {
    suspend fun recognizeText(imageUri: Uri): Result<OcrResult> {
        return ocrService.recognizeText(imageUri)
    }

    suspend fun saveTextToFile(text: String, baseFileName: String? = null): Pair<String, Uri?> = withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("ddMMyyyy_HHmm", Locale.getDefault()).format(Date())
        val cleanName = (baseFileName ?: "OCR_Extracted_$timestamp")
            .replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "")
            .ifBlank { "OCR_Extracted_$timestamp" }

        val fileName = if (cleanName.endsWith(".txt", ignoreCase = true)) cleanName else "$cleanName.txt"
        val bytes = text.toByteArray(StandardCharsets.UTF_8)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/SuperFastScan")
            }

            val uri = context.contentResolver.insert(
                MediaStore.Files.getContentUri("external"),
                contentValues
            ) ?: throw IllegalStateException("Failed to create text document entry")

            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(bytes)
            } ?: throw IllegalStateException("Failed to write to text document")

            Pair(uri.toString(), uri)
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "SuperFastScan"
            )
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, fileName)
            FileOutputStream(file).use { fos ->
                fos.write(bytes)
            }

            Pair(file.absolutePath, Uri.fromFile(file))
        }
    }
}
