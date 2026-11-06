package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun math_duration_calculation_isCorrect() {
        val durationMs = 65000L
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / 1000) / 60
        assertEquals(5L, seconds)
        assertEquals(1L, minutes)
    }
}
