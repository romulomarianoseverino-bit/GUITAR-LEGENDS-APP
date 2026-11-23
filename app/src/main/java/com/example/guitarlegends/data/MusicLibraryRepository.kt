package com.example.guitarlegends.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.guitarlegends.analysis.AudioAnalyzer
import com.example.guitarlegends.analysis.PcmAudioAnalyzer
import com.example.guitarlegends.model.AudioClassification
import com.example.guitarlegends.model.BpmConfidence
import com.example.guitarlegends.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

/**
 * Repositório central de gerenciamento e persistência da Biblioteca Musical.
 * - Valida extensão permitida, MIME type e decodificabilidade real antes de aceitar a música.
 * - Permite clipes curtos legítimos (< 15s) com classificação explícita em vez de rejeição automática.
 * - Analisa áudio real via PcmAudioAnalyzer (RMS + transientes + BPM).
 * - Grava o manifesto com substituição atômica por arquivo temporário e propagação real de falhas.
 */
class MusicLibraryRepository(private val context: Context) {

    private val storageDir: File by lazy {
        File(context.filesDir, "imported_audio").apply {
            if (!exists()) mkdirs()
        }
    }

    private val manifestFile: File by lazy {
        File(context.filesDir, "library_manifest.json")
    }

    private val allowedExtensions = setOf("mp3", "wav", "ogg", "m4a", "aac", "flac")

    suspend fun loadAllTracks(): List<Track> = withContext(Dispatchers.IO) {
        val importedTracks = loadImportedTracksFromManifest()
        val verifiedImported = mutableListOf<Track>()

        for (track in importedTracks) {
            val uriStr = track.uriString
            if (!uriStr.isNullOrBlank()) {
                val file = File(uriStr)
                if (file.exists() && file.length() > 0L) {
                    verifiedImported.add(track)
                }
            }
        }

        if (verifiedImported.size != importedTracks.size) {
            saveManifest(verifiedImported)
        }

        AudioAnalyzer.BUILT_IN_TRACKS + verifiedImported
    }

    suspend fun importAudio(uri: Uri): Result<Track> = withContext(Dispatchers.IO) {
        var copiedFile: File? = null
        try {
            val originalName = getDisplayName(uri) ?: return@withContext Result.failure(
                IllegalArgumentException("Não foi possível identificar o nome do arquivo selecionado.")
            )
            val extension = getExtensionFromName(originalName).lowercase()
            if (extension !in allowedExtensions) {
                return@withContext Result.failure(
                    IllegalArgumentException(
                        "Formato '.$extension' não suportado. Utilize arquivos MP3, WAV, OGG, M4A, AAC ou FLAC."
                    )
                )
            }

            val mimeType = runCatching { context.contentResolver.getType(uri) }.getOrNull().orEmpty()
            if (mimeType.isNotBlank() && !isAllowedMimeType(mimeType)) {
                return@withContext Result.failure(
                    IllegalArgumentException("Tipo MIME '$mimeType' incompatível com arquivo de áudio musical.")
                )
            }

            val uniqueId = "track_user_${UUID.randomUUID().toString().take(8)}"
            val sanitizedName = originalName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val targetFile = File(storageDir, "${uniqueId}_$sanitizedName")
            copiedFile = targetFile

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(
                    Exception("Não foi possível abrir o arquivo selecionado no aparelho.")
                )

            inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (!targetFile.exists() || targetFile.length() <= 0L) {
                targetFile.delete()
                return@withContext Result.failure(Exception("O arquivo importado está vazio ou corrompido."))
            }

            val rawBaseTitle = originalName.substringBeforeLast(".").ifBlank { originalName }
            val nonMusicTerms = listOf(
                "sfx", "sound_effect", "efeito_sonoro", "ringtone",
                "dialogo", "dialogue", "notification", "vinheta"
            )
            if (nonMusicTerms.any { rawBaseTitle.lowercase().contains(it) }) {
                targetFile.delete()
                return@withContext Result.failure(
                    IllegalArgumentException(
                        "O arquivo '$rawBaseTitle' parece ser um efeito sonoro ou diálogo e não uma música."
                    )
                )
            }

            // Análise PCM e verificação real de decodificabilidade
            val analysis = PcmAudioAnalyzer.analyzeFile(targetFile, extension)
            if (!analysis.isValidAudio || !analysis.isDecodable) {
                targetFile.delete()
                return@withContext Result.failure(
                    IllegalArgumentException(
                        analysis.errorMessage ?: "Arquivo de áudio corrompido ou ilegível."
                    )
                )
            }

            if (analysis.durationMs in 1L..999L) {
                targetFile.delete()
                return@withContext Result.failure(
                    IllegalArgumentException("O áudio tem menos de 1 segundo e não pode gerar uma partida jogável.")
                )
            }

            // Bloqueia somente uma cópia binariamente idêntica de áudio já importado.
            // Títulos iguais não são suficientes: versões original/fã, ao vivo ou acústicas
            // continuam permitidas quando o conteúdo de áudio é diferente.
            val importedTracks = loadImportedTracksFromManifest()
            val incomingFingerprint = sha256(targetFile)
            val duplicate = importedTracks.firstOrNull { existing ->
                val existingPath = existing.uriString
                if (existingPath.isNullOrBlank()) {
                    false
                } else {
                    val existingFile = File(existingPath)
                    existingFile.exists() && existingFile.isFile && sha256(existingFile) == incomingFingerprint
                }
            }
            if (duplicate != null) {
                targetFile.delete()
                return@withContext Result.failure(
                    IllegalArgumentException(
                        "Essa mesma gravação já está na sua biblioteca: ${duplicate.title} — ${duplicate.artist}."
                    )
                )
            }

            val meta = extractTags(targetFile)
            val finalTitle = meta.first?.takeIf { it.isNotBlank() }?.trim() ?: rawBaseTitle
            val finalArtist = meta.second?.takeIf { it.isNotBlank() }?.trim() ?: "Artista Local"

            val isShort = analysis.durationMs in 1_000L..14_999L
            val classification = when {
                isShort -> AudioClassification.SHORT_CLIP
                analysis.bpmConfidence == BpmConfidence.LOW_UNRELIABLE -> AudioClassification.UNVERIFIED_STREAM
                else -> AudioClassification.MUSIC_FULL
            }

            val track = Track(
                id = uniqueId,
                title = finalTitle,
                artist = finalArtist,
                durationMs = analysis.durationMs,
                bpm = analysis.bpm,
                genre = "Arquivo Pessoal (${extension.uppercase()})",
                isBuiltIn = false,
                uriString = targetFile.absolutePath,
                bpmConfidence = analysis.bpmConfidence,
                bpmIsEstimated = analysis.bpmIsEstimated,
                onsetTimesMs = analysis.onsetTimesMs,
                isShortAudio = isShort,
                audioClassification = classification,
                mimeType = mimeType
            )

            val currentTracks = loadImportedTracksFromManifest().toMutableList()
            currentTracks.add(0, track)
            val saveResult = saveManifest(currentTracks)
            if (saveResult.isFailure) {
                targetFile.delete()
                return@withContext Result.failure(
                    saveResult.exceptionOrNull() ?: Exception("Erro ao gravar manifesto da biblioteca.")
                )
            }

            Result.success(track)
        } catch (e: Exception) {
            copiedFile?.delete()
            Result.failure(Exception("Falha ao importar áudio: ${e.message}"))
        }
    }

    suspend fun updateTrackCalibration(
        trackId: String,
        manualBpm: Double?,
        syncOffsetMs: Long
    ): Result<Track> = withContext(Dispatchers.IO) {
        val currentTracks = loadImportedTracksFromManifest().toMutableList()
        val index = currentTracks.indexOfFirst { it.id == trackId }
        if (index < 0) {
            return@withContext Result.failure(IllegalArgumentException("Faixa importada não encontrada para calibração."))
        }
        val old = currentTracks[index]
        val updated = old.copy(
            manualBpmOverride = manualBpm?.coerceIn(40.0, 260.0),
            bpmConfidence = if (manualBpm != null) BpmConfidence.MANUAL else old.bpmConfidence,
            syncOffsetMs = syncOffsetMs.coerceIn(-1000L, 1000L)
        )
        currentTracks[index] = updated
        val saveRes = saveManifest(currentTracks)
        if (saveRes.isFailure) {
            return@withContext Result.failure(saveRes.exceptionOrNull() ?: Exception("Falha ao salvar calibração."))
        }
        Result.success(updated)
    }

    suspend fun deleteTrack(track: Track): Result<Unit> = withContext(Dispatchers.IO) {
        if (track.isBuiltIn) {
            return@withContext Result.failure(IllegalStateException("Músicas originais do projeto não podem ser excluídas."))
        }

        return@withContext try {
            val currentTracks = loadImportedTracksFromManifest().filter { it.id != track.id }
            val saveResult = saveManifest(currentTracks)
            if (saveResult.isFailure) {
                return@withContext Result.failure(
                    saveResult.exceptionOrNull() ?: Exception("Falha ao atualizar o índice da biblioteca.")
                )
            }

            track.uriString?.let { path ->
                val file = File(path)
                if (file.exists() && file.parentFile == storageDir) {
                    file.delete()
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Erro ao remover música da biblioteca: ${e.message}"))
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun isAllowedMimeType(mime: String): Boolean {
        val lower = mime.lowercase()
        return lower.startsWith("audio/") ||
            lower == "application/ogg" ||
            lower == "application/octet-stream"
    }

    private fun extractTags(file: File): Pair<String?, String?> {
        val retriever = android.media.MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val title = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)
            title to artist
        } catch (_: Throwable) {
            null to null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun loadImportedTracksFromManifest(): List<Track> {
        if (!manifestFile.exists()) return emptyList()
        return try {
            val jsonText = manifestFile.readText()
            val array = JSONArray(jsonText)
            val list = mutableListOf<Track>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val onsetsArray = obj.optJSONArray("onsetTimesMs")
                val onsets = buildList {
                    if (onsetsArray != null) {
                        for (j in 0 until onsetsArray.length()) {
                            add(onsetsArray.getLong(j))
                        }
                    }
                }
                val manualBpm = if (obj.has("manualBpmOverride") && !obj.isNull("manualBpmOverride")) {
                    obj.getDouble("manualBpmOverride")
                } else null

                val track = Track(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    artist = obj.getString("artist"),
                    durationMs = obj.getLong("durationMs"),
                    bpm = obj.getDouble("bpm"),
                    genre = obj.optString("genre", "Arquivo Pessoal"),
                    isBuiltIn = false,
                    uriString = obj.getString("uriString"),
                    bpmConfidence = runCatching {
                        BpmConfidence.valueOf(obj.optString("bpmConfidence", BpmConfidence.MEDIUM.name))
                    }.getOrDefault(BpmConfidence.MEDIUM),
                    bpmIsEstimated = obj.optBoolean("bpmIsEstimated", false),
                    manualBpmOverride = manualBpm,
                    onsetTimesMs = onsets,
                    isShortAudio = obj.optBoolean("isShortAudio", false),
                    audioClassification = runCatching {
                        AudioClassification.valueOf(obj.optString("audioClassification", AudioClassification.MUSIC_FULL.name))
                    }.getOrDefault(AudioClassification.MUSIC_FULL),
                    mimeType = obj.optString("mimeType", ""),
                    syncOffsetMs = obj.optLong("syncOffsetMs", 0L)
                )
                list.add(track)
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveManifest(tracks: List<Track>): Result<Unit> {
        val tempFile = File(context.filesDir, "library_manifest.json.tmp")
        return try {
            val array = JSONArray()
            for (t in tracks) {
                if (t.isBuiltIn) continue
                val obj = JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("artist", t.artist)
                    put("durationMs", t.durationMs)
                    put("bpm", t.bpm)
                    put("genre", t.genre)
                    put("uriString", t.uriString ?: "")
                    put("bpmConfidence", t.bpmConfidence.name)
                    put("bpmIsEstimated", t.bpmIsEstimated)
                    if (t.manualBpmOverride != null) {
                        put("manualBpmOverride", t.manualBpmOverride)
                    }
                    put("onsetTimesMs", JSONArray(t.onsetTimesMs))
                    put("isShortAudio", t.isShortAudio)
                    put("audioClassification", t.audioClassification.name)
                    put("mimeType", t.mimeType)
                    put("syncOffsetMs", t.syncOffsetMs)
                }
                array.put(obj)
            }

            tempFile.writeText(array.toString())
            if (manifestFile.exists() && !manifestFile.delete()) {
                // Sobrescreve diretamente se delete falhar em algum sistema de arquivos
                manifestFile.writeText(array.toString())
                tempFile.delete()
            } else if (!tempFile.renameTo(manifestFile)) {
                manifestFile.writeText(array.toString())
                tempFile.delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            runCatching { tempFile.delete() }
            Result.failure(Exception("Falha ao persistir biblioteca musical: ${e.message}"))
        }
    }

    private fun getDisplayName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex)
                    }
                }
            }
        }
        if (name.isNullOrBlank()) {
            name = uri.lastPathSegment
        }
        return name
    }

    private fun getExtensionFromName(name: String): String {
        return if (name.contains(".")) name.substringAfterLast(".") else ""
    }
}
