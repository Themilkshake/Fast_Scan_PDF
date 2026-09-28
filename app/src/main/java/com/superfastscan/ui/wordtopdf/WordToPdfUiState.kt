package com.superfastscan.ui.wordtopdf

import android.graphics.Bitmap
import android.net.Uri
import com.superfastscan.domain.usecase.WordToPdfResult

data class WordToPdfUiState(
    val documentUri: Uri? = null,
    val fileName: String = "",
    val fileSizeBytes: Long = 0L,
    val customOutputName: String = "",
    val isLoadingInfo: Boolean = false,
    val isConverting: Boolean = false,
    val conversionResult: WordToPdfResult? = null,
    val pdfThumbnail: Bitmap? = null,
    val error: String? = null
) {
    val isReady: Boolean
        get() = documentUri != null && !isConverting

    val isComplete: Boolean
        get() = conversionResult != null
}
