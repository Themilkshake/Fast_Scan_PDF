package com.superfastscan.ui.compresspdf

import com.superfastscan.domain.usecase.CompressionLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompressPdfViewModelTest {

    @Test
    fun formatFileSize_bytes() {
        assertEquals("0 B", CompressPdfViewModel.formatFileSize(0))
        assertEquals("500 B", CompressPdfViewModel.formatFileSize(500))
    }

    @Test
    fun formatFileSize_kilobytes() {
        assertEquals("1.0 KB", CompressPdfViewModel.formatFileSize(1024))
        assertEquals("250.0 KB", CompressPdfViewModel.formatFileSize(256000))
    }

    @Test
    fun formatFileSize_megabytes() {
        assertEquals("1.00 MB", CompressPdfViewModel.formatFileSize(1048576))
        assertEquals("12.50 MB", CompressPdfViewModel.formatFileSize((12.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun formatFileSize_gigabytes() {
        assertEquals("1.00 GB", CompressPdfViewModel.formatFileSize(1073741824))
    }

    @Test
    fun compressionLevels_correctParameters() {
        // Ultra high quality (Minimum compression)
        assertEquals(1800, CompressionLevel.MINIMUM.targetWidth)
        assertEquals(88, CompressionLevel.MINIMUM.jpegQuality)
        assertEquals(220, CompressionLevel.MINIMUM.approxDpi)

        // Balanced / Recommended (Medium compression)
        assertEquals(1240, CompressionLevel.MEDIUM.targetWidth)
        assertEquals(75, CompressionLevel.MEDIUM.jpegQuality)
        assertEquals(150, CompressionLevel.MEDIUM.approxDpi)

        // High Compression (Maximum compression)
        assertEquals(750, CompressionLevel.MAXIMUM.targetWidth)
        assertEquals(50, CompressionLevel.MAXIMUM.jpegQuality)
        assertEquals(90, CompressionLevel.MAXIMUM.approxDpi)
    }

    @Test
    fun compressionSavingsPercentage_calculation() {
        val originalSize = 10_000_000L // 10 MB
        val compressedSize = 2_500_000L // 2.5 MB
        val savedPercent = (((originalSize - compressedSize).toDouble() / originalSize.toDouble()) * 100).toInt()
        assertEquals(75, savedPercent)
    }
}
