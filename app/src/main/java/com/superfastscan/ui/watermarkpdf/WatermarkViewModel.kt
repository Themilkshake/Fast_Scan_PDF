package com.superfastscan.ui.watermarkpdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.superfastscan.R
import com.superfastscan.domain.usecase.AddWatermarkUseCase
import com.superfastscan.domain.usecase.WatermarkConfig
import com.superfastscan.domain.usecase.WatermarkPageScope
import com.superfastscan.domain.usecase.WatermarkType
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
class WatermarkViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val addWatermarkUseCase: AddWatermarkUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WatermarkUiState())
    val uiState: StateFlow<WatermarkUiState> = _uiState.asStateFlow()

    private val pdfPageRenderer = PdfPageRenderer(context)

    // ────────────────────────────────────────────────────────
    // PDF Loading & Preview Rendering
    // ────────────────────────────────────────────────────────

    fun loadPdf(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    pdfUri = uri,
                    isLoadingInfo = true,
                    watermarkResult = null,
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
                            isLoadingInfo = false,
                            error = context.getString(R.string.invalid_pdf_file)
                        )
                    }
                    return@launch
                }

                val (pageCount, pagePreview, aspectRatio) = withContext(Dispatchers.IO) {
                    var count = 0
                    var preview: Bitmap? = null
                    var aspect = 1.414f

                    pdfPageRenderer.openPdf(uri) { total, renderPage, _ ->
                        count = total
                        if (total > 0) {
                            preview = renderPage(0, 720)
                            preview?.let {
                                aspect = it.height.toFloat() / it.width.toFloat().coerceAtLeast(1f)
                            }
                        }
                    }
                    Triple(count, preview, aspect)
                }

                _uiState.update {
                    it.copy(
                        fileName = fileName,
                        totalPages = pageCount,
                        firstPagePreview = pagePreview,
                        previewAspectRatio = aspectRatio,
                        isLoadingInfo = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingInfo = false,
                        error = "Failed to load PDF: ${e.message}"
                    )
                }
            }
        }
    }

    // ────────────────────────────────────────────────────────
    // Configuration Updates
    // ────────────────────────────────────────────────────────

    fun setTab(tab: WatermarkTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setType(type: WatermarkType) {
        _uiState.update { it.copy(config = it.config.copy(type = type)) }
    }

    fun setText(text: String) {
        _uiState.update { it.copy(config = it.config.copy(text = text)) }
    }

    fun setTextColor(color: Long) {
        _uiState.update { it.copy(config = it.config.copy(textColor = color)) }
    }

    fun setBold(isBold: Boolean) {
        _uiState.update { it.copy(config = it.config.copy(isBold = isBold)) }
    }

    fun setImageUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val original = BitmapFactory.decodeStream(stream) ?: return@use null
                        // Scale to reasonable dimension (max 1000px)
                        val maxDim = 1000
                        if (original.width > maxDim || original.height > maxDim) {
                            val ratio = minOf(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height)
                            val scaled = Bitmap.createScaledBitmap(
                                original,
                                (original.width * ratio).toInt().coerceAtLeast(1),
                                (original.height * ratio).toInt().coerceAtLeast(1),
                                true
                            )
                            if (scaled != original) original.recycle()
                            scaled
                        } else {
                            original
                        }
                    }
                }

                if (bitmap != null) {
                    _uiState.update {
                        it.copy(
                            config = it.config.copy(
                                type = WatermarkType.IMAGE,
                                imageBitmap = bitmap
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load image: ${e.message}") }
            }
        }
    }

    fun setOpacity(opacity: Float) {
        _uiState.update { it.copy(config = it.config.copy(opacity = opacity.coerceIn(0.05f, 1.0f))) }
    }

    fun setRotation(rotation: Float) {
        _uiState.update { it.copy(config = it.config.copy(rotation = rotation.coerceIn(-180f, 180f))) }
    }

    fun setScale(scale: Float) {
        _uiState.update { it.copy(config = it.config.copy(scale = scale.coerceIn(0.2f, 3.5f))) }
    }

    fun setNormalizedPosition(normX: Float, normY: Float) {
        _uiState.update {
            it.copy(
                config = it.config.copy(
                    normalizedX = normX.coerceIn(0.05f, 0.95f),
                    normalizedY = normY.coerceIn(0.05f, 0.95f)
                )
            )
        }
    }

    fun centerWatermark() {
        _uiState.update {
            it.copy(
                config = it.config.copy(
                    normalizedX = 0.5f,
                    normalizedY = 0.5f
                )
            )
        }
    }

    fun setPageScope(pageScope: WatermarkPageScope) {
        _uiState.update { it.copy(config = it.config.copy(pageScope = pageScope)) }
    }

    fun setCustomPages(customPages: String) {
        _uiState.update { it.copy(config = it.config.copy(customPages = customPages)) }
    }

    // ────────────────────────────────────────────────────────
    // PDF Watermark Execution
    // ────────────────────────────────────────────────────────

    fun applyWatermark() {
        val state = _uiState.value
        val uri = state.pdfUri ?: return
        val total = state.totalPages
        if (total == 0) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    progressCurrent = 0,
                    progressTotal = total,
                    error = null
                )
            }

            try {
                val cleanName = state.fileName.substringBeforeLast(".")
                val result = addWatermarkUseCase(
                    pdfUri = uri,
                    config = state.config,
                    baseFileName = cleanName,
                    onProgress = { current, totalSteps ->
                        _uiState.update {
                            it.copy(
                                progressCurrent = current,
                                progressTotal = totalSteps
                            )
                        }
                    }
                )

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        watermarkResult = result
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        error = "Failed to apply watermark: ${e.message}"
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
                firstPagePreview = null,
                watermarkResult = null,
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
