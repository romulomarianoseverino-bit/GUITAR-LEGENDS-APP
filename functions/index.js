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
  calculateMatchCoins
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
      tx.update(userRef, {
        coins: Number(user.coins || 0) + weekly.granted,
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
  const accuracyPercent = Number(data.accuracyPercent);
  const durationMs = Number(data.durationMs);
  const score = Number(data.score);
  const stars = Number(data.stars);
  const hitNotes = Number(data.hitNotes);
  const maxCombo = Number(data.maxCombo);
  const failed = data.failed === true;
  const isCompetitive = data.isCompetitive === true;
  if (![accuracyPercent, durationMs, score, stars, hitNotes, maxCombo].every(Number.isFinite) ||
      accuracyPercent < 0 || accuracyPercent > 100 || durationMs < 0 ||
      score < 0 || score > 50000000 || stars < 1 || stars > 5 ||
      hitNotes < 0 || hitNotes > 1000000 || maxCombo < 0 || maxCombo > 100000) {
    throw new HttpsError("invalid-argument", "Dados da partida fora dos limites permitidos.");
  }
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
