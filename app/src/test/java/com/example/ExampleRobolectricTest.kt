package com.example

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.guitarlegends.analysis.AdaptiveEngine
import com.example.guitarlegends.analysis.AudioAnalyzer
import com.example.guitarlegends.analysis.PcmAudioAnalyzer
import com.example.guitarlegends.audio.AudioPlayerEngine
import com.example.guitarlegends.data.MusicLibraryRepository
import com.example.guitarlegends.data.UpdateCheckStatus
import com.example.guitarlegends.data.UpdateManager
import com.example.guitarlegends.model.*
import com.example.guitarlegends.viewmodel.GameViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Guitar Legends", appName)
    }

    @Test
    fun `test GLAS operates correctly in Easy difficulty without breaking or invading medium bounds`() {
        val easyEngineDisabled = AdaptiveEngine(baseDifficulty = Difficulty.EASY, isEnabled = false)
        assertEquals(Difficulty.EASY.hitWindowMs, easyEngineDisabled.getEffectiveHitWindowMs())
        assertEquals(0.08f, easyEngineDisabled.getMissPenalty(), 0.001f)

        val easyEngineEnabled = AdaptiveEngine(baseDifficulty = Difficulty.EASY, isEnabled = true)
        val initialWindow = easyEngineEnabled.getEffectiveHitWindowMs()
        assertTrue("Easy window should be at least 165ms", initialWindow >= 165L)
        assertTrue("Easy window should not exceed 240ms", initialWindow <= 240L)
        assertEquals(0.04f, easyEngineEnabled.getMissPenalty(), 0.001f)

        for (i in 1..20) {
            easyEngineEnabled.onNoteResult(HitRating.PERFECT, 10L)
        }
        val tightWindow = easyEngineEnabled.getEffectiveHitWindowMs()
        assertTrue("Tightened window on Easy must stay >= 165ms (never invade Medium 140ms)", tightWindow >= 165L)
        assertTrue("Status text should indicate Easy", easyEngineEnabled.statusText.contains("Fácil"))

        for (i in 1..5) {
            easyEngineEnabled.onNoteResult(HitRating.MISS, 0L)
        }
        val assistedWindow = easyEngineEnabled.getEffectiveHitWindowMs()
        assertTrue("Assisted window must expand for beginners", assistedWindow >= 180L)
        assertTrue("Assisted window must not exceed max boundary", assistedWindow <= 240L)
    }

    @Test
    fun `test all four difficulties have distinct, strictly bounded GLAS ranges`() {
        for (diff in Difficulty.values()) {
            val engine = AdaptiveEngine(baseDifficulty = diff, isEnabled = true)
            val window = engine.getEffectiveHitWindowMs()
            assertTrue(window >= engine.minHitWindowMs)
            assertTrue(window <= engine.maxHitWindowMs)
        }
    }

    @Test
    fun `test note chart generation for all four instruments across difficulties`() {
        val track = AudioAnalyzer.BUILT_IN_TRACKS[0]

        for (inst in Instrument.values()) {
            for (diff in Difficulty.values()) {
                val chart = AudioAnalyzer.generateChart(track, inst, diff)
                assertTrue("Chart for $inst at $diff should not be empty", chart.isNotEmpty())
                val maxAllowedLane = diff.lanesUsed - 1
                for (note in chart) {
                    assertTrue("Note lane ${note.lane} must be in 0..$maxAllowedLane", note.lane in 0..maxAllowedLane)
                }
            }
        }
    }

    @Test
    fun `test character creator, styles, instrument gear and highway theme selection in ViewModel`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = GameViewModel(app)

        assertNotNull(vm.playerStats.value)
        assertEquals("theme_ps2_wood", vm.playerStats.value.selectedHighwayThemeId)

        vm.updateCustomRockerMethod(CharacterCreationMethod.PRESERVE_FACIAL_FEATURES)
        assertEquals(CharacterCreationMethod.PRESERVE_FACIAL_FEATURES, vm.playerStats.value.customRocker.creationMethod)

        vm.updateCustomRockerStyle(CharacterArtStyle.CARTOON)
        assertEquals(CharacterArtStyle.CARTOON, vm.playerStats.value.customRocker.artStyle)

        vm.updateCustomRockerAppearance(
            hair = "Metalhead Longo",
            top = "Colete Jeans c/ Patches",
            pants = "Calça Jeans Rasgada",
            shoes = "Coturnos Militares",
            accessories = "Munhequeiras c/ Espinhos",
            tattoos = "Caveiras nos Braços"
        )
        assertEquals("Metalhead Longo", vm.playerStats.value.customRocker.hairStyle)

        // Itens bloqueados não podem ser equipados antes da compra; o padrão já é desbloqueado.
        vm.selectGear(Instrument.GUITAR, "neon_thunder")
        assertEquals("flame_caster", vm.playerStats.value.selectedGuitarId)
        vm.selectGear(Instrument.GUITAR, "flame_caster")
        assertEquals("flame_caster", vm.playerStats.value.selectedGuitarId)

        vm.selectGear(Instrument.BASS, "skull_bass")
        assertEquals("skull_bass", vm.playerStats.value.selectedBassId)

        vm.selectGear(Instrument.DRUMS, "punk_garage_kit")
        assertEquals("punk_garage_kit", vm.playerStats.value.selectedDrumsId)

        vm.selectGear(Instrument.VOCALS, "golden_supreme_mic")
        assertEquals("golden_supreme_mic", vm.playerStats.value.selectedVocalMicId)

        vm.equipHighwayTheme("theme_neon_cyber")
        assertEquals("theme_neon_cyber", vm.playerStats.value.selectedHighwayThemeId)
    }

    private fun createValidMp3File(context: Context, fileName: String, sizeBytes: Int = 80_000): File {
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { out ->
            val buf = ByteArray(sizeBytes)
            buf[0] = 'I'.code.toByte()
            buf[1] = 'D'.code.toByte()
            buf[2] = '3'.code.toByte()
            buf[3] = 3
            buf[4] = 0
            buf[10] = 0xFF.toByte()
            buf[11] = 0xFB.toByte()
            out.write(buf)
        }
        return file
    }

    private fun createValidOggFile(context: Context, fileName: String, sizeBytes: Int = 90_000): File {
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { out ->
            val buf = ByteArray(sizeBytes)
            buf[0] = 'O'.code.toByte()
            buf[1] = 'g'.code.toByte()
            buf[2] = 'g'.code.toByte()
            buf[3] = 'S'.code.toByte()
            out.write(buf)
        }
        return file
    }

    private fun createSyntheticRiffWavFile(
        context: Context,
        fileName: String,
        durationMs: Long,
        bpm: Double
    ): File {
        val sampleRate = 22050
        val totalSamples = ((sampleRate.toLong() * durationMs) / 1000L).toInt().coerceAtLeast(1000)
        val dataSize = totalSamples * 2
        val beatPeriodSamples = ((60.0 / bpm) * sampleRate).roundToInt().coerceAtLeast(100)
        val file = File(context.cacheDir, fileName)

        FileOutputStream(file).use { out ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray(Charsets.US_ASCII))
                putInt(36 + dataSize)
                put("WAVE".toByteArray(Charsets.US_ASCII))
                put("fmt ".toByteArray(Charsets.US_ASCII))
                putInt(16) // PCM fmt chunk size
                putShort(1) // PCM format
                putShort(1) // mono
                putInt(sampleRate)
                putInt(sampleRate * 2) // byte rate
                putShort(2) // block align
                putShort(16) // bits per sample
                put("data".toByteArray(Charsets.US_ASCII))
                putInt(dataSize)
            }
            out.write(header.array())

            val pcm = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until totalSamples) {
                val posInBeat = i % beatPeriodSamples
                val env = when {
                    posInBeat < 700 -> 0.85 * (1.0 - posInBeat / 700.0) // strong transient burst
                    else -> 0.04 // low background sustain
                }
                val sampleVal = (sin(2.0 * PI * 220.0 * i / sampleRate) * env * 30000.0).toInt().toShort()
                pcm.putShort(sampleVal)
            }
            out.write(pcm.array())
        }
        return file
    }

    @Test
    fun `01 - test import MP3 file permanent private storage and manifest persistence`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = MusicLibraryRepository(context)
        val tempMp3 = createValidMp3File(context, "epic_riff.mp3", 80_000)

        val result = repo.importAudio(Uri.fromFile(tempMp3))
        assertTrue("Importing MP3 should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val track = result.getOrThrow()

        assertEquals("epic_riff", track.title)
        assertFalse(track.isBuiltIn)
        assertNotNull(track.uriString)
        val privateFile = File(track.uriString!!)
        assertTrue("Private file should exist in app storage", privateFile.exists())
        assertTrue("Private file should reside in filesDir/imported_audio", privateFile.absolutePath.contains("imported_audio"))

        repo.deleteTrack(track)
        Unit
    }

    @Test
    fun `02 - test import WAV file permanent storage and real PCM duration`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = MusicLibraryRepository(context)
        val tempWav = createSyntheticRiffWavFile(context, "thunder_bass.wav", durationMs = 16_000L, bpm = 120.0)

        val result = repo.importAudio(Uri.fromFile(tempWav))
        assertTrue("Importing WAV should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val track = result.getOrThrow()
        assertEquals("thunder_bass", track.title)
        assertTrue(track.genre.contains("WAV"))
        assertTrue("Duration should be ~16s", track.durationMs in 15_500L..16_500L)
        assertEquals(AudioClassification.MUSIC_FULL, track.audioClassification)
        assertFalse(track.isShortAudio)

        repo.deleteTrack(track)
        Unit
    }

    @Test
    fun `03 - test import OGG file permanent storage`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = MusicLibraryRepository(context)
        val tempOgg = createValidOggFile(context, "drum_assault.ogg", 90_000)

        val result = repo.importAudio(Uri.fromFile(tempOgg))
        assertTrue("Importing OGG should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val track = result.getOrThrow()
        assertEquals("drum_assault", track.title)
        assertTrue(track.genre.contains("OGG"))

        repo.deleteTrack(track)
        Unit
    }

    @Test
    fun `04 - test repeated file names do not collide or overwrite previous tracks`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = MusicLibraryRepository(context)
        val temp1 = createValidMp3File(context, "rock_anthem.mp3", 75_000)

        val track1 = repo.importAudio(Uri.fromFile(temp1)).getOrThrow()
        val track2 = repo.importAudio(Uri.fromFile(temp1)).getOrThrow()

        assertNotEquals("IDs must be distinct", track1.id, track2.id)
        assertNotEquals("Physical file paths must be distinct", track1.uriString, track2.uriString)
        assertTrue(File(track1.uriString!!).exists())
        assertTrue(File(track2.uriString!!).exists())

        repo.deleteTrack(track1)
        repo.deleteTrack(track2)
        Unit
    }

    @Test
    fun `05 - test persistence across simulated process restart and manual calibration`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repoInstance1 = MusicLibraryRepository(context)
        val temp = createValidMp3File(context, "immortal_solo.mp3", 85_000)

        val imported = repoInstance1.importAudio(Uri.fromFile(temp)).getOrThrow()
        repoInstance1.updateTrackCalibration(imported.id, manualBpm = 148.0, syncOffsetMs = -35L).getOrThrow()

        val repoInstance2 = MusicLibraryRepository(context)
        val tracksAfterRestart = repoInstance2.loadAllTracks()

        val found = tracksAfterRestart.find { it.id == imported.id }
        assertNotNull("Imported track must persist and reload across app restarts", found)
        assertEquals("immortal_solo", found?.title)
        assertEquals(148.0, found!!.effectiveBpm, 0.01)
        assertEquals(-35L, found.syncOffsetMs)
        assertEquals(BpmConfidence.MANUAL, found.bpmConfidence)
        assertTrue(File(found.uriString!!).exists())

        repoInstance2.deleteTrack(imported)
        Unit
    }

    @Test
    fun `06 - test remove imported file deletes only private copy and preserves source`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = MusicLibraryRepository(context)
        val originalFile = createValidMp3File(context, "temporary_jam.mp3", 70_000)

        val imported = repo.importAudio(Uri.fromFile(originalFile)).getOrThrow()
        val privateCopy = File(imported.uriString!!)
        assertTrue("Private copy must exist after import", privateCopy.exists())

        val deleteResult = repo.deleteTrack(imported)
        assertTrue("Deletion must succeed", deleteResult.isSuccess)
        assertFalse("Private copy must be deleted", privateCopy.exists())
        assertTrue("Original user file must be preserved untouched", originalFile.exists())

        val remainingTracks = repo.loadAllTracks()
        assertNull("Track must not be in loaded catalog", remainingTracks.find { it.id == imported.id })
    }

    @Test
    fun `07 - test attempt to delete built-in track is strictly rejected`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = MusicLibraryRepository(context)
        val builtIn = AudioAnalyzer.BUILT_IN_TRACKS[0]

        val result = repo.deleteTrack(builtIn)
        assertFalse("Built-in track deletion must fail", result.isSuccess)
        assertTrue("Built-in track must remain in catalog", repo.loadAllTracks().any { it.id == builtIn.id })
    }

    @Test
    fun `08 - test short legitimate audio clip under 15s is allowed and classified explicitly`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = MusicLibraryRepository(context)
        val shortWav = createSyntheticRiffWavFile(context, "short_solo_riff.wav", durationMs = 5_500L, bpm = 128.0)

        val result = repo.importAudio(Uri.fromFile(shortWav))
        assertTrue("Legitimate short musical clip (5.5s) must be accepted: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val track = result.getOrThrow()
        assertTrue("Short clip must be flagged as isShortAudio", track.isShortAudio)
        assertEquals(AudioClassification.SHORT_CLIP, track.audioClassification)

        val subSecondWav = createSyntheticRiffWavFile(context, "tiny_blip.wav", durationMs = 400L, bpm = 120.0)
        val subSecondRes = repo.importAudio(Uri.fromFile(subSecondWav))
        assertFalse("Sub-1s blip must be rejected", subSecondRes.isSuccess)

        repo.deleteTrack(track)
        Unit
    }

    @Test
    fun `09 - test rejection of non-musical audio like SFX, dialogue, invalid extensions and corrupted files`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = MusicLibraryRepository(context)

        val sfxFile = createSyntheticRiffWavFile(context, "explosion_sfx.wav", durationMs = 4_000L, bpm = 120.0)
        val sfxResult = repo.importAudio(Uri.fromFile(sfxFile))
        assertFalse("SFX file must be rejected", sfxResult.isSuccess)
        assertTrue(sfxResult.exceptionOrNull()?.message?.contains("efeito sonoro") == true)

        val dialogueFile = createValidMp3File(context, "character_dialogue.mp3", 60_000)
        val dialResult = repo.importAudio(Uri.fromFile(dialogueFile))
        assertFalse("Dialogue file must be rejected", dialResult.isSuccess)

        val txtFile = File(context.cacheDir, "notes.txt").apply { writeText("not audio data") }
        val txtResult = repo.importAudio(Uri.fromFile(txtFile))
        assertFalse("Unsupported extension must be rejected", txtResult.isSuccess)

        val fakeMp3 = File(context.cacheDir, "corrupted_fake.mp3").apply {
            writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()) + ByteArray(512) { 0x11 })
        }
        val fakeResult = repo.importAudio(Uri.fromFile(fakeMp3))
        assertFalse("Corrupted non-audio file disguised as .mp3 must be rejected", fakeResult.isSuccess)
    }

    @Test
    fun `10 - test match execution and gameplay mechanics with imported track`() {
        val customTrack = Track(
            id = "test_custom_match",
            title = "Custom Rock Anthem",
            artist = "Local Artist",
            durationMs = 120_000L,
            bpm = 140.0,
            genre = "Rock",
            isBuiltIn = false
        )

        val chart = AudioAnalyzer.generateChart(customTrack, Instrument.GUITAR, Difficulty.HARD)
        assertTrue("Generated chart must not be empty", chart.isNotEmpty())

        var score = 0
        var combo = 0
        var multiplier = 1

        for (note in chart.take(15)) {
            score += 50 * multiplier
            combo++
            multiplier = when {
                combo >= 30 -> 4
                combo >= 20 -> 3
                combo >= 10 -> 2
                else -> 1
            }
        }

        assertTrue("Score must increase", score > 0)
        assertEquals("Combo must match number of hit notes", 15, combo)
        assertEquals(2, multiplier)
    }

    @Test
    fun `11 - test preservation of built-in tracks across all states`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = MusicLibraryRepository(context)

        assertEquals(4, repo.loadAllTracks().count { it.isBuiltIn })
        val temp = createValidMp3File(context, "speed_metal.mp3", 75_000)
        val imported = repo.importAudio(Uri.fromFile(temp)).getOrThrow()

        assertEquals(4, repo.loadAllTracks().count { it.isBuiltIn })
        repo.deleteTrack(imported)
        assertEquals(4, repo.loadAllTracks().count { it.isBuiltIn })
    }

    @Test
    fun `12 - test library loading with missing physical files cleans up manifest integrity`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = MusicLibraryRepository(context)

        val temp = createValidMp3File(context, "ghost_track.mp3", 75_000)
        val imported = repo.importAudio(Uri.fromFile(temp)).getOrThrow()
        File(imported.uriString!!).delete()

        val reloaded = repo.loadAllTracks()
        assertNull("Missing physical file must be automatically removed from manifest", reloaded.find { it.id == imported.id })
    }

    @Test
    fun `13 - test chart creation after import across all instruments and difficulties`() {
        val importedTrack = Track(
            id = "imported_heavy",
            title = "Heavy Groove",
            artist = "Studio Band",
            durationMs = 90_000L,
            bpm = 132.0,
            genre = "Metal",
            isBuiltIn = false,
            onsetTimesMs = listOf(1500L, 2100L, 2700L, 3300L, 3900L, 4500L, 5100L, 5700L)
        )

        for (inst in Instrument.values()) {
            for (diff in Difficulty.values()) {
                val chart = AudioAnalyzer.generateChart(importedTrack, inst, diff)
                assertTrue("Chart must be generated for $inst at $diff", chart.isNotEmpty())
                val maxLane = diff.lanesUsed - 1
                for (note in chart) {
                    assertTrue("Note lane ${note.lane} must be in 0..$maxLane", note.lane in 0..maxLane)
                }
            }
        }
    }

    @Test
    fun `14 - test GLAS real sequence adaptation and static mode when disabled`() {
        val sampleNotes = (0 until 30).map { i ->
            Note(id = i.toLong() + 1L, lane = i % 4, timeMs = 3_000L + i * 240L)
        }

        // Desativado: nenhuma alteração na sequência nem na janela
        val disabled = AdaptiveEngine(baseDifficulty = Difficulty.MEDIUM, isEnabled = false)
        val staticRes = disabled.adaptUpcomingNotes(sampleNotes, currentTimeMs = 0L, instrument = Instrument.GUITAR)
        assertEquals(sampleNotes.size, staticRes.size)

        // Ativado em modo Assistência: suaviza notas futuras muito próximas (> 2200ms à frente)
        val assisted = AdaptiveEngine(baseDifficulty = Difficulty.MEDIUM, isEnabled = true)
        for (i in 1..6) assisted.onNoteResult(HitRating.MISS, 0L)
        val adaptedAssist = assisted.adaptUpcomingNotes(sampleNotes, currentTimeMs = 0L, instrument = Instrument.GUITAR)
        assertTrue("Assistance mode should simplify dense future notes", adaptedAssist.size < sampleNotes.size)
        assertTrue(assisted.sequenceAdjustmentsApplied > 0)

        // Ativado em modo Virtuoso no Difícil: adiciona acentos harmônicos no futuro respeitando pistas
        val virtuoso = AdaptiveEngine(baseDifficulty = Difficulty.HARD, isEnabled = true)
        for (i in 1..22) virtuoso.onNoteResult(HitRating.PERFECT, 12L)
        val adaptedVirtuoso = virtuoso.adaptUpcomingNotes(sampleNotes, currentTimeMs = 0L, instrument = Instrument.GUITAR)
        assertTrue("Virtuoso mode should enrich upcoming sequence", adaptedVirtuoso.size > sampleNotes.size)
        assertTrue(adaptedVirtuoso.all { it.lane in 0 until Difficulty.HARD.lanesUsed })
    }

    @Test
    fun `15 - test library loading response time is under 500ms`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = MusicLibraryRepository(context)

        val startTime = System.currentTimeMillis()
        val tracks = repo.loadAllTracks()
        val duration = System.currentTimeMillis() - startTime

        assertTrue("Tracks must not be empty", tracks.isNotEmpty())
        assertTrue("Library load time must be < 500ms (was ${duration}ms)", duration < 500L)
    }

    @Test
    fun `16 - test real PCM WAV analysis extracts distinct tempos and real transients`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val slowWav = createSyntheticRiffWavFile(context, "slow_groove.wav", durationMs = 12_000L, bpm = 100.0)
        val fastWav = createSyntheticRiffWavFile(context, "fast_shred.wav", durationMs = 12_000L, bpm = 150.0)

        val slowAnalysis = PcmAudioAnalyzer.analyzeFile(slowWav, "wav")
        val fastAnalysis = PcmAudioAnalyzer.analyzeFile(fastWav, "wav")

        assertTrue(slowAnalysis.isValidAudio && slowAnalysis.isDecodable)
        assertTrue(fastAnalysis.isValidAudio && fastAnalysis.isDecodable)
        assertTrue("Slow WAV should detect real transients", slowAnalysis.onsetTimesMs.size >= 8)
        assertTrue("Fast WAV should detect more transients than slow WAV", fastAnalysis.onsetTimesMs.size > slowAnalysis.onsetTimesMs.size)
        assertTrue("Detected slow BPM (${slowAnalysis.bpm}) must be close to 100 BPM", slowAnalysis.bpm in 90.0..110.0)
        assertTrue("Detected fast BPM (${fastAnalysis.bpm}) must be close to 150 BPM", fastAnalysis.bpm in 140.0..160.0)
    }

    @Test
    fun `17 - test AudioPlayerEngine rejects missing or corrupted imported files without silent synth fallback`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val player = AudioPlayerEngine(context)

        val missingTrack = Track(
            id = "broken_imported",
            title = "Corrompida",
            artist = "Desconhecido",
            durationMs = 30_000L,
            bpm = 120.0,
            genre = "MP3",
            isBuiltIn = false,
            uriString = File(context.filesDir, "does_not_exist_ever.mp3").absolutePath
        )

        var errorReceived: String? = null
        var readyCalled = false
        player.loadTrack(
            track = missingTrack,
            onReady = { readyCalled = true },
            onError = { errorReceived = it }
        )

        assertFalse("onReady must NOT be called for broken imported track", readyCalled)
        assertNotNull("Explicit playback error must be reported", errorReceived)
        assertNotNull(player.lastErrorMessage)
        assertFalse("Engine must not be playing", player.isPlaying)
        player.release()
    }

    @Test
    fun `18 - test AudioPlayerEngine pause preserves position and stop resets state`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val player = AudioPlayerEngine(context)
        val builtIn = AudioAnalyzer.BUILT_IN_TRACKS[0]

        var ready = false
        player.loadTrack(builtIn, onReady = { ready = true })
        assertTrue(ready)

        player.play()
        assertTrue(player.isPlaying)
        player.seekTo(12_340L)
        player.pause()

        assertFalse(player.isPlaying)
        assertTrue(player.isPaused)
        assertTrue("Paused position must preserve seeked timestamp", player.getCurrentPositionMs() >= 12_340L)

        player.resume()
        assertTrue(player.isPlaying)
        assertFalse(player.isPaused)

        player.stop()
        assertFalse(player.isPlaying)
        assertFalse(player.isPaused)
        assertEquals(0L, player.getCurrentPositionMs())
        player.release()
    }

    @Test
    fun `19 - test DailyRewardCatalog, UpdateManager version evaluation and schema migration`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = UpdateManager(context)

        val migResult = manager.runDataMigrationIfNeeded()
        assertTrue(migResult.isSuccess)
        assertEquals(UpdateManager.CURRENT_SCHEMA_VERSION, manager.getLastSchemaVersion())

        val newerRelease = AppReleaseInfo(
            id = "v2_0",
            versionCode = manager.installedVersionCode + 5,
            versionName = "2.0.0",
            releaseTitle = "Turnê Mundial",
            isMandatory = true,
            downloadUrl = "https://github.com/romulomarianoseverino-bit/GUITAR-LEGENDS-APP/releases"
        )
        val statusAvailable = manager.evaluateUpdateStatus(listOf(newerRelease))
        assertTrue(statusAvailable is UpdateCheckStatus.UpdateAvailable)
        assertTrue((statusAvailable as UpdateCheckStatus.UpdateAvailable).isMandatory)

        val currentRelease = newerRelease.copy(versionCode = manager.installedVersionCode, isMandatory = false)
        val statusUpToDate = manager.evaluateUpdateStatus(listOf(currentRelease))
        assertTrue(statusUpToDate is UpdateCheckStatus.UpToDate)

        // Verifica ciclo completo de recompensas diárias e consistência PT-BR
        val day7 = DailyRewardCatalog.rewardForDay(7)
        assertEquals(100, day7.coinsGranted)
        assertEquals("kai_cyber", day7.cosmeticId)
        assertEquals("Especialista", Difficulty.EXPERT.displayName)
    }
}
