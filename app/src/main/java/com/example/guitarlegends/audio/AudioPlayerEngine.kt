package com.example.guitarlegends.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import com.example.guitarlegends.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.tanh

/**
 * High-precision audio player engine.
 * Supports:
 * 1. Native device audio playback (MP3, WAV, OGG, M4A) via MediaPlayer.
 * 2. Real-time procedural rock backing music generator for built-in tracks.
 * 3. High-precision synchronized millisecond time tracking for zero-latency gameplay.
 */
class AudioPlayerEngine(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var isUsingMediaPlayer = false

    // Synthesis engine for built-in tracks
    private var synthJob: Job? = null
    private var synthTrack: AudioTrack? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    // Timestamp synchronization
    private var startTimeNano: Long = 0L
    private var pausedPositionMs: Long = 0L
    var isPlaying: Boolean = false
        private set

    var currentTrack: Track? = null
        private set

    fun loadTrack(track: Track, onReady: () -> Unit = {}) {
        stop()
        currentTrack = track
        pausedPositionMs = 0L

        if (track.uriString != null) {
            try {
                val uri = Uri.parse(track.uriString)
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .build()
                    )
                    setDataSource(context, uri)
                    prepare()
                }
                isUsingMediaPlayer = true
                onReady()
                return
            } catch (e: Exception) {
                // Fallback to internal synth if user file fails to decode
                isUsingMediaPlayer = false
            }
        }

        // Built-in synthesized track
        isUsingMediaPlayer = false
        onReady()
    }

    fun play() {
        if (isPlaying) return
        isPlaying = true

        if (isUsingMediaPlayer && mediaPlayer != null) {
            mediaPlayer?.seekTo(pausedPositionMs.toInt())
            mediaPlayer?.start()
            startTimeNano = System.nanoTime() - (pausedPositionMs * 1_000_000L)
        } else {
            // Start procedural rock synth
            startTimeNano = System.nanoTime() - (pausedPositionMs * 1_000_000L)
            startSynthPlayback(currentTrack?.bpm ?: 135.0)
        }
    }

    fun pause() {
        if (!isPlaying) return
        pausedPositionMs = getCurrentPositionMs()
        isPlaying = false

        if (isUsingMediaPlayer) {
            mediaPlayer?.pause()
        } else {
            stopSynthPlayback()
        }
    }

    fun stop() {
        isPlaying = false
        pausedPositionMs = 0L
        if (isUsingMediaPlayer) {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (e: Exception) {}
            mediaPlayer = null
            isUsingMediaPlayer = false
        }
        stopSynthPlayback()
    }

    fun seekTo(positionMs: Long) {
        pausedPositionMs = positionMs
        if (isPlaying) {
            if (isUsingMediaPlayer) {
                mediaPlayer?.seekTo(positionMs.toInt())
            }
            startTimeNano = System.nanoTime() - (positionMs * 1_000_000L)
        }
    }

    /**
     * Precision time source interpolating between System nanoTime and MediaPlayer clock
     */
    fun getCurrentPositionMs(): Long {
        if (!isPlaying) return pausedPositionMs

        if (isUsingMediaPlayer && mediaPlayer != null) {
            try {
                val mpPos = mediaPlayer?.currentPosition?.toLong() ?: 0L
                val nanoPos = (System.nanoTime() - startTimeNano) / 1_000_000L
                // Avoid clock drift with a blended estimate
                return if (kotlin.math.abs(mpPos - nanoPos) > 150L) mpPos else nanoPos
            } catch (e: Exception) {
                return (System.nanoTime() - startTimeNano) / 1_000_000L
            }
        }

        return (System.nanoTime() - startTimeNano) / 1_000_000L
    }

    fun getDurationMs(): Long {
        if (isUsingMediaPlayer && mediaPlayer != null) {
            return mediaPlayer?.duration?.toLong() ?: (currentTrack?.durationMs ?: 90_000L)
        }
        return currentTrack?.durationMs ?: 90_000L
    }

    private fun startSynthPlayback(bpm: Double) {
        stopSynthPlayback()
        val sampleRate = 22050
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ) * 2

        synthTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        synthTrack?.play()

        val beatPeriodSec = (60.0 / bpm).toFloat()

        synthJob = scope.launch {
            val chunkSamples = 1024
            val buffer = ShortArray(chunkSamples)
            var sampleIndex = 0L

            val bassRiffs = floatArrayOf(82.4f, 82.4f, 98.0f, 110.0f, 123.5f, 110.0f, 98.0f, 82.4f)

            while (isActive && isPlaying) {
                for (i in 0 until chunkSamples) {
                    val t = (sampleIndex + i).toFloat() / sampleRate
                    val currentBeatFloat = t / beatPeriodSec
                    val beatFraction = currentBeatFloat - currentBeatFloat.toInt()
                    val bar = (currentBeatFloat / 4).toInt()

                    // Kick on downbeat 0 and 2
                    val isKick = (currentBeatFloat.toInt() % 2 == 0) && beatFraction < 0.2f
                    val kickSound = if (isKick) {
                        val kf = (1.0f - beatFraction / 0.2f)
                        sin(2.0 * PI * (60.0 + 80.0 * kf) * t).toFloat() * kf * 0.6f
                    } else 0f

                    // Snare on backbeat 1 and 3
                    val isSnare = (currentBeatFloat.toInt() % 2 == 1) && beatFraction < 0.22f
                    val snareSound = if (isSnare) {
                        val sf = (1.0f - beatFraction / 0.22f)
                        (kotlin.random.Random.nextFloat() * 2f - 1f) * sf * 0.45f
                    } else 0f

                    // Rock Riff
                    val riffStep = (currentBeatFloat * 2).toInt() % bassRiffs.size
                    val noteFreq = bassRiffs[riffStep]
                    val rawGuitar = sin(2.0 * PI * noteFreq * t).toFloat() +
                            0.7f * sin(2.0 * PI * (noteFreq * 1.5) * t).toFloat()
                    val guitarDist = tanh(rawGuitar * 2.2f) * 0.35f

                    // High rhythm cymbal tick
                    val isHat = (beatFraction < 0.08f)
                    val hatSound = if (isHat) (kotlin.random.Random.nextFloat() * 2f - 1f) * 0.15f else 0f

                    val mix = (kickSound + snareSound + guitarDist + hatSound).coerceIn(-1f, 1f)
                    buffer[i] = (mix * Short.MAX_VALUE * 0.7f).toInt().toShort()
                }

                synthTrack?.write(buffer, 0, chunkSamples)
                sampleIndex += chunkSamples
            }
        }
    }

    private fun stopSynthPlayback() {
        synthJob?.cancel()
        synthJob = null
        try {
            synthTrack?.stop()
            synthTrack?.release()
        } catch (e: Exception) {}
        synthTrack = null
    }

    fun release() {
        stop()
    }
}
