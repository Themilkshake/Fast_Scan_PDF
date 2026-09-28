package com.superfastscan.domain.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlin.math.max
import kotlin.math.roundToInt

object BitmapUtils {

    /**
     * Decodes a bitmap from a file path or content URI.
     * If [targetMaxDimension] is provided, uses [BitmapFactory.Options.inSampleSize]
     * to decode a downsampled version directly, avoiding full-resolution memory allocation.
     */
    fun decodeBitmap(context: Context, path: String, targetMaxDimension: Int = 0): Bitmap? {
        return try {
            if (targetMaxDimension > 0) {
                decodeBitmapWithSampling(context, path, targetMaxDimension)
            } else {
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                decodeBitmapFromPath(context, path, options)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Two-pass decode: first reads dimensions only, then decodes with calculated inSampleSize.
     * This dramatically reduces peak memory usage for large camera images.
     */
    private fun decodeBitmapWithSampling(context: Context, path: String, targetMaxDimension: Int): Bitmap? {
        // Pass 1: Read dimensions only (no memory allocation)
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        decodeBitmapFromPath(context, path, boundsOptions)

        val imageWidth = boundsOptions.outWidth
        val imageHeight = boundsOptions.outHeight
        if (imageWidth <= 0 || imageHeight <= 0) return null

        // Pass 2: Decode with calculated inSampleSize
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(imageWidth, imageHeight, targetMaxDimension)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return decodeBitmapFromPath(context, path, decodeOptions)
    }

    private fun decodeBitmapFromPath(context: Context, path: String, options: BitmapFactory.Options): Bitmap? {
        return if (path.startsWith("content://") || path.startsWith("file://")) {
            val uri = Uri.parse(path)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        } else {
            BitmapFactory.decodeFile(path, options)
        }
    }

    /**
     * Calculates the largest power-of-2 inSampleSize such that the decoded image's
     * largest dimension is still >= [targetMaxDimension].
     * E.g., a 4000x3000 image with target 2400 → inSampleSize = 1 (no downsampling needed).
     *       a 8000x6000 image with target 2400 → inSampleSize = 2 (decoded as 4000x3000).
     */
    private fun calculateInSampleSize(width: Int, height: Int, targetMaxDimension: Int): Int {
        var inSampleSize = 1
        val maxSide = max(width, height)
        while (maxSide / (inSampleSize * 2) >= targetMaxDimension) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    /**
     * Scales the bitmap so that its largest dimension does not exceed [maxDimension],
     * preserving the original aspect ratio and using high-quality bilinear filtering.
     * If the bitmap is already within [maxDimension], it is returned as-is.
     */
    fun scaleBitmapForDocument(source: Bitmap, maxDimension: Int = 2048): Bitmap {
        val width = source.width
        val height = source.height
        val maxSide = max(width, height)

        if (maxSide <= maxDimension || maxSide == 0) {
            return source
        }

        val scale = maxDimension.toFloat() / maxSide
        val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
        val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)

        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }
}

