package com.superfastscan.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.superfastscan.domain.repository.DocumentRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class ExportImageUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val processDocumentUseCase: ProcessDocumentUseCase,
    @ApplicationContext private val context: Context
) {
    /**
     * Exports all pages of a document as individual JPEG images.
     * Returns the list of output file paths.
     * Pages are processed one at a time to minimize peak memory usage.
     */
    suspend operator fun invoke(
        documentId: Long,
        fileName: String
    ): List<String> = withContext(Dispatchers.IO) {
        val pages = documentRepository.getPagesList(documentId)
        val outputPaths = mutableListOf<String>()

        val sanitizedName = fileName.replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "")
            .ifBlank { "Scan_${System.currentTimeMillis()}" }

        pages.forEachIndexed { index, page ->
            // Process individual page (rotation, crop, filter)
            val processedPage = processDocumentUseCase.processPage(page)
            val imagePath = processedPage.editedImagePath ?: processedPage.originalImagePath

            // Decode with inSampleSize for memory efficiency
            val bitmap = com.superfastscan.domain.util.BitmapUtils.decodeBitmap(context, imagePath, targetMaxDimension = 1800)
                ?: throw IllegalStateException("Cannot read page image: $imagePath")

            val outputFileName = if (pages.size == 1) {
                "$sanitizedName.jpg"
            } else {
                "${sanitizedName}_page_${index + 1}.jpg"
            }

            val path = saveImageToStorage(bitmap, outputFileName)
            outputPaths.add(path)
            bitmap.recycle()
        }

        // Update document
        documentRepository.updateDocument(
            com.superfastscan.domain.model.ScanDocument(
                id = documentId,
                title = sanitizedName,
                isDraft = false,
                updatedAt = System.currentTimeMillis(),
                exportedFilePath = outputPaths.firstOrNull()
            )
        )

        outputPaths
    }

    private fun saveImageToStorage(bitmap: Bitmap, fileName: String): String {
        val optimizedBitmap = com.superfastscan.domain.util.BitmapUtils.scaleBitmapForDocument(bitmap, maxDimension = 1800)

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SuperFastScan")
                }

                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: throw IllegalStateException("Failed to create MediaStore entry")

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    optimizedBitmap.compress(Bitmap.CompressFormat.JPEG, 82, outputStream)
                } ?: throw IllegalStateException("Failed to open output stream")

                uri.toString()
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "SuperFastScan"
                )
                if (!dir.exists()) dir.mkdirs()

                val file = File(dir, fileName)
                FileOutputStream(file).use { fos ->
                    optimizedBitmap.compress(Bitmap.CompressFormat.JPEG, 82, fos)
                }

                file.absolutePath
            }
        } finally {
            if (optimizedBitmap != bitmap) {
                optimizedBitmap.recycle()
            }
        }
    }
}
