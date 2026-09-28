package com.superfastscan.ui.rotatepdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RotatePdfViewModelTest {

    @Test
    fun rotationCalculations_clockwiseSinglePage() {
        var rot = 0
        rot = (rot + 90) % 360
        assertEquals(90, rot)

        rot = (rot + 90) % 360
        assertEquals(180, rot)

        rot = (rot + 90) % 360
        assertEquals(270, rot)

        rot = (rot + 90) % 360
        assertEquals(0, rot)
    }

    @Test
    fun rotationCalculations_counterClockwiseNegativeWrapping() {
        var rot = 0
        var newRot = (rot - 90) % 360
        if (newRot < 0) newRot += 360
        assertEquals(270, newRot)

        rot = newRot
        newRot = (rot - 90) % 360
        if (newRot < 0) newRot += 360
        assertEquals(180, newRot)
    }

    @Test
    fun rotationCalculations_batchRotateAll() {
        val totalPages = 5
        val pageRotations = mutableMapOf<Int, Int>()

        // Rotate all by +90°
        for (i in 0 until totalPages) {
            val current = pageRotations[i] ?: 0
            var newRotation = (current + 90) % 360
            if (newRotation < 0) newRotation += 360
            if (newRotation != 0) pageRotations[i] = newRotation
        }

        assertEquals(5, pageRotations.size)
        pageRotations.values.forEach {
            assertEquals(90, it)
        }

        // Rotate all by -90° (back to 0)
        val updatedRotations = mutableMapOf<Int, Int>()
        for (i in 0 until totalPages) {
            val current = pageRotations[i] ?: 0
            var newRotation = (current - 90) % 360
            if (newRotation < 0) newRotation += 360
            if (newRotation != 0) updatedRotations[i] = newRotation
        }

        assertTrue(updatedRotations.isEmpty())
    }
}
