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
import java.io.File
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Motor de reprodução de áudio de alta precisão.
 * - Faixas importadas do usuário utilizam exclusivamente MediaPlayer (nunca há fallback silencioso para synth em caso de erro).
 * - Faixas originais embutidas utilizam o sintetizador procedural sincronizado em tempo real.
 * - Suporta reprodução, pausa com preservação exata de posição, retomada contínua e parada limpa.
 */
class AudioPlayerEngine(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var isUsingMediaPlayer = false

    // Motor de síntese exclusivo para faixas embutidas originais
    private var synthJob: Job? = null
    private var synthTrack: AudioTrack? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    // Controle preciso de tempo e estado
    private var startTimeNano: Long = 0L
    private var pausedPositionMs: Long = 0L

    var isPlaying: Boolean = false
        private set

    var isPaused: Boolean = false
        private set

    var lastErrorMessage: String? = null
        private set

    var currentTrack: Track? = null
        private set

    private var activeCompletionListener: (() -> Unit)? = null
    private var activeErrorListener: ((String) -> Unit)? = null

    fun loadTrack(
        track: Track,
        onReady: () -> Unit = {},
        onError: (String) -> Unit = {},
        onCompleted: () -> Unit = {}
    ) {
        stop()
        currentTrack = track
        pausedPositionMs = 0L
        lastErrorMessage = null
        activeCompletionListener = onCompleted
        activeErrorListener = onError

        if (!track.isBuiltIn || track.uriString != null) {
            val uriStr = track.uriString
            if (uriStr.isNullOrBlank()) {
                val msg = "Caminho do arquivo de áudio ausente ou inválido para '${track.title}'."
                lastErrorMessage = msg
                onError(msg)
                return
            }

            try {
                if (uriStr.startsWith("/") && !File(uriStr).exists()) {
                    val msg = "O arquivo de áudio '${track.title}' não foi encontrado no armazenamento interno."
                    lastErrorMessage = msg
                    onError(msg)
                    return
                }

                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .build()
                    )
                    if (uriStr.startsWith("/")) {
                        setDataSource(uriStr)
                    } else {
                        setDataSource(context, Uri.parse(uriStr))
                    }
                    setOnErrorListener { _, what, extra ->
                        val msg = "Erro ao decodificar/reproduzir '${track.title}' (código $what/$extra). Escolha outro arquivo ou tente novamente."
                        this@AudioPlayerEngine.stop()
                        this@AudioPlayerEngine.lastErrorMessage = msg
                        this@AudioPlayerEngine.activeErrorListener?.invoke(msg)
                        true
                    }
                    setOnCompletionListener {
                        this@AudioPlayerEngine.isPlaying = false
                        this@AudioPlayerEngine.isPaused = false
                        this@AudioPlayerEngine.pausedPositionMs = 0L
                        this@AudioPlayerEngine.activeCompletionListener?.invoke()
                    }
                    prepare()
                }
                mediaPlayer = player
                isUsingMediaPlayer = true
                onReady()
            } catch (e: Exception) {
                stop()
                val msg = "Não foi possível abrir o arquivo '${track.title}': formato corrompido ou codec não suportado."
                lastErrorMessage = msg
                onError(msg)
            }
            return
        }

        // Apenas faixas embutidas oficiais usam o sintetizador procedural
        isUsingMediaPlayer = false
        onReady()
    }

    fun play() {
        if (isPlaying) return
        val track = currentTrack ?: return

        if (!track.isBuiltIn && mediaPlayer == null) {
            val msg = "Reprodução bloqueada: o áudio de '${track.title}' não foi carregado."
            lastErrorMessage = msg
            activeErrorListener?.invoke(msg)
            return
        }

        isPlaying = true
        isPaused = false

        if (isUsingMediaPlayer && mediaPlayer != null) {
            try {
                mediaPlayer?.seekTo(pausedPositionMs.toInt())
                mediaPlayer?.start()
                startTimeNano = System.nanoTime() - (pausedPositionMs * 1_000_000L)
            } catch (e: Exception) {
                stop()
                val msg = "Falha ao iniciar reprodução do arquivo: ${e.message}"
                lastErrorMessage = msg
                activeErrorListener?.invoke(msg)
            }
        } else {
            startTimeNano = System.nanoTime() - (pausedPositionMs * 1_000_000L)
            startSynthPlayback(track.effectiveBpm, pausedPositionMs)
        }
    }

    fun pause() {
        if (!isPlaying) return
        pausedPositionMs = getCurrentPositionMs()
        isPlaying = false
        isPaused = true

        if (isUsingMediaPlayer) {
            runCatching { mediaPlayer?.pause() }
        } else {
            stopSynthPlayback()
        }
    }

    fun resume() {
        if (!isPlaying && isPaused) {
            play()
        }
    }

    fun stop() {
        isPlaying = false
        isPaused = false
        pausedPositionMs = 0L
        if (mediaPlayer != null) {
            runCatching { mediaPlayer?.stop() }
            runCatching { mediaPlayer?.release() }
            mediaPlayer = null
        }
        isUsingMediaPlayer = false
        stopSynthPlayback()
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, getDurationMs())
        pausedPositionMs = clamped
        if (isPlaying) {
            if (isUsingMediaPlayer) {
                runCatching { mediaPlayer?.seekTo(clamped.toInt()) }
            } else {
                startSynthPlayback(currentTrack?.effectiveBpm ?: 135.0, clamped)
            }
            startTimeNano = System.nanoTime() - (clamped * 1_000_000L)
        }
    }

    fun getCurrentPositionMs(): Long {
        if (!isPlaying) return pausedPositionMs

        if (isUsingMediaPlayer && mediaPlayer != null) {
            return try {
                val mpPos = mediaPlayer?.currentPosition?.toLong() ?: 0L
                val nanoPos = (System.nanoTime() - startTimeNano) / 1_000_000L
                if (kotlin.math.abs(mpPos - nanoPos) > 150L) mpPos else nanoPos
            } catch (_: Exception) {
                (System.nanoTime() - startTimeNano) / 1_000_000L
            }
        }

        return (System.nanoTime() - startTimeNano) / 1_000_000L
    }

    fun getDurationMs(): Long {
        if (isUsingMediaPlayer && mediaPlayer != null) {
            val dur = runCatching { mediaPlayer?.duration?.toLong() }.getOrNull()
            if (dur != null && dur > 0L) return dur
        }
        return currentTrack?.durationMs ?: 90_000L
    }

    private fun startSynthPlayback(bpm: Double, startOffsetMs: Long = 0L) {
        stopSynthPlayback()
        val sampleRate = 22050
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(2048)

        synthTrack = runCatching {
            AudioTrack.Builder()
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
                .setBufferSizeInBytes(minBuf * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
                .also { it.play() }
        }.getOrNull()

        val beatPeriodSec = (60.0 / bpm).toFloat()

        synthJob = scope.launch {
            val chunkSamples = 1024
            val buffer = ShortArray(chunkSamples)
            var sampleIndex = (startOffsetMs * sampleRate) / 1000L
            val bassRiffs = floatArrayOf(82.4f, 82.4f, 98.0f, 110.0f, 123.5f, 110.0f, 98.0f, 82.4f)

            while (isActive && isPlaying) {
                for (i in 0 until chunkSamples) {
                    val t = (sampleIndex + i).toFloat() / sampleRate
                    val currentBeatFloat = t / beatPeriodSec
                    val beatFraction = currentBeatFloat - currentBeatFloat.toInt()

                    val isKick = (currentBeatFloat.toInt() % 2 == 0) && beatFraction < 0.2f
                    val kickSound = if (isKick) {
                        val kf = (1.0f - beatFraction / 0.2f)
                        sin(2.0 * PI * (60.0 + 80.0 * kf) * t).toFloat() * kf * 0.6f
                    } else 0f

                    val isSnare = (currentBeatFloat.toInt() % 2 == 1) && beatFraction < 0.22f
                    val snareSound = if (isSnare) {
                        val sf = (1.0f - beatFraction / 0.22f)
                        (kotlin.random.Random.nextFloat() * 2f - 1f) * sf * 0.45f
                    } else 0f

                    val riffStep = (currentBeatFloat * 2).toInt() % bassRiffs.size
                    val noteFreq = bassRiffs[riffStep]
                    val rawGuitar = sin(2.0 * PI * noteFreq * t).toFloat() +
                        0.7f * sin(2.0 * PI * (noteFreq * 1.5) * t).toFloat()
                    val guitarDist = tanh(rawGuitar * 2.2f) * 0.35f

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
        runCatching { synthTrack?.stop() }
        runCatching { synthTrack?.release() }
        synthTrack = null
    }

    fun release() {
        stop()
    }
}
