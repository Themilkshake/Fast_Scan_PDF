package com.superfastscan.ui.ocr

import android.graphics.Bitmap
import android.net.Uri
import com.superfastscan.domain.service.OcrResult
import com.superfastscan.domain.service.SearchablePdfResult

enum class OcrMode {
    SEARCHABLE_PDF,
    EXTRACT_TEXT
}

data class OcrUiState(
    val activeMode: OcrMode = OcrMode.SEARCHABLE_PDF,
    val selectedPdfUri: Uri? = null,
    val pdfFileName: String = "",
    val pdfPageCount: Int = 0,
    val pdfFirstPageThumbnail: Bitmap? = null,
    val selectedImageUri: Uri? = null,
    val isProcessing: Boolean = false,
    val progressCurrent: Int = 0,
    val progressTotal: Int = 0,
    val searchablePdfResult: SearchablePdfResult? = null,
    val ocrResult: OcrResult? = null,
    val savedFilePath: String? = null,
    val errorMessage: String? = null
) {
    val isReady: Boolean
        get() = when (activeMode) {
            OcrMode.SEARCHABLE_PDF -> selectedPdfUri != null && !isProcessing
            OcrMode.EXTRACT_TEXT -> selectedImageUri != null && !isProcessing
        }

    val isComplete: Boolean
        get() = searchablePdfResult != null || ocrResult != null
}
