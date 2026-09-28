package com.superfastscan.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.superfastscan.domain.util.PdfPageRenderer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class PdfToJpgResult(
    val outputImagePaths: List<String>,
    val outputImageUris: List<Uri>,
    val totalPages: Int,
    val baseName: String
)

/**
 * 100% Offline PDF to High-Resolution JPG Converter.
 *
 * Renders PDF pages directly on the device using Android's native PdfRenderer
 * without requiring any network connection or remote server.
 */
class PdfToJpgUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(
        pdfUri: Uri,
        outputBaseName: String? = null,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): PdfToJpgResult = withContext(Dispatchers.IO) {
        val rawName = getFileName(pdfUri)?.substringBeforeLast(".") ?: "PDF"
        val timestamp = SimpleDateFormat("ddMMyyyy_HHmm", Locale.getDefault()).format(Date())
        val cleanName = (outputBaseName ?: "${rawName}_$timestamp")
            .replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "")
            .ifBlank { "PDF_$timestamp" }

        val imagePaths = mutableListOf<String>()
        val imageUris = mutableListOf<Uri>()

        val renderer = PdfPageRenderer(context)
        renderer.openPdf(pdfUri) { pageCount, renderPage, _ ->
            if (pageCount <= 0) {
                throw IllegalStateException("PDF dosyası boş veya sayfa okunamadı")
            }

            for (pageIndex in 0 until pageCount) {
                onProgress(pageIndex + 1, pageCount)

                // Render at high-resolution 1800px width for crisp quality
                val bitmap = renderPage(pageIndex, 1800)
                    ?: throw IllegalStateException("Sayfa ${pageIndex + 1} işlenemedi")

                val fileName = if (pageCount == 1) "$cleanName.jpg" else "${cleanName}_page_${pageIndex + 1}.jpg"
                val (path, uri) = saveBitmapToStorage(bitmap, fileName)
                imagePaths.add(path)
                if (uri != null) imageUris.add(uri)

                bitmap.recycle()
            }
        }

        PdfToJpgResult(
            outputImagePaths = imagePaths,
            outputImageUris = imageUris,
            totalPages = imagePaths.size,
            baseName = cleanName
        )
    }

    private fun saveBitmapToStorage(bitmap: Bitmap, fileName: String): Pair<String, Uri?> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SuperFastScan")
            }

            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: throw IllegalStateException("Failed to create MediaStore image entry")

            context.contentResolver.openOutputStream(uri)?.use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            } ?: throw IllegalStateException("Failed to open image output stream")

            Pair(uri.toString(), uri)
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "SuperFastScan"
            )
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, fileName)
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            }

            Pair(file.absolutePath, Uri.fromFile(file))
        }
    }

    fun getFileName(uri: Uri): String? {
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
