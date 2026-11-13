package com.example.guitarlegends.viewmodel

import android.app.Application
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.guitarlegends.analysis.AdaptiveEngine
import com.example.guitarlegends.analysis.AudioAnalyzer
import com.example.guitarlegends.audio.AudioPlayerEngine
import com.example.guitarlegends.audio.SoundFxSynthesizer
import com.example.guitarlegends.engine.ParticleSystem
import com.example.guitarlegends.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

sealed class GameScreen {
    object Splash : GameScreen()
    object MainMenu : GameScreen()
    object Career : GameScreen()
    object CompetitiveMultiplayer : GameScreen()
    object CoopMultiplayer : GameScreen()
    object MusicLibrary : GameScreen()
    object SongDetail : GameScreen()
    object MusicPreview : GameScreen()
    object InstrumentSelect : GameScreen()
    object DifficultySelect : GameScreen()
    object Customization : GameScreen()
    object Gameplay : GameScreen()
    object Results : GameScreen()
    object Statistics : GameScreen()
    object Settings : GameScreen()
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext
    val audioPlayer = AudioPlayerEngine(context)
    val soundFx = SoundFxSynthesizer()
    val particleSystem = ParticleSystem()

    // Screen Navigation
    private val _currentScreen = MutableStateFlow<GameScreen>(GameScreen.Splash)
    val currentScreen: StateFlow<GameScreen> = _currentScreen.asStateFlow()
    private val screenStack = mutableListOf<GameScreen>()

    // Player Stats & Progression
    private val statsPreferences = context.getSharedPreferences("guitar_legends_player_stats", Context.MODE_PRIVATE)
    private val _playerStats = MutableStateFlow(loadPlayerStats())
    val playerStats: StateFlow<PlayerStats> = _playerStats.asStateFlow()

    // Music Library
    private val _tracks = MutableStateFlow<List<Track>>(AudioAnalyzer.BUILT_IN_TRACKS)
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    // Current Selection
    var selectedTrack: Track = AudioAnalyzer.BUILT_IN_TRACKS[0]
        private set
    var selectedInstrument: Instrument = Instrument.GUITAR
        private set
    var selectedDifficulty: Difficulty = Difficulty.MEDIUM
        private set

    // Match Gameplay State
    private val _activeNotes = MutableStateFlow<List<Note>>(emptyList())
    val activeNotes: StateFlow<List<Note>> = _activeNotes.asStateFlow()

    private val _currentSongTimeMs = MutableStateFlow(0L)
    val currentSongTimeMs: StateFlow<Long> = _currentSongTimeMs.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _combo = MutableStateFlow(0)
    val combo: StateFlow<Int> = _combo.asStateFlow()

    private val _maxCombo = MutableStateFlow(0)
    val maxCombo: StateFlow<Int> = _maxCombo.asStateFlow()

    private val _multiplier = MutableStateFlow(1)
    val multiplier: StateFlow<Int> = _multiplier.asStateFlow()

    private val _rockMeter = MutableStateFlow(0.6f) // 0.0 (Fail) to 1.0 (Crowd frenzy)
    val rockMeter: StateFlow<Float> = _rockMeter.asStateFlow()

    private val _starPowerEnergy = MutableStateFlow(0.0f) // 0.0 to 1.0
    val starPowerEnergy: StateFlow<Float> = _starPowerEnergy.asStateFlow()

    private val _isStarPowerActive = MutableStateFlow(false)
    val isStarPowerActive: StateFlow<Boolean> = _isStarPowerActive.asStateFlow()

    private val _lastHitRating = MutableStateFlow<HitRating?>(null)
    val lastHitRating: StateFlow<HitRating?> = _lastHitRating.asStateFlow()

    private val _activeFretLanes = MutableStateFlow<Set<Int>>(emptySet())
    val activeFretLanes: StateFlow<Set<Int>> = _activeFretLanes.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    // GLAS Adaptive Engine
    var adaptiveEngine = AdaptiveEngine(selectedDifficulty, true)
        private set

    // Match Stats
    private var totalHitCount = 0
    private var totalMissCount = 0

    // Latest Result
    var lastMatchResult: MatchResult? = null
        private set

    // Venues for Career Mode
    val careerVenues = listOf(
        CareerVenue(
            id = "venue_garage",
            name = "Garagem Subterrânea",
            location = "São Paulo, SP",
            requiredStars = 0,
            rewardCoins = 150,
            description = "Onde tudo começa. Amplificadores velhos, vizinhos reclamando e puro rock de garagem.",
            trackIds = listOf("track_thunderstrike"),
            isUnlocked = true
        ),
        CareerVenue(
            id = "venue_biker_club",
            name = "Taverna dos Motoqueiros",
            location = "Curitiba, PR",
            requiredStars = 4,
            rewardCoins = 300,
            description = "Público exigente, cheiro de gasolina e jaquetas de couro. Toque alto ou seja vaiado!",
            trackIds = listOf("track_rebel_riot", "track_thunderstrike"),
            isUnlocked = true
        ),
        CareerVenue(
            id = "venue_arena_rock",
            name = "Arena Mega Rock Fest",
            location = "Rio de Janeiro, RJ",
            requiredStars = 10,
            rewardCoins = 600,
            description = "40 mil pessoas esperando pelo seu solo de guitarra. Luzes, fumaça e adrenalina máxima.",
            trackIds = listOf("track_neon_overdrive", "track_midnight_highway"),
            isUnlocked = false
        ),
        CareerVenue(
            id = "venue_world_stadium",
            name = "Estádio Lendário Mundial",
            location = "Wembley / Maracanã",
            requiredStars = 20,
            rewardCoins = 1500,
            description = "A consagração definitiva como Lenda da Guitarra no palco mais famoso do planeta.",
            trackIds = listOf("track_thunderstrike", "track_neon_overdrive", "track_midnight_highway"),
            isUnlocked = false
        )
    )

    // Character Catalog
    val characters = listOf(
        CharacterCustomization(
            id = "axel_rocker",
            name = "Axel 'Shredder'",
            style = "Heavy Metal Clássico",
            outfitName = "Jaqueta de Couro & Tachas",
            priceCoins = 0,
            isUnlocked = true,
            bio = "Nascido com uma palheta na mão e distorção na veia."
        ),
        CharacterCustomization(
            id = "roxie_riot",
            name = "Roxie Punk",
            style = "Skate Punk Riot",
            outfitName = "Coturno & Cabelo Neon",
            priceCoins = 350,
            isUnlocked = true,
            bio = "Velocidade, atitude e energia incontrolável nos palcos."
        ),
        CharacterCustomization(
            id = "kai_cyber",
            name = "Kai Neo-Anime",
            style = "Anime Cyberpunk",
            outfitName = "Colete Holográfico",
            priceCoins = 600,
            isUnlocked = false,
            bio = "Sintetizadores pulsantes e acordes espaciais do futuro."
        )
    )

    // Guitar Catalog
    val guitars = listOf(
        GuitarGear(
            id = "flame_caster",
            name = "Inferno Flame Caster",
            type = "Stratocaster Flamejante",
            colorHex = 0xFFFF3D00,
            priceCoins = 0,
            isUnlocked = true,
            description = "Pintura em chamas com captadores de alto ganho para solos rasgantes."
        ),
        GuitarGear(
            id = "void_v",
            name = "Midnight Flying V",
            type = "Heavy V Metal",
            colorHex = 0xFF7C4DFF,
            priceCoins = 250,
            isUnlocked = true,
            description = "Formato agressivo pontiagudo com sustain infinito para metalhead."
        ),
        GuitarGear(
            id = "neon_thunder",
            name = "Neon Cyber Strat",
            type = "Laser Custom",
            colorHex = 0xFF00E5FF,
            priceCoins = 500,
            isUnlocked = false,
            description = "Cordas de fibra ótica iluminadas com LEDs reativos ao ritmo da música."
        )
    )

    // Game Loop Job
    private var gameLoopJob: Job? = null

    init {
        // Automatically proceed from splash after brief rock intro
        viewModelScope.launch {
            delay(1600L)
            if (_currentScreen.value is GameScreen.Splash) {
                navigateTo(GameScreen.MainMenu)
            }
        }
    }

    fun navigateTo(screen: GameScreen) {
        screenStack.add(_currentScreen.value)
        _currentScreen.value = screen
    }

    fun navigateBack() {
        if (screenStack.isNotEmpty()) {
            val prev = screenStack.removeAt(screenStack.size - 1)
            _currentScreen.value = prev
        } else {
            _currentScreen.value = GameScreen.MainMenu
        }
    }

    fun selectTrack(track: Track) {
        selectedTrack = track
    }

    fun selectInstrument(instrument: Instrument) {
        selectedInstrument = instrument
    }

    fun selectDifficulty(difficulty: Difficulty) {
        selectedDifficulty = difficulty
    }

    private fun loadPlayerStats(): PlayerStats {
        val defaults = PlayerStats()
        return PlayerStats(
            coins = statsPreferences.getInt("coins", defaults.coins).coerceAtLeast(0),
            totalScore = statsPreferences.getLong("totalScore", defaults.totalScore).coerceAtLeast(0L),
            notesHitTotal = statsPreferences.getLong("notesHitTotal", defaults.notesHitTotal).coerceAtLeast(0L),
            maxStreakOverall = statsPreferences.getInt("maxStreakOverall", defaults.maxStreakOverall).coerceAtLeast(0),
            songsCompleted = statsPreferences.getInt("songsCompleted", defaults.songsCompleted).coerceAtLeast(0),
            unlockedStars = statsPreferences.getInt("unlockedStars", defaults.unlockedStars).coerceAtLeast(0),
            selectedCharacterId = statsPreferences.getString("selectedCharacterId", defaults.selectedCharacterId) ?: defaults.selectedCharacterId,
            selectedGuitarId = statsPreferences.getString("selectedGuitarId", defaults.selectedGuitarId) ?: defaults.selectedGuitarId,
            unlockedCharacterIds = statsPreferences.getStringSet("unlockedCharacterIds", defaults.unlockedCharacterIds)?.toSet() ?: defaults.unlockedCharacterIds,
            unlockedGuitarIds = statsPreferences.getStringSet("unlockedGuitarIds", defaults.unlockedGuitarIds)?.toSet() ?: defaults.unlockedGuitarIds,
            glassAdaptiveEnabled = statsPreferences.getBoolean("glassAdaptiveEnabled", defaults.glassAdaptiveEnabled),
            audioLatencyOffsetMs = statsPreferences.getLong("audioLatencyOffsetMs", defaults.audioLatencyOffsetMs),
            controlMode = runCatching {
                ControlMode.valueOf(statsPreferences.getString("controlMode", defaults.controlMode.name) ?: defaults.controlMode.name)
            }.getOrDefault(defaults.controlMode),
            graphicsQuality = runCatching {
                GraphicsQuality.valueOf(statsPreferences.getString("graphicsQuality", defaults.graphicsQuality.name) ?: defaults.graphicsQuality.name)
            }.getOrDefault(defaults.graphicsQuality)
        )
    }

    private fun savePlayerStats(stats: PlayerStats) {
        statsPreferences.edit()
            .putInt("coins", stats.coins)
            .putLong("totalScore", stats.totalScore)
            .putLong("notesHitTotal", stats.notesHitTotal)
            .putInt("maxStreakOverall", stats.maxStreakOverall)
            .putInt("songsCompleted", stats.songsCompleted)
            .putInt("unlockedStars", stats.unlockedStars)
            .putString("selectedCharacterId", stats.selectedCharacterId)
            .putString("selectedGuitarId", stats.selectedGuitarId)
            .putStringSet("unlockedCharacterIds", stats.unlockedCharacterIds)
            .putStringSet("unlockedGuitarIds", stats.unlockedGuitarIds)
            .putBoolean("glassAdaptiveEnabled", stats.glassAdaptiveEnabled)
            .putLong("audioLatencyOffsetMs", stats.audioLatencyOffsetMs)
            .putString("controlMode", stats.controlMode.name)
            .putString("graphicsQuality", stats.graphicsQuality.name)
            .apply()
    }

    private fun updatePlayerStats(transform: (PlayerStats) -> PlayerStats) {
        val updated = transform(_playerStats.value)
        _playerStats.value = updated
        savePlayerStats(updated)
    }

    fun toggleAdaptiveSystem(enabled: Boolean) {
        updatePlayerStats { it.copy(glassAdaptiveEnabled = enabled) }
    }

    fun setControlMode(mode: ControlMode) {
        updatePlayerStats { it.copy(controlMode = mode) }
    }

    fun setGraphicsQuality(quality: GraphicsQuality) {
        updatePlayerStats { it.copy(graphicsQuality = quality) }
    }

    fun setLatencyOffset(offsetMs: Long) {
        updatePlayerStats { it.copy(audioLatencyOffsetMs = offsetMs) }
    }

    /**
     * Imports a user-selected audio file from device storage via SAF URI
     */
    fun importAudioFile(uri: Uri) {
        viewModelScope.launch {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)

                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?: uri.lastPathSegment ?: "Música do Dispositivo"
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?: "Artista Local"
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val durationMs = durationStr?.toLongOrNull() ?: 120_000L

                retriever.release()

                // Generate estimated BPM based on track duration & title seed
                val bpm = (120 + (abs(title.hashCode()) % 40)).toDouble()

                val newTrack = Track(
                    id = "custom_${System.currentTimeMillis()}",
                    title = title,
                    artist = artist,
                    durationMs = durationMs,
                    bpm = bpm,
                    genre = "Arquivo Pessoal (Local)",
                    isBuiltIn = false,
                    uriString = uri.toString()
                )

                _tracks.value = listOf(newTrack) + _tracks.value
                selectedTrack = newTrack
            } catch (e: Exception) {
                // Handle parsing gracefully
            }
        }
    }

    fun startMatch() {
        _score.value = 0
        _combo.value = 0
        _maxCombo.value = 0
        _multiplier.value = 1
        _rockMeter.value = 0.6f
        _starPowerEnergy.value = 0.0f
        _isStarPowerActive.value = false
        _lastHitRating.value = null
        _activeFretLanes.value = emptySet()
        _isPaused.value = false
        totalHitCount = 0
        totalMissCount = 0

        adaptiveEngine = AdaptiveEngine(selectedDifficulty, _playerStats.value.glassAdaptiveEnabled)
        val generatedNotes = AudioAnalyzer.generateChart(selectedTrack, selectedInstrument, selectedDifficulty)
        _activeNotes.value = generatedNotes

        audioPlayer.loadTrack(selectedTrack) {
            audioPlayer.play()
        }

        navigateTo(GameScreen.Gameplay)
        startGameLoop()
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            val latencyOffset = _playerStats.value.audioLatencyOffsetMs

            while (isActive && _currentScreen.value is GameScreen.Gameplay) {
                if (!_isPaused.value) {
                    val pos = audioPlayer.getCurrentPositionMs() + latencyOffset
                    _currentSongTimeMs.value = pos

                    // Check for missed notes that passed beyond strike window
                    checkMissedNotes(pos)

                    // Decay star power if active
                    if (_isStarPowerActive.value) {
                        _starPowerEnergy.value = (_starPowerEnergy.value - 0.003f).coerceAtLeast(0f)
                        if (_starPowerEnergy.value <= 0f) {
                            _isStarPowerActive.value = false
                            recalculateMultiplier()
                        }
                    }

                    // Check song finish condition
                    val totalDuration = audioPlayer.getDurationMs()
                    if (pos >= totalDuration || (totalDuration > 5000L && pos >= totalDuration - 500L)) {
                        finishMatch()
                        break
                    }

                    // Check fail condition (rock meter drops to 0)
                    if (_rockMeter.value <= 0.01f) {
                        finishMatch(failed = true)
                        break
                    }
                }
                delay(14L) // ~70-90 FPS refresh cadence matching Galaxy A15 AMOLED display
            }
        }
    }

    private fun checkMissedNotes(currentPosMs: Long) {
        val window = adaptiveEngine.getEffectiveHitWindowMs()
        val notes = _activeNotes.value

        for (note in notes) {
            if (!note.isHit && !note.isMissed && (currentPosMs - note.timeMs) > window) {
                note.isMissed = true
                onMiss(note.lane)
            }
        }
    }

    fun onFretDown(lane: Int) {
        _activeFretLanes.value = _activeFretLanes.value + lane

        // In Direct Touch mode, touching the lane immediately attempts to strike
        if (_playerStats.value.controlMode == ControlMode.DIRECT_TOUCH) {
            attemptHit(lane)
        }
    }

    fun onFretUp(lane: Int) {
        _activeFretLanes.value = _activeFretLanes.value - lane
    }

    fun onStrumBar() {
        // In Fret + Strum mode, pressing strum evaluates all currently held frets
        val pressed = _activeFretLanes.value
        if (pressed.isNotEmpty()) {
            for (lane in pressed) {
                attemptHit(lane)
            }
        } else {
            // Overstrum penalty
            onMiss(-1)
        }
    }

    fun activateStarPower() {
        if (_starPowerEnergy.value >= 0.5f && !_isStarPowerActive.value) {
            _isStarPowerActive.value = true
            soundFx.playStarPower()
            triggerHaptic(longVibe = true)
            recalculateMultiplier()
        }
    }

    private fun attemptHit(lane: Int) {
        val currentPos = _currentSongTimeMs.value
        val effectiveWindow = adaptiveEngine.getEffectiveHitWindowMs()
        val notes = _activeNotes.value

        // Find closest unhit note in this lane within window
        var closestNote: Note? = null
        var minDiff = Long.MAX_VALUE

        for (note in notes) {
            if (!note.isHit && !note.isMissed && note.lane == lane) {
                val diff = abs(note.timeMs - currentPos)
                if (diff <= effectiveWindow && diff < minDiff) {
                    minDiff = diff
                    closestNote = note
                }
            }
        }

        if (closestNote != null) {
            closestNote.isHit = true
            val rating = when {
                minDiff <= (effectiveWindow * 0.35).toLong() -> HitRating.PERFECT
                minDiff <= (effectiveWindow * 0.70).toLong() -> HitRating.GREAT
                else -> HitRating.GOOD
            }
            onHit(lane, rating, closestNote.isStarPower, minDiff)
        } else {
            // Overstrum or mis-timed fret tap penalty
            onMiss(lane)
        }
    }

    private fun onHit(lane: Int, rating: HitRating, isStarPower: Boolean, diffMs: Long) {
        totalHitCount++
        _lastHitRating.value = rating

        // Update Combo & Multiplier
        _combo.value = _combo.value + 1
        if (_combo.value > _maxCombo.value) {
            _maxCombo.value = _combo.value
        }
        recalculateMultiplier()

        // Update Score
        val baseScore = (rating.score * selectedDifficulty.scoreMultiplier * adaptiveEngine.getScoreBoost()).toInt()
        val addedScore = baseScore * _multiplier.value
        _score.value = _score.value + addedScore

        // Update Rock Meter
        _rockMeter.value = (_rockMeter.value + rating.healthDelta).coerceIn(0f, 1f)

        // Star power accumulation
        if (isStarPower) {
            _starPowerEnergy.value = (_starPowerEnergy.value + 0.12f).coerceAtMost(1.0f)
        }

        // Notify Adaptive Engine
        adaptiveEngine.onNoteResult(rating, diffMs)

        // Audio & Haptic Feedback
        if (selectedInstrument == Instrument.DRUMS) {
            soundFx.playDrumHit(lane)
        } else {
            soundFx.playHitChord(lane)
        }
        triggerHaptic(longVibe = false)
    }

    private fun onMiss(lane: Int) {
        totalMissCount++
        _lastHitRating.value = HitRating.MISS
        _combo.value = 0
        recalculateMultiplier()

        // Health penalty
        _rockMeter.value = (_rockMeter.value - 0.08f).coerceIn(0f, 1f)

        // Notify Adaptive Engine
        adaptiveEngine.onNoteResult(HitRating.MISS, 0L)

        // Discordant miss sound
        soundFx.playMiss()
    }

    private fun recalculateMultiplier() {
        val baseMult = when {
            _combo.value >= 30 -> 4
            _combo.value >= 20 -> 3
            _combo.value >= 10 -> 2
            else -> 1
        }
        _multiplier.value = if (_isStarPowerActive.value) baseMult * 2 else baseMult
    }

    fun togglePause() {
        if (_isPaused.value) {
            _isPaused.value = false
            audioPlayer.play()
        } else {
            _isPaused.value = true
            audioPlayer.pause()
        }
    }

    fun restartMatch() {
        stop()
        startMatch()
    }

    fun finishMatch(failed: Boolean = false) {
        audioPlayer.stop()
        gameLoopJob?.cancel()

        val totalNotesCount = (totalHitCount + totalMissCount).coerceAtLeast(1)
        val accuracy = (totalHitCount.toFloat() / totalNotesCount.toFloat()) * 100f

        val stars = when {
            failed -> 1
            accuracy >= 98f -> 5
            accuracy >= 90f -> 4
            accuracy >= 75f -> 3
            accuracy >= 55f -> 2
            else -> 1
        }

        val coinsEarned = if (failed) 20 else (stars * 60 + (_score.value / 800)).coerceAtLeast(40)

        val result = MatchResult(
            track = selectedTrack,
            instrument = selectedInstrument,
            difficulty = selectedDifficulty,
            score = _score.value,
            maxCombo = _maxCombo.value,
            totalNotes = totalNotesCount,
            hitNotes = totalHitCount,
            missedNotes = totalMissCount,
            accuracyPercent = accuracy,
            stars = stars,
            coinsEarned = coinsEarned,
            isAdaptiveUsed = _playerStats.value.glassAdaptiveEnabled
        )

        lastMatchResult = result

        // Update saved stats
        updatePlayerStats { current ->
            current.copy(
                coins = current.coins + coinsEarned,
                totalScore = current.totalScore + _score.value,
                notesHitTotal = current.notesHitTotal + totalHitCount,
                maxStreakOverall = maxOf(current.maxStreakOverall, _maxCombo.value),
                songsCompleted = current.songsCompleted + (if (!failed) 1 else 0),
                unlockedStars = current.unlockedStars + stars
            )
        }

        navigateTo(GameScreen.Results)
    }

    private fun triggerHaptic(longVibe: Boolean) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val duration = if (longVibe) 120L else 35L
                val amplitude = if (longVibe) 200 else 110
                vibrator?.vibrate(VibrationEffect.createOneShot(duration, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(if (longVibe) 100L else 30L)
            }
        } catch (e: Exception) {}
    }

    fun unlockCharacter(id: String, price: Int) {
        if (_playerStats.value.coins >= price) {
            updatePlayerStats { current ->
                current.copy(coins = current.coins - price, selectedCharacterId = id, unlockedCharacterIds = current.unlockedCharacterIds + id)
            }
        }
    }

    fun selectCharacter(id: String) {
        updatePlayerStats { it.copy(selectedCharacterId = id) }
    }

    fun unlockGuitar(id: String, price: Int) {
        if (_playerStats.value.coins >= price) {
            updatePlayerStats { current ->
                current.copy(coins = current.coins - price, selectedGuitarId = id, unlockedGuitarIds = current.unlockedGuitarIds + id)
            }
        }
    }

    fun selectGuitar(id: String) {
        updatePlayerStats { it.copy(selectedGuitarId = id) }
    }

    fun stop() {
        audioPlayer.stop()
        gameLoopJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
