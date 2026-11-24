"use strict";

const assert = require("node:assert/strict");
const { test } = require("node:test");

const PROJECT_ID = process.env.GCLOUD_PROJECT || process.env.GOOGLE_CLOUD_PROJECT;
const DATABASE_ID = "ai-studio-android-guitarle-43a8bf45-7a84-48fd-9eb3-437bcb0e82ad";
const AUTH_HOST = process.env.FIREBASE_AUTH_EMULATOR_HOST;
const FIRESTORE_HOST = process.env.FIRESTORE_EMULATOR_HOST;
const FUNCTIONS_HOST = process.env.FUNCTIONS_EMULATOR_HOST || "127.0.0.1:5001";

function requireEmulatorEnvironment() {
  assert.match(PROJECT_ID || "", /^demo-/,
    "O teste exige um project ID demo-; nunca execute contra o Firebase de produção.");
  assert.equal(AUTH_HOST, "127.0.0.1:9099",
    "O teste exige o emulador local de Authentication em 127.0.0.1:9099.");
  assert.equal(FIRESTORE_HOST, "127.0.0.1:8085",
    "O teste exige o emulador local de Firestore em 127.0.0.1:8085.");
  assert.equal(FUNCTIONS_HOST, "127.0.0.1:5001",
    "O teste exige o emulador local de Functions em 127.0.0.1:5001.");
}

async function createEmulatorUser(label) {
  const response = await fetch(
    `http://${AUTH_HOST}/identitytoolkit.googleapis.com/v1/accounts:signUp?key=fake-api-key`,
    {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        email: `${label}-${Date.now()}-${Math.random().toString(16).slice(2)}@example.test`,
        password: "EmulatorOnly-Password-123!",
        returnSecureToken: true
      })
    }
  );
  const body = await response.json();
  assert.equal(response.status, 200, JSON.stringify(body));
  assert.ok(body.localId, "O Auth Emulator deve retornar o UID.");
  assert.ok(body.idToken, "O Auth Emulator deve retornar um token de teste.");
  return { uid: body.localId, token: body.idToken };
}

async function callNickname(user, nickname) {
  const response = await fetch(
    `http://${FUNCTIONS_HOST}/${PROJECT_ID}/us-central1/claimNicknameAndCreateAccount`,
    {
      method: "POST",
      headers: {
        "content-type": "application/json",
        authorization: `Bearer ${user.token}`
      },
      body: JSON.stringify({ data: { nickname } })
    }
  );
  return { status: response.status, body: await response.json() };
}

async function readFirestoreDocument(path, token) {
  const url = `http://${FIRESTORE_HOST}/v1/projects/${PROJECT_ID}/databases/${DATABASE_ID}/documents/${path}`;
  const response = await fetch(url, {
    headers: { authorization: `Bearer ${token}` }
  });
  if (response.status === 404) return null;
  const body = await response.json();
  assert.equal(response.status, 200, JSON.stringify(body));
  return body;
}

test("integração isolada: criar, repetir, trocar e proteger nickname", async () => {
  requireEmulatorEnvironment();

  const suffix = String(Date.now()).slice(-7);
  const first = await createEmulatorUser("nickname-first");
  const second = await createEmulatorUser("nickname-second");
  const nicknameA = `NickA${suffix}`;
  const nicknameB = `NickB${suffix}`;
  const keyA = nicknameA.toLowerCase();
  const keyB = nicknameB.toLowerCase();

  const created = await callNickname(first, nicknameA);
  assert.equal(created.status, 200, JSON.stringify(created.body));
  assert.equal(created.body.result.created, true);
  assert.equal(created.body.result.userId, first.uid);

  const firstReservation = await readFirestoreDocument(`nicknames/${keyA}`, first.token);
  assert.ok(firstReservation, "A primeira reserva deve existir.");
  assert.equal(firstReservation.fields.userId.stringValue, first.uid);
  const originalCreatedAt = firstReservation.fields.createdAt.timestampValue;

  const repeated = await callNickname(first, nicknameA);
  assert.equal(repeated.status, 200, JSON.stringify(repeated.body));
  assert.equal(repeated.body.result.created, false);

  const repeatedReservation = await readFirestoreDocument(`nicknames/${keyA}`, first.token);
  assert.equal(
    repeatedReservation.fields.createdAt.timestampValue,
    originalCreatedAt,
    "Salvar o mesmo nickname não pode recriar a reserva nem alterar createdAt."
  );

  const changed = await callNickname(first, nicknameB);
  assert.equal(changed.status, 200, JSON.stringify(changed.body));
  assert.equal(changed.body.result.nicknameNormalized, keyB);

  assert.equal(
    await readFirestoreDocument(`nicknames/${keyA}`, first.token),
    null,
    "A reserva antiga deve ser liberada após a troca legítima."
  );
  const secondReservation = await readFirestoreDocument(`nicknames/${keyB}`, first.token);
  assert.equal(secondReservation.fields.userId.stringValue, first.uid);

  const conflict = await callNickname(second, nicknameB);
  assert.notEqual(conflict.status, 200, "Outra conta não pode tomar um nickname reservado.");
  assert.equal(conflict.body.error.status, "ALREADY_EXISTS", JSON.stringify(conflict.body));

  const reservationAfterConflict = await readFirestoreDocument(`nicknames/${keyB}`, first.token);
  assert.equal(
    reservationAfterConflict.fields.userId.stringValue,
    first.uid,
    "A tentativa de outra conta não pode alterar nem apagar a reserva original."
  );
  assert.equal(
    await readFirestoreDocument(`users/${second.uid}`, second.token),
    null,
    "A conta rejeitada não deve receber um perfil parcialmente criado."
  );
});
