package com.example.guitarlegends.viewmodel

import android.app.Application
import android.content.Context
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
import com.example.guitarlegends.data.*
import com.example.guitarlegends.engine.ParticleSystem
import com.example.guitarlegends.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.abs

sealed class GameScreen {
    object Splash : GameScreen()
    object Auth : GameScreen()
    object MainMenu : GameScreen()
    object Profile : GameScreen()
    object DailyRewards : GameScreen()
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

sealed class AuthUiState {
    object Loading : AuthUiState()
    object SignedOut : AuthUiState()
    data class NeedsNickname(val uid: String, val email: String) : AuthUiState()
    data class Authenticated(val uid: String, val email: String, val nickname: String) : AuthUiState()
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext
    val audioPlayer = AudioPlayerEngine(context)
    val soundFx = SoundFxSynthesizer()
    val particleSystem = ParticleSystem()

    val musicLibraryRepository = MusicLibraryRepository(context)
    val updateManager = UpdateManager(context)

    private val firebaseAuth: FirebaseAuth? = FirebaseClientProvider.getAuthOrNull(context)
    private val accountRepo: AccountCloudRepository? = run {
        val db = FirebaseClientProvider.getFirestoreOrNull(context)
        val auth = firebaseAuth
        if (db != null && auth != null) AccountCloudRepository(db, auth) else null
    }

    // Navegação
    private val _currentScreen = MutableStateFlow<GameScreen>(GameScreen.Splash)
    val currentScreen: StateFlow<GameScreen> = _currentScreen.asStateFlow()
    private val screenStack = mutableListOf<GameScreen>()

    // Autenticação & Nuvem
    private val _authUiState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    private val _isAuthBusy = MutableStateFlow(false)
    val isAuthBusy: StateFlow<Boolean> = _isAuthBusy.asStateFlow()

    private val _nicknameAvailability = MutableStateFlow<Boolean?>(null)
    val nicknameAvailability: StateFlow<Boolean?> = _nicknameAvailability.asStateFlow()

    private val _cloudSyncStatus = MutableStateFlow("Aguardando sessão")
    val cloudSyncStatus: StateFlow<String> = _cloudSyncStatus.asStateFlow()

    private val _rewardHistory = MutableStateFlow<List<DailyRewardClaimCloud>>(emptyList())
    val rewardHistory: StateFlow<List<DailyRewardClaimCloud>> = _rewardHistory.asStateFlow()

    private val _progressionHistory = MutableStateFlow<List<ProgressionHistoryEntryCloud>>(emptyList())
    val progressionHistory: StateFlow<List<ProgressionHistoryEntryCloud>> = _progressionHistory.asStateFlow()

    private val _rewardActionMessage = MutableStateFlow<String?>(null)
    val rewardActionMessage: StateFlow<String?> = _rewardActionMessage.asStateFlow()

    // Atualizações & Versão
    private val _releasesCatalog = MutableStateFlow<List<AppReleaseInfo>>(LocalReleaseCatalog.history)
    val releasesCatalog: StateFlow<List<AppReleaseInfo>> = _releasesCatalog.asStateFlow()

    private val _updateStatus = MutableStateFlow<UpdateCheckStatus>(UpdateCheckStatus.Idle)
    val updateStatus: StateFlow<UpdateCheckStatus> = _updateStatus.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    // Status & Progressão do Jogador
    private val _playerStats = MutableStateFlow(PlayerStats())
    val playerStats: StateFlow<PlayerStats> = _playerStats.asStateFlow()

    // Biblioteca Musical & Prévia
    private val _tracks = MutableStateFlow<List<Track>>(AudioAnalyzer.BUILT_IN_TRACKS)
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _selectedTrackFlow = MutableStateFlow(AudioAnalyzer.BUILT_IN_TRACKS[0])
    val selectedTrackFlow: StateFlow<Track> = _selectedTrackFlow.asStateFlow()

    private val _isImportingAudio = MutableStateFlow(false)
    val isImportingAudio: StateFlow<Boolean> = _isImportingAudio.asStateFlow()

    private val _libraryErrorMessage = MutableStateFlow<String?>(null)
    val libraryErrorMessage: StateFlow<String?> = _libraryErrorMessage.asStateFlow()

    private val _playbackErrorMessage = MutableStateFlow<String?>(null)
    val playbackErrorMessage: StateFlow<String?> = _playbackErrorMessage.asStateFlow()

    private val _previewTrackId = MutableStateFlow<String?>(null)
    val previewTrackId: StateFlow<String?> = _previewTrackId.asStateFlow()

    private val _isPreviewPlaying = MutableStateFlow(false)
    val isPreviewPlaying: StateFlow<Boolean> = _isPreviewPlaying.asStateFlow()

    private val _isPreviewPaused = MutableStateFlow(false)
    val isPreviewPaused: StateFlow<Boolean> = _isPreviewPaused.asStateFlow()

    private val _previewPositionMs = MutableStateFlow(0L)
    val previewPositionMs: StateFlow<Long> = _previewPositionMs.asStateFlow()

    private val _previewDurationMs = MutableStateFlow(0L)
    val previewDurationMs: StateFlow<Long> = _previewDurationMs.asStateFlow()

    // Seleção Atual
    var selectedTrack: Track = AudioAnalyzer.BUILT_IN_TRACKS[0]
        private set
    var selectedInstrument: Instrument = Instrument.GUITAR
        private set
    var selectedDifficulty: Difficulty = Difficulty.MEDIUM
        private set
    var isCompetitiveMatch: Boolean = false
        private set

    // Estado da Partida ao Vivo
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

    private val _rockMeter = MutableStateFlow(0.6f)
    val rockMeter: StateFlow<Float> = _rockMeter.asStateFlow()

    private val _starPowerEnergy = MutableStateFlow(0.0f)
    val starPowerEnergy: StateFlow<Float> = _starPowerEnergy.asStateFlow()

    private val _isStarPowerActive = MutableStateFlow(false)
    val isStarPowerActive: StateFlow<Boolean> = _isStarPowerActive.asStateFlow()

    private val _lastHitRating = MutableStateFlow<HitRating?>(null)
    val lastHitRating: StateFlow<HitRating?> = _lastHitRating.asStateFlow()

    private val _activeFretLanes = MutableStateFlow<Set<Int>>(emptySet())
    val activeFretLanes: StateFlow<Set<Int>> = _activeFretLanes.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    var adaptiveEngine = AdaptiveEngine(selectedDifficulty, true)
        private set

    private var totalHitCount = 0
    private var totalMissCount = 0

    var lastMatchResult: MatchResult? = null
        private set

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

    val basses = listOf(
        InstrumentGear(
            id = "thunder_bass",
            instrumentType = Instrument.BASS,
            name = "Thunder Bass 4-Cordas",
            modelStyle = "Precision Bass Clássico",
            colorHex = 0xFF512DA8,
            priceCoins = 0,
            isUnlocked = true,
            description = "Baixo encorpado com captadores split-coil para graves pesados de arena."
        ),
        InstrumentGear(
            id = "skull_bass",
            instrumentType = Instrument.BASS,
            name = "Skull Crusher 5-Cordas",
            modelStyle = "Groove Metal Ativo",
            colorHex = 0xFFFF1744,
            priceCoins = 300,
            isUnlocked = true,
            description = "Escala estendida e entalhes de caveira com circuito ativo de equalização."
        ),
        InstrumentGear(
            id = "cyber_bass",
            instrumentType = Instrument.BASS,
            name = "Cyber Wave Fretless",
            modelStyle = "Synth Bass Holográfico",
            colorHex = 0xFF00E5FF,
            priceCoins = 450,
            isUnlocked = false,
            description = "Escala lisa de fibra de carbono com sustain infinito e iluminação neon azul."
        )
    )

    val drums = listOf(
        InstrumentGear(
            id = "arena_beast_kit",
            instrumentType = Instrument.DRUMS,
            name = "Arena Beast Acústica",
            modelStyle = "Bateria Bumbo Duplo Hard Rock",
            colorHex = 0xFFFF6D00,
            priceCoins = 0,
            isUnlocked = true,
            description = "Dois bumbos de 24 polegadas, pratos de bronze fundido e ataque devastador."
        ),
        InstrumentGear(
            id = "punk_garage_kit",
            instrumentType = Instrument.DRUMS,
            name = "Garage Riot Compact",
            modelStyle = "Kit Punk 4 Peças",
            colorHex = 0xFF00E676,
            priceCoins = 250,
            isUnlocked = true,
            description = "Caixa estalada com resposta ultrarrápida para punk rock acelerado."
        ),
        InstrumentGear(
            id = "electronic_neon_kit",
            instrumentType = Instrument.DRUMS,
            name = "Cyber Mesh Eletrônica",
            modelStyle = "Pads Mesh com Triggers Reativos",
            colorHex = 0xFF7C4DFF,
            priceCoins = 500,
            isUnlocked = false,
            description = "Pads sensíveis a dinâmicas extremas com aros iluminados por LED RGB."
        )
    )

    val vocalMics = listOf(
        InstrumentGear(
            id = "vintage_shure_55s",
            instrumentType = Instrument.VOCALS,
            name = "Vintage 55S Cromado",
            modelStyle = "Microfone de Metal Anos 50/60",
            colorHex = 0xFFE0E0E0,
            priceCoins = 0,
            isUnlocked = true,
            description = "O icônico microfone cromado dos pioneiros do rock 'n' roll clássico."
        ),
        InstrumentGear(
            id = "golden_supreme_mic",
            instrumentType = Instrument.VOCALS,
            name = "Golden Vocal Supreme",
            modelStyle = "Dinâmico Ouro 24K",
            colorHex = 0xFFFFD700,
            priceCoins = 350,
            isUnlocked = true,
            description = "Cápsula banhada a ouro para vocais que ecoam em estádios lotados."
        ),
        InstrumentGear(
            id = "punk_distress_mic",
            instrumentType = Instrument.VOCALS,
            name = "Riot Distress Custom",
            modelStyle = "Fita Isolante & Grade Amassada",
            colorHex = 0xFFFF3D00,
            priceCoins = 400,
            isUnlocked = false,
            description = "Sobreviveu a centenas de shows no circuito underground."
        )
    )

    val highwayThemes = HighwayTheme.values().toList()

    private var gameLoopJob: Job? = null
    private var previewProgressJob: Job? = null

    init {
        viewModelScope.launch {
            updateManager.runDataMigrationIfNeeded()
        }

        viewModelScope.launch {
            val all = musicLibraryRepository.loadAllTracks()
            _tracks.value = all
            if (all.none { it.id == selectedTrack.id }) {
                selectTrack(all.firstOrNull() ?: AudioAnalyzer.BUILT_IN_TRACKS[0])
            }
        }

        viewModelScope.launch {
            delay(1400L)
            resolveInitialAuthAndRoute()
        }
    }

    private suspend fun resolveInitialAuthAndRoute() {
        val currentUser = firebaseAuth?.currentUser
        if (firebaseAuth == null || accountRepo == null) {
            _authUiState.value = AuthUiState.SignedOut
            if (_currentScreen.value is GameScreen.Splash) {
                navigateTo(GameScreen.Auth, pushStack = false)
            }
            return
        }

        if (currentUser == null) {
            _authUiState.value = AuthUiState.SignedOut
            if (_currentScreen.value is GameScreen.Splash) {
                navigateTo(GameScreen.Auth, pushStack = false)
            }
        } else {
            loadAuthenticatedAccount(currentUser.uid, currentUser.email.orEmpty(), navigateAfter = true)
        }
    }

    private suspend fun loadAuthenticatedAccount(
        uid: String,
        email: String,
        navigateAfter: Boolean
    ) {
        _authUiState.value = AuthUiState.Loading
        val repo = accountRepo ?: return
        val accountRes = repo.fetchAccount()
        accountRes.onSuccess { remote ->
            if (remote == null || remote.nickname.isBlank()) {
                _authUiState.value = AuthUiState.NeedsNickname(uid, email)
                _playerStats.value = _playerStats.value.copy(email = email)
                if (navigateAfter) navigateTo(GameScreen.Auth, pushStack = false)
            } else {
                _playerStats.value = remote.toPlayerStats()
                _authUiState.value = AuthUiState.Authenticated(uid, remote.email.ifBlank { email }, remote.nickname)
                _cloudSyncStatus.value = "Sincronizado na nuvem"
                refreshAccountSubcollections()
                checkForUpdates(silent = true)
                if (navigateAfter) navigateTo(GameScreen.MainMenu, pushStack = false)
            }
        }.onFailure { err ->
            _authErrorMessage.value = "Erro ao carregar conta na nuvem: ${err.message}"
            _authUiState.value = AuthUiState.NeedsNickname(uid, email)
            if (navigateAfter) navigateTo(GameScreen.Auth, pushStack = false)
        }
    }

    fun signInWithGoogleIdToken(idToken: String) {
        val auth = firebaseAuth ?: run {
            _authErrorMessage.value = "Serviço Firebase não inicializado neste ambiente."
            return
        }
        viewModelScope.launch {
            _isAuthBusy.value = true
            _authErrorMessage.value = null
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                val user = result.user
                if (user == null) {
                    _authErrorMessage.value = "Falha ao obter dados do usuário autenticado."
                } else {
                    loadAuthenticatedAccount(user.uid, user.email.orEmpty(), navigateAfter = true)
                }
            } catch (e: Exception) {
                _authErrorMessage.value = "Falha na autenticação Google: ${e.message}"
            } finally {
                _isAuthBusy.value = false
            }
        }
    }

    fun setAuthError(message: String?) {
        _authErrorMessage.value = message
    }

    fun checkNicknameAvailability(nickname: String) {
        val repo = accountRepo ?: return
        viewModelScope.launch {
            _nicknameAvailability.value = null
            val res = repo.isNicknameAvailable(nickname)
            res.onSuccess { avail ->
                _nicknameAvailability.value = avail
                if (!avail) {
                    _authErrorMessage.value = "O nickname '$nickname' já está sendo usado por outro jogador."
                } else {
                    _authErrorMessage.value = null
                }
            }.onFailure { err ->
                _nicknameAvailability.value = false
                _authErrorMessage.value = err.message
            }
        }
    }

    fun submitNickname(nickname: String) {
        val repo = accountRepo ?: return
        viewModelScope.launch {
            _isAuthBusy.value = true
            _authErrorMessage.value = null
            val res = repo.claimNicknameAndSaveAccount(nickname, _playerStats.value)
            res.onSuccess { cloud ->
                _playerStats.value = cloud.toPlayerStats()
                _authUiState.value = AuthUiState.Authenticated(cloud.userId, cloud.email, cloud.nickname)
                _cloudSyncStatus.value = "Conta salva na nuvem"
                refreshAccountSubcollections()
                checkForUpdates(silent = true)
                navigateTo(GameScreen.MainMenu, pushStack = false)
            }.onFailure { err ->
                _authErrorMessage.value = err.message ?: "Não foi possível reservar este nickname."
            }
            _isAuthBusy.value = false
        }
    }

    fun refreshAccountSubcollections() {
        val repo = accountRepo ?: return
        if (repo.currentUidOrNull() == null) return
        viewModelScope.launch {
            repo.fetchRewardHistory().onSuccess { _rewardHistory.value = it }
            repo.fetchProgressionHistory().onSuccess { _progressionHistory.value = it }
        }
    }

    fun syncCloudNow() {
        val repo = accountRepo ?: return
        if (repo.currentUidOrNull() == null) return
        viewModelScope.launch {
            _cloudSyncStatus.value = "Sincronizando..."
            val res = repo.syncPlayerStats(_playerStats.value)
            res.onSuccess { cloud ->
                _playerStats.value = cloud.toPlayerStats()
                _cloudSyncStatus.value = "Sincronizado (rev. ${cloud.syncRevision})"
            }.onFailure { err ->
                _cloudSyncStatus.value = "Falha na sincronização: ${err.message}"
            }
        }
    }

    private fun triggerBackgroundCloudSync() {
        val repo = accountRepo ?: return
        if (repo.currentUidOrNull() == null || _playerStats.value.nickname.isBlank()) return
        viewModelScope.launch {
            repo.syncPlayerStats(_playerStats.value).onSuccess { cloud ->
                _playerStats.value = cloud.toPlayerStats()
                _cloudSyncStatus.value = "Nuvem atualizada (rev. ${cloud.syncRevision})"
            }
        }
    }

    fun claimDailyReward() {
        val repo = accountRepo ?: run {
            _rewardActionMessage.value = "Conecte-se com sua conta para resgatar recompensas diárias."
            return
        }
        if (repo.currentUidOrNull() == null) {
            _rewardActionMessage.value = "Faça login para resgatar recompensas diárias."
            return
        }
        viewModelScope.launch {
            _isAuthBusy.value = true
            _rewardActionMessage.value = null
            val res = repo.claimDailyReward(_playerStats.value)
            res.onSuccess { (claim, updatedUser) ->
                _playerStats.value = updatedUser.toPlayerStats()
                _rewardHistory.value = listOf(claim) + _rewardHistory.value.filter { it.dayKey != claim.dayKey }
                _rewardActionMessage.value = "Dia ${claim.dayNumber} resgatado: ${claim.rewardTitle} (+${claim.coinsGranted} LC)!"
            }.onFailure { err ->
                _rewardActionMessage.value = err.message ?: "Não foi possível resgatar a recompensa."
            }
            _isAuthBusy.value = false
        }
    }

    fun clearRewardActionMessage() {
        _rewardActionMessage.value = null
    }

    fun signOut() {
        viewModelScope.launch {
            stop()
            stopPreview()
            firebaseAuth?.signOut()
            GoogleSignInHelper.clearCredentials(context)
            _playerStats.value = PlayerStats()
            _rewardHistory.value = emptyList()
            _progressionHistory.value = emptyList()
            _cloudSyncStatus.value = "Sessão encerrada"
            _authUiState.value = AuthUiState.SignedOut
            screenStack.clear()
            _currentScreen.value = GameScreen.Auth
        }
    }

    fun checkForUpdates(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _updateStatus.value = UpdateCheckStatus.Checking
            val repo = accountRepo
            val catalog = if (repo != null && repo.currentUidOrNull() != null) {
                repo.fetchReleasesCatalog().getOrElse { LocalReleaseCatalog.history }
            } else {
                LocalReleaseCatalog.history
            }
            _releasesCatalog.value = catalog
            val evaluated = updateManager.evaluateUpdateStatus(catalog)
            _updateStatus.value = evaluated
            if (evaluated is UpdateCheckStatus.UpdateAvailable && (!silent || evaluated.isMandatory)) {
                _showUpdateDialog.value = true
            }
        }
    }

    fun dismissUpdateDialog() {
        val current = _updateStatus.value
        if (current is UpdateCheckStatus.UpdateAvailable && current.isMandatory) {
            return
        }
        _showUpdateDialog.value = false
    }

    fun launchOfficialUpdate() {
        val current = _updateStatus.value
        val url = if (current is UpdateCheckStatus.UpdateAvailable) {
            current.latestRelease.downloadUrl
        } else {
            _releasesCatalog.value.firstOrNull()?.downloadUrl.orEmpty()
        }
        updateManager.openOfficialUpdateChannel(url).onFailure { err ->
            _updateStatus.value = UpdateCheckStatus.Error(err.message ?: "Erro ao abrir link de atualização.")
        }
    }

    fun isCosmeticUnlocked(id: String, defaultUnlocked: Boolean): Boolean {
        return defaultUnlocked || _playerStats.value.unlockedCosmetics.contains(id)
    }

    fun equipCharacter(characterId: String) {
        _playerStats.value = _playerStats.value.copy(selectedCharacterId = characterId)
        triggerBackgroundCloudSync()
    }

    fun equipGuitar(guitarId: String) {
        _playerStats.value = _playerStats.value.copy(selectedGuitarId = guitarId)
        triggerBackgroundCloudSync()
    }

    fun equipBass(bassId: String) {
        _playerStats.value = _playerStats.value.copy(selectedBassId = bassId)
        triggerBackgroundCloudSync()
    }

    fun equipDrums(drumsId: String) {
        _playerStats.value = _playerStats.value.copy(selectedDrumsId = drumsId)
        triggerBackgroundCloudSync()
    }

    fun equipVocalMic(micId: String) {
        _playerStats.value = _playerStats.value.copy(selectedVocalMicId = micId)
        triggerBackgroundCloudSync()
    }

    fun equipHighwayTheme(themeId: String) {
        _playerStats.value = _playerStats.value.copy(selectedHighwayThemeId = themeId)
        triggerBackgroundCloudSync()
    }

    fun updateCustomRockerPhoto(uriString: String) {
        val current = _playerStats.value.customRocker
        _playerStats.value = _playerStats.value.copy(
            customRocker = current.copy(photoUri = uriString)
        )
        triggerBackgroundCloudSync()
    }

    fun updateCustomRockerMethod(method: CharacterCreationMethod) {
        val current = _playerStats.value.customRocker
        _playerStats.value = _playerStats.value.copy(
            customRocker = current.copy(creationMethod = method)
        )
        triggerBackgroundCloudSync()
    }

    fun updateCustomRockerStyle(style: CharacterArtStyle) {
        val current = _playerStats.value.customRocker
        _playerStats.value = _playerStats.value.copy(
            customRocker = current.copy(artStyle = style)
        )
        triggerBackgroundCloudSync()
    }

    fun updateCustomRockerAppearance(
        hair: String,
        top: String,
        pants: String,
        shoes: String,
        accessories: String,
        tattoos: String
    ) {
        val current = _playerStats.value.customRocker
        _playerStats.value = _playerStats.value.copy(
            customRocker = current.copy(
                hairStyle = hair,
                topClothing = top,
                pantsClothing = pants,
                shoesClothing = shoes,
                accessories = accessories,
                bodyTattoos = tattoos
            )
        )
        triggerBackgroundCloudSync()
    }

    fun navigateTo(screen: GameScreen, pushStack: Boolean = true) {
        stopPreview()
        if (pushStack) {
            screenStack.add(_currentScreen.value)
        } else {
            screenStack.clear()
        }
        _currentScreen.value = screen
    }

    fun navigateBack() {
        stopPreview()
        if (screenStack.isNotEmpty()) {
            val prev = screenStack.removeAt(screenStack.size - 1)
            _currentScreen.value = prev
        } else {
            _currentScreen.value = if (_authUiState.value is AuthUiState.Authenticated) {
                GameScreen.MainMenu
            } else {
                GameScreen.Auth
            }
        }
    }

    fun selectTrack(track: Track) {
        selectedTrack = track
        _selectedTrackFlow.value = track
    }

    fun selectInstrument(instrument: Instrument) {
        selectedInstrument = instrument
    }

    fun selectDifficulty(difficulty: Difficulty) {
        selectedDifficulty = difficulty
    }

    fun setCompetitiveMode(competitive: Boolean) {
        isCompetitiveMatch = competitive
    }

    fun toggleAdaptiveSystem(enabled: Boolean) {
        _playerStats.value = _playerStats.value.copy(glassAdaptiveEnabled = enabled)
        triggerBackgroundCloudSync()
    }

    fun setControlMode(mode: ControlMode) {
        _playerStats.value = _playerStats.value.copy(controlMode = mode)
        triggerBackgroundCloudSync()
    }

    fun setGraphicsQuality(quality: GraphicsQuality) {
        _playerStats.value = _playerStats.value.copy(graphicsQuality = quality)
        triggerBackgroundCloudSync()
    }

    fun setLatencyOffset(offsetMs: Long) {
        _playerStats.value = _playerStats.value.copy(audioLatencyOffsetMs = offsetMs)
        triggerBackgroundCloudSync()
    }

    fun importAudioFile(uri: Uri) {
        viewModelScope.launch {
            _isImportingAudio.value = true
            _libraryErrorMessage.value = null
            val result = musicLibraryRepository.importAudio(uri)
            result.onSuccess { newTrack ->
                val updated = musicLibraryRepository.loadAllTracks()
                _tracks.value = updated
                selectTrack(newTrack)
                val updatedAchievements = (_playerStats.value.achievementsUnlocked + "ach_custom_import").distinct()
                _playerStats.value = _playerStats.value.copy(achievementsUnlocked = updatedAchievements)
                triggerBackgroundCloudSync()
            }.onFailure { err ->
                _libraryErrorMessage.value = err.message ?: "Falha ao importar o arquivo de áudio."
            }
            _isImportingAudio.value = false
        }
    }

    fun calibrateTrack(trackId: String, manualBpm: Double?, syncOffsetMs: Long) {
        viewModelScope.launch {
            val res = musicLibraryRepository.updateTrackCalibration(trackId, manualBpm, syncOffsetMs)
            res.onSuccess { updatedTrack ->
                val updatedList = musicLibraryRepository.loadAllTracks()
                _tracks.value = updatedList
                if (selectedTrack.id == updatedTrack.id) {
                    selectTrack(updatedTrack)
                }
            }.onFailure { err ->
                _libraryErrorMessage.value = err.message ?: "Falha ao atualizar calibração da faixa."
            }
        }
    }

    fun deleteImportedTrack(track: Track) {
        if (track.isBuiltIn) return
        viewModelScope.launch {
            if (_previewTrackId.value == track.id) {
                stopPreview()
            }
            val result = musicLibraryRepository.deleteTrack(track)
            result.onSuccess {
                val updated = musicLibraryRepository.loadAllTracks()
                _tracks.value = updated
                if (selectedTrack.id == track.id) {
                    selectTrack(updated.firstOrNull() ?: AudioAnalyzer.BUILT_IN_TRACKS[0])
                }
            }.onFailure { err ->
                _libraryErrorMessage.value = err.message ?: "Erro ao remover a música."
            }
        }
    }

    /**
     * Prévia musical com suporte real a Tocar, Pausar preservando posição exata, Retomar e Parar.
     */
    fun togglePreview(track: Track) {
        _playbackErrorMessage.value = null
        if (_isPreviewPlaying.value && _previewTrackId.value == track.id) {
            pausePreview()
            return
        }
        if (_isPreviewPaused.value && _previewTrackId.value == track.id) {
            resumePreview()
            return
        }

        stopPreview()
        audioPlayer.loadTrack(
            track = track,
            onReady = {
                audioPlayer.play()
                _previewTrackId.value = track.id
                _isPreviewPlaying.value = true
                _isPreviewPaused.value = false
                _previewDurationMs.value = audioPlayer.getDurationMs()
                startPreviewProgressTracker()
            },
            onError = { errMsg ->
                _isPreviewPlaying.value = false
                _isPreviewPaused.value = false
                _previewTrackId.value = null
                _playbackErrorMessage.value = errMsg
            },
            onCompleted = {
                _isPreviewPlaying.value = false
                _isPreviewPaused.value = false
                _previewPositionMs.value = 0L
                previewProgressJob?.cancel()
            }
        )
    }

    fun pausePreview() {
        if (!_isPreviewPlaying.value) return
        audioPlayer.pause()
        _previewPositionMs.value = audioPlayer.getCurrentPositionMs()
        _isPreviewPlaying.value = false
        _isPreviewPaused.value = true
        previewProgressJob?.cancel()
    }

    fun resumePreview() {
        if (_isPreviewPaused.value && _previewTrackId.value != null) {
            audioPlayer.resume()
            _isPreviewPlaying.value = true
            _isPreviewPaused.value = false
            startPreviewProgressTracker()
        }
    }

    fun stopPreview() {
        previewProgressJob?.cancel()
        if (_isPreviewPlaying.value || _isPreviewPaused.value || _previewTrackId.value != null) {
            audioPlayer.stop()
        }
        _isPreviewPlaying.value = false
        _isPreviewPaused.value = false
        _previewTrackId.value = null
        _previewPositionMs.value = 0L
        _previewDurationMs.value = 0L
    }

    private fun startPreviewProgressTracker() {
        previewProgressJob?.cancel()
        previewProgressJob = viewModelScope.launch {
            while (isActive && _isPreviewPlaying.value) {
                val pos = audioPlayer.getCurrentPositionMs()
                val dur = audioPlayer.getDurationMs().coerceAtLeast(1L)
                _previewPositionMs.value = pos.coerceAtMost(dur)
                _previewDurationMs.value = dur
                if (pos >= dur) {
                    stopPreview()
                    break
                }
                delay(120L)
            }
        }
    }

    fun clearLibraryError() {
        _libraryErrorMessage.value = null
    }

    fun clearPlaybackError() {
        _playbackErrorMessage.value = null
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            val all = musicLibraryRepository.loadAllTracks()
            _tracks.value = all
            if (all.none { it.id == selectedTrack.id }) {
                selectTrack(all.firstOrNull() ?: AudioAnalyzer.BUILT_IN_TRACKS[0])
            }
        }
    }

    fun startMatch() {
        stopPreview()
        _playbackErrorMessage.value = null
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

        audioPlayer.loadTrack(
            track = selectedTrack,
            onReady = {
                audioPlayer.play()
                navigateTo(GameScreen.Gameplay)
                startGameLoop()
            },
            onError = { errMsg ->
                _playbackErrorMessage.value = errMsg
            }
        )
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            val latencyOffset = _playerStats.value.audioLatencyOffsetMs

            while (isActive && _currentScreen.value is GameScreen.Gameplay) {
                if (!_isPaused.value) {
                    val pos = audioPlayer.getCurrentPositionMs() + latencyOffset
                    _currentSongTimeMs.value = pos

                    checkMissedNotes(pos)

                    // Adaptação dinâmica real de sequências futuras (> 2.2s à frente) quando GLAS está ativo
                    if (_playerStats.value.glassAdaptiveEnabled) {
                        val updated = adaptiveEngine.adaptUpcomingNotes(
                            currentNotes = _activeNotes.value,
                            currentTimeMs = pos,
                            instrument = selectedInstrument
                        )
                        if (updated !== _activeNotes.value) {
                            _activeNotes.value = updated
                        }
                    }

                    if (_isStarPowerActive.value) {
                        _starPowerEnergy.value = (_starPowerEnergy.value - 0.003f).coerceAtLeast(0f)
                        if (_starPowerEnergy.value <= 0f) {
                            _isStarPowerActive.value = false
                            recalculateMultiplier()
                        }
                    }

                    val totalDuration = audioPlayer.getDurationMs()
                    if (pos >= totalDuration || (totalDuration > 5000L && pos >= totalDuration - 500L)) {
                        finishMatch()
                        break
                    }

                    if (_rockMeter.value <= 0.01f) {
                        finishMatch(failed = true)
                        break
                    }
                }
                delay(14L)
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
        if (_playerStats.value.controlMode == ControlMode.DIRECT_TOUCH) {
            attemptHit(lane)
        }
    }

    fun onFretUp(lane: Int) {
        _activeFretLanes.value = _activeFretLanes.value - lane
    }

    fun onStrumBar() {
        val pressed = _activeFretLanes.value
        if (pressed.isNotEmpty()) {
            for (lane in pressed) {
                attemptHit(lane)
            }
        } else {
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
            onMiss(lane)
        }
    }

    private fun onHit(lane: Int, rating: HitRating, isStarPower: Boolean, diffMs: Long) {
        totalHitCount++
        _lastHitRating.value = rating

        _combo.value = _combo.value + 1
        if (_combo.value > _maxCombo.value) {
            _maxCombo.value = _combo.value
        }
        recalculateMultiplier()

        val baseScore = (rating.score * selectedDifficulty.scoreMultiplier * adaptiveEngine.getScoreBoost()).toInt()
        val addedScore = baseScore * _multiplier.value
        _score.value = _score.value + addedScore

        _rockMeter.value = (_rockMeter.value + rating.healthDelta).coerceIn(0f, 1f)

        if (isStarPower) {
            _starPowerEnergy.value = (_starPowerEnergy.value + 0.12f).coerceAtMost(1.0f)
        }

        adaptiveEngine.onNoteResult(rating, diffMs)

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

        val penalty = adaptiveEngine.getMissPenalty()
        _rockMeter.value = (_rockMeter.value - penalty).coerceIn(0f, 1f)

        adaptiveEngine.onNoteResult(HitRating.MISS, 0L)
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
            isAdaptiveUsed = _playerStats.value.glassAdaptiveEnabled,
            isCompetitive = isCompetitiveMatch
        )

        lastMatchResult = result

        val current = _playerStats.value
        val newAchievements = buildList {
            addAll(current.achievementsUnlocked)
            add("ach_first_show")
            if (stars >= 5) add("ach_five_stars")
            if (_maxCombo.value >= 30) add("ach_combo_30")
        }.distinct()

        val unlockedStageCount = careerVenues.count { (current.unlockedStars + stars) >= it.requiredStars }

        _playerStats.value = current.copy(
            coins = current.coins + coinsEarned,
            totalScore = current.totalScore + _score.value,
            notesHitTotal = current.notesHitTotal + totalHitCount,
            maxStreakOverall = maxOf(current.maxStreakOverall, _maxCombo.value),
            songsCompleted = current.songsCompleted + (if (!failed) 1 else 0),
            unlockedStars = current.unlockedStars + stars,
            competitiveMatches = current.competitiveMatches + (if (isCompetitiveMatch) 1 else 0),
            competitiveWins = current.competitiveWins + (if (isCompetitiveMatch && !failed && stars >= 3) 1 else 0),
            careerStageMax = maxOf(current.careerStageMax, unlockedStageCount),
            achievementsUnlocked = newAchievements
        )

        val repo = accountRepo
        if (repo != null && repo.currentUidOrNull() != null && _playerStats.value.nickname.isNotBlank()) {
            viewModelScope.launch {
                repo.recordProgressionMatch(result)
                repo.syncPlayerStats(_playerStats.value).onSuccess { cloud ->
                    _playerStats.value = cloud.toPlayerStats()
                }
                refreshAccountSubcollections()
            }
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
        } catch (_: Exception) {}
    }

    fun unlockCharacter(id: String, price: Int) {
        if (_playerStats.value.coins >= price) {
            val unlocked = (_playerStats.value.unlockedCosmetics + id).distinct()
            _playerStats.value = _playerStats.value.copy(
                coins = _playerStats.value.coins - price,
                selectedCharacterId = id,
                unlockedCosmetics = unlocked
            )
            triggerBackgroundCloudSync()
        }
    }

    fun selectCharacter(id: String) {
        _playerStats.value = _playerStats.value.copy(selectedCharacterId = id)
        triggerBackgroundCloudSync()
    }

    fun unlockGuitar(id: String, price: Int) {
        if (_playerStats.value.coins >= price) {
            val unlocked = (_playerStats.value.unlockedCosmetics + id).distinct()
            _playerStats.value = _playerStats.value.copy(
                coins = _playerStats.value.coins - price,
                selectedGuitarId = id,
                unlockedCosmetics = unlocked
            )
            triggerBackgroundCloudSync()
        }
    }

    fun selectGuitar(id: String) {
        _playerStats.value = _playerStats.value.copy(selectedGuitarId = id)
        triggerBackgroundCloudSync()
    }

    fun unlockGear(instrumentType: Instrument, id: String, price: Int) {
        if (_playerStats.value.coins >= price) {
            val updatedCoins = _playerStats.value.coins - price
            val unlocked = (_playerStats.value.unlockedCosmetics + id).distinct()
            _playerStats.value = when (instrumentType) {
                Instrument.GUITAR -> _playerStats.value.copy(coins = updatedCoins, selectedGuitarId = id, unlockedCosmetics = unlocked)
                Instrument.BASS -> _playerStats.value.copy(coins = updatedCoins, selectedBassId = id, unlockedCosmetics = unlocked)
                Instrument.DRUMS -> _playerStats.value.copy(coins = updatedCoins, selectedDrumsId = id, unlockedCosmetics = unlocked)
                Instrument.VOCALS -> _playerStats.value.copy(coins = updatedCoins, selectedVocalMicId = id, unlockedCosmetics = unlocked)
            }
            triggerBackgroundCloudSync()
        }
    }

    fun selectGear(instrumentType: Instrument, id: String) {
        _playerStats.value = when (instrumentType) {
            Instrument.GUITAR -> _playerStats.value.copy(selectedGuitarId = id)
            Instrument.BASS -> _playerStats.value.copy(selectedBassId = id)
            Instrument.DRUMS -> _playerStats.value.copy(selectedDrumsId = id)
            Instrument.VOCALS -> _playerStats.value.copy(selectedVocalMicId = id)
        }
        triggerBackgroundCloudSync()
    }

    fun stop() {
        audioPlayer.stop()
        gameLoopJob?.cancel()
        previewProgressJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
