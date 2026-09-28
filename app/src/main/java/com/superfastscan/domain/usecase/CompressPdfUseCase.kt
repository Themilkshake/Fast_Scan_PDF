package com.superfastscan.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
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

/**
 * 3-Level PDF Compression configuration.
 */
enum class CompressionLevel(
    val targetWidth: Int,       // Target pixel width scaled to maintain aspect ratio
    val jpegQuality: Int,       // JPEG lossy compression percentage (1-100)
    val approxDpi: Int
) {
    /** Minimum Compression (Ultra Quality): ~220 DPI, ~88% image quality */
    MINIMUM(targetWidth = 1800, jpegQuality = 88, approxDpi = 220),

    /** Medium / Recommended Compression (Default): ~150 DPI, ~75% image quality */
    MEDIUM(targetWidth = 1240, jpegQuality = 75, approxDpi = 150),

    /** Maximum Compression (High Compression): ~90 DPI, ~50% image quality */
    MAXIMUM(targetWidth = 750, jpegQuality = 50, approxDpi = 90)
}

/**
 * Result returned after a successful compression operation.
 */
data class CompressionResult(
    val outputPath: String,
    val outputUri: Uri?,
    val outputFileName: String,
    val originalSizeBytes: Long,
    val compressedSizeBytes: Long,
    val savedPercentage: Int
)

/**
 * Native PDF Compression Use Case.
 *
 * Utilizes Android's native PDFium engine [PdfPageRenderer] and [FastPdfWriter]
 * to perform true native downsampling, lossy JPEG stream re-encoding, and metadata stripping
 * with memory-bounded page-by-page streaming.
 */
class CompressPdfUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(
        pdfUri: Uri,
        level: CompressionLevel,
        baseFileName: String? = null,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): CompressionResult = withContext(Dispatchers.IO) {
        val originalSize = getFileSize(pdfUri)
        val renderer = PdfPageRenderer(context)
        val timestamp = SimpleDateFormat("ddMMyyyy_HHmm", Locale.getDefault()).format(Date())
        val cleanBaseName = (baseFileName ?: "Document")
            .replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "")
            .ifBlank { "Document" }

        val compressedPagesData = mutableListOf<FastPdfWriter.PdfPageData>()

        renderer.openPdf(pdfUri) { pageCount, renderPage, _ ->
            if (pageCount <= 0) {
                throw IllegalStateException("PDF document has no pages")
            }

            for (pageIndex in 0 until pageCount) {
                onProgress(pageIndex + 1, pageCount)

                // 1. Render native page bitmap at target downsampled resolution
                val bitmap = renderPage(pageIndex, level.targetWidth)
                    ?: throw IllegalStateException("Failed to render page $pageIndex at ${level.targetWidth}px")

                // 2. Lossy compression with target JPEG quality
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, level.jpegQuality, stream)
                val jpegBytes = stream.toByteArray()

                compressedPagesData.add(
                    FastPdfWriter.PdfPageData(
                        width = bitmap.width,
                        height = bitmap.height,
                        jpegBytes = jpegBytes
                    )
                )

                // 3. Immediate native memory recycling to prevent OOM
                bitmap.recycle()
            }
        }

        val outputFileName = "${cleanBaseName}_compressed_$timestamp.pdf"
        val (savedPath, savedUri, compressedSize) = savePdfAndGetSize(compressedPagesData, outputFileName)

        val savedPercent = if (originalSize > 0 && compressedSize < originalSize) {
            (((originalSize - compressedSize).toDouble() / originalSize.toDouble()) * 100).toInt()
        } else {
            0
        }

        CompressionResult(
            outputPath = savedPath,
            outputUri = savedUri,
            outputFileName = outputFileName,
            originalSizeBytes = originalSize,
            compressedSizeBytes = compressedSize,
            savedPercentage = savedPercent
        )
    }

    fun getFileSize(uri: Uri): Long {
        try {
            // 1. Try file descriptor statSize (fastest & most accurate for Content URIs)
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val statSize = pfd.statSize
                if (statSize > 0) return statSize
            }
            // 2. Try MediaStore / ContentResolver Query
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
            // 3. Fallback: stream byte read
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                if (bytes.isNotEmpty()) return bytes.size.toLong()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0L
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
}
