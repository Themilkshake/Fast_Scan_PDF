package com.superfastscan.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.superfastscan.domain.repository.DocumentRepository
import com.superfastscan.domain.util.BitmapUtils
import com.superfastscan.domain.util.FastPdfWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class ExportPdfUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val processDocumentUseCase: ProcessDocumentUseCase,
    @ApplicationContext private val context: Context
) {
    /**
     * Exports all pages of a document as a single PDF file with direct DCT/JPEG streams.
     * Keeps text and photos razor-sharp while keeping PDF file size extremely small (~200KB - 400KB per page).
     *
     * Pages are processed one at a time to minimize peak memory usage on low-end devices.
     */
    suspend operator fun invoke(
        documentId: Long,
        fileName: String
    ): String = withContext(Dispatchers.IO) {
        val pages = documentRepository.getPagesList(documentId)

        val pdfPages = pages.map { page ->
            // Process individual page (rotation, crop, filter)
            val processedPage = processDocumentUseCase.processPage(page)
            val imagePath = processedPage.editedImagePath ?: processedPage.originalImagePath

            // Decode with inSampleSize targeting PDF export resolution
            val rawBitmap = BitmapUtils.decodeBitmap(context, imagePath, targetMaxDimension = 1800)
                ?: throw IllegalStateException("Cannot read page image: $imagePath")

            // Scale to exact target for crisp text at standard DPI
            val bitmap = BitmapUtils.scaleBitmapForDocument(rawBitmap, maxDimension = 1800)

            val jpegStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 82, jpegStream)
            val jpegBytes = jpegStream.toByteArray()
            jpegStream.reset() // Release internal buffer

            val pageData = FastPdfWriter.PdfPageData(
                width = bitmap.width,
                height = bitmap.height,
                jpegBytes = jpegBytes
            )

            // Immediately recycle bitmaps to free memory before next page
            if (bitmap !== rawBitmap) {
                rawBitmap.recycle()
            }
            bitmap.recycle()

            pageData
        }

        val sanitizedName = fileName.replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "")
            .ifBlank { "Scan_${System.currentTimeMillis()}" }

        val outputPath = savePdfToStorage(pdfPages, "$sanitizedName.pdf")

        // Update document title
        documentRepository.updateDocument(
            com.superfastscan.domain.model.ScanDocument(
                id = documentId,
                title = sanitizedName,
                isDraft = false,
                updatedAt = System.currentTimeMillis(),
                exportedFilePath = outputPath
            )
        )

        outputPath
    }

    private fun savePdfToStorage(pages: List<FastPdfWriter.PdfPageData>, fileName: String): String {
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

            uri.toString()
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

            file.absolutePath
        }
    }
}
