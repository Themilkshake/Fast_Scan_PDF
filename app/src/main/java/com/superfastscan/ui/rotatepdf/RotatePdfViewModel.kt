package com.superfastscan.ui.rotatepdf

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.superfastscan.R
import com.superfastscan.domain.usecase.RotatePdfUseCase
import com.superfastscan.domain.util.PdfPageRenderer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class RotatePdfViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rotatePdfUseCase: RotatePdfUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RotatePdfUiState())
    val uiState: StateFlow<RotatePdfUiState> = _uiState.asStateFlow()

    private val pdfPageRenderer = PdfPageRenderer(context)

    // ────────────────────────────────────────────────────────
    // PDF Loading & Page Thumbnails
    // ────────────────────────────────────────────────────────

    fun loadPdf(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    pdfUri = uri,
                    isLoadingThumbnails = true,
                    pageThumbnails = emptyList(),
                    pageRotations = emptyMap(),
                    rotateResult = null,
                    error = null
                )
            }

            try {
                val fileName = getFileName(uri) ?: "Document.pdf"
                val isPdf = fileName.endsWith(".pdf", ignoreCase = true) ||
                        context.contentResolver.getType(uri) == "application/pdf"
                if (!isPdf) {
                    _uiState.update {
                        it.copy(
                            isLoadingThumbnails = false,
                            error = context.getString(R.string.invalid_pdf_file)
                        )
                    }
                    return@launch
                }

                val (pageCount, thumbnails) = withContext(Dispatchers.IO) {
                    val thumbs = mutableListOf<Bitmap>()
                    var count = 0
                    pdfPageRenderer.openPdf(uri) { total, renderPage, _ ->
                        count = total
                        for (pageIndex in 0 until total) {
                            val bitmap = renderPage(pageIndex, 450)
                            if (bitmap != null) {
                                thumbs.add(bitmap)
                            }
                        }
                    }
                    Pair(count, thumbs)
                }

                _uiState.update {
                    it.copy(
                        fileName = fileName,
                        totalPages = pageCount,
                        pageThumbnails = thumbnails,
                        isLoadingThumbnails = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingThumbnails = false,
                        error = "Failed to load PDF: ${e.message}"
                    )
                }
            }
        }
    }

    // ────────────────────────────────────────────────────────
    // Rotation Operations
    // ────────────────────────────────────────────────────────

    /**
     * Rotates a specific page by [deltaDegrees] (defaults to +90° clockwise).
     */
    fun rotatePage(pageIndex: Int, deltaDegrees: Int = 90) {
        _uiState.update { state ->
            val current = state.pageRotations[pageIndex] ?: 0
            var newRotation = (current + deltaDegrees) % 360
            if (newRotation < 0) newRotation += 360

            val updatedMap = state.pageRotations.toMutableMap()
            if (newRotation == 0) {
                updatedMap.remove(pageIndex)
            } else {
                updatedMap[pageIndex] = newRotation
            }
            state.copy(pageRotations = updatedMap)
        }
    }

    /**
     * Rotates all pages by [deltaDegrees] (e.g. +90° or -90°).
     */
    fun rotateAll(deltaDegrees: Int) {
        _uiState.update { state ->
            val updatedMap = mutableMapOf<Int, Int>()
            for (i in 0 until state.totalPages) {
                val current = state.pageRotations[i] ?: 0
                var newRotation = (current + deltaDegrees) % 360
                if (newRotation < 0) newRotation += 360
                if (newRotation != 0) {
                    updatedMap[i] = newRotation
                }
            }
            state.copy(pageRotations = updatedMap)
        }
    }

    fun resetRotations() {
        _uiState.update { it.copy(pageRotations = emptyMap()) }
    }

    // ────────────────────────────────────────────────────────
    // Save Non-Destructive PDF
    // ────────────────────────────────────────────────────────

    fun saveRotatedPdf() {
        val state = _uiState.value
        val uri = state.pdfUri ?: return
        if (state.totalPages == 0) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            try {
                val cleanName = state.fileName.substringBeforeLast(".")
                val result = rotatePdfUseCase(
                    pdfUri = uri,
                    pageRotations = state.pageRotations,
                    baseFileName = cleanName
                )

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        rotateResult = result
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = "Failed to rotate PDF: ${e.message}"
                    )
                }
            }
        }
    }

    fun reset() {
        _uiState.update {
            it.copy(
                pdfUri = null,
                fileName = "",
                totalPages = 0,
                pageThumbnails = emptyList(),
                pageRotations = emptyMap(),
                rotateResult = null,
                error = null
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        }
        if (name == null) {
            name = uri.path?.let { path ->
                val cut = path.lastIndexOf('/')
                if (cut != -1) path.substring(cut + 1) else path
            }
        }
        return name
    }
}
