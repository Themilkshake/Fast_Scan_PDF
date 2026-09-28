package com.superfastscan.ui.jpgtopdf

import android.graphics.Bitmap
import android.net.Uri
import com.superfastscan.domain.usecase.JpgToPdfResult

data class JpgToPdfUiState(
    val selectedImages: List<Uri> = emptyList(),
    val customFileName: String = "",
    val isConverting: Boolean = false,
    val progressCurrent: Int = 0,
    val progressTotal: Int = 0,
    val conversionResult: JpgToPdfResult? = null,
    val pdfThumbnail: Bitmap? = null,
    val error: String? = null
) {
    val isReady: Boolean get() = selectedImages.isNotEmpty() && !isConverting
    val isComplete: Boolean get() = conversionResult != null
}
