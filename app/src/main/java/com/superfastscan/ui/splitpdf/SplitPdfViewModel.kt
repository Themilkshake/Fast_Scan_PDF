package com.superfastscan.ui.splitpdf

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.superfastscan.R
import com.superfastscan.domain.usecase.SplitMode
import com.superfastscan.domain.usecase.SplitPdfUseCase
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
class SplitPdfViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val splitPdfUseCase: SplitPdfUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplitPdfUiState())
    val uiState: StateFlow<SplitPdfUiState> = _uiState.asStateFlow()

    private val pdfPageRenderer = PdfPageRenderer(context)

    // ────────────────────────────────────────────────────────
    // PDF Loading & Page Thumbnail Generation
    // ────────────────────────────────────────────────────────

    fun loadPdf(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    pdfUri = uri,
                    isLoadingPages = true,
                    selectedPages = emptySet(),
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
                            isLoadingPages = false,
                            error = context.getString(R.string.invalid_pdf_file)
                        )
                    }
                    return@launch
                }

                val pageList = withContext(Dispatchers.IO) {
                    val items = mutableListOf<PdfPageItem>()
                    pdfPageRenderer.openPdf(uri) { pageCount, renderPage, _ ->
                        for (pageIndex in 0 until pageCount) {
                            // Render high-clarity grid thumbnail (360px width)
                            val thumbnail = renderPage(pageIndex, 360)
                            items.add(
                                PdfPageItem(
                                    pageIndex = pageIndex,
                                    pageNumber = pageIndex + 1,
                                    thumbnail = thumbnail
                                )
                            )
                        }
                    }
                    items
                }

                _uiState.update {
                    it.copy(
                        fileName = fileName,
                        totalPages = pageList.size,
                        pages = pageList,
                        isLoadingPages = false,
                        extractComplete = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingPages = false,
                        error = "Failed to load PDF pages: ${e.message}"
                    )
                }
            }
        }
    }

    // ────────────────────────────────────────────────────────
    // Tab & Page Selection
    // ────────────────────────────────────────────────────────

    fun setTab(tab: SplitTab) {
        _uiState.update { it.copy(selectedTab = tab, rangeError = null) }
    }

    fun updateRangeInput(input: String) {
        _uiState.update { it.copy(rangeInput = input, rangeError = null) }
    }

    fun applyPageRange(rangeText: String) {
        val total = _uiState.value.totalPages
        if (total == 0 || rangeText.isBlank()) {
            _uiState.update { it.copy(rangeError = "Lütfen sayfa aralığı girin") }
            return
        }

        try {
            val indices = parsePageRanges(rangeText, total)
            if (indices.isEmpty()) {
                _uiState.update { it.copy(rangeError = "Geçersiz sayfa numarası") }
            } else {
                _uiState.update {
                    it.copy(
                        selectedPages = indices,
                        rangeError = null
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(rangeError = "Geçersiz biçim (Örn: 1-3, 5)") }
        }
    }

    fun togglePageSelection(pageIndex: Int) {
        _uiState.update { state ->
            val updated = state.selectedPages.toMutableSet()
            if (updated.contains(pageIndex)) {
                updated.remove(pageIndex)
            } else {
                updated.add(pageIndex)
            }
            state.copy(selectedPages = updated)
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            state.copy(selectedPages = state.pages.map { it.pageIndex }.toSet(), rangeError = null)
        }
    }

    fun deselectAll() {
        _uiState.update { it.copy(selectedPages = emptySet(), rangeError = null) }
    }

    fun selectOddPages() {
        _uiState.update { state ->
            state.copy(
                selectedPages = state.pages.filter { it.pageNumber % 2 != 0 }.map { it.pageIndex }.toSet(),
                rangeError = null
            )
        }
    }

    fun selectEvenPages() {
        _uiState.update { state ->
            state.copy(
                selectedPages = state.pages.filter { it.pageNumber % 2 == 0 }.map { it.pageIndex }.toSet(),
                rangeError = null
            )
        }
    }

    // ────────────────────────────────────────────────────────
    // Extraction Options & Execution
    // ────────────────────────────────────────────────────────

    fun splitAllPages() {
        val state = _uiState.value
        val uri = state.pdfUri ?: return
        val total = state.totalPages
        if (total == 0) return

        val allIndices = (0 until total).toSet()

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExtracting = true,
                    extractProgressCurrent = 0,
                    extractProgressTotal = total,
                    error = null
                )
            }

            try {
                val cleanName = state.fileName.substringBeforeLast(".")
                val resultPaths = splitPdfUseCase(
                    pdfUri = uri,
                    selectedPageIndices = allIndices,
                    mode = SplitMode.SEPARATE_INDIVIDUAL_FILES,
                    baseFileName = cleanName,
                    onProgress = { current, totalProgress ->
                        _uiState.update {
                            it.copy(
                                extractProgressCurrent = current,
                                extractProgressTotal = totalProgress
                            )
                        }
                    }
                )

                _uiState.update {
                    it.copy(
                        isExtracting = false,
                        extractComplete = true,
                        extractedFilePaths = resultPaths
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExtracting = false,
                        error = "Failed to split pages: ${e.message}"
                    )
                }
            }
        }
    }

    fun showExtractOptions() {
        if (_uiState.value.selectedPages.isNotEmpty()) {
            _uiState.update { it.copy(showExtractOptions = true) }
        } else {
            _uiState.update { it.copy(error = "Please select at least 1 page to extract") }
        }
    }

    fun hideExtractOptions() {
        _uiState.update { it.copy(showExtractOptions = false) }
    }

    fun extractPages(mode: SplitMode) {
        val state = _uiState.value
        val uri = state.pdfUri ?: return
        val selected = state.selectedPages
        if (selected.isEmpty()) return

        hideExtractOptions()

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExtracting = true,
                    extractProgressCurrent = 0,
                    extractProgressTotal = selected.size,
                    error = null
                )
            }

            try {
                val cleanName = state.fileName.substringBeforeLast(".")
                val resultPaths = splitPdfUseCase(
                    pdfUri = uri,
                    selectedPageIndices = selected,
                    mode = mode,
                    baseFileName = cleanName,
                    onProgress = { current, total ->
                        _uiState.update {
                            it.copy(
                                extractProgressCurrent = current,
                                extractProgressTotal = total
                            )
                        }
                    }
                )

                _uiState.update {
                    it.copy(
                        isExtracting = false,
                        extractComplete = true,
                        extractedFilePaths = resultPaths
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExtracting = false,
                        error = "Failed to extract pages: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun reset() {
        _uiState.update { SplitPdfUiState() }
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
         * Parses human-entered page strings such as "1-3, 5, 8-10" into 0-based page index set.
         */
        internal fun parsePageRanges(input: String, totalPages: Int): Set<Int> {
            val result = mutableSetOf<Int>()
            val parts = input.split(',', ';', ' ').map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.isEmpty()) return emptySet()

            for (part in parts) {
                if (part.contains('-')) {
                    val rangeParts = part.split('-')
                    if (rangeParts.size != 2) throw IllegalArgumentException("Invalid range: $part")
                    val start = rangeParts[0].trim().toInt()
                    val end = rangeParts[1].trim().toInt()
                    if (start > end || start < 1 || end > totalPages) {
                        throw IllegalArgumentException("Out of bounds range: $part")
                    }
                    for (p in start..end) {
                        result.add(p - 1)
                    }
                } else {
                    val pageNum = part.toInt()
                    if (pageNum < 1 || pageNum > totalPages) {
                        throw IllegalArgumentException("Out of bounds page: $part")
                    }
                    result.add(pageNum - 1)
                }
            }
            return result
        }
    }
}
