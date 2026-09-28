package com.superfastscan.ui.watermarkpdf

import com.superfastscan.domain.usecase.WatermarkConfig
import com.superfastscan.domain.usecase.WatermarkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatermarkViewModelTest {

    @Test
    fun watermarkConfig_defaultValues() {
        val config = WatermarkConfig()
        assertEquals(WatermarkType.TEXT, config.type)
        assertEquals("CONFIDENTIAL", config.text)
        assertEquals(0xFFFF0000L, config.textColor)
        assertTrue(config.isBold)
        assertEquals(0.40f, config.opacity, 0.001f)
        assertEquals(-45f, config.rotation, 0.001f)
        assertEquals(1.0f, config.scale, 0.001f)
        assertEquals(0.5f, config.normalizedX, 0.001f)
        assertEquals(0.5f, config.normalizedY, 0.001f)
        assertTrue(config.applyToAllPages)
    }

    @Test
    fun watermarkConfig_coordinateClamping() {
        val config = WatermarkConfig(
            normalizedX = (-0.5f).coerceIn(0.05f, 0.95f),
            normalizedY = (1.5f).coerceIn(0.05f, 0.95f)
        )
        assertEquals(0.05f, config.normalizedX, 0.001f)
        assertEquals(0.95f, config.normalizedY, 0.001f)
    }

    @Test
    fun watermarkConfig_opacityAndRotationBounds() {
        val opacityClamped = (1.5f).coerceIn(0.05f, 1.0f)
        assertEquals(1.0f, opacityClamped, 0.001f)

        val rotationClamped = (-200f).coerceIn(-180f, 180f)
        assertEquals(-180f, rotationClamped, 0.001f)
    }

    @Test
    fun watermarkConfig_scaleBounds() {
        val scaleMin = (0.01f).coerceIn(0.2f, 3.5f)
        val scaleMax = (5.0f).coerceIn(0.2f, 3.5f)
        assertEquals(0.2f, scaleMin, 0.001f)
        assertEquals(3.5f, scaleMax, 0.001f)
    }
}
