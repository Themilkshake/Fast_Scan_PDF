package com.superfastscan.ui.compresspdf

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.superfastscan.R
import com.superfastscan.domain.usecase.CompressPdfUseCase
import com.superfastscan.domain.usecase.CompressionLevel
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
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CompressPdfViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val compressPdfUseCase: CompressPdfUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompressPdfUiState())
    val uiState: StateFlow<CompressPdfUiState> = _uiState.asStateFlow()

    private val pdfPageRenderer = PdfPageRenderer(context)

    // ────────────────────────────────────────────────────────
    // PDF Loading & Metadata Inspection
    // ────────────────────────────────────────────────────────

    fun loadPdf(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    pdfUri = uri,
                    isLoadingInfo = true,
                    compressionResult = null,
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
                val originalSize = compressPdfUseCase.getFileSize(uri)

                val (pageCount, firstPageThumbnail) = withContext(Dispatchers.IO) {
                    var count = 0
                    var thumb: android.graphics.Bitmap? = null
                    pdfPageRenderer.openPdf(uri) { total, renderPage, _ ->
                        count = total
                        if (total > 0) {
                            thumb = renderPage(0, 360)
                        }
                    }
                    Pair(count, thumb)
                }

                _uiState.update {
                    it.copy(
                        fileName = fileName,
                        originalSizeBytes = originalSize,
                        totalPages = pageCount,
                        thumbnail = firstPageThumbnail,
                        isLoadingInfo = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingInfo = false,
                        error = "Failed to inspect PDF: ${e.message}"
                    )
                }
            }
        }
    }

    // ────────────────────────────────────────────────────────
    // Compression Options
    // ────────────────────────────────────────────────────────

    fun setCompressionLevel(level: CompressionLevel) {
        _uiState.update { it.copy(selectedLevel = level) }
    }

    // ────────────────────────────────────────────────────────
    // Background Compression Execution
    // ────────────────────────────────────────────────────────

    fun compressPdf() {
        val state = _uiState.value
        val uri = state.pdfUri ?: return
        val total = state.totalPages
        if (total == 0) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCompressing = true,
                    compressProgressCurrent = 0,
                    compressProgressTotal = total,
                    error = null
                )
            }

            try {
                val cleanName = state.fileName.substringBeforeLast(".")
                val result = compressPdfUseCase(
                    pdfUri = uri,
                    level = state.selectedLevel,
                    baseFileName = cleanName,
                    onProgress = { current, totalSteps ->
                        _uiState.update {
                            it.copy(
                                compressProgressCurrent = current,
                                compressProgressTotal = totalSteps
                            )
                        }
                    }
                )

                _uiState.update {
                    it.copy(
                        isCompressing = false,
                        compressionResult = result
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCompressing = false,
                        error = "Compression failed: ${e.message}"
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
                originalSizeBytes = 0L,
                totalPages = 0,
                thumbnail = null,
                compressionResult = null,
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

    companion object {
        /**
         * Formats raw bytes into human-readable formatted string (B, KB, MB).
         */
        fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0

            return when {
                gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.2f MB", mb)
                kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
                else -> "$bytes B"
            }
        }
    }
}
