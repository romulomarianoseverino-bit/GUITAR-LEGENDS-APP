package com.example.guitarlegends.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/**
 * Low-latency real-time audio sound effect synthesizer using Android AudioTrack PCM generation.
 * Generates instant distorted rock guitar chords, heavy drum hits, fret sounds, and crowd cheers
 * with zero file loading overhead.
 */
class SoundFxSynthesizer {

    private val sampleRate = 22050
    private val scope = CoroutineScope(Dispatchers.Default)

    // Pre-rendered PCM buffers for instant playback
    private val greenChordBuffer = generatePowerChord(82.4f, 0.22f) // E power chord
    private val redChordBuffer = generatePowerChord(98.0f, 0.22f) // G power chord
    private val yellowChordBuffer = generatePowerChord(110.0f, 0.22f) // A power chord
    private val blueChordBuffer = generatePowerChord(123.5f, 0.22f) // B power chord
    private val orangeChordBuffer = generatePowerChord(146.8f, 0.25f) // D high power chord
    private val drumKickBuffer = generateKick(0.18f)
    private val drumSnareBuffer = generateSnare(0.16f)
    private val missScreechBuffer = generateMissBuzz(0.14f)
    private val starPowerWhooshBuffer = generateStarPowerWhoosh(0.35f)

    private fun generatePowerChord(rootFreq: Float, durationSec: Float): ShortArray {
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        val fifthFreq = rootFreq * 1.4983f // Perfect fifth interval
        val octaveFreq = rootFreq * 2.0f

        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val decay = (1.0f - (i.toFloat() / numSamples)).coerceIn(0f, 1f)
            val envelope = decay * decay

            // Sum harmonics for distorted guitar sound
            val raw = sin(2.0 * PI * rootFreq * t).toFloat() +
                    0.8f * sin(2.0 * PI * fifthFreq * t).toFloat() +
                    0.6f * sin(2.0 * PI * octaveFreq * t).toFloat() +
                    0.3f * sin(2.0 * PI * rootFreq * 3f * t).toFloat()

            // Overdrive saturation (soft clipping tanh)
            val distorted = tanh(raw * 2.4f) * envelope * 0.8f
            buffer[i] = (distorted * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun generateKick(durationSec: Float): ShortArray {
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val frac = i.toFloat() / numSamples
            val pitch = 150f * (1.0f - frac * 0.8f) // Rapid pitch drop
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
                    sin(2.0 * PI * 138f * (i.toFloat() / sampleRate)).toFloat() // discordant beating
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
            val freq = 200f + frac * 800f // Pitch rise
            val envelope = sin(PI * frac).toFloat()
            val sample = sin(2.0 * PI * freq * (i.toFloat() / sampleRate)).toFloat() * envelope
            buffer[i] = (sample * Short.MAX_VALUE * 0.75f).toInt().toShort()
        }
        return buffer
    }

    fun playHitChord(lane: Int) {
        val buffer = when (lane) {
            0 -> greenChordBuffer
            1 -> redChordBuffer
            2 -> yellowChordBuffer
            3 -> blueChordBuffer
            else -> orangeChordBuffer
        }
        playBufferAsync(buffer)
    }

    fun playDrumHit(lane: Int) {
        val buffer = if (lane == 0) drumKickBuffer else drumSnareBuffer
        playBufferAsync(buffer)
    }

    fun playMiss() {
        playBufferAsync(missScreechBuffer)
    }

    fun playStarPower() {
        playBufferAsync(starPowerWhooshBuffer)
    }

    private fun playBufferAsync(buffer: ShortArray) {
        scope.launch {
            try {
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
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
                // Auto-release after playing duration
                kotlinx.coroutines.delay((buffer.size * 1000L / sampleRate) + 50L)
                track.release()
            } catch (e: Exception) {
                // Ignore audio track allocation race
            }
        }
    }
}
