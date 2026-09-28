package com.superfastscan.ui.rotatepdf

import android.graphics.Bitmap
import android.net.Uri
import com.superfastscan.domain.usecase.RotatePdfResult

/**
 * UI State for the Rotate PDF screen.
 */
data class RotatePdfUiState(
    val pdfUri: Uri? = null,
    val fileName: String = "",
    val totalPages: Int = 0,
    val pageThumbnails: List<Bitmap> = emptyList(),
    val pageRotations: Map<Int, Int> = emptyMap(), // pageIndex -> rotation in degrees (0, 90, 180, 270)
    val isLoadingThumbnails: Boolean = false,
    val isSaving: Boolean = false,
    val rotateResult: RotatePdfResult? = null,
    val error: String? = null
) {
    val isComplete: Boolean
        get() = rotateResult != null

    fun getPageRotation(pageIndex: Int): Int = pageRotations[pageIndex] ?: 0
}
