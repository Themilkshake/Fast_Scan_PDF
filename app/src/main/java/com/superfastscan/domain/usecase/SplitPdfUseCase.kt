package com.superfastscan.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.superfastscan.domain.util.FastPdfWriter
import com.superfastscan.domain.util.PdfPageRenderer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Extraction mode for splitting a PDF document.
 */
enum class SplitMode {
    /** Combines all selected pages into a single new PDF document */
    SINGLE_COMBINED_FILE,
    /** Saves each selected page as an independent individual PDF document */
    SEPARATE_INDIVIDUAL_FILES
}

/**
 * Extracts specific pages from a PDF document with memory-efficient page streaming.
 */
class SplitPdfUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Extracts pages based on [selectedPageIndices] and [mode].
     *
     * @param pdfUri Source PDF content URI
     * @param selectedPageIndices Set of 0-based page indices to extract
     * @param mode Single combined PDF vs. separate individual PDFs
     * @param baseFileName Base name to use for generated files
     * @param onProgress Progress callback receiving (currentStep 1-based, totalSteps)
     * @return List of generated file paths or content URIs
     */
    suspend operator fun invoke(
        pdfUri: Uri,
        selectedPageIndices: Set<Int>,
        mode: SplitMode,
        baseFileName: String? = null,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): List<String> = withContext(Dispatchers.IO) {
        if (selectedPageIndices.isEmpty()) {
            throw IllegalArgumentException("No pages selected for extraction")
        }

        val sortedIndices = selectedPageIndices.sorted()
        val renderer = PdfPageRenderer(context)
        val timestamp = SimpleDateFormat("ddMMyyyy_HHmm", Locale.getDefault()).format(Date())
        val cleanBaseName = (baseFileName ?: "Document")
            .replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "")
            .ifBlank { "Document" }

        val resultPaths = mutableListOf<String>()

        when (mode) {
            SplitMode.SINGLE_COMBINED_FILE -> {
                // ── Option A: Combine all selected pages into one PDF ──
                val extractedPagesData = mutableListOf<FastPdfWriter.PdfPageData>()
                val total = sortedIndices.size

                renderer.openPdf(pdfUri) { _, renderPage, _ ->
                    for ((step, pageIdx) in sortedIndices.withIndex()) {
                        onProgress(step + 1, total)

                        val bitmap = renderPage(pageIdx, 1800)
                            ?: throw IllegalStateException("Failed to render page $pageIdx")

                        val stream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                        extractedPagesData.add(
                            FastPdfWriter.PdfPageData(
                                width = bitmap.width,
                                height = bitmap.height,
                                jpegBytes = stream.toByteArray()
                            )
                        )
                        bitmap.recycle()
                    }
                }

                val fileName = "${cleanBaseName}_extracted_$timestamp.pdf"
                val savedPath = savePdfToStorage(extractedPagesData, fileName)
                resultPaths.add(savedPath)
            }

            SplitMode.SEPARATE_INDIVIDUAL_FILES -> {
                // ── Option B: Save each page as an independent PDF ──
                val total = sortedIndices.size

                renderer.openPdf(pdfUri) { _, renderPage, _ ->
                    for ((step, pageIdx) in sortedIndices.withIndex()) {
                        onProgress(step + 1, total)

                        val bitmap = renderPage(pageIdx, 1800)
                            ?: throw IllegalStateException("Failed to render page $pageIdx")

                        val stream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                        val pageData = listOf(
                            FastPdfWriter.PdfPageData(
                                width = bitmap.width,
                                height = bitmap.height,
                                jpegBytes = stream.toByteArray()
                            )
                        )
                        bitmap.recycle()

                        val pageNum = pageIdx + 1
                        val fileName = "${cleanBaseName}_page_${pageNum}_$timestamp.pdf"
                        val savedPath = savePdfToStorage(pageData, fileName)
                        resultPaths.add(savedPath)
                    }
                }
            }
        }

        resultPaths
    }

    private fun savePdfToStorage(pages: List<FastPdfWriter.PdfPageData>, fileName: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/SuperFastScan")
            }

            val uri = context.contentResolver.insert(
                MediaStore.Files.getContentUri("external"),
                contentValues
            ) ?: throw IllegalStateException("Failed to create MediaStore entry")

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                FastPdfWriter.writePdf(pages, outputStream)
            } ?: throw IllegalStateException("Failed to open output stream")

            uri.toString()
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "SuperFastScan"
            )
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, fileName)
            FileOutputStream(file).use { fos ->
                FastPdfWriter.writePdf(pages, fos)
            }

            file.absolutePath
        }
    }
}
