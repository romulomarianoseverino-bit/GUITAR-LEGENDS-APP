package com.example.guitarlegends.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.guitarlegends.analysis.AdaptiveEngine
import com.example.guitarlegends.analysis.AudioAnalyzer
import com.example.guitarlegends.audio.AudioPlayerEngine
import com.example.guitarlegends.audio.SoundFxSynthesizer
import com.example.guitarlegends.data.*
import com.example.guitarlegends.engine.ParticleSystem
import com.example.guitarlegends.input.ConnectedControllerInfo
import com.example.guitarlegends.input.GamepadInputManager
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
import java.util.UUID
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
    val localProgressRepository = LocalPlayerProgressRepository(context)
    val updateManager = UpdateManager(context)
    val gamepadManager = GamepadInputManager(context)
    val highwayImageManager = HighwayImageManager(context)

    val controllerInfo: StateFlow<ConnectedControllerInfo> = gamepadManager.controllerInfo
    val lastCapturedKeyCode: StateFlow<Int?> = gamepadManager.lastCapturedKeyCode

    private val _studioActionMessage = MutableStateFlow<String?>(null)
    val studioActionMessage: StateFlow<String?> = _studioActionMessage.asStateFlow()

    private val _highwayUploadError = MutableStateFlow<String?>(null)
    val highwayUploadError: StateFlow<String?> = _highwayUploadError.asStateFlow()

    private var authJob: Job? = null

    private val firebaseAuth: FirebaseAuth? = FirebaseClientProvider.getAuthOrNull(context)
    private val accountRepo: AccountCloudRepository? = run {
        val db = FirebaseClientProvider.getFirestoreOrNull(context)
        val auth = firebaseAuth
        if (db != null && auth != null) AccountCloudRepository(db, auth) else null
    }
    private val multiplayerRepo: MultiplayerCloudRepository? = run {
        val db = FirebaseClientProvider.getFirestoreOrNull(context)
        val auth = firebaseAuth
        if (db != null && auth != null) MultiplayerCloudRepository(db, auth) else null
    }

    // Navegação
    private val _currentScreen = MutableStateFlow<GameScreen>(GameScreen.Splash)
    val currentScreen: StateFlow<GameScreen> = _currentScreen.asStateFlow()
    private val screenStack = mutableListOf<GameScreen>()

    // Autenticação & Nuvem (Opcional — Modo Visitante liberado por padrão)
    private val _authUiState = MutableStateFlow<AuthUiState>(AuthUiState.SignedOut)
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    private val _isAuthBusy = MutableStateFlow(false)
    val isAuthBusy: StateFlow<Boolean> = _isAuthBusy.asStateFlow()

    private val _nicknameAvailability = MutableStateFlow<Boolean?>(null)
    val nicknameAvailability: StateFlow<Boolean?> = _nicknameAvailability.asStateFlow()

    private val _cloudSyncStatus = MutableStateFlow("Modo Visitante (Salvamento Local)")
    val cloudSyncStatus: StateFlow<String> = _cloudSyncStatus.asStateFlow()

    private val _hasUnlinkedGuestProgress = MutableStateFlow(false)
    val hasUnlinkedGuestProgress: StateFlow<Boolean> = _hasUnlinkedGuestProgress.asStateFlow()

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

    // Multiplayer Online (Firestore em tempo real)
    private val _multiplayerRoom = MutableStateFlow<MultiplayerRoomCloud?>(null)
    val multiplayerRoom: StateFlow<MultiplayerRoomCloud?> = _multiplayerRoom.asStateFlow()

    private val _multiplayerParticipants = MutableStateFlow<List<MultiplayerParticipantCloud>>(emptyList())
    val multiplayerParticipants: StateFlow<List<MultiplayerParticipantCloud>> = _multiplayerParticipants.asStateFlow()

    private val _multiplayerMode = MutableStateFlow(MultiplayerMode.BATTLE_1V1)
    val multiplayerMode: StateFlow<MultiplayerMode> = _multiplayerMode.asStateFlow()

    private val _isMultiplayerBusy = MutableStateFlow(false)
    val isMultiplayerBusy: StateFlow<Boolean> = _isMultiplayerBusy.asStateFlow()

    private val _multiplayerError = MutableStateFlow<String?>(null)
    val multiplayerError: StateFlow<String?> = _multiplayerError.asStateFlow()

    private val _multiplayerConnectionWarning = MutableStateFlow<String?>(null)
    val multiplayerConnectionWarning: StateFlow<String?> = _multiplayerConnectionWarning.asStateFlow()

    private val _disconnectedPeerNicknames = MutableStateFlow<List<String>>(emptyList())
    val disconnectedPeerNicknames: StateFlow<List<String>> = _disconnectedPeerNicknames.asStateFlow()

    private var roomObserveJob: Job? = null
    private var participantsObserveJob: Job? = null
    private var heartbeatJob: Job? = null
    private var hasAutoStartedRoomMatch: Boolean = false

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
        val localSnapshot = localProgressRepository.loadSnapshot()
        _playerStats.value = localSnapshot.stats
        applyCurrentAudioSettings()
        _hasUnlinkedGuestProgress.value = localSnapshot.hasUnlinkedGuestProgress
        if (localSnapshot.localRewards.isNotEmpty()) {
            _rewardHistory.value = localSnapshot.localRewards.map { it.toCloudClaim() }
        }
        if (localSnapshot.localMatches.isNotEmpty()) {
            _progressionHistory.value = localSnapshot.localMatches.map { it.toCloudEntry() }
        }

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
            resolveInitialAuthSilently()
        }
    }

    fun enterGame() {
        navigateTo(GameScreen.MainMenu, pushStack = false)
    }

    private suspend fun resolveInitialAuthSilently() {
        val currentUser = firebaseAuth?.currentUser
        if (firebaseAuth == null || accountRepo == null || currentUser == null) {
            _authUiState.value = AuthUiState.SignedOut
            _cloudSyncStatus.value = "Modo Visitante (Salvamento Local)"
            return
        }

        // Mantém entrada livre sem redirecionamento forçado para tela de login
        loadAuthenticatedAccount(
            uid = currentUser.uid,
            email = currentUser.email.orEmpty(),
            mergeGuestProgress = false
        )
    }

    private suspend fun loadAuthenticatedAccount(
        uid: String,
        email: String,
        mergeGuestProgress: Boolean = true
    ) {
        _authUiState.value = AuthUiState.Loading
        val repo = accountRepo ?: return
        val accountRes = repo.fetchAccount()
        accountRes.onSuccess { remote ->
            if (remote == null || remote.nickname.isBlank()) {
                _authUiState.value = AuthUiState.NeedsNickname(uid, email)
                _playerStats.value = _playerStats.value.copy(email = email)
                _cloudSyncStatus.value = "Defina um nickname para concluir a conta na nuvem"
            } else {
                val guestSnapshot = localProgressRepository.loadSnapshot()
                val remoteStats = remote.toPlayerStats()
                val finalStats = if (mergeGuestProgress && guestSnapshot.hasUnlinkedGuestProgress) {
                    val merged = LocalPlayerProgressRepository.mergeGuestIntoCloud(remoteStats, guestSnapshot)
                    val synced = repo.syncPlayerStats(merged).getOrNull()?.toPlayerStats() ?: merged
                    uploadLocalMatchesToCloud(repo, guestSnapshot.localMatches)
                    _cloudSyncStatus.value = "Conta vinculada e progresso local preservado"
                    synced
                } else {
                    _cloudSyncStatus.value = "Sincronizado na nuvem"
                    remoteStats
                }

                _playerStats.value = finalStats
                applyCurrentAudioSettings()
                _hasUnlinkedGuestProgress.value = false
                localProgressRepository.saveSnapshot(
                    LocalProgressSnapshot(
                        stats = finalStats,
                        hasUnlinkedGuestProgress = false,
                        hasLocalCustomizationChanges = false
                    )
                )
                _authUiState.value = AuthUiState.Authenticated(
                    uid = uid,
                    email = remote.email.ifBlank { email },
                    nickname = remote.nickname
                )
                refreshAccountSubcollections()
                checkForUpdates(silent = true)
            }
        }.onFailure { err ->
            _authErrorMessage.value = "Erro ao carregar conta na nuvem: ${err.message}"
            _authUiState.value = AuthUiState.SignedOut
            _cloudSyncStatus.value = "Modo Visitante (Falha na rede ao consultar nuvem)"
        }
    }

    private suspend fun uploadLocalMatchesToCloud(
        repo: AccountCloudRepository,
        localMatches: List<LocalMatchRecord>
    ) {
        if (localMatches.isEmpty()) return
        val availableTracks = _tracks.value
        for (record in localMatches.take(25)) {
            val track = availableTracks.find { it.id == record.trackId }
                ?: AudioAnalyzer.BUILT_IN_TRACKS.first().copy(
                    id = record.trackId.ifBlank { "local_track" },
                    title = record.trackTitle.ifBlank { "Música Local" }
                )
            repo.recordProgressionMatch(record.toMatchResult(track), entryIdOverride = record.id)
        }
    }

    fun signInWithGoogleIdToken(idToken: String, mergeGuestProgress: Boolean = true) {
        val auth = firebaseAuth ?: run {
            _authErrorMessage.value = "Serviço Firebase não inicializado neste ambiente."
            return
        }
        authJob?.cancel()
        authJob = viewModelScope.launch {
            _isAuthBusy.value = true
            _authErrorMessage.value = null
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                val user = result.user
                if (user == null) {
                    _authErrorMessage.value = "Falha ao obter dados do usuário autenticado."
                    _authUiState.value = AuthUiState.SignedOut
                } else {
                    loadAuthenticatedAccount(
                        uid = user.uid,
                        email = user.email.orEmpty(),
                        mergeGuestProgress = mergeGuestProgress
                    )
                }
            } catch (e: Exception) {
                _authErrorMessage.value = "Falha na autenticação Google: ${e.message}"
                _authUiState.value = AuthUiState.SignedOut
            } finally {
                _isAuthBusy.value = false
            }
        }
    }

    fun cancelPendingAuth() {
        authJob?.cancel()
        authJob = null
        _isAuthBusy.value = false
        _authErrorMessage.value = null
        _nicknameAvailability.value = null
        _authUiState.value = AuthUiState.SignedOut
        _cloudSyncStatus.value = "Modo Visitante (Salvamento Local)"
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
            val guestSnapshot = localProgressRepository.loadSnapshot()
            val res = repo.claimNicknameAndSaveAccount(nickname, _playerStats.value)
            res.onSuccess { cloud ->
                uploadLocalMatchesToCloud(repo, guestSnapshot.localMatches)
                val cloudStats = cloud.toPlayerStats()
                _playerStats.value = cloudStats
                _hasUnlinkedGuestProgress.value = false
                localProgressRepository.saveSnapshot(
                    LocalProgressSnapshot(
                        stats = cloudStats,
                        hasUnlinkedGuestProgress = false,
                        hasLocalCustomizationChanges = false
                    )
                )
                _authUiState.value = AuthUiState.Authenticated(cloud.userId, cloud.email, cloud.nickname)
                _cloudSyncStatus.value = "Conta salva e sincronizada na nuvem"
                refreshAccountSubcollections()
                checkForUpdates(silent = true)
                if (_currentScreen.value is GameScreen.Auth) {
                    navigateTo(GameScreen.MainMenu, pushStack = false)
                }
            }.onFailure { err ->
                _authErrorMessage.value = err.message ?: "Não foi possível reservar este nickname."
            }
            _isAuthBusy.value = false
        }
    }

    fun updateGuestNickname(nickname: String) {
        val clean = nickname.trim().take(24)
        if (clean.length < 2) return
        _playerStats.value = _playerStats.value.copy(nickname = clean)
        persistLocalMutation(customizationChanged = true)
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
                val synced = cloud.toPlayerStats()
                _playerStats.value = synced
                _hasUnlinkedGuestProgress.value = false
                localProgressRepository.saveSnapshot(
                    localProgressRepository.loadSnapshot().copy(
                        stats = synced,
                        hasUnlinkedGuestProgress = false,
                        hasLocalCustomizationChanges = false
                    )
                )
                _cloudSyncStatus.value = "Sincronizado (rev. ${cloud.syncRevision})"
            }.onFailure { err ->
                _cloudSyncStatus.value = "Falha na sincronização: ${err.message}"
            }
        }
    }

    private fun persistLocalMutation(
        customizationChanged: Boolean = false,
        newMatchRecord: LocalMatchRecord? = null
    ) {
        val isSignedIn = accountRepo?.currentUidOrNull() != null &&
            _authUiState.value is AuthUiState.Authenticated
        val current = localProgressRepository.loadSnapshot()
        val updatedMatches = if (newMatchRecord != null && !isSignedIn) {
            (listOf(newMatchRecord) + current.localMatches).take(25)
        } else {
            current.localMatches
        }
        val nextUnlinked = if (isSignedIn) false else true
        val nextCustomization = if (isSignedIn) false else (current.hasLocalCustomizationChanges || customizationChanged)
        _hasUnlinkedGuestProgress.value = nextUnlinked

        localProgressRepository.saveSnapshot(
            current.copy(
                stats = _playerStats.value,
                hasUnlinkedGuestProgress = nextUnlinked,
                hasLocalCustomizationChanges = nextCustomization,
                localMatches = updatedMatches
            )
        )
        if (!isSignedIn && newMatchRecord != null) {
            _progressionHistory.value = updatedMatches.map { it.toCloudEntry() }
        }
    }

    private fun triggerBackgroundCloudSync(customizationChanged: Boolean = false) {
        persistLocalMutation(customizationChanged = customizationChanged)
        val repo = accountRepo ?: return
        if (repo.currentUidOrNull() == null || _playerStats.value.nickname.isBlank()) return
        viewModelScope.launch {
            repo.syncPlayerStats(_playerStats.value).onSuccess { cloud ->
                val synced = cloud.toPlayerStats()
                _playerStats.value = synced
                localProgressRepository.saveSnapshot(
                    localProgressRepository.loadSnapshot().copy(
                        stats = synced,
                        hasUnlinkedGuestProgress = false,
                        hasLocalCustomizationChanges = false
                    )
                )
                _cloudSyncStatus.value = "Nuvem atualizada (rev. ${cloud.syncRevision})"
            }
        }
    }

    fun claimDailyReward() {
        val repo = accountRepo
        val isCloudAuthenticated = repo != null &&
            repo.currentUidOrNull() != null &&
            _authUiState.value is AuthUiState.Authenticated

        if (!isCloudAuthenticated) {
            // Modo Visitante: resgate local com controle diário em UTC e persistência garantida
            val helperRepo = repo ?: AccountCloudRepository(
                firestore = FirebaseClientProvider.getFirestoreOrNull(context) ?: run {
                    // Fallback seguro para datas UTC mesmo sem Firestore inicializado
                    claimDailyRewardLocallyOnly()
                    return
                },
                auth = firebaseAuth ?: run {
                    claimDailyRewardLocallyOnly()
                    return
                }
            )
            claimDailyRewardLocallyOnly(
                todayKey = helperRepo.utcTodayKey(),
                yesterdayKey = helperRepo.utcYesterdayKey()
            )
            return
        }

        viewModelScope.launch {
            _isAuthBusy.value = true
            _rewardActionMessage.value = null
            val res = repo!!.claimDailyReward(_playerStats.value)
            res.onSuccess { (claim, updatedUser) ->
                val updatedStats = updatedUser.toPlayerStats()
                _playerStats.value = updatedStats
                localProgressRepository.saveSnapshot(
                    localProgressRepository.loadSnapshot().copy(
                        stats = updatedStats,
                        hasUnlinkedGuestProgress = false
                    )
                )
                _rewardHistory.value = listOf(claim) + _rewardHistory.value.filter { it.dayKey != claim.dayKey }
                _rewardActionMessage.value = "Dia ${claim.dayNumber} resgatado: ${claim.rewardTitle} (+${claim.coinsGranted} LC)!"
            }.onFailure { err ->
                _rewardActionMessage.value = err.message ?: "Não foi possível resgatar a recompensa."
            }
            _isAuthBusy.value = false
        }
    }

    private fun claimDailyRewardLocallyOnly(
        todayKey: String = utcDayKeyForOffset(0),
        yesterdayKey: String = utcDayKeyForOffset(-1)
    ) {
        _rewardActionMessage.value = null
        val res = localProgressRepository.claimGuestDailyReward(todayKey, yesterdayKey)
        res.onSuccess { (record, snapshot) ->
            _playerStats.value = snapshot.stats
            _hasUnlinkedGuestProgress.value = snapshot.hasUnlinkedGuestProgress
            _rewardHistory.value = snapshot.localRewards.map { it.toCloudClaim() }
            _rewardActionMessage.value =
                "Dia ${record.dayNumber} resgatado (Visitante): ${record.rewardTitle} (+${record.coinsGranted} LC)!"
        }.onFailure { err ->
            _rewardActionMessage.value = err.message ?: "Não foi possível resgatar a recompensa."
        }
    }

    private fun utcDayKeyForOffset(daysOffset: Int): String {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            if (daysOffset != 0) add(java.util.Calendar.DAY_OF_YEAR, daysOffset)
        }
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        return sdf.format(cal.time)
    }

    fun clearRewardActionMessage() {
        _rewardActionMessage.value = null
    }

    fun signOut() {
        authJob?.cancel()
        authJob = null
        _isAuthBusy.value = false
        viewModelScope.launch {
            leaveMultiplayerRoom()
            stop()
            stopPreview()
            firebaseAuth?.signOut()
            GoogleSignInHelper.clearCredentials(context)
            val freshGuest = PlayerStats()
            _playerStats.value = freshGuest
            applyCurrentAudioSettings()
            _hasUnlinkedGuestProgress.value = false
            localProgressRepository.saveSnapshot(
                LocalProgressSnapshot(
                    stats = freshGuest,
                    hasUnlinkedGuestProgress = false,
                    hasLocalCustomizationChanges = false
                )
            )
            _rewardHistory.value = emptyList()
            _progressionHistory.value = emptyList()
            _cloudSyncStatus.value = "Modo Visitante (Sessão encerrada)"
            _authUiState.value = AuthUiState.SignedOut
            if (_currentScreen.value is GameScreen.Auth) {
                screenStack.clear()
                _currentScreen.value = GameScreen.MainMenu
            }
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
        triggerBackgroundCloudSync(customizationChanged = true)
    }

    fun equipGuitar(guitarId: String) {
        _playerStats.value = _playerStats.value.copy(selectedGuitarId = guitarId)
        triggerBackgroundCloudSync(customizationChanged = true)
    }

    fun equipBass(bassId: String) {
        _playerStats.value = _playerStats.value.copy(selectedBassId = bassId)
        triggerBackgroundCloudSync(customizationChanged = true)
    }

    fun equipDrums(drumsId: String) {
        _playerStats.value = _playerStats.value.copy(selectedDrumsId = drumsId)
        triggerBackgroundCloudSync(customizationChanged = true)
    }

    fun equipVocalMic(micId: String) {
        _playerStats.value = _playerStats.value.copy(selectedVocalMicId = micId)
        triggerBackgroundCloudSync(customizationChanged = true)
    }

    fun equipHighwayTheme(themeId: String) {
        _playerStats.value = _playerStats.value.copy(selectedHighwayThemeId = themeId)
        triggerBackgroundCloudSync(customizationChanged = true)
    }

    fun updateCustomRockerPhoto(uriString: String) {
        val current = _playerStats.value.customRocker
        _playerStats.value = _playerStats.value.copy(
            customRocker = current.copy(photoUri = uriString)
        )
        triggerBackgroundCloudSync(customizationChanged = true)
    }

    fun updateCustomRockerMethod(method: CharacterCreationMethod) {
        val current = _playerStats.value.customRocker
        _playerStats.value = _playerStats.value.copy(
            customRocker = current.copy(creationMethod = method)
        )
        triggerBackgroundCloudSync(customizationChanged = true)
    }

    fun updateCustomRockerStyle(style: CharacterArtStyle) {
        val current = _playerStats.value.customRocker
        _playerStats.value = _playerStats.value.copy(
            customRocker = current.copy(artStyle = style)
        )
        triggerBackgroundCloudSync(customizationChanged = true)
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
        triggerBackgroundCloudSync(customizationChanged = true)
    }

    // ---- Estúdio 3D, Loja, Desafios e Presets ----

    fun clearStudioMessages() {
        _studioActionMessage.value = null
        _highwayUploadError.value = null
    }

    fun updateRockerBodyAndSkin(bodyModel: String, skinToneHex: Long) {
        val current = _playerStats.value
        _playerStats.value = current.copy(
            selectedCharacterId = "custom_rocker",
            studioProfile = current.studioProfile.copy(
                rockerBodyModel = bodyModel,
                skinToneHex = skinToneHex
            )
        )
        triggerBackgroundCloudSync(customizationChanged = true)
    }

    fun updateRockerStudioColors(
        hairColorHex: Long,
        topColorHex: Long,
        pantsColorHex: Long,
        shoesColorHex: Long,
        accessoryColorHex: Long
    ) {
        val current = _playerStats.value
        _playerStats.value = current.copy(
            selectedCharacterId = "custom_rocker",
            customRocker = current.customRocker.copy(hairColorHex = hairColorHex),
            studioProfile = current.studioProfile.copy(
                topColorHex = topColorHex,
                pantsColorHex = pantsColorHex,
                shoesColorHex = shoesColorHex,
                accessoryColorHex = accessoryColorHex,
                fullOutfitSkinId = "outfit_skin_none"
            )
        )
        triggerBackgroundCloudSync(customizationChanged = true)
    }

    fun updateRockerExpression(expression: StagePoseExpression) {
        val current = _playerStats.value
        _playerStats.value = current.copy(
            studioProfile = current.studioProfile.copy(stageExpression = expression)
        )
        triggerBackgroundCloudSync(customizationChanged = true)
    }

    fun equipOutfitSkin(skinId: String) {
        val current = _playerStats.value
        _playerStats.value = current.copy(
            selectedCharacterId = "custom_rocker",
            studioProfile = current.studioProfile.copy(fullOutfitSkinId = skinId)
        )
        triggerBackgroundCloudSync(customizationChanged = true)
    }

    fun getInstrumentConfig(stats: PlayerStats, instrument: Instrument): InstrumentCustomConfig {
        return when (instrument) {
            Instrument.GUITAR -> stats.studioProfile.guitarConfig
            Instrument.BASS -> stats.studioProfile.bassConfig
            Instrument.DRUMS -> stats.studioProfile.drumsConfig
            Instrument.VOCALS -> stats.studioProfile.vocalConfig
        }
    }

    fun updateInstrumentCustomConfig(instrument: Instrument, config: InstrumentCustomConfig) {
        val studio = _playerStats.value.studioProfile
        val updatedStudio = when (instrument) {
            Instrument.GUITAR -> studio.copy(guitarConfig = config)
            Instrument.BASS -> studio.copy(bassConfig = config)
            Instrument.DRUMS -> studio.copy(drumsConfig = config)
            Instrument.VOCALS -> studio.copy(vocalConfig = config)
        }
        _playerStats.value = _playerStats.value.copy(studioProfile = updatedStudio)
        triggerBackgroundCloudSync(customizationChanged = true)
    }

    fun unlockAndEquipCosmetic(item: CosmeticShopItem): Boolean {
        val current = _playerStats.value
        val alreadyUnlocked = item.unlockedByDefault || current.unlockedCosmetics.contains(item.id)
        if (!alreadyUnlocked && current.coins < item.priceCoins) {
            _studioActionMessage.value = "Moedas insuficientes (${item.priceCoins} LC necessários)."
            return false
        }
        val nextCoins = if (alreadyUnlocked) current.coins else (current.coins - item.priceCoins)
        val nextUnlocked = (current.unlockedCosmetics + item.id).distinct()
        val withUnlock = current.copy(coins = nextCoins, unlockedCosmetics = nextUnlocked)
        _playerStats.value = StudioCatalog.applyCosmeticLook(withUnlock, item.id)
        soundFx.playUiClick(isConfirm = true)
        _studioActionMessage.value = if (alreadyUnlocked) {
            "${item.title} equipado!"
        } else {
            "${item.title} desbloqueado e equipado (-${item.priceCoins} LC)!"
        }
        triggerBackgroundCloudSync(customizationChanged = true)
        return true
    }

    fun unlockHighwayStamp(stamp: HighwayStampItem) {
        val current = _playerStats.value
        val unlocked = stamp.unlockedByDefault || current.unlockedCosmetics.contains(stamp.id)
        if (unlocked) {
            selectHighwayStamp(stamp.id)
            return
        }
        if (current.coins < stamp.priceCoins) {
            _studioActionMessage.value = "Moedas insuficientes para esta estampa (${stamp.priceCoins} LC)."
            return
        }
        val studio = current.studioProfile
        _playerStats.value = current.copy(
            coins = current.coins - stamp.priceCoins,
            unlockedCosmetics = (current.unlockedCosmetics + stamp.id).distinct(),
            studioProfile = studio.copy(
                highwayConfig = studio.highwayConfig.copy(
                    selectedStampId = stamp.id,
                    customImagePath = null
                )
            )
        )
        soundFx.playUiClick(isConfirm = true)
        _studioActionMessage.value = "Estampa '${stamp.title}' desbloqueada!"
        triggerBackgroundCloudSync(customizationChanged = true)
    }

    fun selectHighwayStamp(stampId: String) {
        val studio = _playerStats.value.studioProfile
        _playerStats.value = _playerStats.value.copy(
            studioProfile = studio.copy(
                highwayConfig = studio.highwayConfig.copy(
                    selectedStampId = stampId,
                    customImagePath = null
                )
            )
        )
        soundFx.playUiClick()
        triggerBackgroundCloudSync(customizationChanged = true)
    }

    fun updateHighwayStampOpacity(opacity: Float) {
        val studio = _playerStats.value.studioProfile
        _playerStats.value = _playerStats.value.copy(
            studioProfile = studio.copy(
                highwayConfig = studio.highwayConfig.copy(
                    stampOpacity = opacity.coerceIn(0.15f, 0.90f)
                )
            )
        )
        triggerBackgroundCloudSync(customizationChanged = true)
    }

    fun importCustomHighwayImage(uri: Uri) {
        _highwayUploadError.value = null
        viewModelScope.launch {
            val res = highwayImageManager.importAndValidateHighwayImage(uri)
            res.onSuccess { savedPath ->
                val studio = _playerStats.value.studioProfile
                _playerStats.value = _playerStats.value.copy(
                    studioProfile = studio.copy(
                        highwayConfig = studio.highwayConfig.copy(
                            selectedStampId = "stamp_custom",
                            customImagePath = savedPath
                        )
                    )
                )
                soundFx.playUiClick(isConfirm = true)
                _studioActionMessage.value = "Imagem aplicada com segurança à esteira!"
                triggerBackgroundCloudSync(customizationChanged = true)
            }.onFailure { err ->
                _highwayUploadError.value = err.message ?: "Imagem inválida para a esteira."
            }
        }
    }

    fun clearCustomHighwayImage() {
        val currentPath = _playerStats.value.studioProfile.highwayConfig.customImagePath
        highwayImageManager.deleteCustomImage(currentPath)
        val studio = _playerStats.value.studioProfile
        _playerStats.value = _playerStats.value.copy(
            studioProfile = studio.copy(
                highwayConfig = studio.highwayConfig.copy(
                    selectedStampId = "stamp_none",
                    customImagePath = null
                )
            )
        )
        _highwayUploadError.value = null
        triggerBackgroundCloudSync(customizationChanged = true)
    }

    fun saveAppearancePreset(rawName: String) {
        val cleanName = rawName.trim().ifBlank { "Visual Palco #${_playerStats.value.studioProfile.savedPresets.size + 1}" }.take(28)
        val cur = _playerStats.value
        val studio = cur.studioProfile
        val preset = AppearancePreset(
            id = "preset_${UUID.randomUUID().toString().take(8)}",
            name = cleanName,
            selectedCharacterId = cur.selectedCharacterId,
            rockerBodyModel = studio.rockerBodyModel,
            skinToneHex = studio.skinToneHex,
            customRocker = cur.customRocker,
            topColorHex = studio.topColorHex,
            pantsColorHex = studio.pantsColorHex,
            shoesColorHex = studio.shoesColorHex,
            stageExpression = studio.stageExpression,
            fullOutfitSkinId = studio.fullOutfitSkinId,
            guitarConfig = studio.guitarConfig,
            selectedHighwayThemeId = cur.selectedHighwayThemeId,
            highwayConfig = studio.highwayConfig
        )
        val updatedPresets = (listOf(preset) + studio.savedPresets).take(12)
        _playerStats.value = cur.copy(
            studioProfile = studio.copy(savedPresets = updatedPresets)
        )
        soundFx.playUiClick(isConfirm = true)
        _studioActionMessage.value = "Preset '$cleanName' salvo!"
        triggerBackgroundCloudSync(customizationChanged = true)
    }

    fun applyAppearancePreset(presetId: String) {
        val cur = _playerStats.value
        val preset = cur.studioProfile.savedPresets.find { it.id == presetId } ?: return
        _playerStats.value = cur.copy(
            selectedCharacterId = preset.selectedCharacterId,
            customRocker = preset.customRocker,
            selectedHighwayThemeId = preset.selectedHighwayThemeId,
            studioProfile = cur.studioProfile.copy(
                rockerBodyModel = preset.rockerBodyModel,
                skinToneHex = preset.skinToneHex,
                topColorHex = preset.topColorHex,
                pantsColorHex = preset.pantsColorHex,
                shoesColorHex = preset.shoesColorHex,
                stageExpression = preset.stageExpression,
                fullOutfitSkinId = preset.fullOutfitSkinId,
                guitarConfig = preset.guitarConfig,
                highwayConfig = preset.highwayConfig
            )
        )
        soundFx.playUiClick(isConfirm = true)
        _studioActionMessage.value = "Preset '${preset.name}' aplicado!"
        triggerBackgroundCloudSync(customizationChanged = true)
    }

    fun deleteAppearancePreset(presetId: String) {
        val cur = _playerStats.value
        val updated = cur.studioProfile.savedPresets.filterNot { it.id == presetId }
        _playerStats.value = cur.copy(
            studioProfile = cur.studioProfile.copy(savedPresets = updated)
        )
        triggerBackgroundCloudSync(customizationChanged = true)
    }

    fun claimChallengeReward(challenge: ChallengeItem) {
        val cur = _playerStats.value
        if (cur.studioProfile.claimedChallenges.contains(challenge.id)) return
        val progress = challenge.progressProvider(cur)
        if (progress < challenge.targetValue) {
            _studioActionMessage.value = "Progresso insuficiente: $progress/${challenge.targetValue}"
            return
        }
        val updatedClaimed = (cur.studioProfile.claimedChallenges + challenge.id).distinct()
        val coinsGranted = challenge.rewardCoins.coerceIn(0, 100)
        _playerStats.value = cur.copy(
            coins = cur.coins + coinsGranted,
            studioProfile = cur.studioProfile.copy(claimedChallenges = updatedClaimed)
        )
        soundFx.playUiClick(isConfirm = true)
        _studioActionMessage.value = "Desafio '${challenge.title}' resgatado: +${coinsGranted} LC!"
        triggerBackgroundCloudSync(customizationChanged = true)
    }

    // ---- Mixer de Áudio & Volume ----

    private fun applyCurrentAudioSettings() {
        val audio = _playerStats.value.studioProfile.audioSettings
        audioPlayer.applyAudioSettings(audio)
        soundFx.applyAudioSettings(audio)
    }

    fun updateAudioSettings(settings: AudioSettings) {
        val clamped = settings.copy(
            masterVolume = settings.masterVolume.coerceIn(0f, 1f),
            musicVolume = settings.musicVolume.coerceIn(0f, 1f),
            gameplaySfxVolume = settings.gameplaySfxVolume.coerceIn(0f, 1f),
            uiSfxVolume = settings.uiSfxVolume.coerceIn(0f, 1f),
            stageFxVolume = settings.stageFxVolume.coerceIn(0f, 1f)
        )
        _playerStats.value = _playerStats.value.copy(
            studioProfile = _playerStats.value.studioProfile.copy(audioSettings = clamped)
        )
        applyCurrentAudioSettings()
        triggerBackgroundCloudSync(customizationChanged = true)
    }

    fun toggleMuteAudio() {
        val current = _playerStats.value.studioProfile.audioSettings
        updateAudioSettings(current.copy(isMuted = !current.isMuted))
    }

    fun previewAudioMixer() {
        applyCurrentAudioSettings()
        soundFx.playMixerPreview()
    }

    fun resetAudioSettings() {
        updateAudioSettings(AudioSettings())
        soundFx.playUiClick(isConfirm = true)
    }

    fun playUiClick(isConfirm: Boolean = false) {
        soundFx.playUiClick(isConfirm)
    }

    // ---- Controle Bluetooth / DualShock 4 & Remapeamento ----

    fun updateControllerSettings(settings: ControllerSettings) {
        _playerStats.value = _playerStats.value.copy(
            studioProfile = _playerStats.value.studioProfile.copy(controllerSettings = settings)
        )
        triggerBackgroundCloudSync(customizationChanged = true)
    }

    fun assignControllerLaneKey(laneIndex: Int, keyCode: Int) {
        if (laneIndex !in 0..4) return
        val cur = _playerStats.value.studioProfile.controllerSettings
        val mutable = cur.laneKeyCodes.toMutableList()
        while (mutable.size < 5) mutable.add(ControllerSettings.DEFAULT_PS4_LANE_KEYCODES[mutable.size])
        mutable[laneIndex] = keyCode
        updateControllerSettings(cur.copy(laneKeyCodes = mutable.take(5)))
        gamepadManager.stopKeyCapture()
        soundFx.playUiClick(isConfirm = true)
    }

    fun assignControllerActionKey(action: String, keyCode: Int) {
        val cur = _playerStats.value.studioProfile.controllerSettings
        val next = when (action) {
            "pause" -> cur.copy(pauseKeyCode = keyCode)
            "star_power" -> cur.copy(starPowerKeyCode = keyCode)
            "strum" -> cur.copy(strumKeyCode = keyCode)
            else -> cur
        }
        updateControllerSettings(next)
        gamepadManager.stopKeyCapture()
        soundFx.playUiClick(isConfirm = true)
    }

    fun applyDualShock4DefaultPreset() {
        val cur = _playerStats.value.studioProfile.controllerSettings
        updateControllerSettings(
            cur.copy(
                enabled = true,
                laneKeyCodes = ControllerSettings.DEFAULT_PS4_LANE_KEYCODES,
                pauseKeyCode = KeyEvent.KEYCODE_BUTTON_START,
                starPowerKeyCode = KeyEvent.KEYCODE_BUTTON_Y,
                strumKeyCode = KeyEvent.KEYCODE_DPAD_DOWN,
                useAnalogTriggers = true
            )
        )
        soundFx.playUiClick(isConfirm = true)
    }

    fun applyFaceButtonsControllerPreset() {
        val cur = _playerStats.value.studioProfile.controllerSettings
        updateControllerSettings(
            cur.copy(
                enabled = true,
                laneKeyCodes = ControllerSettings.FACE_BUTTONS_PRESET,
                pauseKeyCode = KeyEvent.KEYCODE_BUTTON_START,
                starPowerKeyCode = KeyEvent.KEYCODE_BUTTON_L1,
                strumKeyCode = KeyEvent.KEYCODE_DPAD_DOWN
            )
        )
        soundFx.playUiClick(isConfirm = true)
    }

    fun startGamepadKeyCapture() {
        gamepadManager.startKeyCapture()
    }

    fun stopGamepadKeyCapture() {
        gamepadManager.stopKeyCapture()
    }

    fun refreshGamepadConnection() {
        gamepadManager.refreshConnectedDevices()
    }

    fun onHardwareKeyEvent(event: KeyEvent): Boolean {
        return gamepadManager.handleKeyEvent(
            event = event,
            settings = _playerStats.value.studioProfile.controllerSettings,
            isGameplayScreen = (_currentScreen.value is GameScreen.Gameplay),
            isPaused = _isPaused.value,
            onLaneDown = { lane -> onFretDown(lane) },
            onLaneUp = { lane -> onFretUp(lane) },
            onStrum = { onStrumBar() },
            onStarPower = { activateStarPower() },
            onTogglePause = { togglePause() },
            onMenuBack = {
                if (_currentScreen.value !is GameScreen.MainMenu && _currentScreen.value !is GameScreen.Splash) {
                    navigateBack()
                }
            }
        )
    }

    fun onHardwareMotionEvent(event: MotionEvent): Boolean {
        return gamepadManager.handleMotionEvent(
            event = event,
            settings = _playerStats.value.studioProfile.controllerSettings,
            isGameplayScreen = (_currentScreen.value is GameScreen.Gameplay),
            isPaused = _isPaused.value,
            onLaneDown = { lane -> onFretDown(lane) },
            onLaneUp = { lane -> onFretUp(lane) }
        )
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
            _currentScreen.value = GameScreen.MainMenu
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
        triggerBackgroundCloudSync(customizationChanged = true)
    }

    fun setControlMode(mode: ControlMode) {
        _playerStats.value = _playerStats.value.copy(controlMode = mode)
        triggerBackgroundCloudSync(customizationChanged = true)
    }

    fun setGraphicsQuality(quality: GraphicsQuality) {
        _playerStats.value = _playerStats.value.copy(graphicsQuality = quality)
        triggerBackgroundCloudSync(customizationChanged = true)
    }

    fun setLatencyOffset(offsetMs: Long) {
        _playerStats.value = _playerStats.value.copy(audioLatencyOffsetMs = offsetMs)
        triggerBackgroundCloudSync(customizationChanged = true)
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
            var lastMultiplayerSyncMs = 0L

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

                    val activeRoom = _multiplayerRoom.value
                    val nowWallMs = System.currentTimeMillis()
                    if (activeRoom != null && (nowWallMs - lastMultiplayerSyncMs) >= 450L) {
                        lastMultiplayerSyncMs = nowWallMs
                        multiplayerRepo?.let { mRepo ->
                            launch {
                                mRepo.updateParticipantProgress(
                                    roomCode = activeRoom.roomCode,
                                    score = _score.value,
                                    combo = _combo.value,
                                    rockMeter = _rockMeter.value,
                                    starPowerActive = _isStarPowerActive.value
                                )
                            }
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
            gamepadManager.releaseAllActiveLanes { lane ->
                _activeFretLanes.value = _activeFretLanes.value - lane
            }
        }
    }

    fun restartMatch() {
        stop()
        startMatch()
    }

    fun finishMatch(failed: Boolean = false) {
        gamepadManager.releaseAllActiveLanes { lane ->
            _activeFretLanes.value = _activeFretLanes.value - lane
        }
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

        val localRecord = LocalMatchRecord(
            trackId = selectedTrack.id,
            trackTitle = selectedTrack.title,
            instrument = selectedInstrument.name,
            difficulty = selectedDifficulty.name,
            score = _score.value,
            stars = stars,
            accuracyPercent = accuracy.toDouble(),
            coinsEarned = coinsEarned,
            isCompetitive = isCompetitiveMatch
        )
        persistLocalMutation(newMatchRecord = localRecord)

        val activeRoom = _multiplayerRoom.value
        val activeUid = multiplayerRepo?.currentUidOrNull()
        if (activeRoom != null && activeUid != null) {
            val finalScore = _score.value
            val finalCombo = _combo.value
            val finalMeter = _rockMeter.value
            val updatedParticipants = _multiplayerParticipants.value.map {
                if (it.userId == activeUid) it.copy(score = finalScore) else it
            }
            val topUid = updatedParticipants.maxByOrNull { it.score }?.userId ?: activeUid
            viewModelScope.launch {
                multiplayerRepo.updateParticipantProgress(
                    roomCode = activeRoom.roomCode,
                    score = finalScore,
                    combo = finalCombo,
                    rockMeter = finalMeter,
                    starPowerActive = false
                )
                if (activeRoom.hostUid == activeUid) {
                    multiplayerRepo.updateRoomHostConfig(
                        roomCode = activeRoom.roomCode,
                        mode = activeRoom.modeEnum,
                        track = selectedTrack,
                        status = RoomStatus.FINISHED,
                        winnerUid = topUid
                    )
                }
            }
        }

        val repo = accountRepo
        if (repo != null && repo.currentUidOrNull() != null && _playerStats.value.nickname.isNotBlank()) {
            viewModelScope.launch {
                repo.recordProgressionMatch(result, entryIdOverride = localRecord.id)
                repo.syncPlayerStats(_playerStats.value).onSuccess { cloud ->
                    val synced = cloud.toPlayerStats()
                    _playerStats.value = synced
                    localProgressRepository.saveSnapshot(
                        localProgressRepository.loadSnapshot().copy(
                            stats = synced,
                            hasUnlinkedGuestProgress = false
                        )
                    )
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
            triggerBackgroundCloudSync(customizationChanged = true)
        }
    }

    fun selectCharacter(id: String) {
        _playerStats.value = _playerStats.value.copy(selectedCharacterId = id)
        triggerBackgroundCloudSync(customizationChanged = true)
    }

    fun unlockGuitar(id: String, price: Int) {
        if (_playerStats.value.coins >= price) {
            val unlocked = (_playerStats.value.unlockedCosmetics + id).distinct()
            _playerStats.value = _playerStats.value.copy(
                coins = _playerStats.value.coins - price,
                selectedGuitarId = id,
                unlockedCosmetics = unlocked
            )
            triggerBackgroundCloudSync(customizationChanged = true)
        }
    }

    fun selectGuitar(id: String) {
        _playerStats.value = _playerStats.value.copy(selectedGuitarId = id)
        triggerBackgroundCloudSync(customizationChanged = true)
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
            triggerBackgroundCloudSync(customizationChanged = true)
        }
    }

    fun selectGear(instrumentType: Instrument, id: String) {
        _playerStats.value = when (instrumentType) {
            Instrument.GUITAR -> _playerStats.value.copy(selectedGuitarId = id)
            Instrument.BASS -> _playerStats.value.copy(selectedBassId = id)
            Instrument.DRUMS -> _playerStats.value.copy(selectedDrumsId = id)
            Instrument.VOCALS -> _playerStats.value.copy(selectedVocalMicId = id)
        }
        triggerBackgroundCloudSync(customizationChanged = true)
    }

    // ---- Multiplayer Online (Salas, Convite, Sync & Desconexão) ----

    fun currentMultiplayerUid(): String? = multiplayerRepo?.currentUidOrNull()

    fun clearMultiplayerError() {
        _multiplayerError.value = null
    }

    fun clearMultiplayerWarning() {
        _multiplayerConnectionWarning.value = null
    }

    private fun effectiveMultiplayerNickname(): String {
        val fromAuth = (authUiState.value as? AuthUiState.Authenticated)?.nickname
        val fromStats = _playerStats.value.nickname.trim()
        val emailPrefix = firebaseAuth?.currentUser?.email?.substringBefore("@")?.trim()
        return listOf(fromAuth, fromStats, emailPrefix, "Rocker")
            .firstOrNull { !it.isNullOrBlank() && it.length >= 2 }
            ?.take(24) ?: "Rocker"
    }

    fun setMultiplayerMode(mode: MultiplayerMode) {
        _multiplayerMode.value = mode
        setCompetitiveMode(mode == MultiplayerMode.BATTLE_1V1)
        val room = _multiplayerRoom.value ?: return
        val uid = multiplayerRepo?.currentUidOrNull() ?: return
        if (room.hostUid != uid) {
            _multiplayerError.value = "Apenas o anfitrião (${room.hostNickname}) pode alterar o modo da sala."
            return
        }
        if (room.memberUids.size > mode.maxPlayers) {
            _multiplayerError.value = "Há ${room.memberUids.size} jogadores na sala. Não é possível reduzir para ${mode.maxPlayers} vagas."
            return
        }
        viewModelScope.launch {
            multiplayerRepo.updateRoomHostConfig(
                roomCode = room.roomCode,
                mode = mode,
                track = selectedTrack
            ).onFailure { err ->
                _multiplayerError.value = err.message ?: "Falha ao atualizar o modo da sala."
            }
        }
    }

    fun updateMultiplayerTrack(track: Track) {
        selectTrack(track)
        val room = _multiplayerRoom.value ?: return
        val uid = multiplayerRepo?.currentUidOrNull() ?: return
        if (room.hostUid == uid) {
            viewModelScope.launch {
                multiplayerRepo.updateRoomHostConfig(
                    roomCode = room.roomCode,
                    mode = _multiplayerMode.value,
                    track = track
                ).onFailure { err ->
                    _multiplayerError.value = err.message ?: "Falha ao sincronizar música da sala."
                }
            }
        }
    }

    fun updateMultiplayerLoadout(
        instrument: Instrument = selectedInstrument,
        difficulty: Difficulty = selectedDifficulty
    ) {
        selectInstrument(instrument)
        selectDifficulty(difficulty)
        val room = _multiplayerRoom.value ?: return
        val repo = multiplayerRepo ?: return
        val uid = repo.currentUidOrNull() ?: return
        val self = _multiplayerParticipants.value.find { it.userId == uid }
        viewModelScope.launch {
            repo.updateParticipantLoadout(
                roomCode = room.roomCode,
                nickname = effectiveMultiplayerNickname(),
                instrument = instrument,
                difficulty = difficulty,
                ready = self?.ready ?: false
            ).onFailure { err ->
                _multiplayerError.value = err.message ?: "Falha ao atualizar instrumento/dificuldade."
            }
        }
    }

    fun createMultiplayerRoom(
        mode: MultiplayerMode = _multiplayerMode.value,
        customRoomCode: String? = null
    ) {
        val repo = multiplayerRepo ?: run {
            _multiplayerError.value = "Serviço multiplayer na nuvem indisponível neste momento."
            return
        }
        if (repo.currentUidOrNull() == null) {
            _multiplayerError.value = "Faça login com Google para criar ou entrar em salas online."
            return
        }
        viewModelScope.launch {
            _isMultiplayerBusy.value = true
            _multiplayerError.value = null
            _multiplayerConnectionWarning.value = null
            _multiplayerMode.value = mode
            setCompetitiveMode(mode == MultiplayerMode.BATTLE_1V1)

            val previousCode = _multiplayerRoom.value?.roomCode
            val res = repo.createRoom(
                mode = mode,
                track = selectedTrack,
                nickname = effectiveMultiplayerNickname(),
                instrument = selectedInstrument,
                difficulty = selectedDifficulty,
                customRoomCode = customRoomCode
            )
            res.onSuccess { room ->
                if (!previousCode.isNullOrBlank() && previousCode != room.roomCode) {
                    stopRoomObserversOnly()
                    repo.leaveOrCloseRoom(previousCode)
                }
                _multiplayerRoom.value = room
                startObservingMultiplayerRoom(room.roomCode)
            }.onFailure { err ->
                _multiplayerError.value = err.message ?: "Não foi possível criar a sala multiplayer."
            }
            _isMultiplayerBusy.value = false
        }
    }

    fun joinMultiplayerRoom(rawCode: String) {
        val repo = multiplayerRepo ?: run {
            _multiplayerError.value = "Serviço multiplayer na nuvem indisponível neste momento."
            return
        }
        if (repo.currentUidOrNull() == null) {
            _multiplayerError.value = "Faça login com Google para entrar em uma sala online."
            return
        }
        viewModelScope.launch {
            _isMultiplayerBusy.value = true
            _multiplayerError.value = null
            _multiplayerConnectionWarning.value = null

            val normalized = repo.normalizeRoomCode(rawCode)
            val previousCode = _multiplayerRoom.value?.roomCode

            val res = repo.joinRoom(
                rawCode = normalized,
                nickname = effectiveMultiplayerNickname(),
                instrument = selectedInstrument,
                difficulty = selectedDifficulty
            )
            res.onSuccess { room ->
                if (!previousCode.isNullOrBlank() && previousCode != room.roomCode) {
                    stopRoomObserversOnly()
                    repo.leaveOrCloseRoom(previousCode)
                }
                _multiplayerRoom.value = room
                _multiplayerMode.value = room.modeEnum
                setCompetitiveMode(room.modeEnum == MultiplayerMode.BATTLE_1V1)
                _tracks.value.find { it.id == room.trackId }?.let { selectTrack(it) }
                startObservingMultiplayerRoom(room.roomCode)
            }.onFailure { err ->
                _multiplayerError.value = err.message ?: "Não foi possível entrar na sala."
            }
            _isMultiplayerBusy.value = false
        }
    }

    fun toggleMultiplayerReady() {
        val room = _multiplayerRoom.value ?: return
        val repo = multiplayerRepo ?: return
        val uid = repo.currentUidOrNull() ?: return
        val self = _multiplayerParticipants.value.find { it.userId == uid }
        val nextReady = !(self?.ready ?: false)
        viewModelScope.launch {
            _multiplayerError.value = null
            repo.updateParticipantLoadout(
                roomCode = room.roomCode,
                nickname = effectiveMultiplayerNickname(),
                instrument = selectedInstrument,
                difficulty = selectedDifficulty,
                ready = nextReady
            ).onFailure { err ->
                _multiplayerError.value = err.message ?: "Falha ao atualizar status de prontidão."
            }
        }
    }

    fun startMultiplayerMatch() {
        val room = _multiplayerRoom.value
        val repo = multiplayerRepo
        if (room == null || repo == null) {
            setCompetitiveMode(_multiplayerMode.value == MultiplayerMode.BATTLE_1V1)
            navigateTo(GameScreen.MusicLibrary)
            return
        }
        val uid = repo.currentUidOrNull() ?: return
        if (room.hostUid != uid) {
            _multiplayerError.value = "Aguarde o anfitrião (${room.hostNickname}) iniciar a partida sincronizada."
            return
        }
        val participants = _multiplayerParticipants.value
        if (participants.size < 2) {
            _multiplayerError.value = "Aguarde pelo menos 1 outro jogador entrar na sala '${room.roomCode}' (ou compartilhe o código)."
            return
        }
        val unreadyGuests = participants.filter { it.userId != uid && !it.ready }
        if (unreadyGuests.isNotEmpty()) {
            val names = unreadyGuests.joinToString { it.nickname }
            _multiplayerError.value = "Aguardando confirmação PRONTO de: $names."
            return
        }

        viewModelScope.launch {
            _isMultiplayerBusy.value = true
            _multiplayerError.value = null
            repo.updateParticipantLoadout(
                roomCode = room.roomCode,
                nickname = effectiveMultiplayerNickname(),
                instrument = selectedInstrument,
                difficulty = selectedDifficulty,
                ready = true
            )
            val res = repo.updateRoomHostConfig(
                roomCode = room.roomCode,
                mode = _multiplayerMode.value,
                track = selectedTrack,
                status = RoomStatus.PLAYING,
                winnerUid = ""
            )
            res.onFailure { err ->
                _multiplayerError.value = err.message ?: "Não foi possível iniciar a partida online."
            }
            _isMultiplayerBusy.value = false
        }
    }

    fun reconnectMultiplayerRoom() {
        val room = _multiplayerRoom.value
        val repo = multiplayerRepo ?: return
        val uid = repo.currentUidOrNull()
        if (room == null || uid == null) {
            _multiplayerError.value = "Nenhuma sala ativa para reconectar."
            return
        }
        viewModelScope.launch {
            _isMultiplayerBusy.value = true
            _multiplayerError.value = null
            _multiplayerConnectionWarning.value = "Reconectando à sala ${room.roomCode}..."
            repo.sendHeartbeat(room.roomCode, ParticipantConnectionState.RECONNECTING)
            val res = repo.joinRoom(
                rawCode = room.roomCode,
                nickname = effectiveMultiplayerNickname(),
                instrument = selectedInstrument,
                difficulty = selectedDifficulty
            )
            res.onSuccess { refreshed ->
                _multiplayerRoom.value = refreshed
                _multiplayerConnectionWarning.value = null
                startObservingMultiplayerRoom(refreshed.roomCode)
            }.onFailure { err ->
                _multiplayerConnectionWarning.value = "Falha ao reconectar: ${err.message}"
            }
            _isMultiplayerBusy.value = false
        }
    }

    fun leaveMultiplayerRoom() {
        val roomCode = _multiplayerRoom.value?.roomCode
        stopRoomObserversOnly()
        _multiplayerRoom.value = null
        _multiplayerParticipants.value = emptyList()
        _disconnectedPeerNicknames.value = emptyList()
        _multiplayerConnectionWarning.value = null
        _multiplayerError.value = null
        hasAutoStartedRoomMatch = false

        val repo = multiplayerRepo ?: return
        if (!roomCode.isNullOrBlank() && repo.currentUidOrNull() != null) {
            viewModelScope.launch {
                repo.leaveOrCloseRoom(roomCode)
            }
        }
    }

    fun refreshMultiplayerPeerHealth(nowMs: Long = System.currentTimeMillis()) {
        val participants = _multiplayerParticipants.value
        val selfUid = multiplayerRepo?.currentUidOrNull()
        val staleNames = participants
            .filter { it.userId != selfUid && it.isStale(nowMs) }
            .map { it.nickname.ifBlank { "Jogador" } }
        _disconnectedPeerNicknames.value = staleNames
        if (staleNames.isNotEmpty()) {
            _multiplayerConnectionWarning.value =
                "Conexão instável ou desconectado(s): ${staleNames.joinToString(", ")}"
        }
    }

    private fun stopRoomObserversOnly() {
        roomObserveJob?.cancel()
        roomObserveJob = null
        participantsObserveJob?.cancel()
        participantsObserveJob = null
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun startObservingMultiplayerRoom(roomCode: String) {
        val repo = multiplayerRepo ?: return
        stopRoomObserversOnly()
        hasAutoStartedRoomMatch = false

        roomObserveJob = viewModelScope.launch {
            try {
                repo.observeRoom(roomCode).collect { room ->
                    if (room == null || room.statusEnum == RoomStatus.CLOSED) {
                        val closedMsg = "A sala $roomCode foi encerrada pelo anfitrião."
                        stopRoomObserversOnly()
                        _multiplayerRoom.value = null
                        _multiplayerParticipants.value = emptyList()
                        _multiplayerConnectionWarning.value = closedMsg
                        hasAutoStartedRoomMatch = false
                        return@collect
                    }

                    _multiplayerRoom.value = room
                    _multiplayerMode.value = room.modeEnum
                    setCompetitiveMode(room.modeEnum == MultiplayerMode.BATTLE_1V1)
                    _tracks.value.find { it.id == room.trackId }?.let { selectTrack(it) }

                    if (room.statusEnum == RoomStatus.PLAYING &&
                        !hasAutoStartedRoomMatch &&
                        _currentScreen.value !is GameScreen.Gameplay
                    ) {
                        hasAutoStartedRoomMatch = true
                        startMatch()
                    } else if (room.statusEnum != RoomStatus.PLAYING) {
                        hasAutoStartedRoomMatch = false
                    }
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    _multiplayerConnectionWarning.value = "Conexão com a sala $roomCode interrompida. Toque em RECONECTAR."
                }
            }
        }

        participantsObserveJob = viewModelScope.launch {
            try {
                repo.observeParticipants(roomCode).collect { participants ->
                    _multiplayerParticipants.value = participants
                    refreshMultiplayerPeerHealth()
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    _multiplayerConnectionWarning.value = "Erro ao sincronizar participantes. Toque em RECONECTAR."
                }
            }
        }

        heartbeatJob = viewModelScope.launch {
            var consecutiveFailures = 0
            while (isActive && _multiplayerRoom.value?.roomCode == roomCode) {
                val res = repo.sendHeartbeat(roomCode, ParticipantConnectionState.ONLINE)
                if (res.isFailure) {
                    consecutiveFailures++
                    if (consecutiveFailures >= 2) {
                        _multiplayerConnectionWarning.value =
                            "Falha de rede na sala $roomCode. Verifique sua conexão ou toque em RECONECTAR."
                    }
                } else {
                    consecutiveFailures = 0
                    if (_disconnectedPeerNicknames.value.isEmpty() &&
                        _multiplayerConnectionWarning.value?.contains("Falha de rede") == true
                    ) {
                        _multiplayerConnectionWarning.value = null
                    }
                }
                refreshMultiplayerPeerHealth()
                delay(4_000L)
            }
        }
    }

    fun stop() {
        gamepadManager.releaseAllActiveLanes { lane ->
            _activeFretLanes.value = _activeFretLanes.value - lane
        }
        audioPlayer.stop()
        gameLoopJob?.cancel()
        previewProgressJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        stopRoomObserversOnly()
        audioPlayer.release()
        gamepadManager.release()
    }
}
