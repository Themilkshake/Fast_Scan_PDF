package com.superfastscan.domain.util

import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Fast, lightweight PDF writer that embeds JPEG images directly into the PDF stream
 * using standard PDF /Filter /DCTDecode.
 *
 * Avoids Android's PdfDocument which stores uncompressed raw bitmaps resulting in 20-30MB files.
 * With FastPdfWriter, resulting PDF files are compact (~200KB - 400KB per page) with zero quality loss.
 */
object FastPdfWriter {

    data class PdfPageData(
        val width: Int,
        val height: Int,
        val jpegBytes: ByteArray
    )

    fun writePdf(pages: List<PdfPageData>, outputStream: OutputStream): Long {
        if (pages.isEmpty()) return 0L

        val offsets = mutableListOf<Long>()
        var currentOffset = 0L

        fun writeString(str: String) {
            val bytes = str.toByteArray(StandardCharsets.US_ASCII)
            outputStream.write(bytes)
            currentOffset += bytes.size
        }

        fun writeBytes(bytes: ByteArray) {
            outputStream.write(bytes)
            currentOffset += bytes.size
        }

        fun markObject(): Int {
            offsets.add(currentOffset)
            return offsets.size // 1-based object number
        }

        // 1. PDF Header
        writeString("%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n")

        val numPages = pages.size

        // 2. Object 1: Catalog
        val catalogObj = markObject()
        writeString("$catalogObj 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")

        // 3. Object 2: Pages root
        val pagesObj = markObject()
        val kids = StringBuilder()
        for (i in 0 until numPages) {
            val pageObjId = 3 + i * 3
            kids.append("$pageObjId 0 R ")
        }
        writeString("$pagesObj 0 obj\n<< /Type /Pages /Kids [ $kids] /Count $numPages >>\nendobj\n")

        // 4. Page, Image, and Content Stream objects
        for (i in 0 until numPages) {
            val page = pages[i]
            val pageObjId = 3 + i * 3
            val imageObjId = 4 + i * 3
            val contentObjId = 5 + i * 3

            // Standard PDF points (72 DPI) - match image aspect ratio
            val pdfWidth = 595.28
            val pdfHeight = pdfWidth * (page.height.toDouble() / page.width.toDouble().coerceAtLeast(1.0))

            // Page Object
            markObject()
            writeString(
                String.format(
                    Locale.US,
                    "%d 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 %.2f %.2f] /Resources << /XObject << /Im1 %d 0 R >> >> /Contents %d 0 R >>\nendobj\n",
                    pageObjId, pdfWidth, pdfHeight, imageObjId, contentObjId
                )
            )

            // Image Object with /Filter /DCTDecode
            markObject()
            val imgHeader = String.format(
                Locale.US,
                "%d 0 obj\n<< /Type /XObject /Subtype /Image /Width %d /Height %d /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length %d >>\nstream\n",
                imageObjId, page.width, page.height, page.jpegBytes.size
            )
            writeString(imgHeader)
            writeBytes(page.jpegBytes)
            writeString("\nendstream\nendobj\n")

            // Content Stream Object (places image to fill the page)
            val contentStr = String.format(
                Locale.US,
                "q\n%.2f 0 0 %.2f 0 0 cm\n/Im1 Do\nQ\n",
                pdfWidth, pdfHeight
            )
            val contentBytes = contentStr.toByteArray(StandardCharsets.US_ASCII)

            markObject()
            writeString(
                String.format(
                    Locale.US,
                    "%d 0 obj\n<< /Length %d >>\nstream\n%sendstream\nendobj\n",
                    contentObjId, contentBytes.size, contentStr
                )
            )
        }

        // 5. Cross-reference table (xref)
        val xrefOffset = currentOffset
        val totalObjects = offsets.size

        writeString("xref\n0 ${totalObjects + 1}\n")
        writeString("0000000000 65535 f \n")

        for (offset in offsets) {
            writeString(String.format(Locale.US, "%010d 00000 n \n", offset))
        }

        // 6. Trailer
        writeString("trailer\n<< /Size ${totalObjects + 1} /Root 1 0 R >>\n")
        writeString("startxref\n$xrefOffset\n%%EOF\n")
        outputStream.flush()
        return currentOffset
    }
}
