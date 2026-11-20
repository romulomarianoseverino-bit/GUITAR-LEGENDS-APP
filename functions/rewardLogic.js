"use strict";

const DAILY_REWARDS = [
  { title: "10 Moedas de Lenda", rewardType: "coins", coinsGranted: 10, cosmeticId: "" },
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
  // Indicadores separados do progresso histórico importado do dispositivo.
  chl_first_3_shows: { title: "Aquecimento da Turnê", progressField: "verifiedSongsCompleted", targetValue: 3, rewardCoins: 100 },
  chl_hit_250_notes: { title: "Dedos Incansáveis", progressField: "verifiedNotesHitTotal", targetValue: 250, rewardCoins: 100 },
  chl_combo_35: { title: "Ritmo Inabalável", progressField: "verifiedMaxStreakOverall", targetValue: 35, rewardCoins: 100 },
  chl_stars_12: { title: "Caçador de Estrelas", progressField: "verifiedUnlockedStars", targetValue: 12, rewardCoins: 100 },
  chl_duel_win: { title: "Rei do Duelo 1v1", progressField: "verifiedCompetitiveWins", targetValue: 1, rewardCoins: 100 }
};

const COSMETIC_PRICES = Object.freeze({
  hair_moicano_turquesa: 150,
  roxie_riot: 350,
  void_v: 250,
  skull_bass: 300,
  punk_garage_kit: 250,
  golden_supreme_mic: 350,
  outfit_chamas_palco: 220,
  acc_oculos_neon: 180,
  tattoo_raio_carmesim: 160,
  kai_cyber: 600,
  neon_thunder: 500,
  cyber_bass: 450,
  electronic_neon_kit: 500,
  punk_distress_mic: 400,
  outfit_skin_inferno: 280,
  outfit_skin_cyber: 420,
  skin_chrome_hero: 220,
  skin_golden_tour: 450,
  skin_plasma_neon: 380,
  stamp_cyber_circuit: 180,
  stamp_skull_stage: 260
});

function getCosmeticPrice(cosmeticId) {
  return Object.prototype.hasOwnProperty.call(COSMETIC_PRICES, cosmeticId)
    ? COSMETIC_PRICES[cosmeticId] : null;
}

const WEEKLY_REWARD_CAP = 100;
const MATCH_REWARD_CAP = 50;

const REWARD_TIME_ZONE = "America/Sao_Paulo";

function getDateKeyInRewardTimeZone(date) {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: REWARD_TIME_ZONE,
    year: "numeric",
    month: "2-digit",
    day: "2-digit"
  }).formatToParts(date);
  const values = Object.fromEntries(parts.map(part => [part.type, part.value]));
  return values.year + "-" + values.month + "-" + values.day;
}

function getUtcWeekStart(date) {
  // O identificador é a segunda-feira do calendário brasileiro, não da semana UTC.
  const [year, month, dayOfMonth] = getDateKeyInRewardTimeZone(date).split("-").map(Number);
  const localCalendarDate = new Date(Date.UTC(year, month - 1, dayOfMonth));
  const day = localCalendarDate.getUTCDay();
  localCalendarDate.setUTCDate(localCalendarDate.getUTCDate() - ((day + 6) % 7));
  return localCalendarDate.toISOString().slice(0, 10);
}

function getUtcDayKey(date) {
  return getDateKeyInRewardTimeZone(date);
}

function getDailyReward(dayNumber) {
  const index = ((dayNumber - 1) % DAILY_REWARDS.length + DAILY_REWARDS.length) % DAILY_REWARDS.length;
  return { ...DAILY_REWARDS[index], dayNumber };
}

function calculateDailyRewardGrant(requestedCoins, claimedDailyCoins) {
  const used = Math.max(0, Math.min(WEEKLY_REWARD_CAP, Number(claimedDailyCoins) || 0));
  const requested = Math.max(0, Number(requestedCoins) || 0);
  const granted = Math.min(requested, WEEKLY_REWARD_CAP - used);
  return { granted, total: used + granted };
}

function calculateMatchStars({ accuracyPercent, failed = false }) {
  if (failed) return 1;
  const accuracy = Math.max(0, Math.min(100, Number(accuracyPercent) || 0));
  if (accuracy >= 98) return 5;
  if (accuracy >= 90) return 4;
  if (accuracy >= 75) return 3;
  if (accuracy >= 55) return 2;
  return 1;
}

function calculateMatchCoins({ accuracyPercent, difficulty, durationMs, failed = false }) {
  if (failed) return 0;
  const accuracy = Math.max(0, Math.min(100, Number(accuracyPercent) || 0));
  if (!["EASY", "MEDIUM", "HARD", "EXPERT"].includes(difficulty)) throw new Error("Dificuldade inválida.");
  // Partida concluída: 20 moedas de base + até 30 proporcionais à precisão.
  // Duração e dificuldade não reduzem a recompensa-base; falhas continuam sem prêmio.
  return Math.max(20, Math.min(MATCH_REWARD_CAP, Math.floor(20 + (accuracy / 100) * 30)));
}

module.exports = {
  DAILY_REWARDS,
  CHALLENGES,
  COSMETIC_PRICES,
  getCosmeticPrice,
  WEEKLY_REWARD_CAP,
  MATCH_REWARD_CAP,
  getUtcWeekStart,
  getUtcDayKey,
  getDailyReward,
  calculateDailyRewardGrant,
  calculateMatchCoins,
  calculateMatchStars
};
