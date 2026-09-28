package com.superfastscan.ui.splitpdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitPdfViewModelTest {

    @Test
    fun parsePageRanges_singlePages() {
        val result = SplitPdfViewModel.parsePageRanges("1, 3, 5", totalPages = 10)
        assertEquals(setOf(0, 2, 4), result)
    }

    @Test
    fun parsePageRanges_rangesAndSingles() {
        val result = SplitPdfViewModel.parsePageRanges("1-3, 5, 8-10", totalPages = 10)
        assertEquals(setOf(0, 1, 2, 4, 7, 8, 9), result)
    }

    @Test
    fun parsePageRanges_semicolonsAndSpaces() {
        val result = SplitPdfViewModel.parsePageRanges(" 2-4 ; 7 , 9 ", totalPages = 10)
        assertEquals(setOf(1, 2, 3, 6, 8), result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parsePageRanges_outOfBounds_throws() {
        SplitPdfViewModel.parsePageRanges("1-15", totalPages = 10)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parsePageRanges_invalidRangeOrder_throws() {
        SplitPdfViewModel.parsePageRanges("5-2", totalPages = 10)
    }

    @Test
    fun parsePageRanges_emptyInput_returnsEmpty() {
        val result = SplitPdfViewModel.parsePageRanges("", totalPages = 10)
        assertTrue(result.isEmpty())
    }
}
