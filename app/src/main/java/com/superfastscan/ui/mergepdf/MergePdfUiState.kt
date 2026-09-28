package com.superfastscan.ui.mergepdf

import com.superfastscan.domain.model.PdfFileItem

/**
 * UI State for the Merge PDF tool.
 */
data class MergePdfUiState(
    val files: List<PdfFileItem> = emptyList(),
    val isLoadingFiles: Boolean = false,
    val isMerging: Boolean = false,
    val mergeProgressCurrent: Int = 0,
    val mergeProgressTotal: Int = 0,
    val mergeComplete: Boolean = false,
    val mergedFilePath: String? = null,
    val error: String? = null
) {
    val totalPages: Int
        get() = files.sumOf { it.pageCount }
}
