"use strict";

const DAILY_REWARDS = [
  { title: "100 Moedas de Lenda", rewardType: "coins", coinsGranted: 100, cosmeticId: "" },
  { title: "Jaqueta Chamas de Palco", rewardType: "cosmetic", coinsGranted: 100, cosmeticId: "outfit_chamas_palco" },
  { title: "100 Moedas de Lenda", rewardType: "coins", coinsGranted: 100, cosmeticId: "" },
  { title: "Óculos Neon Holográficos", rewardType: "cosmetic", coinsGranted: 100, cosmeticId: "acc_oculos_neon" },
  { title: "Moicano Turquesa Elétrico", rewardType: "cosmetic", coinsGranted: 100, cosmeticId: "hair_moicano_turquesa" },
  { title: "100 Moedas de Lenda", rewardType: "coins", coinsGranted: 100, cosmeticId: "" },
  { title: "Bônus Semanal: Kai Neo-Anime", rewardType: "bundle", coinsGranted: 100, cosmeticId: "kai_cyber" },
  { title: "100 Moedas de Lenda", rewardType: "coins", coinsGranted: 100, cosmeticId: "" },
  { title: "Tatuagem Raio Carmesim", rewardType: "cosmetic", coinsGranted: 100, cosmeticId: "tattoo_raio_carmesim" },
  { title: "100 Moedas de Lenda", rewardType: "coins", coinsGranted: 100, cosmeticId: "" },
  { title: "Neon Cyber Strat", rewardType: "skin", coinsGranted: 100, cosmeticId: "neon_thunder" },
  { title: "100 Moedas de Lenda", rewardType: "coins", coinsGranted: 100, cosmeticId: "" },
  { title: "Cyber Wave Fretless", rewardType: "skin", coinsGranted: 100, cosmeticId: "cyber_bass" },
  { title: "Bônus Quinzenal: Bateria Cyber", rewardType: "bundle", coinsGranted: 100, cosmeticId: "electronic_neon_kit" },
  { title: "Microfone Riot Distress", rewardType: "skin", coinsGranted: 100, cosmeticId: "punk_distress_mic" }
];

const CHALLENGES = {
  chl_first_3_shows: { title: "Aquecimento da Turnê", progressField: "songsCompleted", targetValue: 3, rewardCoins: 100 },
  chl_hit_250_notes: { title: "Dedos Incansáveis", progressField: "notesHitTotal", targetValue: 250, rewardCoins: 100 },
  chl_combo_35: { title: "Ritmo Inabalável", progressField: "maxStreakOverall", targetValue: 35, rewardCoins: 100 },
  chl_stars_12: { title: "Caçador de Estrelas", progressField: "unlockedStars", targetValue: 12, rewardCoins: 100 },
  chl_duel_win: { title: "Rei do Duelo 1v1", progressField: "competitiveWins", targetValue: 1, rewardCoins: 100 }
};

const WEEKLY_REWARD_CAP = 100;
const MATCH_REWARD_CAP = 50;

function getUtcWeekStart(date) {
  const d = new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()));
  const day = d.getUTCDay();
  d.setUTCDate(d.getUTCDate() - ((day + 6) % 7));
  return d.toISOString().slice(0, 10);
}

function getUtcDayKey(date) {
  return date.toISOString().slice(0, 10);
}

function getDailyReward(dayNumber) {
  const index = ((dayNumber - 1) % DAILY_REWARDS.length + DAILY_REWARDS.length) % DAILY_REWARDS.length;
  return { ...DAILY_REWARDS[index], dayNumber };
}

function grantWithinWeeklyCap(requestedCoins, alreadyGranted) {
  const used = Math.max(0, Math.min(WEEKLY_REWARD_CAP, Number(alreadyGranted) || 0));
  const requested = Math.max(0, Math.min(WEEKLY_REWARD_CAP, Number(requestedCoins) || 0));
  const granted = Math.min(requested, WEEKLY_REWARD_CAP - used);
  return { granted, total: used + granted };
}

function calculateMatchCoins({ accuracyPercent, difficulty, durationMs, failed = false }) {
  if (failed) return 0;
  const accuracy = Math.max(0, Math.min(100, Number(accuracyPercent) || 0));
  const durationMinutes = Math.max(0.5, Math.min(6, Math.max(30000, Number(durationMs) || 30000) / 60000));
  const factors = { EASY: 0.75, MEDIUM: 0.9, HARD: 1.05, EXPERT: 1.2 };
  const factor = factors[difficulty];
  if (!factor) throw new Error("Dificuldade inválida.");
  const accuracyFactor = 0.25 + (accuracy / 100) * 0.75;
  const durationBase = 4 + durationMinutes * (19 / 3);
  return Math.max(0, Math.min(MATCH_REWARD_CAP, Math.floor(durationBase * factor * accuracyFactor)));
}

module.exports = {
  DAILY_REWARDS,
  CHALLENGES,
  WEEKLY_REWARD_CAP,
  MATCH_REWARD_CAP,
  getUtcWeekStart,
  getUtcDayKey,
  getDailyReward,
  grantWithinWeeklyCap,
  calculateMatchCoins
};
