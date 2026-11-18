"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const {
  getUtcWeekStart,
  getDailyReward,
  grantWithinWeeklyCap,
  calculateMatchCoins,
  calculateMatchStars,
  getCosmeticPrice,
  CHALLENGES
} = require("./rewardLogic");

test("semana começa na segunda-feira e termina no domingo", () => {
  assert.equal(getUtcWeekStart(new Date("2026-11-16T12:00:00Z")), "2026-11-16");
  assert.equal(getUtcWeekStart(new Date("2026-11-22T12:00:00Z")), "2026-11-16");
  assert.equal(getUtcWeekStart(new Date("2026-11-23T00:00:00Z")), "2026-11-23");
});

test("limite semanal soma recompensas e nunca ultrapassa 100 moedas", () => {
  assert.deepEqual(grantWithinWeeklyCap(100, 0), { granted: 100, total: 100 });
  assert.deepEqual(grantWithinWeeklyCap(100, 60), { granted: 40, total: 100 });
  assert.deepEqual(grantWithinWeeklyCap(100, 100), { granted: 0, total: 100 });
});

test("catálogo diário preserva os itens cosméticos e o ciclo", () => {
  assert.equal(getDailyReward(1).coinsGranted, 100);
  assert.equal(getDailyReward(7).cosmeticId, "kai_cyber");
  assert.equal(getDailyReward(16).title, getDailyReward(1).title);
});

test("recompensa de música tem teto exato de 50 e falha paga zero", () => {
  assert.equal(calculateMatchCoins({
    accuracyPercent: 100, difficulty: "EXPERT", durationMs: 360000, failed: false
  }), 50);
  assert.equal(calculateMatchCoins({
    accuracyPercent: 100, difficulty: "EXPERT", durationMs: 360000, failed: true
  }), 0);
  assert.ok(calculateMatchCoins({
    accuracyPercent: 60, difficulty: "MEDIUM", durationMs: 120000, failed: false
  }) < 50);
});

test("preços de cosméticos são definidos pelo catálogo do servidor", () => {
  assert.equal(getCosmeticPrice("hair_moicano_turquesa"), 150);
  assert.equal(getCosmeticPrice("kai_cyber"), 600);
  assert.equal(getCosmeticPrice("stamp_skull_stage"), 260);
  assert.equal(getCosmeticPrice("item_inexistente"), null);
});

test("desafios usam indicadores atualizados pelo servidor após partidas", () => {
  assert.equal(CHALLENGES.chl_first_3_shows.progressField, "verifiedSongsCompleted");
  assert.equal(CHALLENGES.chl_hit_250_notes.progressField, "verifiedNotesHitTotal");
  assert.equal(CHALLENGES.chl_combo_35.progressField, "verifiedMaxStreakOverall");
  assert.equal(CHALLENGES.chl_stars_12.progressField, "verifiedUnlockedStars");
  assert.equal(CHALLENGES.chl_duel_win.progressField, "verifiedCompetitiveWins");
});

test("estrelas são calculadas no servidor pela precisão, sem confiar no app", () => {
  assert.equal(calculateMatchStars({ accuracyPercent: 99, failed: false }), 5);
  assert.equal(calculateMatchStars({ accuracyPercent: 92, failed: false }), 4);
  assert.equal(calculateMatchStars({ accuracyPercent: 80, failed: false }), 3);
  assert.equal(calculateMatchStars({ accuracyPercent: 60, failed: false }), 2);
  assert.equal(calculateMatchStars({ accuracyPercent: 100, failed: true }), 1);
});
