package com.superfastscan.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class RotatePdfResult(
    val outputPath: String,
    val outputUri: Uri?,
    val outputFileName: String,
    val pageCount: Int,
    val fileSizeBytes: Long
)

/**
 * 100% Non-Destructive PDF Rotation Use Case.
 *
 * Modifies the /Rotate dictionary metadata of individual PDPage objects in-place
 * using PDFBox-Android. Does not rasterize or re-encode, preserving original resolution,
 * text searchability, vector paths, and file size.
 */
class RotatePdfUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(
        pdfUri: Uri,
        pageRotations: Map<Int, Int>, // pageIndex -> rotationOffset (0, 90, 180, 270)
        baseFileName: String? = null
    ): RotatePdfResult = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)

        val inputStream = context.contentResolver.openInputStream(pdfUri)
            ?: throw IllegalStateException("Cannot open input stream for $pdfUri")

        val document = PDDocument.load(inputStream)
        val totalPages = document.numberOfPages

        try {
            for (pageIndex in 0 until totalPages) {
                val delta = pageRotations[pageIndex] ?: 0
                if (delta != 0) {
                    val page = document.getPage(pageIndex)
                    val currentRotation = page.rotation
                    var newRotation = (currentRotation + delta) % 360
                    if (newRotation < 0) newRotation += 360
                    page.rotation = newRotation
                }
            }

            val timestamp = SimpleDateFormat("ddMMyyyy_HHmm", Locale.getDefault()).format(Date())
            val cleanBaseName = (baseFileName ?: "Document")
                .replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "")
                .ifBlank { "Document" }

            val outputFileName = "${cleanBaseName}_rotated_$timestamp.pdf"
            val (savedPath, savedUri, writtenBytes) = saveDocumentToStorage(document, outputFileName)

            RotatePdfResult(
                outputPath = savedPath,
                outputUri = savedUri,
                outputFileName = outputFileName,
                pageCount = totalPages,
                fileSizeBytes = writtenBytes
            )
        } finally {
            document.close()
            inputStream.close()
        }
    }

    private fun saveDocumentToStorage(
        document: PDDocument,
        fileName: String
    ): Triple<String, Uri?, Long> {
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
                document.save(outputStream)
            } ?: throw IllegalStateException("Failed to open output stream")

            var size = 0L
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                size = pfd.statSize
            }

            Triple(uri.toString(), uri, size)
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "SuperFastScan"
            )
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, fileName)
            FileOutputStream(file).use { fos ->
                document.save(fos)
            }

            Triple(file.absolutePath, Uri.fromFile(file), file.length())
        }
    }
}
