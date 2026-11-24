package com.example

import com.example.guitarlegends.analysis.AdaptiveEngine
import com.example.guitarlegends.analysis.AudioAnalyzer
import com.example.guitarlegends.analysis.SustainScoreAccumulator
import com.example.guitarlegends.model.Difficulty
import com.example.guitarlegends.model.HitRating
import com.example.guitarlegends.model.Instrument
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testAudioAnalyzerGeneratesValidNotes() {
        val tracks = AudioAnalyzer.BUILT_IN_TRACKS
        assertTrue("Must have built-in tracks", tracks.isNotEmpty())

        val testTrack = tracks[0]
        val chart = AudioAnalyzer.generateChart(testTrack, Instrument.GUITAR, Difficulty.MEDIUM)

        assertTrue("Generated chart must not be empty", chart.isNotEmpty())
        for (note in chart) {
            assertTrue("Lane must be in valid range for Medium difficulty", note.lane in 0 until Difficulty.MEDIUM.lanesUsed)
            assertTrue("Note timestamp must be positive", note.timeMs > 0)
        }
    }

    @Test
    fun testAdaptiveEngineAdjustsWindow() {
        val engine = AdaptiveEngine(Difficulty.HARD, isEnabled = true)
        val initialWindow = engine.getEffectiveHitWindowMs()

        // Consecutive misses should expand window
        repeat(5) {
            engine.onNoteResult(HitRating.MISS, 0L)
        }

        val expandedWindow = engine.getEffectiveHitWindowMs()
        assertTrue("Hit window should expand or stay forgiving during consecutive misses", expandedWindow >= initialWindow)

        // Multiple perfect hits should reward player
        repeat(20) {
            engine.onNoteResult(HitRating.PERFECT, 10L)
        }

        assertTrue("Player should have accumulated perfect count", engine.perfectCount > 0)
    }

    @Test
    fun sustainScoring_awardsIncrementallyOnlyWhileHeld_andHandlesLongFramesSafely() {
        val tracker = SustainScoreAccumulator()
        assertEquals(0, tracker.update(0L, isHeld = true, multiplier = 1))
        assertEquals(0, tracker.update(40L, isHeld = true, multiplier = 1))
        assertEquals(0, tracker.update(80L, isHeld = true, multiplier = 1))
        assertEquals(1, tracker.update(120L, isHeld = true, multiplier = 1))
        assertEquals(0, tracker.update(160L, isHeld = false, multiplier = 1))
        assertEquals(0, tracker.update(200L, isHeld = false, multiplier = 1))
        assertEquals(0, tracker.update(240L, isHeld = true, multiplier = 1))
        assertEquals(0, tracker.update(5000L, isHeld = true, multiplier = 1))
    }

    @Test
    fun sustainScoring_scalesWithMultiplier_andResetStartsCleanly() {
        val tracker = SustainScoreAccumulator()
        tracker.update(100L, isHeld = true, multiplier = 3)
        assertEquals(1, tracker.update(140L, isHeld = true, multiplier = 3))
        tracker.reset()
        assertEquals(0, tracker.update(9000L, isHeld = true, multiplier = 5))
        assertEquals(2, tracker.update(9040L, isHeld = true, multiplier = 5))
    }


    @Test
    fun generatedGuitarChartIncludesMediumLongAndVeryLongSustains() {
        val track = AudioAnalyzer.BUILT_IN_TRACKS[0]
        val chart = AudioAnalyzer.generateChart(track, Instrument.GUITAR, Difficulty.EXPERT)
        val beatMs = (60_000.0 / track.effectiveBpm).toLong()
        val sustainDurations = chart.map { it.durationMs }.filter { it > 50L }

        assertTrue("Chart must include medium sustains", sustainDurations.any { it in (beatMs..(beatMs * 2L)) })
        assertTrue("Chart must include long sustains", sustainDurations.any { it in (beatMs * 2L..(beatMs * 4L)) })
        assertTrue("Chart must include very long sustains", sustainDurations.any { it >= beatMs * 5L })
    }

}
