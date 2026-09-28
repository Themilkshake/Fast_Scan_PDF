package com.superfastscan.domain.usecase

import android.net.Uri
import com.superfastscan.domain.service.SearchablePdfResult
import com.superfastscan.domain.service.SearchablePdfService
import javax.inject.Inject

class CreateSearchablePdfUseCase @Inject constructor(
    private val searchablePdfService: SearchablePdfService
) {
    suspend operator fun invoke(
        pdfUri: Uri,
        customOutputName: String? = null,
        onProgress: (currentPage: Int, totalPages: Int) -> Unit = { _, _ -> }
    ): Result<SearchablePdfResult> {
        return searchablePdfService.convertToSearchablePdf(
            pdfUri = pdfUri,
            customOutputName = customOutputName,
            onProgress = onProgress
        )
    }
}
