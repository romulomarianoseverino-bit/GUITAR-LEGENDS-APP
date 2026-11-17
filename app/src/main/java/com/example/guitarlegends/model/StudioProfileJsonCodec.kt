package com.example.guitarlegends.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Codec JSON seguro e tolerante a versões anteriores para serializar/desserializar
 * configurações do estúdio 3D, áudio, controle Bluetooth, presets e desafios.
 */
object StudioProfileJsonCodec {

    fun toJsonObject(profile: StudioProfileConfig): JSONObject = JSONObject().apply {
        put("rockerBodyModel", profile.rockerBodyModel)
        put("skinToneHex", profile.skinToneHex)
        put("topColorHex", profile.topColorHex)
        put("pantsColorHex", profile.pantsColorHex)
        put("shoesColorHex", profile.shoesColorHex)
        put("accessoryColorHex", profile.accessoryColorHex)
        put("stageExpression", profile.stageExpression.name)
        put("fullOutfitSkinId", profile.fullOutfitSkinId)

        put("guitarConfig", instrumentConfigToJson(profile.guitarConfig))
        put("bassConfig", instrumentConfigToJson(profile.bassConfig))
        put("drumsConfig", instrumentConfigToJson(profile.drumsConfig))
        put("vocalConfig", instrumentConfigToJson(profile.vocalConfig))

        put("highwayConfig", JSONObject().apply {
            put("selectedStampId", profile.highwayConfig.selectedStampId)
            put("customImagePath", profile.highwayConfig.customImagePath ?: JSONObject.NULL)
            put("stampOpacity", profile.highwayConfig.stampOpacity.toDouble())
        })

        put("audioSettings", JSONObject().apply {
            put("masterVolume", profile.audioSettings.masterVolume.toDouble())
            put("musicVolume", profile.audioSettings.musicVolume.toDouble())
            put("gameplaySfxVolume", profile.audioSettings.gameplaySfxVolume.toDouble())
            put("uiSfxVolume", profile.audioSettings.uiSfxVolume.toDouble())
            put("stageFxVolume", profile.audioSettings.stageFxVolume.toDouble())
            put("isMuted", profile.audioSettings.isMuted)
        })

        put("controllerSettings", JSONObject().apply {
            put("enabled", profile.controllerSettings.enabled)
            put("laneKeyCodes", JSONArray(profile.controllerSettings.laneKeyCodes))
            put("pauseKeyCode", profile.controllerSettings.pauseKeyCode)
            put("starPowerKeyCode", profile.controllerSettings.starPowerKeyCode)
            put("strumKeyCode", profile.controllerSettings.strumKeyCode)
            put("useAnalogTriggers", profile.controllerSettings.useAnalogTriggers)
        })

        put("savedPresets", JSONArray().apply {
            profile.savedPresets.take(12).forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("selectedCharacterId", p.selectedCharacterId)
                    put("rockerBodyModel", p.rockerBodyModel)
                    put("skinToneHex", p.skinToneHex)
                    put("topColorHex", p.topColorHex)
                    put("pantsColorHex", p.pantsColorHex)
                    put("shoesColorHex", p.shoesColorHex)
                    put("stageExpression", p.stageExpression.name)
                    put("fullOutfitSkinId", p.fullOutfitSkinId)
                    put("guitarConfig", instrumentConfigToJson(p.guitarConfig))
                    put("selectedHighwayThemeId", p.selectedHighwayThemeId)
                    put("selectedStampId", p.highwayConfig.selectedStampId)
                    put("stampOpacity", p.highwayConfig.stampOpacity.toDouble())
                    put("hairStyle", p.customRocker.hairStyle)
                    put("hairColorHex", p.customRocker.hairColorHex)
                    put("topClothing", p.customRocker.topClothing)
                    put("pantsClothing", p.customRocker.pantsClothing)
                    put("shoesClothing", p.customRocker.shoesClothing)
                    put("accessories", p.customRocker.accessories)
                    put("bodyTattoos", p.customRocker.bodyTattoos)
                    put("artStyle", p.customRocker.artStyle.name)
                })
            }
        })

        put("claimedChallenges", JSONArray(profile.claimedChallenges.distinct()))
    }

    fun toJsonString(profile: StudioProfileConfig): String = toJsonObject(profile).toString()

    fun fromJsonString(json: String?): StudioProfileConfig {
        if (json.isNullOrBlank()) return StudioProfileConfig()
        return runCatching {
            fromJsonObject(JSONObject(json))
        }.getOrDefault(StudioProfileConfig())
    }

    fun fromJsonObject(obj: JSONObject?): StudioProfileConfig {
        if (obj == null) return StudioProfileConfig()
        val defaults = StudioProfileConfig()

        val hwObj = obj.optJSONObject("highwayConfig")
        val highwayConfig = if (hwObj != null) {
            HighwayCustomConfig(
                selectedStampId = hwObj.optString("selectedStampId", defaults.highwayConfig.selectedStampId),
                customImagePath = if (hwObj.isNull("customImagePath")) null
                else hwObj.optString("customImagePath", "").ifBlank { null },
                stampOpacity = hwObj.optDouble("stampOpacity", defaults.highwayConfig.stampOpacity.toDouble())
                    .toFloat().coerceIn(0.15f, 0.95f)
            )
        } else defaults.highwayConfig

        val audioObj = obj.optJSONObject("audioSettings")
        val audioSettings = if (audioObj != null) {
            AudioSettings(
                masterVolume = audioObj.optDouble("masterVolume", 1.0).toFloat().coerceIn(0f, 1f),
                musicVolume = audioObj.optDouble("musicVolume", 0.85).toFloat().coerceIn(0f, 1f),
                gameplaySfxVolume = audioObj.optDouble("gameplaySfxVolume", 0.90).toFloat().coerceIn(0f, 1f),
                uiSfxVolume = audioObj.optDouble("uiSfxVolume", 0.80).toFloat().coerceIn(0f, 1f),
                stageFxVolume = audioObj.optDouble("stageFxVolume", 0.75).toFloat().coerceIn(0f, 1f),
                isMuted = audioObj.optBoolean("isMuted", false)
            )
        } else defaults.audioSettings

        val ctrlObj = obj.optJSONObject("controllerSettings")
        val controllerSettings = if (ctrlObj != null) {
            val codesArr = ctrlObj.optJSONArray("laneKeyCodes")
            val parsedCodes = if (codesArr != null && codesArr.length() == 5) {
                (0 until 5).map { idx -> codesArr.optInt(idx, ControllerSettings.DEFAULT_PS4_LANE_KEYCODES[idx]) }
            } else {
                ControllerSettings.DEFAULT_PS4_LANE_KEYCODES
            }
            ControllerSettings(
                enabled = ctrlObj.optBoolean("enabled", true),
                laneKeyCodes = parsedCodes,
                pauseKeyCode = ctrlObj.optInt("pauseKeyCode", defaults.controllerSettings.pauseKeyCode),
                starPowerKeyCode = ctrlObj.optInt("starPowerKeyCode", defaults.controllerSettings.starPowerKeyCode),
                strumKeyCode = ctrlObj.optInt("strumKeyCode", defaults.controllerSettings.strumKeyCode),
                useAnalogTriggers = ctrlObj.optBoolean("useAnalogTriggers", true)
            )
        } else defaults.controllerSettings

        val presetsArr = obj.optJSONArray("savedPresets")
        val savedPresets = if (presetsArr != null) {
            buildList {
                for (i in 0 until presetsArr.length()) {
                    val p = presetsArr.optJSONObject(i) ?: continue
                    add(
                        AppearancePreset(
                            id = p.optString("id", "preset_$i"),
                            name = p.optString("name", "Visual ${i + 1}"),
                            selectedCharacterId = p.optString("selectedCharacterId", "axel_rocker"),
                            rockerBodyModel = p.optString("rockerBodyModel", defaults.rockerBodyModel),
                            skinToneHex = p.optLong("skinToneHex", defaults.skinToneHex),
                            customRocker = CustomRockerAppearance(
                                artStyle = runCatching {
                                    CharacterArtStyle.valueOf(p.optString("artStyle", CharacterArtStyle.STYLIZED_REALISTIC.name))
                                }.getOrDefault(CharacterArtStyle.STYLIZED_REALISTIC),
                                hairStyle = p.optString("hairStyle", "Moicano Punk Clássico"),
                                hairColorHex = p.optLong("hairColorHex", 0xFFFF5722L),
                                topClothing = p.optString("topClothing", "Jaqueta de Couro c/ Rebites"),
                                pantsClothing = p.optString("pantsClothing", "Calça Jeans Rasgada"),
                                shoesClothing = p.optString("shoesClothing", "Coturnos de Couro Pesado"),
                                accessories = p.optString("accessories", "Munhequeiras de Espinhos & Óculos Escuros Aviador"),
                                bodyTattoos = p.optString("bodyTattoos", "Tatuagem Tribal e Chamas nos Braços")
                            ),
                            topColorHex = p.optLong("topColorHex", defaults.topColorHex),
                            pantsColorHex = p.optLong("pantsColorHex", defaults.pantsColorHex),
                            shoesColorHex = p.optLong("shoesColorHex", defaults.shoesColorHex),
                            stageExpression = runCatching {
                                StagePoseExpression.valueOf(p.optString("stageExpression", StagePoseExpression.SMIRK_LEGEND.name))
                            }.getOrDefault(StagePoseExpression.SMIRK_LEGEND),
                            fullOutfitSkinId = p.optString("fullOutfitSkinId", defaults.fullOutfitSkinId),
                            guitarConfig = jsonToInstrumentConfig(p.optJSONObject("guitarConfig"), defaults.guitarConfig),
                            selectedHighwayThemeId = p.optString("selectedHighwayThemeId", "theme_ps2_wood"),
                            highwayConfig = HighwayCustomConfig(
                                selectedStampId = p.optString("selectedStampId", "stamp_none"),
                                customImagePath = null,
                                stampOpacity = p.optDouble("stampOpacity", 0.55).toFloat().coerceIn(0.15f, 0.95f)
                            )
                        )
                    )
                }
            }
        } else emptyList()

        val challengesArr = obj.optJSONArray("claimedChallenges")
        val claimedChallenges = if (challengesArr != null) {
            buildList {
                for (i in 0 until challengesArr.length()) {
                    val v = challengesArr.optString(i, "")
                    if (v.isNotBlank()) add(v)
                }
            }.distinct()
        } else emptyList()

        return StudioProfileConfig(
            rockerBodyModel = obj.optString("rockerBodyModel", defaults.rockerBodyModel),
            skinToneHex = obj.optLong("skinToneHex", defaults.skinToneHex),
            topColorHex = obj.optLong("topColorHex", defaults.topColorHex),
            pantsColorHex = obj.optLong("pantsColorHex", defaults.pantsColorHex),
            shoesColorHex = obj.optLong("shoesColorHex", defaults.shoesColorHex),
            accessoryColorHex = obj.optLong("accessoryColorHex", defaults.accessoryColorHex),
            stageExpression = runCatching {
                StagePoseExpression.valueOf(obj.optString("stageExpression", defaults.stageExpression.name))
            }.getOrDefault(defaults.stageExpression),
            fullOutfitSkinId = obj.optString("fullOutfitSkinId", defaults.fullOutfitSkinId),
            guitarConfig = jsonToInstrumentConfig(obj.optJSONObject("guitarConfig"), defaults.guitarConfig),
            bassConfig = jsonToInstrumentConfig(obj.optJSONObject("bassConfig"), defaults.bassConfig),
            drumsConfig = jsonToInstrumentConfig(obj.optJSONObject("drumsConfig"), defaults.drumsConfig),
            vocalConfig = jsonToInstrumentConfig(obj.optJSONObject("vocalConfig"), defaults.vocalConfig),
            highwayConfig = highwayConfig,
            audioSettings = audioSettings,
            controllerSettings = controllerSettings,
            savedPresets = savedPresets,
            claimedChallenges = claimedChallenges
        )
    }

    private fun instrumentConfigToJson(c: InstrumentCustomConfig): JSONObject = JSONObject().apply {
        put("modelStyleId", c.modelStyleId)
        put("primaryColorHex", c.primaryColorHex)
        put("secondaryColorHex", c.secondaryColorHex)
        put("hardwareColorHex", c.hardwareColorHex)
        put("finish", c.finish.name)
        put("stampId", c.stampId)
        put("stickerId", c.stickerId)
        put("fullSkinId", c.fullSkinId)
    }

    private fun jsonToInstrumentConfig(obj: JSONObject?, defaults: InstrumentCustomConfig): InstrumentCustomConfig {
        if (obj == null) return defaults
        return InstrumentCustomConfig(
            modelStyleId = obj.optString("modelStyleId", defaults.modelStyleId),
            primaryColorHex = obj.optLong("primaryColorHex", defaults.primaryColorHex),
            secondaryColorHex = obj.optLong("secondaryColorHex", defaults.secondaryColorHex),
            hardwareColorHex = obj.optLong("hardwareColorHex", defaults.hardwareColorHex),
            finish = runCatching {
                InstrumentFinish.valueOf(obj.optString("finish", defaults.finish.name))
            }.getOrDefault(defaults.finish),
            stampId = obj.optString("stampId", defaults.stampId),
            stickerId = obj.optString("stickerId", defaults.stickerId),
            fullSkinId = obj.optString("fullSkinId", defaults.fullSkinId)
        )
    }
}
