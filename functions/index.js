"use strict";

const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const {
  CHALLENGES,
  getUtcWeekStart,
  getUtcDayKey,
  getDailyReward,
  grantWithinWeeklyCap,
  calculateMatchCoins,
  COSMETIC_PRICES,
  getCosmeticPrice
} = require("./rewardLogic");

initializeApp();

const DATABASE_ID = "ai-studio-android-guitarle-43a8bf45-7a84-48fd-9eb3-437bcb0e82ad";
const db = getFirestore(undefined, DATABASE_ID);

function requireUid(request) {
  const uid = request.auth && request.auth.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Faça login para continuar.");
  return uid;
}

function safeId(value, label) {
  if (typeof value !== "string" || !/^[a-zA-Z0-9_-]{1,128}$/.test(value)) {
    throw new HttpsError("invalid-argument", label + " inválido.");
  }
  return value;
}

function getWeekRefs(uid, now) {
  const weekStart = getUtcWeekStart(now);
  const userRef = db.collection("users").doc(uid);
  return {
    userRef,
    weekStart,
    weekRef: userRef.collection("weekly_reward_totals").doc(weekStart)
  };
}

exports.claimDailyReward = onCall(async (request) => {
  const uid = requireUid(request);
  const now = new Date();
  const dayKey = getUtcDayKey(now);
  const { userRef, weekStart, weekRef } = getWeekRefs(uid, now);
  const claimRef = userRef.collection("reward_claims").doc(dayKey);

  try {
    return await db.runTransaction(async (tx) => {
      const [userSnap, claimSnap, weekSnap] = await Promise.all([
        tx.get(userRef), tx.get(claimRef), tx.get(weekRef)
      ]);
      if (!userSnap.exists) throw new HttpsError("failed-precondition", "Finalize seu cadastro antes de resgatar recompensas.");
      if (claimSnap.exists) throw new HttpsError("already-exists", "A recompensa diária de hoje já foi resgatada.");

      const user = userSnap.data();
      const previousDay = new Date(now);
      previousDay.setUTCDate(previousDay.getUTCDate() - 1);
      const yesterdayKey = getUtcDayKey(previousDay);
      const streak = user.lastRewardDayKey === yesterdayKey ? Number(user.rewardStreak || 0) + 1 : 1;
      const dayNumber = ((streak - 1) % 30) + 1;
      const reward = getDailyReward(dayNumber);
      const weekly = grantWithinWeeklyCap(reward.coinsGranted, weekSnap.exists ? weekSnap.get("coinsGranted") : 0);
      const updatedCosmetics = [...new Set([...(user.unlockedCosmetics || []), ...(reward.cosmeticId ? [reward.cosmeticId] : [])])];
      const updatedAchievements = [...new Set([...(user.achievementsUnlocked || []), ...(streak >= 3 ? ["ach_streak_3"] : [])])];

      tx.create(claimRef, {
        userId: uid, dayKey, dayNumber, streakCount: streak,
        rewardTitle: reward.title, rewardType: reward.rewardType,
        coinsGranted: weekly.granted, cosmeticId: reward.cosmeticId,
        claimedAt: FieldValue.serverTimestamp()
      });
      tx.set(weekRef, {
        weekStart, coinsGranted: weekly.total, updatedAt: FieldValue.serverTimestamp()
      }, { merge: true });
      tx.update(userRef, {
        coins: Number(user.coins || 0) + weekly.granted,
        unlockedCosmetics: updatedCosmetics,
        achievementsUnlocked: updatedAchievements,
        rewardStreak: streak,
        lastRewardDayKey: dayKey,
        syncRevision: Number(user.syncRevision || 0) + 1,
        updatedAt: FieldValue.serverTimestamp()
      });

      return {
        dayKey, dayNumber, streakCount: streak,
        rewardTitle: reward.title, rewardType: reward.rewardType,
        coinsGranted: weekly.granted, cosmeticId: reward.cosmeticId,
        weeklyCoinsGranted: weekly.total, weeklyLimit: 100, weekStart
      };
    });
  } catch (error) {
    if (error instanceof HttpsError) throw error;
    throw new HttpsError("internal", "Não foi possível resgatar a recompensa diária.");
  }
});

exports.claimChallengeReward = onCall(async (request) => {
  const uid = requireUid(request);
  const challengeId = safeId(request.data && request.data.challengeId, "Desafio");
  const challenge = CHALLENGES[challengeId];
  if (!challenge) throw new HttpsError("not-found", "Desafio não reconhecido pelo servidor.");

  const now = new Date();
  const { userRef, weekStart, weekRef } = getWeekRefs(uid, now);
  const claimRef = userRef.collection("challenge_claims").doc(challengeId);

  try {
    return await db.runTransaction(async (tx) => {
      const [userSnap, claimSnap, weekSnap] = await Promise.all([
        tx.get(userRef), tx.get(claimRef), tx.get(weekRef)
      ]);
      if (!userSnap.exists) throw new HttpsError("failed-precondition", "Conta não encontrada.");
      if (claimSnap.exists) throw new HttpsError("already-exists", "Este desafio já foi resgatado.");
      const user = userSnap.data();
      const progress = Number(user[challenge.progressField] || 0);
      if (progress < challenge.targetValue) {
        throw new HttpsError("failed-precondition", "Você ainda não concluiu este desafio.");
      }
      const weekly = grantWithinWeeklyCap(challenge.rewardCoins, weekSnap.exists ? weekSnap.get("coinsGranted") : 0);

      tx.create(claimRef, {
        userId: uid, challengeId, title: challenge.title,
        coinsGranted: weekly.granted, claimedAt: FieldValue.serverTimestamp()
      });
      tx.set(weekRef, {
        weekStart, coinsGranted: weekly.total, updatedAt: FieldValue.serverTimestamp()
      }, { merge: true });
      let studioProfile = {};
      try {
        studioProfile = user.studioProfileJson ? JSON.parse(user.studioProfileJson) : {};
      } catch (_) {
        studioProfile = {};
      }
      const claimedChallenges = Array.isArray(studioProfile.claimedChallenges)
        ? studioProfile.claimedChallenges : [];
      studioProfile.claimedChallenges = [...new Set([...claimedChallenges, challengeId])];

      tx.update(userRef, {
        coins: Number(user.coins || 0) + weekly.granted,
        studioProfileJson: JSON.stringify(studioProfile),
        syncRevision: Number(user.syncRevision || 0) + 1,
        updatedAt: FieldValue.serverTimestamp()
      });

      return {
        challengeId, title: challenge.title,
        coinsGranted: weekly.granted, weeklyCoinsGranted: weekly.total,
        weeklyLimit: 100, weekStart
      };
    });
  } catch (error) {
    if (error instanceof HttpsError) throw error;
    throw new HttpsError("internal", "Não foi possível resgatar o desafio.");
  }
});

exports.recordProgressionMatch = onCall(async (request) => {
  const uid = requireUid(request);
  const data = request.data || {};
  const trackId = safeId(data.trackId, "Música");
  const entryId = data.entryId ? safeId(data.entryId, "Identificador da partida") : db.collection("_ids").doc().id;
  const difficulty = String(data.difficulty || "");
  const instrument = String(data.instrument || "");
  const allowedDifficulties = ["EASY", "MEDIUM", "HARD", "EXPERT"];
  const allowedInstruments = ["GUITAR", "BASS", "DRUMS", "VOCALS"];
  if (!allowedDifficulties.includes(difficulty) || !allowedInstruments.includes(instrument)) {
    throw new HttpsError("invalid-argument", "Instrumento ou dificuldade inválidos.");
  }
  const durationMs = Number(data.durationMs);
  const score = Number(data.score);
  const totalNotes = Number(data.totalNotes);
  const hitNotes = Number(data.hitNotes);
  const maxCombo = Number(data.maxCombo);
  const failed = data.failed === true;
  const isCompetitive = data.isCompetitive === true;
  if (![durationMs, score, totalNotes, hitNotes, maxCombo].every(Number.isFinite) ||
      durationMs < 0 || score < 0 || score > 50000000 ||
      totalNotes < 1 || totalNotes > 1000000 || hitNotes < 0 || hitNotes > totalNotes ||
      maxCombo < 0 || maxCombo > 100000 || maxCombo > hitNotes) {
    throw new HttpsError("invalid-argument", "Dados da partida fora dos limites permitidos.");
  }
  // A precisão e as estrelas são recalculadas no servidor; não aceitamos estrelas enviadas pelo app.
  const accuracyPercent = (hitNotes / totalNotes) * 100;
  const stars = failed ? 1 : accuracyPercent >= 98 ? 5
    : accuracyPercent >= 90 ? 4 : accuracyPercent >= 75 ? 3
    : accuracyPercent >= 55 ? 2 : 1;
  const calculatedCoins = calculateMatchCoins({ accuracyPercent, difficulty, durationMs, failed });
  const { userRef, weekStart, weekRef } = getWeekRefs(uid, new Date());
  const entryRef = userRef.collection("progression_history").doc(entryId);

  try {
    return await db.runTransaction(async (tx) => {
      const [userSnap, entrySnap, weekSnap] = await Promise.all([
        tx.get(userRef), tx.get(entryRef), tx.get(weekRef)
      ]);
      if (!userSnap.exists) throw new HttpsError("failed-precondition", "Conta não encontrada.");
      if (entrySnap.exists) return { entryId, coinsEarned: entrySnap.get("coinsEarned") || 0, duplicate: true };
      const user = userSnap.data();
      const weekly = grantWithinWeeklyCap(calculatedCoins, weekSnap.exists ? weekSnap.get("coinsGranted") : 0);
      const coinsEarned = weekly.granted;

      tx.create(entryRef, {
        userId: uid, trackId, trackTitle: String(data.trackTitle || "Música").slice(0, 160),
        instrument, difficulty, score: Math.floor(score), stars: Math.floor(stars),
        accuracyPercent, coinsEarned, calculatedCoins, isCompetitive,
        createdAt: FieldValue.serverTimestamp()
      });
      tx.set(weekRef, {
        weekStart, coinsGranted: weekly.total, updatedAt: FieldValue.serverTimestamp()
      }, { merge: true });
      tx.update(userRef, {
        coins: Number(user.coins || 0) + coinsEarned,
        totalScore: Number(user.totalScore || 0) + Math.floor(score),
        notesHitTotal: Number(user.notesHitTotal || 0) + Math.floor(hitNotes),
        maxStreakOverall: Math.max(Number(user.maxStreakOverall || 0), Math.floor(maxCombo)),
        songsCompleted: Number(user.songsCompleted || 0) + (failed ? 0 : 1),
        unlockedStars: Number(user.unlockedStars || 0) + Math.floor(stars),
        competitiveMatches: Number(user.competitiveMatches || 0) + (isCompetitive ? 1 : 0),
        competitiveWins: Number(user.competitiveWins || 0) + (isCompetitive && !failed && stars >= 3 ? 1 : 0),
        // Indicadores separados: importações antigas não podem concluir desafios com recompensa.
        verifiedSongsCompleted: Number(user.verifiedSongsCompleted || 0) + (failed ? 0 : 1),
        verifiedNotesHitTotal: Number(user.verifiedNotesHitTotal || 0) + Math.floor(hitNotes),
        verifiedMaxStreakOverall: Math.max(Number(user.verifiedMaxStreakOverall || 0), Math.floor(maxCombo)),
        verifiedUnlockedStars: Number(user.verifiedUnlockedStars || 0) + (failed ? 0 : Math.floor(stars)),
        verifiedCompetitiveWins: Number(user.verifiedCompetitiveWins || 0) + (isCompetitive && !failed && stars >= 3 ? 1 : 0),
        syncRevision: Number(user.syncRevision || 0) + 1,
        updatedAt: FieldValue.serverTimestamp()
      });
      return { entryId, coinsEarned, duplicate: false };
    });
  } catch (error) {
    if (error instanceof HttpsError) throw error;
    throw new HttpsError("internal", "Não foi possível registrar a partida.");
  }
});

const DEFAULT_UNLOCKED_COSMETICS = [
  "axel_rocker", "roxie_riot", "flame_caster", "void_v",
  "thunder_bass", "skull_bass", "arena_beast_kit", "punk_garage_kit",
  "vintage_shure_55s", "golden_supreme_mic", "theme_ps2_wood",
  "theme_neon_cyber", "theme_inferno_flame", "theme_galactic_star",
  "outfit_skin_none", "outfit_skin_ps2_legend", "skin_none",
  "stamp_none", "stamp_ps2_flames", "stamp_checker_punk"
];


const KNOWN_COSMETICS = new Set([
  ...DEFAULT_UNLOCKED_COSMETICS,
  ...Object.keys(COSMETIC_PRICES)
]);
const STUDIO_SELECTION_FIELDS = new Set([
  "selectedCharacterId", "selectedGuitarId", "selectedBassId",
  "selectedDrumsId", "selectedVocalMicId", "selectedHighwayThemeId",
  "fullOutfitSkinId", "fullSkinId", "selectedStampId"
]);
const STUDIO_SELECTION_DEFAULTS = {
  selectedCharacterId: "axel_rocker",
  selectedGuitarId: "flame_caster",
  selectedBassId: "thunder_bass",
  selectedDrumsId: "arena_beast_kit",
  selectedVocalMicId: "vintage_shure_55s",
  selectedHighwayThemeId: "theme_ps2_wood",
  fullOutfitSkinId: "outfit_skin_none",
  fullSkinId: "skin_none",
  selectedStampId: "stamp_ps2_flames"
};

function sanitizeStudioSelections(incoming, existing, unlocked) {
  if (Array.isArray(incoming)) {
    return incoming.map((value, index) =>
      sanitizeStudioSelections(value, Array.isArray(existing) ? existing[index] : undefined, unlocked)
    );
  }
  if (!incoming || typeof incoming !== "object") return incoming;
  const result = {};
  for (const [key, value] of Object.entries(incoming)) {
    const previous = existing && typeof existing === "object" ? existing[key] : undefined;
    if (STUDIO_SELECTION_FIELDS.has(key) && typeof value === "string" &&
        KNOWN_COSMETICS.has(value) && !unlocked.has(value)) {
      result[key] = typeof previous === "string" &&
        (!KNOWN_COSMETICS.has(previous) || unlocked.has(previous))
        ? previous : (STUDIO_SELECTION_DEFAULTS[key] || "");
    } else {
      result[key] = sanitizeStudioSelections(value, previous, unlocked);
    }
  }
  return result;
}

function parseStudioProfile(value) {
  if (typeof value !== "string" || !value) return {};
  try {
    const parsed = JSON.parse(value);
    return parsed && typeof parsed === "object" && !Array.isArray(parsed) ? parsed : {};
  } catch (_) {
    return {};
  }
}

function cleanCustomRocker(value, fallback) {
  if (!value || typeof value !== "object" || Array.isArray(value)) return fallback;
  const out = { ...fallback };
  const stringFields = [
    "creationMethod", "artStyle", "hairStyle", "topClothing",
    "pantsClothing", "shoesClothing", "accessories", "bodyTattoos"
  ];
  for (const key of stringFields) {
    if (typeof value[key] === "string") out[key] = value[key].slice(0, 120);
  }
  if (typeof value.photoUri === "string" || value.photoUri === null) {
    out.photoUri = typeof value.photoUri === "string" ? value.photoUri.slice(0, 512) : null;
  }
  if (Number.isFinite(Number(value.hairColorHex))) {
    out.hairColorHex = Math.max(0, Math.min(0xFFFFFFFF, Math.floor(Number(value.hairColorHex))));
  }
  return out;
}

exports.claimNicknameAndCreateAccount = onCall(async (request) => {
  const uid = requireUid(request);
  const nickname = String(request.data && request.data.nickname || "").trim();
  if (!/^[a-zA-Z0-9_-]{3,24}$/.test(nickname)) {
    throw new HttpsError("invalid-argument", "O nickname deve ter de 3 a 24 caracteres (letras, números, _ ou -).");
  }
  const normalized = nickname.toLowerCase();
  const email = String(request.auth.token.email || "").slice(0, 200);
  const userRef = db.collection("users").doc(uid);
  const nicknameRef = db.collection("nicknames").doc(normalized);

  try {
    return await db.runTransaction(async (tx) => {
      const [userSnap, nicknameSnap] = await Promise.all([tx.get(userRef), tx.get(nicknameRef)]);
      if (nicknameSnap.exists && nicknameSnap.get("userId") !== uid) {
        throw new HttpsError("already-exists", "Este nickname já está em uso.");
      }
      const existing = userSnap.exists ? userSnap.data() : null;
      const oldNormalized = existing ? String(existing.nicknameNormalized || "") : "";
      let oldNicknameRef = null;
      if (oldNormalized && oldNormalized !== normalized) {
        oldNicknameRef = db.collection("nicknames").doc(oldNormalized);
        await tx.get(oldNicknameRef);
      }

      const now = FieldValue.serverTimestamp();
      if (existing) {
        tx.set(nicknameRef, {
          userId: uid, nickname, createdAt: nicknameSnap.exists
            ? (nicknameSnap.get("createdAt") || now) : now
        });
        if (oldNicknameRef) tx.delete(oldNicknameRef);
        tx.update(userRef, {
          email: email || String(existing.email || "").slice(0, 200), nickname, nicknameNormalized: normalized,
          syncRevision: Number(existing.syncRevision || 0) + 1,
          updatedAt: now
        });
      } else {
        tx.set(nicknameRef, { userId: uid, nickname, createdAt: now });
        tx.create(userRef, {
          userId: uid, email, nickname, nicknameNormalized: normalized,
          coins: 0, totalScore: 0, notesHitTotal: 0, maxStreakOverall: 0,
          songsCompleted: 0, unlockedStars: 6, competitiveWins: 0,
          competitiveMatches: 0, careerStageMax: 1,
          selectedCharacterId: "axel_rocker", selectedGuitarId: "flame_caster",
          selectedBassId: "thunder_bass", selectedDrumsId: "arena_beast_kit",
          selectedVocalMicId: "vintage_shure_55s", selectedHighwayThemeId: "theme_ps2_wood",
          unlockedCosmetics: DEFAULT_UNLOCKED_COSMETICS,
          achievementsUnlocked: [], customRocker: {},
          glassAdaptiveEnabled: true, audioLatencyOffsetMs: 0,
          controlMode: "DIRECT_TOUCH", graphicsQuality: "HIGH",
          rewardStreak: 0, lastRewardDayKey: "", schemaVersion: 3,
          syncRevision: 1, studioProfileJson: "",
          createdAt: now, updatedAt: now
        });
      }
      return { userId: uid, nickname, nicknameNormalized: normalized, created: !existing };
    });
  } catch (error) {
    if (error instanceof HttpsError) throw error;
    throw new HttpsError("internal", "Não foi possível salvar o nickname.");
  }
});

exports.syncAccountProfile = onCall(async (request) => {
  const uid = requireUid(request);
  const data = request.data || {};
  const userRef = db.collection("users").doc(uid);
  try {
    await db.runTransaction(async (tx) => {
      const snap = await tx.get(userRef);
      if (!snap.exists) throw new HttpsError("failed-precondition", "Conclua o cadastro antes de sincronizar o perfil.");
      const user = snap.data();
      const unlocked = new Set(user.unlockedCosmetics || []);
      const selections = {
        selectedCharacterId: ["axel_rocker", "roxie_riot", "kai_cyber", "custom_rocker"],
        selectedGuitarId: ["flame_caster", "void_v", "neon_thunder"],
        selectedBassId: ["thunder_bass", "skull_bass", "cyber_bass"],
        selectedDrumsId: ["arena_beast_kit", "punk_garage_kit", "electronic_neon_kit"],
        selectedVocalMicId: ["vintage_shure_55s", "golden_supreme_mic", "punk_distress_mic"],
        selectedHighwayThemeId: ["theme_ps2_wood", "theme_neon_cyber", "theme_inferno_flame", "theme_galactic_star"]
      };
      const update = {
        email: String(request.auth.token.email || user.email || "").slice(0, 200),
        syncRevision: Number(user.syncRevision || 0) + 1,
        updatedAt: FieldValue.serverTimestamp()
      };
      for (const [field, allowed] of Object.entries(selections)) {
        const candidate = data[field];
        if (typeof candidate === "string" && allowed.includes(candidate) &&
            (candidate === "custom_rocker" || unlocked.has(candidate))) {
          update[field] = candidate;
        }
      }
      update.customRocker = cleanCustomRocker(data.customRocker, user.customRocker || {});
      if (typeof data.glassAdaptiveEnabled === "boolean") update.glassAdaptiveEnabled = data.glassAdaptiveEnabled;
      if (Number.isInteger(data.audioLatencyOffsetMs)) {
        update.audioLatencyOffsetMs = Math.max(-500, Math.min(500, data.audioLatencyOffsetMs));
      }
      if (["DIRECT_TOUCH", "FRET_AND_STRUM"].includes(data.controlMode)) {
        update.controlMode = data.controlMode;
      }
      if (["HIGH", "BALANCED", "BATTERY_SAVER"].includes(data.graphicsQuality)) {
        update.graphicsQuality = data.graphicsQuality;
      }
      if (typeof data.studioProfileJson === "string" && data.studioProfileJson.length <= 24576) {
        const incomingStudio = parseStudioProfile(data.studioProfileJson);
        const existingStudio = parseStudioProfile(user.studioProfileJson);
        const sanitizedStudio = sanitizeStudioSelections(incomingStudio, existingStudio, unlocked);
        sanitizedStudio.claimedChallenges = Array.isArray(existingStudio.claimedChallenges)
          ? existingStudio.claimedChallenges : [];
        update.studioProfileJson = JSON.stringify(sanitizedStudio);
      }
      tx.update(userRef, update);
    });
    return { success: true };
  } catch (error) {
    if (error instanceof HttpsError) throw error;
    throw new HttpsError("internal", "Não foi possível sincronizar as configurações do perfil.");
  }
});

exports.purchaseCosmetic = onCall(async (request) => {
  const uid = requireUid(request);
  const cosmeticId = safeId(request.data && request.data.cosmeticId, "Item");
  const price = getCosmeticPrice(cosmeticId);
  if (price === null) throw new HttpsError("not-found", "Este item não está disponível na loja.");
  const userRef = db.collection("users").doc(uid);
  try {
    return await db.runTransaction(async (tx) => {
      const snap = await tx.get(userRef);
      if (!snap.exists) throw new HttpsError("failed-precondition", "Conta não encontrada.");
      const user = snap.data();
      const unlocked = Array.isArray(user.unlockedCosmetics) ? user.unlockedCosmetics : [];
      if (unlocked.includes(cosmeticId)) {
        return { cosmeticId, price, coins: Number(user.coins || 0), alreadyOwned: true };
      }
      const coins = Number(user.coins || 0);
      if (coins < price) throw new HttpsError("failed-precondition", "Moedas insuficientes.");
      tx.update(userRef, {
        coins: coins - price,
        unlockedCosmetics: [...unlocked, cosmeticId],
        syncRevision: Number(user.syncRevision || 0) + 1,
        updatedAt: FieldValue.serverTimestamp()
      });
      return { cosmeticId, price, coins: coins - price, alreadyOwned: false };
    });
  } catch (error) {
    if (error instanceof HttpsError) throw error;
    throw new HttpsError("internal", "Não foi possível concluir a compra.");
  }
});

exports.importLegacyProgress = onCall(async (request) => {
  const uid = requireUid(request);
  const legacy = request.data && request.data.legacyStats || {};
  const numericLimits = {
    totalScore: 50000000000,
    notesHitTotal: 10000000,
    maxStreakOverall: 100000,
    songsCompleted: 100000,
    unlockedStars: 1000000,
    competitiveWins: 100000,
    competitiveMatches: 100000,
    careerStageMax: 1000
  };
  const imported = {};
  for (const [field, limit] of Object.entries(numericLimits)) {
    const value = Number(legacy[field] || 0);
    if (!Number.isFinite(value) || value < 0) {
      throw new HttpsError("invalid-argument", "Progresso legado inválido.");
    }
    imported[field] = Math.min(limit, Math.floor(value));
  }
  const achievements = Array.isArray(legacy.achievementsUnlocked)
    ? legacy.achievementsUnlocked.filter(x => typeof x === "string" && /^[a-zA-Z0-9_-]{1,64}$/.test(x)).slice(0, 60)
    : [];
  const userRef = db.collection("users").doc(uid);
  try {
    return await db.runTransaction(async (tx) => {
      const snap = await tx.get(userRef);
      if (!snap.exists) throw new HttpsError("failed-precondition", "Conta não encontrada.");
      const user = snap.data();
      if (user.legacyProgressImportedAt) {
        return { imported: false, alreadyImported: true };
      }
      const update = {
        totalScore: Math.max(Number(user.totalScore || 0), imported.totalScore),
        notesHitTotal: Math.max(Number(user.notesHitTotal || 0), imported.notesHitTotal),
        maxStreakOverall: Math.max(Number(user.maxStreakOverall || 0), imported.maxStreakOverall),
        songsCompleted: Math.max(Number(user.songsCompleted || 0), imported.songsCompleted),
        unlockedStars: Math.max(Number(user.unlockedStars || 0), imported.unlockedStars),
        competitiveWins: Math.max(Number(user.competitiveWins || 0), imported.competitiveWins),
        competitiveMatches: Math.max(Number(user.competitiveMatches || 0), imported.competitiveMatches),
        careerStageMax: Math.max(Number(user.careerStageMax || 1), imported.careerStageMax),
        achievementsUnlocked: [...new Set([...(user.achievementsUnlocked || []), ...achievements])].slice(0, 60),
        legacyProgressImportedAt: FieldValue.serverTimestamp(),
        legacyProgressImportVersion: 1,
        syncRevision: Number(user.syncRevision || 0) + 1,
        updatedAt: FieldValue.serverTimestamp()
      };
      // Moedas, inventário, recompensas e desafios já resgatados nunca são importados do cliente.
      tx.update(userRef, update);
      return { imported: true, alreadyImported: false };
    });
  } catch (error) {
    if (error instanceof HttpsError) throw error;
    throw new HttpsError("internal", "Não foi possível importar o progresso antigo.");
  }
});
