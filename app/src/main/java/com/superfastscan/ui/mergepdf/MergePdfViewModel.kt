package com.superfastscan.ui.mergepdf

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.superfastscan.R
import com.superfastscan.domain.model.PdfFileItem
import com.superfastscan.domain.usecase.MergePdfUseCase
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
import java.text.DecimalFormat
import javax.inject.Inject
import kotlin.math.log10
import kotlin.math.pow

@HiltViewModel
class MergePdfViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mergePdfUseCase: MergePdfUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MergePdfUiState())
    val uiState: StateFlow<MergePdfUiState> = _uiState.asStateFlow()

    private val pdfPageRenderer = PdfPageRenderer(context)

    // ────────────────────────────────────────────────────────
    // File Management
    // ────────────────────────────────────────────────────────

    /**
     * Inspects and adds multiple PDF files to the merge list.
     * Extracts page counts, file sizes, and first-page thumbnails in the background.
     */
    fun addFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFiles = true, error = null) }

            try {
                val newItems = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri ->
                        try {
                            val fileName = getFileName(uri) ?: "Document.pdf"
                            val isPdf = fileName.endsWith(".pdf", ignoreCase = true) ||
                                    context.contentResolver.getType(uri) == "application/pdf"
                            if (!isPdf) {
                                return@mapNotNull null
                            }

                            val fileSize = getFileSizeFormatted(uri)

                            var pageCount = 1
                            var thumbnail: android.graphics.Bitmap? = null

                            try {
                                pdfPageRenderer.openPdf(uri) { count, renderPage, _ ->
                                    pageCount = count
                                    // Generate crisp thumbnail for file card preview (450px width)
                                    thumbnail = renderPage(0, 450)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            PdfFileItem(
                                uri = uri,
                                fileName = fileName,
                                fileSizeFormatted = fileSize,
                                pageCount = pageCount,
                                thumbnail = thumbnail
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                    }
                }

                if (newItems.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoadingFiles = false,
                            error = context.getString(R.string.merge_no_valid_pdf)
                        )
                    }
                    return@launch
                }

                _uiState.update { state ->
                    state.copy(
                        files = state.files + newItems,
                        isLoadingFiles = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingFiles = false,
                        error = "Failed to load files: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Swaps elements in the list when a drag-and-drop action completes.
     */
    fun moveItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        _uiState.update { state ->
            val list = state.files.toMutableList()
            if (fromIndex in list.indices && toIndex in list.indices) {
                val item = list.removeAt(fromIndex)
                list.add(toIndex, item)
                state.copy(files = list)
            } else {
                state
            }
        }
    }

    /**
     * Removes a single file from the merge list.
     */
    fun removeFile(id: String) {
        _uiState.update { state ->
            state.copy(files = state.files.filterNot { it.id == id })
        }
    }

    /**
     * Clears all selected files.
     */
    fun clearAll() {
        _uiState.update { it.copy(files = emptyList()) }
    }

    fun reset() {
        _uiState.update { MergePdfUiState() }
    }

    // ────────────────────────────────────────────────────────
    // Merging Action
    // ────────────────────────────────────────────────────────

    /**
     * Merges all selected PDFs in their exact current list sequence.
     */
    fun mergePdfs(customFileName: String? = null) {
        val currentFiles = _uiState.value.files
        if (currentFiles.size < 2) {
            _uiState.update { it.copy(error = "Please select at least 2 PDF files to merge") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isMerging = true,
                    mergeProgressCurrent = 0,
                    mergeProgressTotal = currentFiles.size,
                    error = null
                )
            }

            try {
                val pdfUris = currentFiles.map { it.uri }
                val savedPath = mergePdfUseCase(
                    pdfUris = pdfUris,
                    outputFileName = customFileName,
                    onProgress = { current, total ->
                        _uiState.update {
                            it.copy(
                                mergeProgressCurrent = current,
                                mergeProgressTotal = total
                            )
                        }
                    }
                )

                _uiState.update {
                    it.copy(
                        isMerging = false,
                        mergeComplete = true,
                        mergedFilePath = savedPath
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isMerging = false,
                        error = "Failed to merge PDFs: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // ────────────────────────────────────────────────────────
    // Helper Methods
    // ────────────────────────────────────────────────────────

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

    private fun getFileSizeFormatted(uri: Uri): String {
        var sizeBytes: Long = 0
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1 && cursor.moveToFirst()) {
                    sizeBytes = cursor.getLong(sizeIndex)
                }
            }
        }
        if (sizeBytes <= 0) return ""
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (log10(sizeBytes.toDouble()) / log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(sizeBytes / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
    }
}
