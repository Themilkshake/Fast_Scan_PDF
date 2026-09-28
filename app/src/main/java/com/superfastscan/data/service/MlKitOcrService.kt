package com.superfastscan.data.service

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.superfastscan.domain.service.OcrResult
import com.superfastscan.domain.service.OcrService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class MlKitOcrService @Inject constructor(
    @ApplicationContext private val context: Context
) : OcrService {

    override suspend fun recognizeText(imageUri: Uri): Result<OcrResult> = withContext(Dispatchers.IO) {
        try {
            val inputImage = InputImage.fromFilePath(context, imageUri)
            processImage(inputImage)
        } catch (e: IOException) {
            Result.failure(IOException("Failed to read image from URI ($imageUri): ${e.localizedMessage}", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recognizeText(bitmap: Bitmap): Result<OcrResult> = withContext(Dispatchers.Default) {
        if (bitmap.isRecycled) {
            return@withContext Result.failure(IllegalStateException("Cannot process recycled bitmap"))
        }

        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            processImage(inputImage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recognizeText(imageFile: File): Result<OcrResult> = withContext(Dispatchers.IO) {
        if (!imageFile.exists() || imageFile.length() == 0L) {
            return@withContext Result.failure(
                IllegalArgumentException("Image file does not exist or is empty: ${imageFile.absolutePath}")
            )
        }

        try {
            val inputImage = InputImage.fromFilePath(context, Uri.fromFile(imageFile))
            processImage(inputImage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun processImage(inputImage: InputImage): Result<OcrResult> {
        val startTime = System.currentTimeMillis()
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        return try {
            suspendCancellableCoroutine { continuation ->
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val duration = System.currentTimeMillis() - startTime
                        val extractedBlocks = visionText.textBlocks.map { it.text.trim() }

                        val result = OcrResult(
                            fullText = visionText.text.trim(),
                            paragraphs = extractedBlocks,
                            blockCount = visionText.textBlocks.size,
                            executionTimeMs = duration
                        )
                        continuation.resume(Result.success(result))
                    }
                    .addOnFailureListener { exception ->
                        continuation.resume(Result.failure(exception))
                    }

                continuation.invokeOnCancellation {
                    recognizer.close()
                }
            }
        } finally {
            recognizer.close()
        }
    }
}
