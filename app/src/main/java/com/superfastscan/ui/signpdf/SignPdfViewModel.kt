package com.superfastscan.ui.signpdf

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.provider.OpenableColumns
import com.superfastscan.R
import com.superfastscan.domain.usecase.FlattenSignatureUseCase
import com.superfastscan.domain.util.PdfPageRenderer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignPdfViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val flattenSignatureUseCase: FlattenSignatureUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignPdfUiState())
    val uiState: StateFlow<SignPdfUiState> = _uiState.asStateFlow()

    private val pdfPageRenderer = PdfPageRenderer(context)

    // ────────────────────────────────────────────────────────
    // PDF Loading
    // ────────────────────────────────────────────────────────

    /**
     * Loads a PDF from the given content URI and renders the first page.
     */
    fun loadPdf(uri: Uri) {
        viewModelScope.launch {
            try {
                val fileName = getFileName(uri) ?: "Document.pdf"
                val isPdf = fileName.endsWith(".pdf", ignoreCase = true) ||
                        context.contentResolver.getType(uri) == "application/pdf"
                if (!isPdf) {
                    _uiState.update {
                        it.copy(error = context.getString(R.string.invalid_pdf_file))
                    }
                    return@launch
                }

                _uiState.update { it.copy(pdfUri = uri, error = null) }

                pdfPageRenderer.openPdf(uri) { pageCount, renderPage, _ ->
                    val bitmap = renderPage(0, RENDER_WIDTH)
                    _uiState.update {
                        it.copy(
                            pageCount = pageCount,
                            currentPage = 0,
                            pageBitmap = bitmap,
                            // Reset signature when loading new PDF
                            signatureBitmap = null,
                            isSignaturePlaced = false,
                            saveComplete = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load PDF: ${e.message}") }
            }
        }
    }

    /**
     * Navigates to a specific PDF page and renders it.
     */
    fun goToPage(pageIndex: Int) {
        val state = _uiState.value
        if (pageIndex < 0 || pageIndex >= state.pageCount || state.pdfUri == null) return

        viewModelScope.launch {
            try {
                pdfPageRenderer.openPdf(state.pdfUri) { _, renderPage, _ ->
                    val bitmap = renderPage(pageIndex, RENDER_WIDTH)
                    _uiState.update {
                        it.copy(
                            currentPage = pageIndex,
                            pageBitmap = bitmap
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to render page: ${e.message}") }
            }
        }
    }

    // ────────────────────────────────────────────────────────
    // Signature Drawing & Placement
    // ────────────────────────────────────────────────────────

    fun showSignaturePad() {
        _uiState.update { it.copy(showSignaturePad = true) }
    }

    fun hideSignaturePad() {
        _uiState.update { it.copy(showSignaturePad = false) }
    }

    /**
     * Called when the user finishes drawing their signature.
     * Places the signature at the exact center of the page using normalized coordinates.
     */
    fun setSignature(bitmap: Bitmap) {
        val baseWidthFraction = 0.40f
        val defaultScale = 1.0f
        val normWidth = baseWidthFraction * defaultScale

        val pageBitmap = _uiState.value.pageBitmap
        val pageAspect = if (pageBitmap != null && pageBitmap.width > 0) {
            pageBitmap.height.toFloat() / pageBitmap.width.toFloat()
        } else {
            1.414f // Standard A4 aspect ratio fallback
        }
        val sigAspect = bitmap.height.toFloat() / bitmap.width.toFloat().coerceAtLeast(1f)
        val normHeight = normWidth * sigAspect / pageAspect

        // Center position in normalized space [0.0 .. 1.0]
        val centerX = ((1.0f - normWidth) / 2f).coerceIn(0f, 1f - normWidth)
        val centerY = ((1.0f - normHeight) / 2f).coerceIn(0f, 1f - normHeight)

        _uiState.update {
            it.copy(
                signatureBitmap = bitmap,
                isSignaturePlaced = true,
                showSignaturePad = false,
                normOffsetX = centerX,
                normOffsetY = centerY,
                signatureScale = defaultScale
            )
        }
    }

    /**
     * Removes the placed signature.
     */
    fun removeSignature() {
        _uiState.update {
            it.copy(
                signatureBitmap = null,
                isSignaturePlaced = false
            )
        }
    }

    // ────────────────────────────────────────────────────────
    // Normalized Coordinate & Scale Updates
    // ────────────────────────────────────────────────────────

    fun updateNormalizedPosition(normX: Float, normY: Float) {
        _uiState.update {
            it.copy(
                normOffsetX = normX.coerceIn(-1.5f, 1.5f),
                normOffsetY = normY.coerceIn(-1.5f, 1.5f)
            )
        }
    }

    fun updateSignatureScale(scale: Float) {
        _uiState.update {
            it.copy(signatureScale = scale.coerceIn(0.2f, 2.5f))
        }
    }

    // ────────────────────────────────────────────────────────
    // Save Signed PDF
    // ────────────────────────────────────────────────────────

    fun saveSignedPdf() {
        val state = _uiState.value
        if (state.pdfUri == null || state.signatureBitmap == null || state.pageBitmap == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            try {
                val baseWidthFraction = 0.40f
                val normWidth = (baseWidthFraction * state.signatureScale).coerceIn(0.05f, 0.95f)

                val pageAspect = state.pageBitmap.height.toFloat() / state.pageBitmap.width.toFloat().coerceAtLeast(1f)
                val sigAspect = state.signatureBitmap.height.toFloat() / state.signatureBitmap.width.toFloat().coerceAtLeast(1f)
                val normHeight = normWidth * sigAspect / pageAspect

                val placement = FlattenSignatureUseCase.SignaturePlacement(
                    pageIndex = state.currentPage,
                    normalizedX = state.normOffsetX,
                    normalizedY = state.normOffsetY,
                    normalizedWidth = normWidth,
                    normalizedHeight = normHeight
                )

                val savedPath = flattenSignatureUseCase(
                    pdfUri = state.pdfUri,
                    signatureBitmap = state.signatureBitmap,
                    placement = placement
                )

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveComplete = true,
                        savedFilePath = savedPath
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = "Failed to save: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun reset() {
        _uiState.update { SignPdfUiState() }
    }

    private fun getFileName(uri: Uri): String? {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    return cursor.getString(nameIndex)
                }
            }
        }
        return uri.path?.substringAfterLast('/')
    }

    companion object {
        const val RENDER_WIDTH = 1200
    }
}
