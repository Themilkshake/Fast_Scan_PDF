package com.superfastscan.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.superfastscan.domain.util.BitmapUtils
import com.superfastscan.domain.util.FastPdfWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class JpgToPdfResult(
    val outputPath: String,
    val outputUri: Uri?,
    val outputFileName: String,
    val pdfSizeBytes: Long,
    val totalImages: Int
)

class JpgToPdfUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(
        imageUris: List<Uri>,
        customFileName: String? = null,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): JpgToPdfResult = withContext(Dispatchers.IO) {
        if (imageUris.isEmpty()) {
            throw IllegalArgumentException("No images provided for PDF conversion")
        }

        val total = imageUris.size
        val pdfPages = mutableListOf<FastPdfWriter.PdfPageData>()

        for ((index, uri) in imageUris.withIndex()) {
            onProgress(index + 1, total)

            val rawBitmap = BitmapUtils.decodeBitmap(context, uri.toString())
                ?: throw IllegalStateException("Failed to decode image at index $index")

            val scaledBitmap = BitmapUtils.scaleBitmapForDocument(rawBitmap, maxDimension = 1800)

            val stream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            val jpegBytes = stream.toByteArray()

            pdfPages.add(
                FastPdfWriter.PdfPageData(
                    width = scaledBitmap.width,
                    height = scaledBitmap.height,
                    jpegBytes = jpegBytes
                )
            )

            if (scaledBitmap != rawBitmap) {
                rawBitmap.recycle()
            }
            scaledBitmap.recycle()
        }

        val timestamp = SimpleDateFormat("ddMMyyyy_HHmm", Locale.getDefault()).format(Date())
        val baseName = (customFileName ?: "Images_$timestamp")
            .replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "")
            .ifBlank { "Images_$timestamp" }

        val finalFileName = if (baseName.endsWith(".pdf", ignoreCase = true)) baseName else "$baseName.pdf"
        val (savedPath, savedUri, fileSize) = savePdfToStorage(pdfPages, finalFileName)

        JpgToPdfResult(
            outputPath = savedPath,
            outputUri = savedUri,
            outputFileName = finalFileName,
            pdfSizeBytes = fileSize,
            totalImages = total
        )
    }

    private fun savePdfToStorage(pages: List<FastPdfWriter.PdfPageData>, fileName: String): Triple<String, Uri?, Long> {
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
                FastPdfWriter.writePdf(pages, outputStream)
            } ?: throw IllegalStateException("Failed to open output stream")

            var size = 0L
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                size = pfd.statSize
            }

            Triple(uri.toString(), uri, size)
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "SuperFastScan"
            )
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, fileName)
            FileOutputStream(file).use { fos ->
                FastPdfWriter.writePdf(pages, fos)
            }

            Triple(file.absolutePath, Uri.fromFile(file), file.length())
        }
    }
}
