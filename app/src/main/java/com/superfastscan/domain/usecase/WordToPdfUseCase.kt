package com.superfastscan.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.superfastscan.R
import com.superfastscan.data.remote.StirlingPdfConversionService
import com.superfastscan.domain.util.NetworkUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class WordToPdfResult(
    val outputPath: String,
    val outputUri: Uri?,
    val outputFileName: String,
    val pdfSizeBytes: Long,
    val originalFileName: String,
    val originalSizeBytes: Long
)

/**
 * UseCase that converts Office documents (.docx, .doc, .pptx, .ppt, .xlsx, .xls) to PDF
 * via Stirling-PDF conversion service.
 */
class WordToPdfUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stirlingPdfConversionService: StirlingPdfConversionService
) {
    suspend operator fun invoke(
        documentUri: Uri,
        customFileName: String? = null
    ): WordToPdfResult = withContext(Dispatchers.IO) {
        val originalFileName = getFileName(documentUri) ?: "document.docx"
        val originalSize = getFileSize(documentUri)

        // 1. Prepare temp cache file to safely receive stream without corrupting storage on failures
        val tempPdfFile = File(context.cacheDir, "converted_${System.currentTimeMillis()}.pdf")
        if (tempPdfFile.exists()) tempPdfFile.delete()

        try {
            val inputStream = context.contentResolver.openInputStream(documentUri)
                ?: throw IOException("Cannot open input file: $originalFileName")

            val pdfBytesWritten = try {
                FileOutputStream(tempPdfFile).use { fos ->
                    stirlingPdfConversionService.convertOfficeToPdf(
                        fileName = originalFileName,
                        fileInputStream = inputStream,
                        fileSizeBytes = originalSize,
                        destinationStream = fos
                    )
                }
            } catch (e: Exception) {
                if (!NetworkUtils.isNetworkAvailable(context)) {
                    throw IOException(context.getString(R.string.error_no_internet), e)
                } else {
                    throw IOException(context.getString(R.string.error_server_connection), e)
                }
            }

            // 2. Generate final sanitized output name
            val baseName = if (!customFileName.isNullOrBlank()) {
                customFileName.trim()
            } else {
                originalFileName.substringBeforeLast(".")
            }

            val sanitizedName = baseName
                .replace(Regex("[^a-zA-Z0-9_\\-\\s\u00C0-\u017F]"), "")
                .ifBlank { "Word_Converted_${SimpleDateFormat("ddMMyyyy_HHmm", Locale.getDefault()).format(Date())}" }

            val finalPdfFileName = if (sanitizedName.endsWith(".pdf", ignoreCase = true)) {
                sanitizedName
            } else {
                "$sanitizedName.pdf"
            }

            // 3. Move/Save cached PDF to user documents storage
            val (savedPath, savedUri) = savePdfToStorage(tempPdfFile, finalPdfFileName)

            WordToPdfResult(
                outputPath = savedPath,
                outputUri = savedUri,
                outputFileName = finalPdfFileName,
                pdfSizeBytes = pdfBytesWritten,
                originalFileName = originalFileName,
                originalSizeBytes = originalSize
            )
        } finally {
            if (tempPdfFile.exists()) {
                tempPdfFile.delete()
            }
        }
    }

    private fun savePdfToStorage(sourceFile: File, fileName: String): Pair<String, Uri?> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/SuperFastScan")
            }

            val uri = context.contentResolver.insert(
                MediaStore.Files.getContentUri("external"),
                contentValues
            ) ?: throw IllegalStateException("Failed to create MediaStore entry")

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                FileInputStream(sourceFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: throw IllegalStateException("Failed to open output stream")

            Pair(uri.toString(), uri)
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "SuperFastScan"
            )
            if (!dir.exists()) dir.mkdirs()

            val destinationFile = File(dir, fileName)
            FileInputStream(sourceFile).use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            Pair(destinationFile.absolutePath, Uri.fromFile(destinationFile))
        }
    }

    fun getFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        }
        if (name == null) {
            name = uri.path?.let { path ->
                val cut = path.lastIndexOf('/')
                if (cut != -1) path.substring(cut + 1) else path
            }
        }
        return name
    }

    fun getFileSize(uri: Uri): Long {
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val statSize = pfd.statSize
                if (statSize > 0) return statSize
            }
            if (uri.scheme == "content") {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1 && cursor.moveToFirst()) {
                        val size = cursor.getLong(sizeIndex)
                        if (size > 0) return size
                    }
                }
            } else if (uri.scheme == "file") {
                uri.path?.let { path ->
                    val f = File(path)
                    if (f.exists()) return f.length()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0L
    }
}
