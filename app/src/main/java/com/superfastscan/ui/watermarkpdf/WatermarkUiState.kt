package com.superfastscan.ui.watermarkpdf

import android.graphics.Bitmap
import android.net.Uri
import com.superfastscan.domain.usecase.WatermarkConfig
import com.superfastscan.domain.usecase.WatermarkResult

enum class WatermarkTab {
    CONTENT,
    STYLE,
    PAGES
}

/**
 * UI State for the Watermark PDF screen.
 */
data class WatermarkUiState(
    val pdfUri: Uri? = null,
    val fileName: String = "",
    val totalPages: Int = 0,
    val firstPagePreview: Bitmap? = null,
    val previewAspectRatio: Float = 1.414f, // Page height / width aspect ratio
    val config: WatermarkConfig = WatermarkConfig(),
    val selectedTab: WatermarkTab = WatermarkTab.CONTENT,
    val isLoadingInfo: Boolean = false,
    val isProcessing: Boolean = false,
    val progressCurrent: Int = 0,
    val progressTotal: Int = 0,
    val watermarkResult: WatermarkResult? = null,
    val error: String? = null
) {
    val isComplete: Boolean
        get() = watermarkResult != null
}
