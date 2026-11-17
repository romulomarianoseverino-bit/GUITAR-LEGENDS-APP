const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const path = require("node:path");
const rulesUnitDir = path.dirname(require.resolve("@firebase/rules-unit-testing"));
const firebase = require(require.resolve("firebase/compat/app", { paths: [rulesUnitDir] }));
require(require.resolve("firebase/compat/firestore", { paths: [rulesUnitDir] }));
const serverTimestamp = () => firebase.firestore.FieldValue.serverTimestamp();

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function utcTodayKey() {
  return new Date().toISOString().slice(0, 10);
}

function makeValidUserAccount(uid, nickname = "Axel_Shredder", syncRevision = 1) {
  return {
    userId: uid,
    email: `${uid}@rocklegends.test`,
    nickname,
    nicknameNormalized: nickname.toLowerCase(),
    coins: 450,
    totalScore: 0,
    notesHitTotal: 0,
    maxStreakOverall: 0,
    songsCompleted: 0,
    unlockedStars: 6,
    competitiveWins: 0,
    competitiveMatches: 0,
    careerStageMax: 1,
    selectedCharacterId: "axel_rocker",
    selectedGuitarId: "flame_caster",
    selectedBassId: "thunder_bass",
    selectedDrumsId: "arena_beast_kit",
    selectedVocalMicId: "vintage_shure_55s",
    selectedHighwayThemeId: "theme_ps2_wood",
    unlockedCosmetics: ["axel_rocker", "flame_caster"],
    achievementsUnlocked: [],
    customRocker: {
      photoUri: null,
      creationMethod: "INSPIRED_BY_PHOTO",
      artStyle: "STYLIZED_REALISTIC",
      hairStyle: "Moicano Punk Clássico",
      hairColorHex: 0xFFFF5722,
      topClothing: "Jaqueta de Couro c/ Rebites",
      pantsClothing: "Calça Jeans Rasgada",
      shoesClothing: "Coturnos de Couro Pesado",
      accessories: "Munhequeiras de Espinhos & Óculos Escuros Aviador",
      bodyTattoos: "Tatuagem Tribal e Chamas nos Braços",
    },
    glassAdaptiveEnabled: true,
    audioLatencyOffsetMs: 0,
    controlMode: "DIRECT_TOUCH",
    graphicsQuality: "HIGH",
    rewardStreak: 0,
    lastRewardDayKey: "",
    schemaVersion: 1,
    syncRevision,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("nicknames").doc("axel_shredder").get());
  await assertFails(unauthDb.collection("app_releases").get());
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("reward_claims")
      .doc(utcTodayKey())
      .get()
  );
});

test("Owner: can create, read, and update own UserAccount profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(docRef.set(makeValidUserAccount(ALICE_UID, "Axel_Shredder", 1)));
  await assertSucceeds(docRef.get());

  await assertSucceeds(
    docRef.update({
      coins: 600,
      syncRevision: 2,
      studioProfileJson: JSON.stringify({ audioSettings: { masterVolume: 0.9 } }),
      updatedAt: serverTimestamp(),
    })
  );

  await assertFails(
    docRef.update({
      syncRevision: 3,
      studioProfileJson: "x".repeat(25000),
      updatedAt: serverTimestamp(),
    })
  );
});

test("Cross-user isolation: Bob cannot read or modify Alice profile or subcollections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(makeValidUserAccount(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({ coins: 99999 })
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("reward_claims")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("progression_history")
      .get()
  );
});

test("Anti-tampering: rejects extra shadow fields and syncRevision rollback", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID);

  const invalidPayload = { ...makeValidUserAccount(ALICE_UID), isAdmin: true };
  await assertFails(docRef.set(invalidPayload));

  await assertSucceeds(docRef.set(makeValidUserAccount(ALICE_UID, "Axel_Shredder", 3)));
  await assertFails(
    docRef.update({
      syncRevision: 2,
      updatedAt: serverTimestamp(),
    })
  );
});

test("Nicknames: unique claim enforcement, no open listing, no cross-user hijack", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("nicknames").doc("slash_br").set({
      userId: ALICE_UID,
      nickname: "Slash_BR",
      createdAt: serverTimestamp(),
    })
  );

  // Availability lookup by signed-in user works on exact doc, list is denied
  await assertSucceeds(bobDb.collection("nicknames").doc("slash_br").get());
  await assertFails(bobDb.collection("nicknames").get());

  // Duplicate claim by Bob must fail
  await assertFails(
    bobDb.collection("nicknames").doc("slash_br").set({
      userId: BOB_UID,
      nickname: "Slash_BR",
      createdAt: serverTimestamp(),
    })
  );

  // Bob cannot delete Alice's nickname
  await assertFails(bobDb.collection("nicknames").doc("slash_br").delete());
});

test("Daily Rewards: enforces UTC server dayKey and immutability", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const today = utcTodayKey();
  const claimsRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("reward_claims");

  await assertSucceeds(
    claimsRef.doc(today).set({
      userId: ALICE_UID,
      dayKey: today,
      dayNumber: 1,
      streakCount: 1,
      rewardTitle: "150 Moedas de Lenda",
      rewardType: "coins",
      coinsGranted: 150,
      cosmeticId: "",
      claimedAt: serverTimestamp(),
    })
  );

  // Updating or deleting an immutable claim is rejected
  await assertFails(claimsRef.doc(today).update({ coinsGranted: 5000 }));
  await assertFails(claimsRef.doc(today).delete());

  // Forging past/future date is rejected by utcDayKey(request.time)
  await assertFails(
    claimsRef.doc("2099-01-01").set({
      userId: ALICE_UID,
      dayKey: "2099-01-01",
      dayNumber: 2,
      streakCount: 2,
      rewardTitle: "Recompensa Forjada",
      rewardType: "coins",
      coinsGranted: 500,
      cosmeticId: "",
      claimedAt: serverTimestamp(),
    })
  );
});

test("Progression history: append-only owner log", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const entryRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("progression_history")
    .doc("entry_1");

  await assertSucceeds(
    entryRef.set({
      userId: ALICE_UID,
      trackId: "track_thunderstrike",
      trackTitle: "Thunderstrike Anthem",
      instrument: "GUITAR",
      difficulty: "HARD",
      score: 48500,
      stars: 5,
      accuracyPercent: 98.4,
      coinsEarned: 360,
      isCompetitive: false,
      createdAt: serverTimestamp(),
    })
  );

  await assertFails(entryRef.update({ score: 999999 }));
  await assertFails(entryRef.delete());
});

test("App releases: authenticated read-only catalog", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("app_releases").doc("v1_2_0").set({
      versionCode: 2,
      versionName: "1.2.0",
      releaseTitle: "Edição Lendária 1.2.0",
      changelog: "Contas na nuvem, análise PCM real e recompensas diárias.",
      bugFixes: "Correção na prévia e validação de formatos.",
      performanceNotes: "Decoder PCM otimizado em background.",
      isMandatory: false,
      downloadUrl: "https://example.invalid/guitar-legends-1.2.0.apk",
      sha256Checksum: "",
      minSupportedVersionCode: 1,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("app_releases").get());
  await assertFails(
    aliceDb.collection("app_releases").doc("fake").set({ versionCode: 99 })
  );
});
