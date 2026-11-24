"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const { canReleaseNicknameReservation } = require("./nicknameLogic");

test("libera reserva antiga pertencente ao mesmo usuário", () => {
  const snapshot = { exists: true, get: (field) => field === "userId" ? "uid-a" : null };
  assert.equal(canReleaseNicknameReservation(snapshot, "uid-a"), true);
});

test("não libera reserva pertencente a outra conta", () => {
  const snapshot = { exists: true, get: (field) => field === "userId" ? "uid-b" : null };
  assert.equal(canReleaseNicknameReservation(snapshot, "uid-a"), false);
});

test("não tenta liberar reserva inexistente", () => {
  assert.equal(canReleaseNicknameReservation({ exists: false, get: () => null }, "uid-a"), false);
  assert.equal(canReleaseNicknameReservation(null, "uid-a"), false);
});
