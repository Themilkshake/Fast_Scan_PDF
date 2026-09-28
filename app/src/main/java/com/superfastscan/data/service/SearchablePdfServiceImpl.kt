package com.superfastscan.data.service

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.superfastscan.domain.service.SearchablePdfResult
import com.superfastscan.domain.service.SearchablePdfService
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.pdmodel.graphics.state.RenderingMode
import com.tom_roush.pdfbox.util.Matrix
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Production-ready on-device Searchable (Sandwich) PDF generation service.
 * Converts raster/scanned PDFs into searchable documents by rendering pages,
 * extracting text and bounding boxes with Google ML Kit, and assembling a 2-layer PDF:
 * - Layer 1: Background scanned page image.
 * - Layer 2: Invisible text overlay (Tr 3 RenderingMode.NEITHER) perfectly aligned to text coordinates.
 */
@Singleton
class SearchablePdfServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SearchablePdfService {

    override suspend fun convertToSearchablePdf(
        pdfUri: Uri,
        customOutputName: String?,
        onProgress: (currentPage: Int, totalPages: Int) -> Unit
    ): Result<SearchablePdfResult> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        PDFBoxResourceLoader.init(context)

        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var recognizer: TextRecognizer? = null
        var outDoc: PDDocument? = null

        try {
            pfd = context.contentResolver.openFileDescriptor(pdfUri, "r")
                ?: return@withContext Result.failure(IllegalArgumentException("Cannot open PDF file descriptor for URI: $pdfUri"))

            renderer = PdfRenderer(pfd)
            val totalPages = renderer.pageCount
            if (totalPages == 0) {
                return@withContext Result.failure(IllegalStateException("The selected PDF has 0 pages."))
            }

            recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            outDoc = PDDocument()

            // Attempt to load system Unicode font (supports Turkish ç, ğ, ı, ö, ş, ü), or fallback to Standard 14 Helvetica
            val font: PDFont = loadBestAvailableFont(outDoc)

            var totalRecognizedWords = 0
            val targetDpi = 200f
            val scaleFactor = targetDpi / 72f

            for (pageIndex in 0 until totalPages) {
                onProgress(pageIndex + 1, totalPages)

                val page = renderer.openPage(pageIndex)
                val originalWidthPt = page.width.toFloat()
                val originalHeightPt = page.height.toFloat()

                val bmpW = (originalWidthPt * scaleFactor).toInt().coerceAtLeast(1)
                val bmpH = (originalHeightPt * scaleFactor).toInt().coerceAtLeast(1)

                val bitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                // Step 1: Perform on-device OCR on the page bitmap
                val visionText = processOcrOnBitmap(recognizer, bitmap)

                // Step 2: Compress bitmap to JPEG for background image Layer 1
                val jpegStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, jpegStream)
                val jpegBytes = jpegStream.toByteArray()
                bitmap.recycle() // Free memory immediately

                // Step 3: Construct PDF Page
                val pdPage = PDPage(PDRectangle(originalWidthPt, originalHeightPt))
                outDoc.addPage(pdPage)

                val pdImage = PDImageXObject.createFromByteArray(outDoc, jpegBytes, "page_img_$pageIndex")
                val contentStream = PDPageContentStream(outDoc, pdPage)

                try {
                    // Layer 1: Render background page image
                    contentStream.drawImage(pdImage, 0f, 0f, originalWidthPt, originalHeightPt)

                    // Layer 2: Render invisible searchable text overlay
                    contentStream.beginText()
                    // RenderingMode.NEITHER (Mode 3 Tr): Neither fill nor stroke text (invisible searchable text)
                    contentStream.setRenderingMode(RenderingMode.NEITHER)

                    val scaleX = originalWidthPt / bmpW.toFloat()
                    val scaleY = originalHeightPt / bmpH.toFloat()

                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            val box = line.boundingBox ?: continue

                            // Convert from ML Kit Top-Left pixel coordinates to PDF Bottom-Left point coordinates
                            val pdfX = box.left * scaleX
                            val pdfY = (bmpH - box.bottom) * scaleY
                            val pdfW = box.width() * scaleX
                            val pdfH = box.height() * scaleY

                            if (pdfW > 0 && pdfH > 0) {
                                val fontSize = (pdfH * 0.85f).coerceAtLeast(4f)
                                contentStream.setFont(font, fontSize)
                                contentStream.setTextMatrix(Matrix(1f, 0f, 0f, 1f, pdfX, pdfY))

                                val safeText = sanitizeText(line.text, font)
                                if (safeText.isNotBlank()) {
                                    try {
                                        contentStream.showText(safeText)
                                        totalRecognizedWords += safeText.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                                    } catch (e: Exception) {
                                        // Ignore single unencodable character errors
                                    }
                                }
                            }
                        }
                    }

                    contentStream.endText()
                } finally {
                    contentStream.close()
                }
            }

            // Step 4: Save searchable PDF to storage
            val rawName = getFileName(pdfUri)?.substringBeforeLast(".") ?: "Scanned_Document"
            val timestamp = SimpleDateFormat("ddMMyyyy_HHmm", Locale.getDefault()).format(Date())
            val baseName = (customOutputName ?: "${rawName}_Searchable_$timestamp")
                .replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "")
                .ifBlank { "Searchable_$timestamp" }
            val finalFileName = if (baseName.endsWith(".pdf", ignoreCase = true)) baseName else "$baseName.pdf"

            val tempFile = File(context.cacheDir, "temp_searchable_${System.currentTimeMillis()}.pdf")
            FileOutputStream(tempFile).use { fos ->
                outDoc.save(fos)
            }

            val (savedPath, savedUri, fileSize) = savePdfToStorage(tempFile, finalFileName)
            tempFile.delete()

            val duration = System.currentTimeMillis() - startTime

            Result.success(
                SearchablePdfResult(
                    outputPath = savedPath,
                    outputUri = savedUri,
                    outputFileName = finalFileName,
                    totalPages = totalPages,
                    totalWords = totalRecognizedWords,
                    fileSizeBytes = fileSize,
                    executionTimeMs = duration
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try { outDoc?.close() } catch (e: Exception) { e.printStackTrace() }
            try { recognizer?.close() } catch (e: Exception) { e.printStackTrace() }
            try { renderer?.close() } catch (e: Exception) { e.printStackTrace() }
            try { pfd?.close() } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun loadBestAvailableFont(doc: PDDocument): PDFont {
        val systemFontCandidates = listOf(
            "/system/fonts/Roboto-Regular.ttf",
            "/system/fonts/NotoSans-Regular.ttf",
            "/system/fonts/DroidSans.ttf"
        )
        for (path in systemFontCandidates) {
            val file = File(path)
            if (file.exists() && file.canRead()) {
                try {
                    return PDType0Font.load(doc, file)
                } catch (e: Exception) {
                    // Try next font
                }
            }
        }
        return PDType1Font.HELVETICA
    }

    private fun sanitizeText(text: String, font: PDFont): String {
        val sb = StringBuilder()
        for (ch in text) {
            try {
                font.encode(ch.toString())
                sb.append(ch)
            } catch (e: Exception) {
                // Map Turkish/Unicode characters if font is standard 14 font
                val ascii = when (ch) {
                    'ç' -> 'c'
                    'Ç' -> 'C'
                    'ğ' -> 'g'
                    'Ğ' -> 'G'
                    'ı' -> 'i'
                    'İ' -> 'I'
                    'ö' -> 'o'
                    'Ö' -> 'O'
                    'ş' -> 's'
                    'Ş' -> 'S'
                    'ü' -> 'u'
                    'Ü' -> 'U'
                    else -> ' '
                }
                sb.append(ascii)
            }
        }
        return sb.toString()
    }

    private suspend fun processOcrOnBitmap(
        recognizer: TextRecognizer,
        bitmap: Bitmap
    ) = suspendCancellableCoroutine<com.google.mlkit.vision.text.Text> { continuation ->
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                continuation.resume(visionText)
            }
            .addOnFailureListener { ex ->
                continuation.resumeWithException(ex)
            }
    }

    private fun savePdfToStorage(sourceFile: File, fileName: String): Triple<String, Uri?, Long> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/SuperFastScan")
            }

            val uri = context.contentResolver.insert(
                MediaStore.Files.getContentUri("external"),
                contentValues
            ) ?: throw IllegalStateException("Failed to create MediaStore entry for Searchable PDF")

            context.contentResolver.openOutputStream(uri)?.use { os ->
                sourceFile.inputStream().use { inputStream ->
                    inputStream.copyTo(os)
                }
            } ?: throw IllegalStateException("Failed to write searchable PDF to MediaStore")

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

            val destFile = File(dir, fileName)
            sourceFile.copyTo(destFile, overwrite = true)
            Triple(destFile.absolutePath, Uri.fromFile(destFile), destFile.length())
        }
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
}
