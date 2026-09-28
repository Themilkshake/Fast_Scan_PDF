package com.superfastscan.ui.compresspdf

import android.graphics.Bitmap
import android.net.Uri
import com.superfastscan.domain.usecase.CompressionLevel
import com.superfastscan.domain.usecase.CompressionResult

/**
 * UI State for the Compress PDF tool.
 */
data class CompressPdfUiState(
    val pdfUri: Uri? = null,
    val fileName: String = "",
    val originalSizeBytes: Long = 0L,
    val totalPages: Int = 0,
    val thumbnail: Bitmap? = null,
    val selectedLevel: CompressionLevel = CompressionLevel.MEDIUM,
    val isLoadingInfo: Boolean = false,
    val isCompressing: Boolean = false,
    val compressProgressCurrent: Int = 0,
    val compressProgressTotal: Int = 0,
    val compressionResult: CompressionResult? = null,
    val error: String? = null
) {
    val isComplete: Boolean
        get() = compressionResult != null
}
