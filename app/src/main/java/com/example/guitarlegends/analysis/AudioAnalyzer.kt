package com.example.guitarlegends.analysis

import com.example.guitarlegends.model.AudioClassification
import com.example.guitarlegends.model.BpmConfidence
import com.example.guitarlegends.model.Difficulty
import com.example.guitarlegends.model.Instrument
import com.example.guitarlegends.model.Note
import com.example.guitarlegends.model.Track
import kotlin.math.sin

object AudioAnalyzer {

    // Catálogo original embutido com batidas e durações reais do sintetizador interno
    val BUILT_IN_TRACKS = listOf(
        Track(
            id = "track_thunderstrike",
            title = "Thunderstrike Anthem",
            artist = "The Electric Titans",
            durationMs = 95_000L,
            bpm = 138.0,
            genre = "Hard Rock / Heavy Metal",
            isBuiltIn = true,
            bpmConfidence = BpmConfidence.HIGH,
            audioClassification = AudioClassification.BUILT_IN_SYNTH
        ),
        Track(
            id = "track_neon_overdrive",
            title = "Neon Overdrive Shred",
            artist = "Cyber Punk 88",
            durationMs = 88_000L,
            bpm = 152.0,
            genre = "Speed Rock / Synth Shred",
            isBuiltIn = true,
            bpmConfidence = BpmConfidence.HIGH,
            audioClassification = AudioClassification.BUILT_IN_SYNTH
        ),
        Track(
            id = "track_rebel_riot",
            title = "Rebel Riot City",
            artist = "The Distortion Skaters",
            durationMs = 76_000L,
            bpm = 168.0,
            genre = "Skate Punk / Grunge",
            isBuiltIn = true,
            bpmConfidence = BpmConfidence.HIGH,
            audioClassification = AudioClassification.BUILT_IN_SYNTH
        ),
        Track(
            id = "track_midnight_highway",
            title = "Midnight Highway Solo",
            artist = "Axel & The Nightriders",
            durationMs = 110_000L,
            bpm = 116.0,
            genre = "Classic Arena Rock",
            isBuiltIn = true,
            bpmConfidence = BpmConfidence.HIGH,
            audioClassification = AudioClassification.BUILT_IN_SYNTH
        )
    )

    /**
     * Gera sequência rítmica alinhada:
     * 1) Para faixas importadas com picos transientes reais (`onsetTimesMs`), posiciona notas nos eventos musicais detectados.
     * 2) Para faixas originais ou com ajuste manual de BPM, usa grade rítmica musical estruturada em torno do BPM efetivo.
     */
    fun generateChart(
        track: Track,
        instrument: Instrument,
        difficulty: Difficulty
    ): List<Note> {
        val bpm = track.effectiveBpm
        val beatIntervalMs = (60_000.0 / bpm).toLong().coerceIn(220L, 1500L)
        val totalDurationMs = track.durationMs.coerceAtLeast(3_000L)
        val warmupMs = if (totalDurationMs < 15_000L) 900L else 2_000L
        val maxAllowedTime = (totalDurationMs - 600L).coerceAtLeast(warmupMs + 500L)

        if (track.onsetTimesMs.isNotEmpty() && track.manualBpmOverride == null) {
            return generateFromDetectedOnsets(
                track = track,
                instrument = instrument,
                difficulty = difficulty,
                beatIntervalMs = beatIntervalMs,
                warmupMs = warmupMs,
                maxAllowedTime = maxAllowedTime
            )
        }

        return generateStructuredBeatChart(
            track = track,
            instrument = instrument,
            difficulty = difficulty,
            beatIntervalMs = beatIntervalMs,
            warmupMs = warmupMs,
            maxAllowedTime = maxAllowedTime,
            totalDurationMs = totalDurationMs
        )
    }

    private fun generateFromDetectedOnsets(
        track: Track,
        instrument: Instrument,
        difficulty: Difficulty,
        beatIntervalMs: Long,
        warmupMs: Long,
        maxAllowedTime: Long
    ): List<Note> {
        val notes = mutableListOf<Note>()
        val lanesCount = difficulty.lanesUsed
        val minSpacingMs = when (difficulty) {
            Difficulty.EASY -> (beatIntervalMs * 0.90).toLong().coerceAtLeast(320L)
            Difficulty.MEDIUM -> (beatIntervalMs * 0.48).toLong().coerceAtLeast(210L)
            Difficulty.HARD -> (beatIntervalMs * 0.30).toLong().coerceAtLeast(150L)
            Difficulty.EXPERT -> (beatIntervalMs * 0.22).toLong().coerceAtLeast(110L)
        }

        var noteIdCounter = 1L
        var lastAcceptedTime = -minSpacingMs
        var phraseStep = 0

        for (rawOnset in track.onsetTimesMs) {
            val alignedTime = rawOnset + track.syncOffsetMs
            if (alignedTime < warmupMs || alignedTime > maxAllowedTime) continue
            if (alignedTime - lastAcceptedTime < minSpacingMs) continue

            val lane = selectMusicalLane(
                instrument = instrument,
                lanesCount = lanesCount,
                phraseStep = phraseStep,
                energy = 0.75
            )
            val isStarPower = (alignedTime % 28_000L) in 0L..4_000L
            val nextGap = minSpacingMs * 2
            val isSustain = (instrument == Instrument.VOCALS || instrument == Instrument.GUITAR || instrument == Instrument.ACOUSTIC_GUITAR) &&
                phraseStep % 7 == 0 && alignedTime + nextGap < maxAllowedTime
            val durationMs = if (isSustain) (beatIntervalMs * 1.1).toLong() else 0L

            notes.add(
                Note(
                    id = noteIdCounter++,
                    lane = lane,
                    timeMs = alignedTime,
                    durationMs = durationMs,
                    isStarPower = isStarPower
                )
            )

            // Acordes musicais ocasionais no Difícil/Especialista em cabeças de compasso
            if ((difficulty == Difficulty.HARD || difficulty == Difficulty.EXPERT) &&
                instrument == Instrument.GUITAR &&
                phraseStep % 8 == 0 &&
                lanesCount >= 3
            ) {
                val harmonyLane = (lane + 2) % lanesCount
                if (harmonyLane != lane) {
                    notes.add(
                        Note(
                            id = noteIdCounter++,
                            lane = harmonyLane,
                            timeMs = alignedTime,
                            durationMs = durationMs,
                            isStarPower = isStarPower
                        )
                    )
                }
            }

            lastAcceptedTime = alignedTime
            phraseStep++
        }

        // Caso os picos filtrados fiquem muito esparsos em clipe curto, complementa na grade do BPM
        if (notes.size < 4) {
            return generateStructuredBeatChart(
                track = track,
                instrument = instrument,
                difficulty = difficulty,
                beatIntervalMs = beatIntervalMs,
                warmupMs = warmupMs,
                maxAllowedTime = maxAllowedTime,
                totalDurationMs = track.durationMs.coerceAtLeast(3_000L)
            )
        }

        return notes
    }

    private fun generateStructuredBeatChart(
        track: Track,
        instrument: Instrument,
        difficulty: Difficulty,
        beatIntervalMs: Long,
        warmupMs: Long,
        maxAllowedTime: Long,
        totalDurationMs: Long
    ): List<Note> {
        val notes = mutableListOf<Note>()
        var noteIdCounter = 1L
        val lanesCount = difficulty.lanesUsed
        val stepMs = when (difficulty) {
            Difficulty.EASY -> beatIntervalMs
            Difficulty.MEDIUM -> (beatIntervalMs / 2L).coerceAtLeast(160L)
            Difficulty.HARD -> (beatIntervalMs / 2L).coerceAtLeast(140L)
            Difficulty.EXPERT -> (beatIntervalMs / 4L).coerceAtLeast(105L)
        }

        var currentTimeMs = warmupMs + track.syncOffsetMs
        if (currentTimeMs < 400L) currentTimeMs = 400L
        var stepIndex = 0

        while (currentTimeMs < maxAllowedTime) {
            val barStep = stepIndex % 16
            val section = (stepIndex / 16) % 4
            val progress = currentTimeMs.toDouble() / totalDurationMs.toDouble()
            val energy = (0.55 + 0.30 * sin(progress * Math.PI * 4.0) + if (section == 2) 0.25 else 0.0)
                .coerceIn(0.25, 1.0)

            val shouldSpawn = when (difficulty) {
                Difficulty.EASY -> barStep % 2 == 0
                Difficulty.MEDIUM -> barStep % 2 == 0 || (energy > 0.65 && barStep % 4 == 3)
                Difficulty.HARD -> barStep % 2 == 0 || energy > 0.55
                Difficulty.EXPERT -> barStep % 4 != 3 || energy > 0.60
            }

            if (shouldSpawn) {
                val lane = selectMusicalLane(instrument, lanesCount, stepIndex + section, energy)
                val isSustain = (instrument == Instrument.GUITAR || instrument == Instrument.ACOUSTIC_GUITAR || instrument == Instrument.VOCALS) &&
                    barStep == 0 && (currentTimeMs + beatIntervalMs < maxAllowedTime)
                val duration = if (isSustain) (beatIntervalMs * 1.25).toLong() else 0L
                val isStarPower = (currentTimeMs % 28_000L) in 0L..4_000L

                notes.add(
                    Note(
                        id = noteIdCounter++,
                        lane = lane,
                        timeMs = currentTimeMs,
                        durationMs = duration,
                        isStarPower = isStarPower
                    )
                )

                if ((difficulty == Difficulty.HARD || difficulty == Difficulty.EXPERT) &&
                    instrument == Instrument.GUITAR &&
                    barStep % 8 == 0 &&
                    lanesCount >= 3
                ) {
                    val chordLane = (lane + 2) % lanesCount
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
            stepIndex++
        }

        return notes
    }

    private fun selectMusicalLane(
        instrument: Instrument,
        lanesCount: Int,
        phraseStep: Int,
        energy: Double
    ): Int {
        val safeLanes = lanesCount.coerceIn(1, 5)
        return when (instrument) {
            Instrument.GUITAR, Instrument.ACOUSTIC_GUITAR -> {
                val pattern = intArrayOf(0, 1, 2, 1, 3, 2, 4, 2)
                val raw = pattern[phraseStep % pattern.size]
                if (energy > 0.8 && raw + 1 < safeLanes && phraseStep % 3 == 0) raw + 1 else raw % safeLanes
            }
            Instrument.BASS -> {
                val bassMax = minOf(safeLanes, 3)
                if (phraseStep % 4 == 0) 0 else (phraseStep % bassMax)
            }
            Instrument.DRUMS -> {
                when (phraseStep % 4) {
                    0 -> 0 // Bumbo
                    2 -> minOf(1, safeLanes - 1) // Caixa
                    3 -> minOf(safeLanes - 1, 3) // Prato / Tom
                    else -> minOf(2, safeLanes - 1) // Chimbal
                }
            }
            Instrument.VOCALS -> {
                val contour = intArrayOf(1, 2, 2, 3, 2, 1, 0, 1)
                contour[phraseStep % contour.size] % safeLanes
            }
        }
    }
}
