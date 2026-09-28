package com.superfastscan.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import com.superfastscan.domain.model.CropRect
import com.superfastscan.domain.model.FilterType
import com.superfastscan.domain.model.ScanPage
import com.superfastscan.domain.repository.DocumentRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class ProcessDocumentUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
    @ApplicationContext private val context: Context
) {
    /**
     * Applies all edits (rotation, crop, filter) to a page and saves the processed image.
     * Original image is always preserved; edits produce a new file.
     */
    suspend fun processPage(page: ScanPage): ScanPage = withContext(Dispatchers.IO) {
        val originalBitmap = com.superfastscan.domain.util.BitmapUtils.decodeBitmap(context, page.originalImagePath, targetMaxDimension = 2400)
            ?: throw IllegalStateException("Could not decode image: ${page.originalImagePath}")

        var processedBitmap = originalBitmap

        // 1. Apply rotation
        if (page.rotation != 0f) {
            val matrix = Matrix().apply { postRotate(page.rotation) }
            processedBitmap = Bitmap.createBitmap(
                processedBitmap, 0, 0,
                processedBitmap.width, processedBitmap.height,
                matrix, true
            )
            if (processedBitmap != originalBitmap) originalBitmap.recycle()
        }

        // 2. Apply crop (normalized coordinates 0..1)
        val crop = page.cropRect
        if (crop != null && crop != CropRect()) {
            val w = processedBitmap.width
            val h = processedBitmap.height
            val left = (crop.left * w).toInt().coerceIn(0, w - 1)
            val top = (crop.top * h).toInt().coerceIn(0, h - 1)
            val right = (crop.right * w).toInt().coerceIn(left + 1, w)
            val bottom = (crop.bottom * h).toInt().coerceIn(top + 1, h)

            val prevBitmap = processedBitmap
            processedBitmap = Bitmap.createBitmap(
                processedBitmap, left, top,
                right - left, bottom - top
            )
            if (prevBitmap != processedBitmap) prevBitmap.recycle()
        }

        // 3. Apply filter
        if (page.filterType != FilterType.ORIGINAL) {
            processedBitmap = applyFilter(processedBitmap, page.filterType)
        }

        // Save processed image
        val scanDir = File(context.filesDir, "scans/${page.documentId}")
        if (!scanDir.exists()) {
            scanDir.mkdirs()
        }
        val editedFile = File(scanDir, "edited_page_${page.pageIndex}.jpg")

        val optimizedProcessedBitmap = com.superfastscan.domain.util.BitmapUtils.scaleBitmapForDocument(processedBitmap, maxDimension = 2400)
        FileOutputStream(editedFile).use { fos ->
            optimizedProcessedBitmap.compress(Bitmap.CompressFormat.JPEG, 88, fos)
        }
        if (optimizedProcessedBitmap != processedBitmap) {
            optimizedProcessedBitmap.recycle()
        }
        processedBitmap.recycle()

        val updatedPage = page.copy(editedImagePath = editedFile.absolutePath)
        documentRepository.updatePage(updatedPage)
        updatedPage
    }

    /**
     * Process all pages of a document for export.
     */
    suspend fun processAllPages(documentId: Long): List<ScanPage> = withContext(Dispatchers.IO) {
        val pages = documentRepository.getPagesList(documentId)
        pages.map { page -> processPage(page) }
    }

    private fun applyFilter(source: Bitmap, filterType: FilterType): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(getColorMatrix(filterType))
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        if (source != result) source.recycle()
        return result
    }

    private fun getColorMatrix(filterType: FilterType): ColorMatrix = when (filterType) {
        FilterType.GRAYSCALE -> ColorMatrix().apply { setSaturation(0f) }

        FilterType.BLACK_WHITE -> ColorMatrix(
            floatArrayOf(
                1.5f, 1.5f, 1.5f, 0f, -200f,
                1.5f, 1.5f, 1.5f, 0f, -200f,
                1.5f, 1.5f, 1.5f, 0f, -200f,
                0f, 0f, 0f, 1f, 0f
            )
        )

        FilterType.ENHANCED -> ColorMatrix(
            floatArrayOf(
                1.3f, 0f, 0f, 0f, 10f,
                0f, 1.3f, 0f, 0f, 10f,
                0f, 0f, 1.3f, 0f, 10f,
                0f, 0f, 0f, 1f, 0f
            )
        )

        FilterType.ORIGINAL -> ColorMatrix() // Identity
    }
}
