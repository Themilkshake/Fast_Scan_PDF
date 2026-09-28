package com.superfastscan.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.superfastscan.domain.model.OrganizePageItem
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class OrganizePdfResult(
    val outputPath: String,
    val outputUri: Uri?,
    val outputFileName: String,
    val pageCount: Int,
    val fileSizeBytes: Long
)

/**
 * High-performance, non-destructive PDF Reordering & Organizing Use Case.
 *
 * Reassembles pages from one or multiple source PDFs in the exact user-defined sequence
 * using PDFBox-Android page cloning. Allows inserting blank pages, deleting pages,
 * and appending other PDFs while preserving 100% original vector paths, typography, and image quality.
 */
class OrganizePdfUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(
        pages: List<OrganizePageItem>,
        baseFileName: String? = null,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): OrganizePdfResult = withContext(Dispatchers.IO) {
        if (pages.isEmpty()) {
            throw IllegalArgumentException("Cannot organize PDF with zero pages")
        }

        PDFBoxResourceLoader.init(context)

        val outputDocument = PDDocument()
        val loadedSourceDocs = mutableMapOf<Uri, PDDocument>()
        val openedStreams = mutableListOf<InputStream>()

        try {
            val total = pages.size
            for ((index, item) in pages.withIndex()) {
                onProgress(index + 1, total)

                if (item.isBlankPage || item.sourceUri == null) {
                    // Create standard A4 blank page
                    val blankPage = PDPage(PDRectangle.A4)
                    outputDocument.addPage(blankPage)
                } else {
                    val uri = item.sourceUri
                    val sourceDoc = loadedSourceDocs.getOrPut(uri) {
                        val stream = context.contentResolver.openInputStream(uri)
                            ?: throw IllegalStateException("Cannot open stream for $uri")
                        openedStreams.add(stream)
                        PDDocument.load(stream)
                    }

                    if (item.sourcePageIndex in 0 until sourceDoc.numberOfPages) {
                        val originalPage = sourceDoc.getPage(item.sourcePageIndex)
                        outputDocument.importPage(originalPage)
                    }
                }
            }

            val timestamp = SimpleDateFormat("ddMMyyyy_HHmm", Locale.getDefault()).format(Date())
            val cleanBaseName = (baseFileName ?: "Document")
                .replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "")
                .ifBlank { "Document" }

            val outputFileName = "${cleanBaseName}_organized_$timestamp.pdf"
            val (savedPath, savedUri, writtenBytes) = saveDocumentToStorage(outputDocument, outputFileName)

            OrganizePdfResult(
                outputPath = savedPath,
                outputUri = savedUri,
                outputFileName = outputFileName,
                pageCount = outputDocument.numberOfPages,
                fileSizeBytes = writtenBytes
            )
        } finally {
            outputDocument.close()
            for (doc in loadedSourceDocs.values) {
                try { doc.close() } catch (e: Exception) { e.printStackTrace() }
            }
            for (stream in openedStreams) {
                try { stream.close() } catch (e: Exception) { e.printStackTrace() }
            }
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
