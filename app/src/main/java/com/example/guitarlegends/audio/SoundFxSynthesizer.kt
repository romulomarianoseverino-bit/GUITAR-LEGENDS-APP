package com.example.guitarlegends.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.guitarlegends.model.AudioSettings
import com.example.guitarlegends.model.Instrument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/**
 * Sintetizador de efeitos sonoros em tempo real via AudioTrack PCM.
 * Suporta controle independente de volume para Gameplay SFX, Interface (UI) SFX e Ambiente de Palco.
 */
class SoundFxSynthesizer {

    private val sampleRate = 22050
    private val scope = CoroutineScope(Dispatchers.Default)

    @Volatile
    var currentSettings: AudioSettings = AudioSettings()
        private set

    fun applyAudioSettings(settings: AudioSettings) {
        currentSettings = settings
    }

    // Pre-rendered PCM buffers for instant playback
    private val greenChordBuffer = generatePowerChord(82.4f, 0.22f)
    private val redChordBuffer = generatePowerChord(98.0f, 0.22f)
    private val yellowChordBuffer = generatePowerChord(110.0f, 0.22f)
    private val blueChordBuffer = generatePowerChord(123.5f, 0.22f)
    private val orangeChordBuffer = generatePowerChord(146.8f, 0.25f)
    private val acousticStringBuffers = listOf(164.8f, 196.0f, 220.0f, 246.9f, 293.7f).map {
        generatePluckedTone(it, 0.19f, 0.42f)
    }
    private val bassNoteBuffers = listOf(55.0f, 65.4f, 73.4f, 82.4f, 98.0f).map {
        generatePluckedTone(it, 0.20f, 0.22f)
    }
    private val vocalNoteBuffers = listOf(220.0f, 261.6f, 293.7f, 329.6f, 392.0f).map {
        generateVocalTone(it, 0.16f)
    }
    private val drumKickBuffer = generateKick(0.18f)
    private val drumSnareBuffer = generateSnare(0.16f)
    private val missScreechBuffer = generateMissBuzz(0.14f)
    private val starPowerWhooshBuffer = generateStarPowerWhoosh(0.35f)
    private val uiClickBuffer = generateUiTone(640f, 920f, 0.055f)
    private val uiSuccessBuffer = generateUiTone(520f, 1040f, 0.14f)
    private val stageCheerBuffer = generateCrowdCheer(0.45f)

    private fun generatePowerChord(rootFreq: Float, durationSec: Float): ShortArray {
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        val fifthFreq = rootFreq * 1.4983f
        val octaveFreq = rootFreq * 2.0f

        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val decay = (1.0f - (i.toFloat() / numSamples)).coerceIn(0f, 1f)
            val envelope = decay * decay

            val raw = sin(2.0 * PI * rootFreq * t).toFloat() +
                0.8f * sin(2.0 * PI * fifthFreq * t).toFloat() +
                0.6f * sin(2.0 * PI * octaveFreq * t).toFloat() +
                0.3f * sin(2.0 * PI * rootFreq * 3f * t).toFloat()

            val distorted = tanh(raw * 2.4f) * envelope * 0.8f
            buffer[i] = (distorted * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun generatePluckedTone(frequency: Float, durationSec: Float, body: Float): ShortArray {
        val count = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toFloat() / sampleRate
            val progress = i.toFloat() / count
            val attack = (i.toFloat() / (sampleRate * 0.006f)).coerceIn(0f, 1f)
            val decay = kotlin.math.exp(-progress * 5.5f)
            val fundamental = sin(2.0 * PI * frequency * t).toFloat()
            val second = sin(2.0 * PI * frequency * 2.01f * t).toFloat() * body
            val third = sin(2.0 * PI * frequency * 3.98f * t).toFloat() * body * 0.28f
            val sample = (fundamental + second + third) * attack * decay * 0.42f
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun generateVocalTone(frequency: Float, durationSec: Float): ShortArray {
        val count = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toFloat() / sampleRate
            val progress = i.toFloat() / count
            val envelope = kotlin.math.exp(-progress * 3.8f) * (0.35f + 0.65f * (i.toFloat() / (sampleRate * 0.012f)).coerceIn(0f, 1f))
            val fundamental = sin(2.0 * PI * frequency * t).toFloat()
            val formantA = sin(2.0 * PI * frequency * 2.7f * t).toFloat() * 0.34f
            val formantB = sin(2.0 * PI * frequency * 4.2f * t).toFloat() * 0.16f
            buffer[i] = ((fundamental + formantA + formantB) * envelope * Short.MAX_VALUE * 0.30f).toInt().toShort()
        }
        return buffer
    }

    /** Som complementar, curto e discreto, disparado apenas quando uma nota é acertada. */
    fun playInstrumentHit(instrument: Instrument, lane: Int) {
        val gain = currentSettings.effectiveGameplaySfx() * 0.24f
        if (gain <= 0.01f) return
        val index = lane.coerceIn(0, 4)
        val buffer = when (instrument) {
            Instrument.GUITAR -> when (index) {
                0 -> greenChordBuffer
                1 -> redChordBuffer
                2 -> yellowChordBuffer
                3 -> blueChordBuffer
                else -> orangeChordBuffer
            }
            Instrument.ACOUSTIC_GUITAR -> acousticStringBuffers[index]
            Instrument.BASS -> bassNoteBuffers[index]
            Instrument.DRUMS -> if (index == 0) drumKickBuffer else drumSnareBuffer
            Instrument.VOCALS -> vocalNoteBuffers[index]
        }
        playBufferAsync(buffer, gain)
    }

    private fun generateKick(durationSec: Float): ShortArray {        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val frac = i.toFloat() / numSamples
            val pitch = 150f * (1.0f - frac * 0.8f)
            val t = i.toFloat() / sampleRate
            val envelope = (1.0f - frac) * (1.0f - frac)
            val sample = sin(2.0 * PI * pitch * t).toFloat() * envelope
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun generateSnare(durationSec: Float): ShortArray {
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val frac = i.toFloat() / numSamples
            val noise = (Random.nextFloat() * 2f - 1f) * 0.7f
            val tone = sin(2.0 * PI * 220f * (i.toFloat() / sampleRate)).toFloat() * 0.3f
            val envelope = (1.0f - frac) * (1.0f - frac)
            val sample = (noise + tone) * envelope
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun generateMissBuzz(durationSec: Float): ShortArray {
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val frac = i.toFloat() / numSamples
            val noise = (Random.nextFloat() * 2f - 1f) * 0.4f
            val dissonant = sin(2.0 * PI * 130f * (i.toFloat() / sampleRate)).toFloat() +
                sin(2.0 * PI * 138f * (i.toFloat() / sampleRate)).toFloat()
            val sample = (dissonant * 0.4f + noise) * (1.0f - frac)
            buffer[i] = (sample * Short.MAX_VALUE * 0.5f).toInt().toShort()
        }
        return buffer
    }

    private fun generateStarPowerWhoosh(durationSec: Float): ShortArray {
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val frac = i.toFloat() / numSamples
            val freq = 200f + frac * 800f
            val envelope = sin(PI * frac).toFloat()
            val sample = sin(2.0 * PI * freq * (i.toFloat() / sampleRate)).toFloat() * envelope
            buffer[i] = (sample * Short.MAX_VALUE * 0.75f).toInt().toShort()
        }
        return buffer
    }

    private fun generateUiTone(startFreq: Float, endFreq: Float, durationSec: Float): ShortArray {
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val frac = i.toFloat() / numSamples
            val freq = startFreq + (endFreq - startFreq) * frac
            val env = (1.0f - frac) * (1.0f - frac)
            val sample = sin(2.0 * PI * freq * (i.toFloat() / sampleRate)).toFloat() * env * 0.45f
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun generateCrowdCheer(durationSec: Float): ShortArray {
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val frac = i.toFloat() / numSamples
            val env = sin(PI * frac).toFloat() * 0.55f
            val roar = (Random.nextFloat() * 2f - 1f) * 0.65f +
                sin(2.0 * PI * 320f * (i.toFloat() / sampleRate)).toFloat() * 0.15f
            buffer[i] = (roar * env * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    fun playHitChord(lane: Int) {
        val gain = currentSettings.effectiveGameplaySfx()
        if (gain <= 0.01f) return
        val buffer = when (lane) {
            0 -> greenChordBuffer
            1 -> redChordBuffer
            2 -> yellowChordBuffer
            3 -> blueChordBuffer
            else -> orangeChordBuffer
        }
        playBufferAsync(buffer, gain)
    }

    fun playDrumHit(lane: Int) {
        val gain = currentSettings.effectiveGameplaySfx()
        if (gain <= 0.01f) return
        val buffer = if (lane == 0) drumKickBuffer else drumSnareBuffer
        playBufferAsync(buffer, gain)
    }

    fun playMiss() {
        val gain = currentSettings.effectiveGameplaySfx()
        if (gain <= 0.01f) return
        playBufferAsync(missScreechBuffer, gain)
    }

    fun playStarPower() {
        val sfxGain = currentSettings.effectiveGameplaySfx()
        if (sfxGain > 0.01f) {
            playBufferAsync(starPowerWhooshBuffer, sfxGain)
        }
        val stageGain = currentSettings.effectiveStageFx()
        if (stageGain > 0.01f) {
            playBufferAsync(stageCheerBuffer, stageGain)
        }
    }

    fun playStageCheer() {
        val stageGain = currentSettings.effectiveStageFx()
        if (stageGain > 0.01f) {
            playBufferAsync(stageCheerBuffer, stageGain)
        }
    }

    fun playUiClick(isConfirm: Boolean = false) {
        val uiGain = currentSettings.effectiveUiSfx()
        if (uiGain <= 0.01f) return
        playBufferAsync(if (isConfirm) uiSuccessBuffer else uiClickBuffer, uiGain)
    }

    fun playMixerPreview() {
        playUiClick(isConfirm = true)
        playHitChord(2)
        playStageCheer()
    }

    private fun playBufferAsync(source: ShortArray, gain: Float) {
        val clamped = gain.coerceIn(0f, 1f)
        if (clamped <= 0.01f) return
        scope.launch {
            try {
                val scaled = if (clamped >= 0.99f) source else ShortArray(source.size) { idx ->
                    (source[idx] * clamped).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(scaled.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(scaled, 0, scaled.size)
                track.play()
                kotlinx.coroutines.delay((scaled.size * 1000L / sampleRate) + 50L)
                track.release()
            } catch (_: Exception) {
            }
        }
    }
}
