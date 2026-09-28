package com.superfastscan.data.remote

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.PushbackInputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Robust, production-ready server-side conversion service powered by Stirling-PDF.
 * Communicates with self-hosted Stirling-PDF instance exposed via Cloudflare Tunnel.
 */
@Singleton
class StirlingPdfConversionService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    @ApplicationContext private val context: Context
) {
    companion object {
        const val BASE_URL = "https://superfastpdf.duckdns.org"

        // Configured API Key for Stirling-PDF (header: X-API-KEY). Leave empty if auth is disabled.
        var apiKey: String = ""

        // Endpoints
        private const val ENDPOINT_FILE_TO_PDF = "$BASE_URL/api/v1/convert/file/pdf"
        private const val ENDPOINT_PDF_TO_WORD = "$BASE_URL/api/v1/convert/pdf/word"
        private const val ENDPOINT_PDF_TO_PRESENTATION = "$BASE_URL/api/v1/convert/pdf/presentation"
        private const val ENDPOINT_PDF_TO_EXCEL = "$BASE_URL/api/v1/convert/pdf/xlsx"
        private const val ENDPOINT_PDF_TO_IMAGE = "$BASE_URL/api/v1/convert/pdf/img"
    }

    // Configured with 30s connect and 90s read/write timeouts for complex document processing
    private val client: OkHttpClient = okHttpClient.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Converts an Office document (.docx, .doc, .pptx, .ppt, .xlsx, .xls, .odt, .ods, .odp) to PDF.
     * Endpoint: POST /api/v1/convert/file/pdf (Multipart key: "fileInput")
     */
    suspend fun convertOfficeToPdf(
        fileName: String,
        fileInputStream: InputStream,
        fileSizeBytes: Long,
        destinationStream: OutputStream
    ): Long = withContext(Dispatchers.IO) {
        val mediaType = getMediaTypeForFileName(fileName)
        val requestBody = createStreamingRequestBody(fileInputStream, fileSizeBytes, mediaType)

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("fileInput", fileName, requestBody)
            .build()

        executeRequestAndStreamOutput(ENDPOINT_FILE_TO_PDF, multipartBody, destinationStream)
    }

    /**
     * Converts a PDF to a Word document (.docx).
     * Endpoint: POST /api/v1/convert/pdf/word (Multipart key: "fileInput", outputFormat="docx")
     */
    suspend fun convertPdfToWord(
        fileName: String,
        pdfInputStream: InputStream,
        fileSizeBytes: Long,
        destinationStream: OutputStream,
        outputFormat: String = "docx"
    ): Long = withContext(Dispatchers.IO) {
        val requestBody = createStreamingRequestBody(pdfInputStream, fileSizeBytes, "application/pdf".toMediaTypeOrNull())

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("fileInput", fileName, requestBody)
            .addFormDataPart("outputFormat", outputFormat)
            .build()

        executeRequestAndStreamOutput(ENDPOINT_PDF_TO_WORD, multipartBody, destinationStream)
    }

    /**
     * Converts a PDF to a PowerPoint presentation (.pptx).
     * Endpoint: POST /api/v1/convert/pdf/presentation (Multipart key: "fileInput", outputFormat="pptx")
     */
    suspend fun convertPdfToPresentation(
        fileName: String,
        pdfInputStream: InputStream,
        fileSizeBytes: Long,
        destinationStream: OutputStream,
        outputFormat: String = "pptx"
    ): Long = withContext(Dispatchers.IO) {
        val requestBody = createStreamingRequestBody(pdfInputStream, fileSizeBytes, "application/pdf".toMediaTypeOrNull())

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("fileInput", fileName, requestBody)
            .addFormDataPart("outputFormat", outputFormat)
            .build()

        executeRequestAndStreamOutput(ENDPOINT_PDF_TO_PRESENTATION, multipartBody, destinationStream)
    }

    /**
     * Converts a PDF to an Excel document (.xlsx).
     * Endpoint: POST /api/v1/convert/pdf/xlsx (Multipart key: "fileInput", pageNumbers="all")
     */
    suspend fun convertPdfToExcel(
        fileName: String,
        pdfInputStream: InputStream,
        fileSizeBytes: Long,
        destinationStream: OutputStream,
        pageNumbers: String = "all"
    ): Long = withContext(Dispatchers.IO) {
        val requestBody = createStreamingRequestBody(pdfInputStream, fileSizeBytes, "application/pdf".toMediaTypeOrNull())

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("fileInput", fileName, requestBody)
            .addFormDataPart("pageNumbers", pageNumbers)
            .build()

        executeRequestAndStreamOutput(ENDPOINT_PDF_TO_EXCEL, multipartBody, destinationStream)
    }

    /**
     * Converts a PDF to JPEG image(s).
     * Endpoint: POST /api/v1/convert/pdf/img
     * If multiple pages are converted, Stirling-PDF returns a ZIP archive containing images.
     * If single page is converted, it may return a raw JPEG.
     * This method automatically detects the format, extracts ZIP entries to the target directory,
     * and returns the list of extracted image files.
     */
    suspend fun convertPdfToImages(
        fileName: String,
        pdfInputStream: InputStream,
        fileSizeBytes: Long,
        outputDirectory: File,
        baseName: String
    ): List<File> = withContext(Dispatchers.IO) {
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }

        val requestBody = createStreamingRequestBody(pdfInputStream, fileSizeBytes, "application/pdf".toMediaTypeOrNull())

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("fileInput", fileName, requestBody)
            .addFormDataPart("imageFormat", "jpeg")
            .addFormDataPart("singleOrMultiple", "multiple")
            .addFormDataPart("colorType", "color")
            .addFormDataPart("dpi", "300")
            .addFormDataPart("pageNumbers", "all")
            .build()

        val requestBuilder = Request.Builder()
            .url(ENDPOINT_PDF_TO_IMAGE)
            .post(multipartBody)

        if (apiKey.isNotBlank()) {
            requestBuilder.addHeader("X-API-KEY", apiKey)
        }

        val request = requestBuilder.build()

        client.newCall(request).execute().use { response ->
            validateResponse(response)

            val body = response.body ?: throw StirlingPdfException.EmptyResponseBodyException()
            val rawStream = body.byteStream()

            // Peek first 4 bytes to check if response is a ZIP file (PK..) or JPEG (0xFF, 0xD8)
            val pushbackStream = PushbackInputStream(rawStream, 4)
            val header = ByteArray(4)
            val bytesRead = pushbackStream.read(header)
            if (bytesRead > 0) {
                pushbackStream.unread(header, 0, bytesRead)
            }

            val isZip = bytesRead >= 2 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() // "PK"
            val outputFiles = mutableListOf<File>()

            if (isZip) {
                // Extract ZIP contents
                val zipInputStream = ZipInputStream(pushbackStream)
                var entry: ZipEntry? = zipInputStream.nextEntry
                var pageIndex = 1

                while (entry != null) {
                    if (!entry.isDirectory) {
                        val entryName = entry.name
                        val extension = entryName.substringAfterLast('.', "jpg")
                        val destFile = File(outputDirectory, "${baseName}_page_$pageIndex.$extension")

                        FileOutputStream(destFile).use { fos ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            while (zipInputStream.read(buffer).also { read = it } != -1) {
                                fos.write(buffer, 0, read)
                            }
                        }
                        outputFiles.add(destFile)
                        pageIndex++
                    }
                    zipInputStream.closeEntry()
                    entry = zipInputStream.nextEntry
                }
            } else {
                // Single JPEG image returned directly
                val singleFile = File(outputDirectory, "$baseName.jpg")
                FileOutputStream(singleFile).use { fos ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (pushbackStream.read(buffer).also { read = it } != -1) {
                        fos.write(buffer, 0, read)
                    }
                }
                outputFiles.add(singleFile)
            }

            if (outputFiles.isEmpty()) {
                throw StirlingPdfException.ConversionException("No images extracted from server response")
            }

            outputFiles
        }
    }

    /**
     * Executes the HTTP request and streams the response directly to destinationStream.
     */
    private fun executeRequestAndStreamOutput(
        url: String,
        body: RequestBody,
        destinationStream: OutputStream
    ): Long {
        val requestBuilder = Request.Builder()
            .url(url)
            .post(body)

        if (apiKey.isNotBlank()) {
            requestBuilder.addHeader("X-API-KEY", apiKey)
        }

        val request = requestBuilder.build()

        return client.newCall(request).execute().use { response ->
            validateResponse(response)

            val responseBody = response.body ?: throw StirlingPdfException.EmptyResponseBodyException()
            var totalBytesWritten = 0L

            responseBody.byteStream().use { responseStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (responseStream.read(buffer).also { bytesRead = it } != -1) {
                    destinationStream.write(buffer, 0, bytesRead)
                    totalBytesWritten += bytesRead
                }
                destinationStream.flush()
            }

            if (totalBytesWritten == 0L) {
                throw StirlingPdfException.EmptyResponseBodyException()
            }

            totalBytesWritten
        }
    }

    /**
     * Translates HTTP error status codes into descriptive domain exceptions.
     */
    private fun validateResponse(response: Response) {
        if (response.isSuccessful) return

        val errorBody = try {
            response.body?.string()?.take(500) ?: ""
        } catch (_: Exception) {
            ""
        }

        when (response.code) {
            400 -> throw StirlingPdfException.BadRequestException(
                "Invalid document format or corrupted file ($errorBody)"
            )
            401 -> throw StirlingPdfException.UnauthorizedException(
                "Stirling-PDF API Key authentication failed (HTTP 401). Please check X-API-KEY configuration."
            )
            413 -> throw StirlingPdfException.PayloadTooLargeException(
                "The file exceeds the maximum upload size allowed by the server (HTTP 413)."
            )
            422 -> throw StirlingPdfException.UnprocessableEntityException(
                "Document could not be processed by Stirling-PDF ($errorBody)."
            )
            500 -> throw StirlingPdfException.ServerException(
                "Stirling-PDF internal conversion error (HTTP 500): $errorBody"
            )
            else -> throw StirlingPdfException.HttpException(
                response.code,
                "Server returned error (${response.code}): ${errorBody.ifBlank { response.message }}"
            )
        }
    }

    /**
     * Creates an efficient streaming RequestBody that does not load the entire file into memory.
     */
    private fun createStreamingRequestBody(
        inputStream: InputStream,
        fileSizeBytes: Long,
        mediaType: okhttp3.MediaType?
    ): RequestBody {
        return object : RequestBody() {
            override fun contentType() = mediaType

            override fun contentLength(): Long = if (fileSizeBytes > 0) fileSizeBytes else -1L

            override fun writeTo(sink: BufferedSink) {
                inputStream.use { input ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        sink.write(buffer, 0, read)
                    }
                }
            }
        }
    }

    private fun getMediaTypeForFileName(fileName: String): okhttp3.MediaType? {
        return when {
            fileName.endsWith(".docx", ignoreCase = true) ->
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document".toMediaTypeOrNull()
            fileName.endsWith(".doc", ignoreCase = true) ->
                "application/msword".toMediaTypeOrNull()
            fileName.endsWith(".pptx", ignoreCase = true) ->
                "application/vnd.openxmlformats-officedocument.presentationml.presentation".toMediaTypeOrNull()
            fileName.endsWith(".ppt", ignoreCase = true) ->
                "application/vnd.ms-powerpoint".toMediaTypeOrNull()
            fileName.endsWith(".xlsx", ignoreCase = true) ->
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".toMediaTypeOrNull()
            fileName.endsWith(".xls", ignoreCase = true) ->
                "application/vnd.ms-excel".toMediaTypeOrNull()
            fileName.endsWith(".csv", ignoreCase = true) ->
                "text/csv".toMediaTypeOrNull()
            fileName.endsWith(".rtf", ignoreCase = true) ->
                "application/rtf".toMediaTypeOrNull()
            fileName.endsWith(".odt", ignoreCase = true) ->
                "application/vnd.oasis.opendocument.text".toMediaTypeOrNull()
            fileName.endsWith(".odp", ignoreCase = true) ->
                "application/vnd.oasis.opendocument.presentation".toMediaTypeOrNull()
            fileName.endsWith(".ods", ignoreCase = true) ->
                "application/vnd.oasis.opendocument.spreadsheet".toMediaTypeOrNull()
            fileName.endsWith(".pdf", ignoreCase = true) ->
                "application/pdf".toMediaTypeOrNull()
            else ->
                "application/octet-stream".toMediaTypeOrNull()
        }
    }
}

/**
 * Sealed hierarchy of user-friendly Stirling-PDF conversion exceptions.
 */
sealed class StirlingPdfException(message: String, cause: Throwable? = null) : IOException(message, cause) {
    class BadRequestException(message: String) : StirlingPdfException(message)
    class UnauthorizedException(message: String) : StirlingPdfException(message)
    class PayloadTooLargeException(message: String) : StirlingPdfException(message)
    class UnprocessableEntityException(message: String) : StirlingPdfException(message)
    class ServerException(message: String) : StirlingPdfException(message)
    class EmptyResponseBodyException : StirlingPdfException("Server returned 0 bytes or an empty response")
    class ConversionException(message: String) : StirlingPdfException(message)
    class HttpException(val code: Int, message: String) : StirlingPdfException(message)
}
