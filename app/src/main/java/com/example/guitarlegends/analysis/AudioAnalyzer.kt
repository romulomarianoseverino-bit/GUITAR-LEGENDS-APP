package com.example.guitarlegends.analysis

import com.example.guitarlegends.model.Difficulty
import com.example.guitarlegends.model.Instrument
import com.example.guitarlegends.model.Note
import com.example.guitarlegends.model.Track
import kotlin.math.sin
import kotlin.random.Random

object AudioAnalyzer {

    // Built-in catalog of tracks available immediately without any external files
    val BUILT_IN_TRACKS = listOf(
        Track(
            id = "track_thunderstrike",
            title = "Thunderstrike Anthem",
            artist = "The Electric Titans",
            durationMs = 95_000L,
            bpm = 138.0,
            genre = "Hard Rock / Heavy Metal"
        ),
        Track(
            id = "track_neon_overdrive",
            title = "Neon Overdrive Shred",
            artist = "Cyber Punk 88",
            durationMs = 88_000L,
            bpm = 152.0,
            genre = "Speed Rock / Synth Shred"
        ),
        Track(
            id = "track_rebel_riot",
            title = "Rebel Riot City",
            artist = "The Distortion Skaters",
            durationMs = 76_000L,
            bpm = 168.0,
            genre = "Skate Punk / Grunge"
        ),
        Track(
            id = "track_midnight_highway",
            title = "Midnight Highway Solo",
            artist = "Axel & The Nightriders",
            durationMs = 110_000L,
            bpm = 116.0,
            genre = "Classic Arena Rock"
        )
    )

    /**
     * Generates a rhythm chart (notes sequence) for any given track, instrument, and difficulty.
     * Can generate for built-in rock tracks or dynamically analyze user local audio files.
     */
    fun generateChart(
        track: Track,
        instrument: Instrument,
        difficulty: Difficulty
    ): List<Note> {
        val notes = mutableListOf<Note>()
        var noteIdCounter = 1L

        val bpm = if (track.bpm > 0) track.bpm else 130.0
        val beatIntervalMs = (60_000.0 / bpm).toLong()
        val totalDurationMs = if (track.durationMs > 10_000L) track.durationMs else 90_000L

        // Difficulty parameters
        val lanesCount = difficulty.lanesUsed
        val stepMs = when (difficulty) {
            Difficulty.EASY -> beatIntervalMs // 1 beat
            Difficulty.MEDIUM -> beatIntervalMs / 2 // 1/2 beat
            Difficulty.HARD -> beatIntervalMs / 2 // 1/2 beat with 1/4 bursts
            Difficulty.EXPERT -> beatIntervalMs / 4 // 1/4 beat
        }

        // Deterministic seed based on track ID, instrument, and difficulty
        val seed = (track.id.hashCode() * 31 + instrument.ordinal * 17 + difficulty.ordinal * 7).toLong()
        val random = Random(seed)

        var currentTimeMs = 2500L // initial warmup pause before first note
        var currentSection = 0
        var measureBeat = 0

        val maxAllowedTime = totalDurationMs - 1500L

        while (currentTimeMs < maxAllowedTime) {
            measureBeat++
            if (measureBeat >= 16) {
                measureBeat = 0
                currentSection++
            }

            // Energy curve simulates verse, chorus, guitar solo, breakdown
            val progress = currentTimeMs.toDouble() / totalDurationMs.toDouble()
            val energy = (0.5 + 0.3 * sin(progress * Math.PI * 4.0) + (if (progress in 0.55..0.75) 0.35 else 0.0)).coerceIn(0.2, 1.0)

            val shouldSpawnNote: Boolean = when (difficulty) {
                Difficulty.EASY -> measureBeat % 2 == 0 && random.nextFloat() < (energy * 0.75f)
                Difficulty.MEDIUM -> (measureBeat % 2 == 0 || random.nextFloat() < 0.35f) && random.nextFloat() < (energy * 0.85f)
                Difficulty.HARD -> random.nextFloat() < (0.6f + energy * 0.35f)
                Difficulty.EXPERT -> random.nextFloat() < (0.75f + energy * 0.25f)
            }

            if (shouldSpawnNote) {
                // Determine lane based on instrument archetype
                val lane = when (instrument) {
                    Instrument.GUITAR -> {
                        // High melodic range, climbs up and down
                        if (random.nextFloat() < 0.4f && currentSection % 2 == 1) {
                            (measureBeat % lanesCount)
                        } else {
                            random.nextInt(lanesCount)
                        }
                    }
                    Instrument.BASS -> {
                        // Grounded on lower 3 lanes (0, 1, 2)
                        val maxLane = (lanesCount - 1).coerceAtMost(2)
                        if (measureBeat % 4 == 0) 0 else random.nextInt(maxLane + 1)
                    }
                    Instrument.DRUMS -> {
                        // 0 = Kick, 1 = Snare, 2 = Hi-hat, 3 = Cymbal, 4 = Tom fill
                        if (measureBeat % 4 == 0) 0 // Kick on downbeats
                        else if (measureBeat % 4 == 2) 1.coerceAtMost(lanesCount - 1) // Snare on backbeats
                        else random.nextInt(lanesCount)
                    }
                    Instrument.VOCALS -> {
                        // Vocal line phrases with occasional rests
                        val vocalLane = ((measureBeat / 2) % lanesCount)
                        vocalLane
                    }
                }

                // Check for sustain notes (held notes) on guitar / vocals
                val isSustain = (instrument == Instrument.GUITAR || instrument == Instrument.VOCALS) &&
                        (random.nextFloat() < 0.15f) &&
                        (currentTimeMs + beatIntervalMs < maxAllowedTime)

                val duration = if (isSustain) (beatIntervalMs * 1.5).toLong() else 0L

                // Star Power phrases (approx every 30-40 seconds)
                val isStarPower = (currentTimeMs % 28_000L) in 0L..4_000L

                notes.add(
                    Note(
                        id = noteIdCounter++,
                        lane = lane.coerceIn(0, lanesCount - 1),
                        timeMs = currentTimeMs,
                        durationMs = duration,
                        isStarPower = isStarPower
                    )
                )

                // Optional double chord on Hard/Expert for Guitar
                if ((difficulty == Difficulty.HARD || difficulty == Difficulty.EXPERT) &&
                    instrument == Instrument.GUITAR &&
                    random.nextFloat() < 0.18f &&
                    lanesCount >= 3
                ) {
                    val chordLane = ((lane + 2) % lanesCount).coerceIn(0, lanesCount - 1)
                    if (chordLane != lane) {
                        notes.add(
                            Note(
                                id = noteIdCounter++,
                                lane = chordLane,
                                timeMs = currentTimeMs,
                                durationMs = duration,
                                isStarPower = isStarPower
                            )
                        )
                    }
                }
            }

            currentTimeMs += stepMs
        }

        return notes
    }
}
