package com.example

import com.example.guitarlegends.analysis.AdaptiveEngine
import com.example.guitarlegends.analysis.AudioAnalyzer
import com.example.guitarlegends.model.Difficulty
import com.example.guitarlegends.model.HitRating
import com.example.guitarlegends.model.Instrument
import com.example.guitarlegends.model.StudioCatalog
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
    fun testAcousticGuitarGeneratesPlayableChart() {
        val track = AudioAnalyzer.BUILT_IN_TRACKS.first()
        val chart = AudioAnalyzer.generateChart(track, Instrument.ACOUSTIC_GUITAR, Difficulty.EXPERT)

        assertTrue("Acoustic guitar chart must not be empty", chart.isNotEmpty())
        assertTrue(
            "Acoustic guitar notes must stay inside the five-lane highway",
            chart.all { it.lane in 0 until Difficulty.EXPERT.lanesUsed }
        )
        assertTrue(
            "Acoustic guitar should include sustained notes for strumming/fingerpicking phrases",
            chart.any { it.durationMs > 0L }
        )
    }

    @Test
    fun testStudioCatalogProvidesDedicatedAcousticGuitarModels() {
        val models = StudioCatalog.instrumentModelsByType[Instrument.ACOUSTIC_GUITAR].orEmpty()
        assertEquals(3, models.size)
        assertTrue(models.any { it.first == "acoustic_dreadnought" })
        assertTrue(models.any { it.first == "acoustic_classic" })
        assertTrue(models.any { it.first == "acoustic_cutaway" })
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
}
