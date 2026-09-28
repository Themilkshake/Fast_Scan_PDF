package com.superfastscan.domain.model

import android.graphics.Bitmap
import android.net.Uri
import java.util.UUID

/**
 * Represents a selected PDF file to be merged.
 *
 * @param id Unique identifier used for keying LazyColumn items during drag-and-drop
 * @param uri Content URI of the PDF file
 * @param fileName Display name of the PDF file
 * @param fileSizeFormatted Human-readable file size (e.g. "1.4 MB")
 * @param pageCount Number of pages in the PDF document
 * @param thumbnail First page preview thumbnail
 */
data class PdfFileItem(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val fileName: String,
    val fileSizeFormatted: String,
    val pageCount: Int,
    val thumbnail: Bitmap? = null
)
