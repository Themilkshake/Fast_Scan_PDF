package com.superfastscan.domain.service

import android.graphics.Bitmap
import android.net.Uri
import java.io.File

data class OcrResult(
    val fullText: String,
    val paragraphs: List<String>,
    val blockCount: Int,
    val executionTimeMs: Long
) {
    val wordCount: Int
        get() = if (fullText.isBlank()) 0 else fullText.trim().split(Regex("\\s+")).size

    val charCount: Int
        get() = fullText.length
}

interface OcrService {
    suspend fun recognizeText(imageUri: Uri): Result<OcrResult>
    suspend fun recognizeText(bitmap: Bitmap): Result<OcrResult>
    suspend fun recognizeText(imageFile: File): Result<OcrResult>
}
