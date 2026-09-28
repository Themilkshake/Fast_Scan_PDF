package com.superfastscan.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
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

/**
 * Flattens a signature bitmap onto a specific page of a PDF document
 * using normalized [0.0 .. 1.0] relative coordinates.
 *
 * Normalized coordinates guarantee 100% position and scale fidelity regardless of
 * screen density, device orientation (portrait/landscape), or rendering resolution.
 */
class FlattenSignatureUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class SignaturePlacement(
        val pageIndex: Int,
        val normalizedX: Float,      // X as a fraction of page width [0.0 .. 1.0]
        val normalizedY: Float,      // Y as a fraction of page height [0.0 .. 1.0]
        val normalizedWidth: Float,  // Signature width as a fraction of page width [0.0 .. 1.0]
        val normalizedHeight: Float  // Signature height as a fraction of page height [0.0 .. 1.0]
    )

    /**
     * Renders each page at high resolution, stamps the signature on the target page,
     * and writes a new PDF to device storage.
     *
     * @return The file path or content URI of the saved PDF
     */
    suspend operator fun invoke(
        pdfUri: Uri,
        signatureBitmap: Bitmap,
        placement: SignaturePlacement,
        fileName: String? = null
    ): String = withContext(Dispatchers.IO) {
        val renderer = PdfPageRenderer(context)
        val highResMaxWidth = 1800 // High-res render for razor-sharp text and signature

        val pdfPages = renderer.openPdf(pdfUri) { pageCount, renderPage, _ ->
            (0 until pageCount).map { pageIndex ->
                // Render the page at high resolution
                val pageBitmap = renderPage(pageIndex, highResMaxWidth)
                    ?: throw IllegalStateException("Failed to render page $pageIndex")

                val finalBitmap = if (pageIndex == placement.pageIndex) {
                    // This is the page where the signature goes — overlay it
                    stampSignature(pageBitmap, signatureBitmap, placement)
                } else {
                    pageBitmap
                }

                // Compress to JPEG
                val jpegStream = ByteArrayOutputStream()
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, jpegStream)
                val pageData = FastPdfWriter.PdfPageData(
                    width = finalBitmap.width,
                    height = finalBitmap.height,
                    jpegBytes = jpegStream.toByteArray()
                )

                // Recycle bitmaps
                if (finalBitmap != pageBitmap) pageBitmap.recycle()
                finalBitmap.recycle()

                pageData
            }
        }

        val sanitizedName = (fileName ?: generateFileName())
            .replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "")
            .ifBlank { generateFileName() }

        savePdfToStorage(pdfPages, "$sanitizedName.pdf")
    }

    /**
     * Stamps the signature bitmap onto the page bitmap using normalized coordinates.
     */
    private fun stampSignature(
        pageBitmap: Bitmap,
        signatureBitmap: Bitmap,
        placement: SignaturePlacement
    ): Bitmap {
        val result = pageBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        // Exact pixel position on the high-res page bitmap
        val destX = placement.normalizedX * pageBitmap.width.toFloat()
        val destY = placement.normalizedY * pageBitmap.height.toFloat()

        // Exact pixel dimensions on the high-res page bitmap
        val destWidth = (placement.normalizedWidth * pageBitmap.width.toFloat()).toInt().coerceAtLeast(1)
        val destHeight = (placement.normalizedHeight * pageBitmap.height.toFloat()).toInt().coerceAtLeast(1)

        // Scale the signature bitmap to the destination size
        val scaledSignature = Bitmap.createScaledBitmap(
            signatureBitmap,
            destWidth,
            destHeight,
            true // bilinear filtering for smooth edges
        )

        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }

        canvas.drawBitmap(scaledSignature, destX, destY, paint)
        scaledSignature.recycle()

        return result
    }

    private fun generateFileName(): String {
        return "Signed_${SimpleDateFormat("ddMMyyyy_HHmm", Locale.getDefault()).format(Date())}"
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
