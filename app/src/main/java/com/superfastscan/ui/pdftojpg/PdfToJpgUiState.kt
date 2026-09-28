package com.superfastscan.ui.pdftojpg

import android.graphics.Bitmap
import android.net.Uri
import com.superfastscan.domain.usecase.PdfToJpgResult

data class PdfToJpgUiState(
    val pdfUri: Uri? = null,
    val fileName: String = "",
    val pageCount: Int = 0,
    val firstPageThumbnail: Bitmap? = null,
    val isExtracting: Boolean = false,
    val progressCurrent: Int = 0,
    val progressTotal: Int = 0,
    val result: PdfToJpgResult? = null,
    val error: String? = null
) {
    val isReady: Boolean get() = pdfUri != null && !isExtracting
    val isComplete: Boolean get() = result != null
}
