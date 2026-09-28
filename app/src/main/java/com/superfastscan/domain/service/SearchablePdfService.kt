package com.superfastscan.domain.service

import android.net.Uri

data class SearchablePdfResult(
    val outputPath: String,
    val outputUri: Uri?,
    val outputFileName: String,
    val totalPages: Int,
    val totalWords: Int,
    val fileSizeBytes: Long,
    val executionTimeMs: Long
)

interface SearchablePdfService {
    suspend fun convertToSearchablePdf(
        pdfUri: Uri,
        customOutputName: String? = null,
        onProgress: (currentPage: Int, totalPages: Int) -> Unit = { _, _ -> }
    ): Result<SearchablePdfResult>
}
