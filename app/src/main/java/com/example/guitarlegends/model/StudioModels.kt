package com.example.guitarlegends.model

import android.view.KeyEvent

enum class InstrumentFinish(val label: String, val specularity: Float, val metallicTint: Float) {
    GLOSS("Brilhante (Verniz)", 0.92f, 0.15f),
    MATTE("Fosco (Acetinado)", 0.18f, 0.05f),
    METALLIC("Metálico Sparkle", 0.85f, 0.75f),
    SUNBURST("Sunburst Clássico", 0.78f, 0.25f),
    NEON_GLOW("Neon Reativo", 1.0f, 0.35f)
}

enum class StagePoseExpression(val label: String) {
    CONCENTRATED("Focado no Solo"),
    SHOUTING_ROCK("Grito de Arena"),
    SMIRK_LEGEND("Sorriso Confiante"),
    HEADBANG_FURY("Fúria Headbang")
}

data class InstrumentCustomConfig(
    val modelStyleId: String = "strat_classic",
    val primaryColorHex: Long = 0xFFFF3D00L,
    val secondaryColorHex: Long = 0xFF1A1423L,
    val hardwareColorHex: Long = 0xFFE0E0E0L,
    val finish: InstrumentFinish = InstrumentFinish.GLOSS,
    val stampId: String = "inst_stamp_flames",
    val stickerId: String = "sticker_lightning",
    val fullSkinId: String = "skin_none"
)

data class HighwayCustomConfig(
    val selectedStampId: String = "stamp_none",
    val customImagePath: String? = null,
    val stampOpacity: Float = 0.55f
)

data class AudioSettings(
    val masterVolume: Float = 1.0f,
    val musicVolume: Float = 0.85f,
    val gameplaySfxVolume: Float = 0.90f,
    val uiSfxVolume: Float = 0.80f,
    val stageFxVolume: Float = 0.75f,
    val isMuted: Boolean = false
) {
    fun effectiveMusic(): Float = if (isMuted) 0f else (masterVolume * musicVolume).coerceIn(0f, 1f)
    fun effectiveGameplaySfx(): Float = if (isMuted) 0f else (masterVolume * gameplaySfxVolume).coerceIn(0f, 1f)
    fun effectiveUiSfx(): Float = if (isMuted) 0f else (masterVolume * uiSfxVolume).coerceIn(0f, 1f)
    fun effectiveStageFx(): Float = if (isMuted) 0f else (masterVolume * stageFxVolume).coerceIn(0f, 1f)
}

data class ControllerSettings(
    val enabled: Boolean = true,
    val laneKeyCodes: List<Int> = DEFAULT_PS4_LANE_KEYCODES,
    val pauseKeyCode: Int = KeyEvent.KEYCODE_BUTTON_START,
    val starPowerKeyCode: Int = KeyEvent.KEYCODE_BUTTON_Y,
    val strumKeyCode: Int = KeyEvent.KEYCODE_DPAD_DOWN,
    val useAnalogTriggers: Boolean = true
) {
    companion object {
        val DEFAULT_PS4_LANE_KEYCODES = listOf(
            KeyEvent.KEYCODE_BUTTON_L2,
            KeyEvent.KEYCODE_BUTTON_L1,
            KeyEvent.KEYCODE_BUTTON_R1,
            KeyEvent.KEYCODE_BUTTON_R2,
            KeyEvent.KEYCODE_BUTTON_A
        )

        val FACE_BUTTONS_PRESET = listOf(
            KeyEvent.KEYCODE_BUTTON_X,
            KeyEvent.KEYCODE_BUTTON_Y,
            KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_BUTTON_R1
        )

        fun keyCodeDisplayName(keyCode: Int): String = when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_L2 -> "L2 / LT"
            KeyEvent.KEYCODE_BUTTON_L1 -> "L1 / LB"
            KeyEvent.KEYCODE_BUTTON_R1 -> "R1 / RB"
            KeyEvent.KEYCODE_BUTTON_R2 -> "R2 / RT"
            KeyEvent.KEYCODE_BUTTON_A -> "✕ / A"
            KeyEvent.KEYCODE_BUTTON_B -> "○ / B"
            KeyEvent.KEYCODE_BUTTON_X -> "□ / X"
            KeyEvent.KEYCODE_BUTTON_Y -> "△ / Y"
            KeyEvent.KEYCODE_BUTTON_START -> "OPTIONS / START"
            KeyEvent.KEYCODE_BUTTON_SELECT -> "SHARE / SELECT"
            KeyEvent.KEYCODE_DPAD_DOWN -> "D-Pad Baixo"
            KeyEvent.KEYCODE_DPAD_UP -> "D-Pad Cima"
            KeyEvent.KEYCODE_DPAD_LEFT -> "D-Pad Esquerda"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "D-Pad Direita"
            KeyEvent.KEYCODE_BUTTON_THUMBL -> "L3"
            KeyEvent.KEYCODE_BUTTON_THUMBR -> "R3"
            else -> KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_").replace("BUTTON_", "")
        }
    }
}

data class AppearancePreset(
    val id: String,
    val name: String,
    val selectedCharacterId: String,
    val rockerBodyModel: String,
    val skinToneHex: Long,
    val customRocker: CustomRockerAppearance,
    val topColorHex: Long,
    val pantsColorHex: Long,
    val shoesColorHex: Long,
    val stageExpression: StagePoseExpression,
    val fullOutfitSkinId: String,
    val guitarConfig: InstrumentCustomConfig,
    val selectedHighwayThemeId: String,
    val highwayConfig: HighwayCustomConfig
)

data class StudioProfileConfig(
    val rockerBodyModel: String = "Heavy Metal Clássico",
    val skinToneHex: Long = 0xFFE2B490L,
    val topColorHex: Long = 0xFF221F28L,
    val pantsColorHex: Long = 0xFF253548L,
    val shoesColorHex: Long = 0xFF141218L,
    val accessoryColorHex: Long = 0xFFFFD700L,
    val stageExpression: StagePoseExpression = StagePoseExpression.SMIRK_LEGEND,
    val fullOutfitSkinId: String = "outfit_skin_none",
    val guitarConfig: InstrumentCustomConfig = InstrumentCustomConfig(
        modelStyleId = "strat_classic",
        primaryColorHex = 0xFFFF3D00L,
        secondaryColorHex = 0xFF14111AL,
        hardwareColorHex = 0xFFE5E5E5L,
        finish = InstrumentFinish.GLOSS,
        stampId = "inst_stamp_flames",
        stickerId = "sticker_lightning"
    ),
    val bassConfig: InstrumentCustomConfig = InstrumentCustomConfig(
        modelStyleId = "precision_bass",
        primaryColorHex = 0xFF512DA8L,
        secondaryColorHex = 0xFF110D1CL,
        hardwareColorHex = 0xFFC0C0C0L,
        finish = InstrumentFinish.METALLIC,
        stampId = "inst_stamp_none",
        stickerId = "sticker_skull"
    ),
    val drumsConfig: InstrumentCustomConfig = InstrumentCustomConfig(
        modelStyleId = "double_kick_arena",
        primaryColorHex = 0xFFFF6D00L,
        secondaryColorHex = 0xFF18141EL,
        hardwareColorHex = 0xFFFFD700L,
        finish = InstrumentFinish.GLOSS,
        stampId = "inst_stamp_stripes",
        stickerId = "sticker_star"
    ),
    val selectedAcousticId: String = "violao_nylon_classico",
    val acousticConfig: InstrumentCustomConfig = InstrumentCustomConfig(
        modelStyleId = "acoustic_dreadnought",
        primaryColorHex = 0xFFC68642L,
        secondaryColorHex = 0xFF4E2A14L,
        hardwareColorHex = 0xFFE5C158L,
        finish = InstrumentFinish.GLOSS,
        stampId = "inst_stamp_none",
        stickerId = "sticker_none"
    ),
    val vocalConfig: InstrumentCustomConfig = InstrumentCustomConfig(
        modelStyleId = "vintage_55s",
        primaryColorHex = 0xFFE0E0E0L,
        secondaryColorHex = 0xFF262230L,
        hardwareColorHex = 0xFFFFD700L,
        finish = InstrumentFinish.METALLIC,
        stampId = "inst_stamp_none",
        stickerId = "sticker_none"
    ),
    val highwayConfig: HighwayCustomConfig = HighwayCustomConfig(),
    val audioSettings: AudioSettings = AudioSettings(),
    val controllerSettings: ControllerSettings = ControllerSettings(),
    val savedPresets: List<AppearancePreset> = emptyList(),
    val claimedChallenges: List<String> = emptyList()
)

data class HighwayStampItem(
    val id: String,
    val title: String,
    val description: String,
    val priceCoins: Int,
    val accentHex: Long,
    val unlockedByDefault: Boolean = false
)

data class CosmeticShopItem(
    val id: String,
    val category: String, // "Roupas & Visual", "Acessórios & Tatuagens", "Instrumentos", "Estampas de Esteira"
    val title: String,
    val subtitle: String,
    val priceCoins: Int,
    val colorHex: Long,
    val unlockedByDefault: Boolean = false
)

data class ChallengeItem(
    val id: String,
    val title: String,
    val description: String,
    val rewardCoins: Int,
    val targetValue: Int,
    val progressProvider: (PlayerStats) -> Int
)

object StudioCatalog {
    val bodyModels = listOf(
        "Heavy Metal Clássico",
        "Punk Esguio",
        "Glam Rock Estrela",
        "Neo Cyberpunk"
    )

    val skinTones = listOf(
        0xFFF6D5B8L,
        0xFFE2B490L,
        0xFFC98E66L,
        0xFF9E6240L,
        0xFF6B3E26L,
        0xFF3E2218L
    )

    val paletteColors = listOf(
        0xFF1F1B26L,
        0xFFFF1744L,
        0xFFFF6D00L,
        0xFFFFD700L,
        0xFF00E676L,
        0xFF00E5FFL,
        0xFF7C4DFFL,
        0xFFE0E0E0L,
        0xFF253548L,
        0xFF6D1B4BL
    )

    val hairStyles = listOf(
        "Moicano Punk Clássico",
        "Metalhead Longo",
        "Glam Rock Desfiado",
        "Dreads Rocker",
        "Cyber Undercut",
        "Moicano Turquesa Elétrico"
    )

    val topClothings = listOf(
        "Jaqueta de Couro c/ Rebites",
        "Colete Jeans c/ Patches",
        "Camiseta Band Vintage",
        "Regata Rasgada",
        "Jaqueta Chamas de Palco"
    )

    val pantsClothings = listOf(
        "Calça Jeans Rasgada",
        "Calça de Couro Preto",
        "Calça Xadrez Punk",
        "Bermuda Cargo Metal"
    )

    val shoeStyles = listOf(
        "Coturnos de Couro Pesado",
        "Tênis Skate Cano Alto",
        "Botas de Motociclista",
        "Coturnos Militares"
    )

    val accessoryStyles = listOf(
        "Munhequeiras c/ Espinhos & Óculos Aviador",
        "Correntes & Piercings",
        "Faixa de Cabeça & Bracelete",
        "Óculos Neon Holográficos"
    )

    val tattooStyles = listOf(
        "Tatuagem Tribal e Chamas",
        "Caveiras nos Braços",
        "Maquiagem Gótica Clássica",
        "Tatuagem Raio Carmesim",
        "Sem Tatuagens"
    )

    val outfitSkins = listOf(
        CosmeticShopItem("outfit_skin_none", "Skins de Traje", "Visual Customizado Manual", "Usa suas cores e peças escolhidas", 0, 0xFF00E5FFL, true),
        CosmeticShopItem("outfit_skin_ps2_legend", "Skins de Traje", "Lenda dos Anos 2000", "Couro preto, tachas douradas e óculos espelhados", 0, 0xFFFFD700L, true),
        CosmeticShopItem("outfit_skin_inferno", "Skins de Traje", "Fúria Flamejante", "Traje vulcânico com chamas carmesim no palco", 280, 0xFFFF3D00L, false),
        CosmeticShopItem("outfit_skin_cyber", "Skins de Traje", "Neon Holográfico", "Armadura urbana com circuitos luminosos ciano", 420, 0xFF00E5FFL, false)
    )

    val instrumentModelsByType: Map<Instrument, List<Pair<String, String>>> = mapOf(
        Instrument.GUITAR to listOf(
            "strat_classic" to "Stratocaster Clássica",
            "flying_v" to "Flying V Agressiva",
            "explorer_metal" to "Explorer Heavy",
            "cyber_laser" to "Cyber Laser Futurista"
        ),
        Instrument.ACOUSTIC_GUITAR to listOf(
            "acoustic_dreadnought" to "Violão Folk Dreadnought",
            "acoustic_cutaway" to "Eletroacústico Cutaway",
            "acoustic_classic_nylon" to "Clássico Nylon Acústico"
        ),
        Instrument.BASS to listOf(
            "precision_bass" to "Precision Bass 4-Cordas",
            "skull_5string" to "Groove Skull 5-Cordas",
            "cyber_fretless" to "Synth Fretless Neon"
        ),
        Instrument.DRUMS to listOf(
            "double_kick_arena" to "Bumbo Duplo Arena",
            "punk_compact" to "Kit Punk 4 Peças",
            "cyber_mesh" to "Pads Eletrônicos RGB"
        ),
        Instrument.VOCALS to listOf(
            "vintage_55s" to "Vintage Cromado 55S",
            "gold_supreme" to "Cápsula Ouro 24K",
            "riot_distress" to "Underground Fita & Aço"
        )
    )

    val instrumentStamps = listOf(
        "inst_stamp_none" to "Lisa Clássica",
        "inst_stamp_flames" to "Chamas Old-School",
        "inst_stamp_stripes" to "Listras Racing",
        "inst_stamp_lightning" to "Raios Elétricos",
        "inst_stamp_checker" to "Xadrez Ska/Punk"
    )

    val instrumentStickers = listOf(
        "sticker_none" to "Sem Adesivo",
        "sticker_lightning" to "Raio ⚡",
        "sticker_skull" to "Caveira Rock 💀",
        "sticker_star" to "Estrela Lendária ★",
        "sticker_pick" to "Palheta Anarquia"
    )

    val instrumentSkins = listOf(
        CosmeticShopItem("skin_none", "Skins de Instrumento", "Pintura Customizada", "Usa suas cores, acabamento, estampa e adesivos", 0, 0xFF00E5FFL, true),
        CosmeticShopItem("skin_chrome_hero", "Skins de Instrumento", "Cromo Espelhado", "Acabamento metálico de palco prateado", 220, 0xFFE0E0E0L, false),
        CosmeticShopItem("skin_golden_tour", "Skins de Instrumento", "Ouro Turnê Mundial", "Chassi dourado edição colecionador", 450, 0xFFFFD700L, false),
        CosmeticShopItem("skin_plasma_neon", "Skins de Instrumento", "Plasma Neon", "Cordas e bordas fluorescentes de alta energia", 380, 0xFF7C4DFFL, false)
    )

    val highwayStamps = listOf(
        HighwayStampItem("stamp_none", "Gradiente Limpo", "Apenas as cores e trilhos do tema escolhido", 0, 0xFF00E5FFL, true),
        HighwayStampItem("stamp_ps2_flames", "Chamas de Rosewood", "Madeira clássica com labaredas estilizadas", 0, 0xFFFF6D00L, true),
        HighwayStampItem("stamp_checker_punk", "Xadrez Punk & Rebites", "Visual quadriculado roxo/grafite estilo garagem", 0, 0xFFAB47BCL, true),
        HighwayStampItem("stamp_cyber_circuit", "Circuitos Cyber Neon", "Malha hexagonal futurista em ciano elétrico", 180, 0xFF00E5FFL, false),
        HighwayStampItem("stamp_skull_stage", "Caveira Alada Metal", "Aço escovado com emblema metálico e raios", 260, 0xFF7C4DFFL, false)
    )

    val shopCosmetics: List<CosmeticShopItem> = listOf(
        CosmeticShopItem("hair_moicano_turquesa", "Cabelos & Visual", "Moicano Turquesa Elétrico", "Penteado punk neon de destaque no palco", 150, 0xFF00E5FFL),
        CosmeticShopItem("outfit_chamas_palco", "Roupas & Visual", "Jaqueta Chamas de Palco", "Jaqueta pesada com grafismo de fogo", 220, 0xFFFF5722L),
        CosmeticShopItem("acc_oculos_neon", "Acessórios & Tatuagens", "Óculos Neon Holográficos", "Visor luminoso estilo cyberpunk", 180, 0xFF00E5FFL),
        CosmeticShopItem("tattoo_raio_carmesim", "Acessórios & Tatuagens", "Tatuagem Raio Carmesim", "Arte tribal e raios nos braços", 160, 0xFFFF1744L),
        CosmeticShopItem("kai_cyber", "Personagens", "Kai Neo-Anime", "Integrante estilo cyberpunk futurista", 600, 0xFF00E5FFL),
        CosmeticShopItem("neon_thunder", "Instrumentos", "Neon Cyber Strat", "Guitarra com LEDs reativos", 500, 0xFF00E5FFL),
        CosmeticShopItem("violao_eletro_cutaway", "Instrumentos", "Eletroacústico Stage Cutaway", "Violão de aço com recorte ergonômico e brilho de arena", 450, 0xFFFFB74DL),
        CosmeticShopItem("cyber_bass", "Instrumentos", "Cyber Wave Fretless", "Baixo holográfico de fibra de carbono", 450, 0xFF00E5FFL),
        CosmeticShopItem("electronic_neon_kit", "Instrumentos", "Cyber Mesh Eletrônica", "Bateria eletrônica com aros RGB", 500, 0xFF7C4DFFL),
        CosmeticShopItem("punk_distress_mic", "Instrumentos", "Riot Distress Custom", "Microfone clássico de circuito underground", 400, 0xFFFF3D00L)
    ) + outfitSkins.filter { !it.unlockedByDefault } +
        instrumentSkins.filter { !it.unlockedByDefault } +
        highwayStamps.filter { !it.unlockedByDefault }.map {
            CosmeticShopItem(it.id, "Estampas de Esteira", it.title, it.description, it.priceCoins, it.accentHex, false)
        }

    val challenges = listOf(
        ChallengeItem(
            id = "chl_first_3_shows",
            title = "Aquecimento da Turnê",
            description = "Conclua 3 apresentações no palco em qualquer modo.",
            rewardCoins = 100,
            targetValue = 3,
            progressProvider = { it.songsCompleted }
        ),
        ChallengeItem(
            id = "chl_hit_250_notes",
            title = "Dedos Incansáveis",
            description = "Acerte um total de 250 notas acumuladas.",
            rewardCoins = 100,
            targetValue = 250,
            progressProvider = { it.notesHitTotal.toInt() }
        ),
        ChallengeItem(
            id = "chl_combo_35",
            title = "Ritmo Inabalável",
            description = "Atinja um combo contínuo de 35+ notas.",
            rewardCoins = 100,
            targetValue = 35,
            progressProvider = { it.maxStreakOverall }
        ),
        ChallengeItem(
            id = "chl_stars_12",
            title = "Caçador de Estrelas",
            description = "Acumule 12 estrelas de carreira.",
            rewardCoins = 100,
            targetValue = 12,
            progressProvider = { it.unlockedStars }
        ),
        ChallengeItem(
            id = "chl_duel_win",
            title = "Rei do Duelo 1v1",
            description = "Vença 1 partida no modo Batalha Competitiva.",
            rewardCoins = 100,
            targetValue = 1,
            progressProvider = { it.competitiveWins }
        )
    )

    fun applyCosmeticLook(base: PlayerStats, cosmeticId: String): PlayerStats {
        val studio = base.studioProfile
        val rocker = base.customRocker
        return when (cosmeticId) {
            "hair_moicano_turquesa" -> base.copy(
                selectedCharacterId = "custom_rocker",
                customRocker = rocker.copy(hairStyle = "Moicano Turquesa Elétrico", hairColorHex = 0xFF00E5FFL)
            )
            "outfit_chamas_palco" -> base.copy(
                selectedCharacterId = "custom_rocker",
                customRocker = rocker.copy(topClothing = "Jaqueta Chamas de Palco"),
                studioProfile = studio.copy(topColorHex = 0xFFFF5722L, fullOutfitSkinId = "outfit_skin_none")
            )
            "acc_oculos_neon" -> base.copy(
                selectedCharacterId = "custom_rocker",
                customRocker = rocker.copy(accessories = "Óculos Neon Holográficos"),
                studioProfile = studio.copy(accessoryColorHex = 0xFF00E5FFL)
            )
            "tattoo_raio_carmesim" -> base.copy(
                selectedCharacterId = "custom_rocker",
                customRocker = rocker.copy(bodyTattoos = "Tatuagem Raio Carmesim")
            )
            "axel_rocker", "roxie_riot", "kai_cyber" -> base.copy(selectedCharacterId = cosmeticId)
            "flame_caster", "void_v", "neon_thunder" -> base.copy(selectedGuitarId = cosmeticId)
            "violao_nylon_classico", "violao_aco_folk", "violao_eletro_cutaway" ->
                base.copy(
                    studioProfile = studio.copy(
                        selectedAcousticId = cosmeticId,
                        acousticConfig = studio.acousticConfig.copy(modelStyleId = cosmeticId)
                    )
                )
            "thunder_bass", "skull_bass", "cyber_bass" -> base.copy(selectedBassId = cosmeticId)
            "arena_beast_kit", "punk_garage_kit", "electronic_neon_kit" -> base.copy(selectedDrumsId = cosmeticId)
            "vintage_shure_55s", "golden_supreme_mic", "punk_distress_mic" -> base.copy(selectedVocalMicId = cosmeticId)
            "outfit_skin_none", "outfit_skin_ps2_legend", "outfit_skin_inferno", "outfit_skin_cyber" ->
                base.copy(studioProfile = studio.copy(fullOutfitSkinId = cosmeticId))
            "skin_none", "skin_chrome_hero", "skin_golden_tour", "skin_plasma_neon" ->
                base.copy(
                    studioProfile = studio.copy(
                        guitarConfig = studio.guitarConfig.copy(fullSkinId = cosmeticId),
                        acousticConfig = studio.acousticConfig.copy(fullSkinId = cosmeticId),
                        bassConfig = studio.bassConfig.copy(fullSkinId = cosmeticId),
                        drumsConfig = studio.drumsConfig.copy(fullSkinId = cosmeticId),
                        vocalConfig = studio.vocalConfig.copy(fullSkinId = cosmeticId)
                    )
                )
            "stamp_none", "stamp_ps2_flames", "stamp_checker_punk", "stamp_cyber_circuit", "stamp_skull_stage" ->
                base.copy(
                    studioProfile = studio.copy(
                        highwayConfig = studio.highwayConfig.copy(selectedStampId = cosmeticId, customImagePath = null)
                    )
                )
            else -> base
        }
    }
}
