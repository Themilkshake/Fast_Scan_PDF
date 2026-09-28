package com.superfastscan.ui.pdftooffice

import android.graphics.Bitmap
import android.net.Uri
import com.superfastscan.domain.usecase.PdfToOfficeResult

data class PdfToOfficeUiState(
    val pdfUri: Uri? = null,
    val fileName: String = "",
    val pageCount: Int = 0,
    val customFileName: String = "",
    val firstPageThumbnail: Bitmap? = null,
    val isConverting: Boolean = false,
    val result: PdfToOfficeResult? = null,
    val error: String? = null
) {
    val isReady: Boolean get() = pdfUri != null && !isConverting
    val isComplete: Boolean get() = result != null
}
