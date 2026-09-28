package com.superfastscan.ui.signpdf

import android.graphics.Bitmap
import android.net.Uri

/**
 * Represents the UI state for the Sign PDF feature.
 */
data class SignPdfUiState(
    // ── PDF Document ──
    val pdfUri: Uri? = null,
    val pageCount: Int = 0,
    val currentPage: Int = 0,
    val pageBitmap: Bitmap? = null,

    // ── Signature Placement (Normalized 0.0 .. 1.0) ──
    val signatureBitmap: Bitmap? = null,
    val normOffsetX: Float = 0.3f,       // X position as a fraction of page width [0.0 .. 1.0]
    val normOffsetY: Float = 0.45f,      // Y position as a fraction of page height [0.0 .. 1.0]
    val signatureScale: Float = 1.0f,    // Scale multiplier (1.0 = 40% of page width)
    val isSignaturePlaced: Boolean = false,
    val showSignaturePad: Boolean = false,

    // ── Save State ──
    val isSaving: Boolean = false,
    val saveComplete: Boolean = false,
    val savedFilePath: String? = null,

    // ── Error ──
    val error: String? = null
)
