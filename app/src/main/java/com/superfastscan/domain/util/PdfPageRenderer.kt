package com.superfastscan.domain.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Wraps Android's [PdfRenderer] to provide page-level bitmap rendering
 * from a PDF document referenced by a content:// URI.
 *
 * This class is designed for single-use: open → render pages → close.
 * It must be used within a coroutine scope on Dispatchers.IO.
 */
class PdfPageRenderer(
    private val context: Context
) {
    /**
     * Holds metadata about a single PDF page.
     *
     * @param widthPt  Page width in PDF points (1 point = 1/72 inch)
     * @param heightPt Page height in PDF points
     */
    data class PageInfo(
        val widthPt: Int,
        val heightPt: Int
    )

    /**
     * Opens a PDF from a content URI, invokes [block] with page count and
     * a render function, then automatically closes the renderer.
     *
     * Usage:
     * ```
     * renderer.openPdf(uri) { pageCount, renderPage ->
     *     val bitmap = renderPage(0, targetWidth)
     * }
     * ```
     */
    suspend fun <T> openPdf(
        uri: Uri,
        block: suspend (pageCount: Int, renderPage: suspend (pageIndex: Int, targetWidth: Int) -> Bitmap?, getPageInfo: (pageIndex: Int) -> PageInfo?) -> T
    ): T = withContext(Dispatchers.IO) {
        val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalStateException("Cannot open PDF file descriptor for $uri")

        val renderer = PdfRenderer(fileDescriptor)

        try {
            block(
                renderer.pageCount,
                { pageIndex, targetWidth -> renderPage(renderer, pageIndex, targetWidth) },
                { pageIndex -> getPageInfo(renderer, pageIndex) }
            )
        } finally {
            renderer.close()
            fileDescriptor.close()
        }
    }

    /**
     * Renders a single PDF page to a [Bitmap] scaled to [targetWidth] pixels,
     * preserving the original aspect ratio.
     *
     * The bitmap uses ARGB_8888 config with a white background (PDFs expect it).
     */
    private fun renderPage(renderer: PdfRenderer, pageIndex: Int, targetWidth: Int): Bitmap? {
        if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null

        val page = renderer.openPage(pageIndex)
        try {
            // PDF page dimensions are in points (72 DPI).
            // We scale to targetWidth pixels maintaining aspect ratio.
            val scale = targetWidth.toFloat() / page.width.toFloat()
            val targetHeight = (page.height * scale).toInt()

            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            // Fill with white — PDF standard background
            bitmap.eraseColor(android.graphics.Color.WHITE)

            page.render(
                bitmap,
                null, // null = full page
                null, // null = identity transform
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
            )

            return bitmap
        } finally {
            page.close()
        }
    }

    /**
     * Returns the page dimensions in PDF points for coordinate mapping.
     */
    private fun getPageInfo(renderer: PdfRenderer, pageIndex: Int): PageInfo? {
        if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null

        val page = renderer.openPage(pageIndex)
        val info = PageInfo(widthPt = page.width, heightPt = page.height)
        page.close()
        return info
    }
}
