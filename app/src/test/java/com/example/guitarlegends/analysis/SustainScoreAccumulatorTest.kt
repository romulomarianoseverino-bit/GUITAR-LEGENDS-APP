package com.example.guitarlegends.analysis

import org.junit.Assert.assertEquals
import org.junit.Test

class SustainScoreAccumulatorTest {

    @Test
    fun awardsPointsOnlyWhileTheSustainIsHeld() {
        val accumulator = SustainScoreAccumulator()
        assertEquals(0, accumulator.update(0L, isHeld = true, multiplier = 1))
        assertEquals(0, accumulator.update(40L, isHeld = true, multiplier = 1))
        assertEquals(0, accumulator.update(80L, isHeld = true, multiplier = 1))
        assertEquals(1, accumulator.update(120L, isHeld = true, multiplier = 1))
        assertEquals(0, accumulator.update(160L, isHeld = false, multiplier = 1))
        assertEquals(0, accumulator.update(200L, isHeld = false, multiplier = 1))
    }

    @Test
    fun scalesPointsWithMultiplierAndResetsCleanly() {
        val accumulator = SustainScoreAccumulator()
        accumulator.update(100L, isHeld = true, multiplier = 3)
        assertEquals(1, accumulator.update(140L, isHeld = true, multiplier = 3))
        accumulator.reset()
        assertEquals(0, accumulator.update(9000L, isHeld = true, multiplier = 5))
        assertEquals(2, accumulator.update(9040L, isHeld = true, multiplier = 5))
    }

    @Test
    fun clampsLongFrameGapsToAvoidUnexpectedScoreJumps() {
        val accumulator = SustainScoreAccumulator()
        accumulator.update(0L, isHeld = true, multiplier = 1)
        assertEquals(0, accumulator.update(5000L, isHeld = true, multiplier = 1))
        assertEquals(0, accumulator.update(5040L, isHeld = true, multiplier = 1))
        assertEquals(1, accumulator.update(5080L, isHeld = true, multiplier = 1))
    }
}
