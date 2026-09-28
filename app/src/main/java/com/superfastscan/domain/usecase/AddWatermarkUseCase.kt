package com.superfastscan.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.superfastscan.domain.util.FastPdfWriter
import com.superfastscan.domain.util.PdfPageRenderer
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

enum class WatermarkType {
    TEXT,
    IMAGE
}

enum class WatermarkPageScope {
    ALL_PAGES,
    EXCLUDE_FIRST, // Exclude Cover (Page 2+)
    FIRST_PAGE_ONLY,
    CUSTOM
}

data class WatermarkConfig(
    val type: WatermarkType = WatermarkType.TEXT,
    val text: String = "GİZLİDİR",
    val textColor: Long = 0xFFFF0000L, // Red by default
    val isBold: Boolean = true,
    val imageBitmap: Bitmap? = null,
    val opacity: Float = 0.30f, // 30% default (ideal 20%..40% range)
    val rotation: Float = -45f, // -45 deg diagonal by default
    val scale: Float = 1.2f,    // Prominent diagonal scale
    val normalizedX: Float = 0.5f, // Center X [0.0 .. 1.0]
    val normalizedY: Float = 0.5f, // Center Y [0.0 .. 1.0]
    val pageScope: WatermarkPageScope = WatermarkPageScope.ALL_PAGES,
    val customPages: String = ""
)

data class WatermarkResult(
    val outputPath: String,
    val outputUri: Uri?,
    val outputFileName: String,
    val pageCount: Int,
    val fileSizeBytes: Long
)

/**
 * High-performance Watermark PDF Use Case.
 *
 * Renders pages using native PDFium at ultra-crisp resolution (1800px width),
 * translates normalized coordinate math onto high-resolution Canvas matrices,
 * and streams compact, optimized PDF 1.4 DCTDecode files with instant memory recycling.
 */
class AddWatermarkUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(
        pdfUri: Uri,
        config: WatermarkConfig,
        baseFileName: String? = null,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): WatermarkResult = withContext(Dispatchers.IO) {
        val renderer = PdfPageRenderer(context)
        val highResWidth = 1800 // Ultra crisp high resolution
        val timestamp = SimpleDateFormat("ddMMyyyy_HHmm", Locale.getDefault()).format(Date())
        val cleanBaseName = (baseFileName ?: "Document")
            .replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "")
            .ifBlank { "Document" }

        val stampedPages = mutableListOf<FastPdfWriter.PdfPageData>()
        var total = 0

        renderer.openPdf(pdfUri) { pageCount, renderPage, _ ->
            if (pageCount <= 0) {
                throw IllegalStateException("PDF document has no pages")
            }
            total = pageCount

            val pageIndicesToStamp = when (config.pageScope) {
                WatermarkPageScope.ALL_PAGES -> (0 until pageCount).toSet()
                WatermarkPageScope.EXCLUDE_FIRST -> (1 until pageCount).toSet()
                WatermarkPageScope.FIRST_PAGE_ONLY -> setOf(0)
                WatermarkPageScope.CUSTOM -> parsePageRange(config.customPages, pageCount)
            }

            for (pageIndex in 0 until pageCount) {
                onProgress(pageIndex + 1, pageCount)

                val pageBitmap = renderPage(pageIndex, highResWidth)
                    ?: throw IllegalStateException("Failed to render page $pageIndex")

                val shouldStamp = pageIndicesToStamp.contains(pageIndex)
                val finalBitmap = if (shouldStamp) {
                    stampWatermark(pageBitmap, config)
                } else {
                    pageBitmap
                }

                val jpegStream = ByteArrayOutputStream()
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 88, jpegStream)
                val jpegBytes = jpegStream.toByteArray()

                stampedPages.add(
                    FastPdfWriter.PdfPageData(
                        width = finalBitmap.width,
                        height = finalBitmap.height,
                        jpegBytes = jpegBytes
                    )
                )

                if (finalBitmap != pageBitmap) {
                    pageBitmap.recycle()
                }
                finalBitmap.recycle()
            }
        }

        val outputFileName = "${cleanBaseName}_watermarked_$timestamp.pdf"
        val (savedPath, savedUri, writtenBytes) = savePdfAndGetSize(stampedPages, outputFileName)

        WatermarkResult(
            outputPath = savedPath,
            outputUri = savedUri,
            outputFileName = outputFileName,
            pageCount = total,
            fileSizeBytes = writtenBytes
        )
    }

    private fun stampWatermark(
        pageBitmap: Bitmap,
        config: WatermarkConfig
    ): Bitmap {
        val result = pageBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val targetCenterX = config.normalizedX * pageBitmap.width.toFloat()
        val targetCenterY = config.normalizedY * pageBitmap.height.toFloat()

        canvas.save()
        canvas.translate(targetCenterX, targetCenterY)
        canvas.rotate(config.rotation)

        val alphaInt = (config.opacity.coerceIn(0.05f, 1.0f) * 255).toInt()

        if (config.type == WatermarkType.TEXT) {
            val baseSize = pageBitmap.width * 0.10f // Base text size ~10% of page width at scale 1.0 (ideal for diagonal prominent coverage)
            val computedTextSize = baseSize * config.scale.coerceIn(0.2f, 4.0f)

            val paint = Paint().apply {
                isAntiAlias = true
                isDither = true
                textSize = computedTextSize
                typeface = if (config.isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                color = (config.textColor.toInt() and 0x00FFFFFF) or (alphaInt shl 24)
                textAlign = Paint.Align.CENTER
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    blendMode = android.graphics.BlendMode.MULTIPLY
                } else {
                    xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.MULTIPLY)
                }
            }

            val text = config.text.ifBlank { "WATERMARK" }
            val bounds = Rect()
            paint.getTextBounds(text, 0, text.length, bounds)
            val yOffset = bounds.height() / 2f - bounds.bottom

            canvas.drawText(text, 0f, yOffset, paint)
        } else if (config.type == WatermarkType.IMAGE && config.imageBitmap != null) {
            val baseImageWidth = pageBitmap.width * 0.45f // Base image width ~45% of page width
            val destWidth = (baseImageWidth * config.scale.coerceIn(0.2f, 4.0f)).toInt().coerceAtLeast(1)
            val aspect = config.imageBitmap.height.toFloat() / config.imageBitmap.width.toFloat().coerceAtLeast(1f)
            val destHeight = (destWidth * aspect).toInt().coerceAtLeast(1)

            val scaledImage = Bitmap.createScaledBitmap(config.imageBitmap, destWidth, destHeight, true)
            val paint = Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
                alpha = alphaInt
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    blendMode = android.graphics.BlendMode.MULTIPLY
                } else {
                    xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.MULTIPLY)
                }
            }

            canvas.drawBitmap(scaledImage, -destWidth / 2f, -destHeight / 2f, paint)
            scaledImage.recycle()
        }

        canvas.restore()
        return result
    }

    private fun savePdfAndGetSize(
        pages: List<FastPdfWriter.PdfPageData>,
        fileName: String
    ): Triple<String, Uri?, Long> {
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

            var writtenBytes = 0L
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                writtenBytes = FastPdfWriter.writePdf(pages, outputStream)
            } ?: throw IllegalStateException("Failed to open output stream")

            Triple(uri.toString(), uri, writtenBytes)
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "SuperFastScan"
            )
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, fileName)
            var writtenBytes = 0L
            FileOutputStream(file).use { fos ->
                writtenBytes = FastPdfWriter.writePdf(pages, fos)
            }

            Triple(file.absolutePath, Uri.fromFile(file), writtenBytes)
        }
    }

    private fun parsePageRange(rangeStr: String, totalPages: Int): Set<Int> {
        if (rangeStr.isBlank()) return (0 until totalPages).toSet()
        val result = mutableSetOf<Int>()
        val parts = rangeStr.split(",", ";", " ")
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed.contains("-")) {
                val sub = trimmed.split("-")
                if (sub.size == 2) {
                    val start = sub[0].toIntOrNull()?.minus(1) ?: continue
                    val end = sub[1].toIntOrNull()?.minus(1) ?: continue
                    for (p in minOf(start, end)..maxOf(start, end)) {
                        if (p in 0 until totalPages) result.add(p)
                    }
                }
            } else {
                val page = trimmed.toIntOrNull()?.minus(1)
                if (page != null && page in 0 until totalPages) {
                    result.add(page)
                }
            }
        }
        return if (result.isEmpty()) (0 until totalPages).toSet() else result
    }
}
