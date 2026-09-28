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
import com.superfastscan.data.remote.StirlingPdfException
import com.superfastscan.domain.util.NetworkUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class OfficeTargetFormat(val extension: String, val mimeType: String) {
    DOCX(
        extension = "docx",
        mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    ),
    PPT(
        extension = "pptx",
        mimeType = "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    ),
    EXCEL(
        extension = "xlsx",
        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )
}

data class PdfToOfficeResult(
    val outputPath: String,
    val outputUri: Uri?,
    val outputFileName: String,
    val fileSize: Long,
    val format: OfficeTargetFormat
)

/**
 * Server-Side PDF to Office (Word .docx, PowerPoint .pptx, Excel .xlsx) Conversion Use Case.
 *
 * Exclusively processed by the Stirling-PDF conversion engine via Cloudflare Tunnel.
 * Does not fall back to local plain-text extraction to guarantee 100% document fidelity.
 */
class PdfToOfficeUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stirlingPdfConversionService: StirlingPdfConversionService
) {
    suspend operator fun invoke(
        pdfUri: Uri,
        targetFormat: OfficeTargetFormat,
        outputBaseName: String? = null
    ): PdfToOfficeResult = withContext(Dispatchers.IO) {
        val rawName = getFileName(pdfUri)?.substringBeforeLast(".") ?: "Document"
        val timestamp = SimpleDateFormat("ddMMyyyy_HHmm", Locale.getDefault()).format(Date())
        val cleanName = (outputBaseName ?: "${rawName}_converted_$timestamp")
            .replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "")
            .ifBlank { "Document_$timestamp" }

        val finalFileName = "$cleanName.${targetFormat.extension}"
        val fileSize = getFileSize(pdfUri)

        val tempFile = File(context.cacheDir, "temp_office_${System.currentTimeMillis()}.${targetFormat.extension}")
        if (tempFile.exists()) tempFile.delete()

        try {
            // Stream to Stirling-PDF server endpoint
            val inputStream = context.contentResolver.openInputStream(pdfUri)
                ?: throw IOException("PDF dosyası okunamadı: $rawName")

            try {
                inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        when (targetFormat) {
                            OfficeTargetFormat.DOCX -> {
                                stirlingPdfConversionService.convertPdfToWord(
                                    fileName = "$cleanName.pdf",
                                    pdfInputStream = input,
                                    fileSizeBytes = fileSize,
                                    destinationStream = output,
                                    outputFormat = "docx"
                                )
                            }
                            OfficeTargetFormat.PPT -> {
                                stirlingPdfConversionService.convertPdfToPresentation(
                                    fileName = "$cleanName.pdf",
                                    pdfInputStream = input,
                                    fileSizeBytes = fileSize,
                                    destinationStream = output,
                                    outputFormat = "pptx"
                                )
                            }
                            OfficeTargetFormat.EXCEL -> {
                                stirlingPdfConversionService.convertPdfToExcel(
                                    fileName = "$cleanName.pdf",
                                    pdfInputStream = input,
                                    fileSizeBytes = fileSize,
                                    destinationStream = output,
                                    pageNumbers = "all"
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (!NetworkUtils.isNetworkAvailable(context)) {
                    throw IOException(context.getString(R.string.error_no_internet), e)
                } else {
                    throw IOException(context.getString(R.string.error_server_connection), e)
                }
            }

            if (!tempFile.exists() || tempFile.length() == 0L) {
                throw IOException("Dönüştürülen dosya oluşturulamadı veya boş döndü.")
            }

            val (savedPath, savedUri, size) = saveFileToStorage(tempFile, finalFileName, targetFormat.mimeType)

            PdfToOfficeResult(
                outputPath = savedPath,
                outputUri = savedUri,
                outputFileName = finalFileName,
                fileSize = size,
                format = targetFormat
            )
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    private fun saveFileToStorage(sourceFile: File, fileName: String, mimeType: String): Triple<String, Uri?, Long> {
        val size = sourceFile.length()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SuperFastScan")
            }

            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: throw IllegalStateException("Dosya kaydı oluşturulamadı")

            context.contentResolver.openOutputStream(uri)?.use { fos ->
                FileInputStream(sourceFile).use { fis ->
                    fis.copyTo(fos)
                }
            } ?: throw IllegalStateException("Çıktı akışı açılamadı")

            Triple(uri.toString(), uri, size)
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "SuperFastScan"
            )
            if (!dir.exists()) dir.mkdirs()

            val targetFile = File(dir, fileName)
            FileInputStream(sourceFile).use { fis ->
                FileOutputStream(targetFile).use { fos ->
                    fis.copyTo(fos)
                }
            }

            Triple(targetFile.absolutePath, Uri.fromFile(targetFile), size)
        }
    }

    private fun getFileName(uri: Uri): String? {
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

    private fun getFileSize(uri: Uri): Long {
        var size: Long = 0
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1 && cursor.moveToFirst()) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        }
        if (size <= 0) {
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    size = pfd.statSize
                }
            } catch (_: Exception) {}
        }
        return size
    }
}
