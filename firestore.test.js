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
const PROJECT_ID = process.env.GCP_PROJECT || "guitar-legends-d7708";
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
      databaseId: "ai-studio-android-guitarle-43a8bf45-7a84-48fd-9eb3-437bcb0e82ad",
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

test("Owner: can read own account but all client writes are denied", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID)
      .set(makeValidUserAccount(ALICE_UID, "Axel_Shredder", 1));
  });
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(docRef.get());
  await assertFails(docRef.set(makeValidUserAccount(ALICE_UID, "Axel_Shredder", 2)));
  await assertFails(docRef.update({ coins: 600 }));
  await assertFails(docRef.update({
    studioProfileJson: JSON.stringify({ audioSettings: { masterVolume: 0.9 } })
  }));
  await assertFails(docRef.delete());
});

test("Cross-user isolation: Bob cannot read or modify Alice profile or subcollections", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID)
      .set(makeValidUserAccount(ALICE_UID));
  });

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

test("Anti-tampering: client cannot create shadow fields or roll back account data", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID);
  const invalidPayload = { ...makeValidUserAccount(ALICE_UID), isAdmin: true };
  await assertFails(docRef.set(invalidPayload));
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID)
      .set(makeValidUserAccount(ALICE_UID, "Axel_Shredder", 3));
  });
  await assertFails(docRef.update({ syncRevision: 2, updatedAt: serverTimestamp() }));
});

test("Nicknames: unique claim enforcement, no open listing, no cross-user hijack", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("nicknames").doc("slash_br").set({
      userId: ALICE_UID,
      nickname: "Slash_BR",
      createdAt: serverTimestamp(),
    });
  });

  // Availability lookup by signed-in user works on exact doc, list is denied
  await assertSucceeds(bobDb.collection("nicknames").doc("slash_br").get());
  await assertFails(bobDb.collection("nicknames").get());

  // Duplicate claim by Bob must fail
  await assertFails(
    bobDb.collection("nicknames").doc("another_nick").set({
      userId: BOB_UID,
      nickname: "Another_Nick",
      createdAt: serverTimestamp(),
    })
  );
  await assertFails(aliceDb.collection("nicknames").doc("slash_br").delete());

  // Clients cannot delete or change nickname reservations.
  await assertFails(bobDb.collection("nicknames").doc("slash_br").delete());
});

test("Daily Rewards: clients can read but cannot create or modify claims", async () => {
  const today = utcTodayKey();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID)
      .collection("reward_claims").doc(today).set({
        userId: ALICE_UID, dayKey: today, dayNumber: 1, streakCount: 1,
        rewardTitle: "100 Moedas de Lenda", rewardType: "coins",
        coinsGranted: 100, cosmeticId: "", claimedAt: serverTimestamp(),
      });
  });
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const claimsRef = aliceDb.collection("users").doc(ALICE_UID).collection("reward_claims");
  await assertSucceeds(claimsRef.doc(today).get());
  await assertFails(claimsRef.doc(today).update({ coinsGranted: 5000 }));
  await assertFails(claimsRef.doc("2099-01-01").set({
    userId: ALICE_UID, dayKey: "2099-01-01", dayNumber: 2, streakCount: 2,
    rewardTitle: "Recompensa Forjada", rewardType: "coins",
    coinsGranted: 500, cosmeticId: "", claimedAt: serverTimestamp(),
  }));
  await assertFails(claimsRef.doc(today).delete());
});

test("Challenge claims and weekly totals: clients cannot forge, edit or inspect weekly counters", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const userRef = ctx.firestore().collection("users").doc(ALICE_UID);
    await userRef.set(makeValidUserAccount(ALICE_UID));
    await userRef.collection("challenge_claims").doc("chl_first_3_shows").set({
      userId: ALICE_UID, challengeId: "chl_first_3_shows", coinsGranted: 40,
      claimedAt: serverTimestamp(),
    });
    await userRef.collection("weekly_reward_totals").doc("2026-11-16").set({
      weekStart: "2026-11-16", coinsGranted: 100,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const userRef = aliceDb.collection("users").doc(ALICE_UID);
  const challengeClaims = userRef.collection("challenge_claims");
  const weeklyTotals = userRef.collection("weekly_reward_totals");

  await assertSucceeds(challengeClaims.doc("chl_first_3_shows").get());
  await assertFails(challengeClaims.doc("chl_second").set({
    userId: ALICE_UID, challengeId: "chl_second", coinsGranted: 100,
  }));
  await assertFails(challengeClaims.doc("chl_first_3_shows").update({ coinsGranted: 1000 }));
  await assertFails(weeklyTotals.doc("2026-11-16").get());
  await assertFails(weeklyTotals.doc("2026-11-16").set({
    weekStart: "2026-11-16", coinsGranted: 0,
  }));
});

test("Progression history: owner can read, clients cannot append or edit", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID)
      .collection("progression_history").doc("entry_1").set({
        userId: ALICE_UID, trackId: "track_thunderstrike",
        trackTitle: "Thunderstrike Anthem", instrument: "GUITAR",
        difficulty: "HARD", score: 48500, stars: 5,
        accuracyPercent: 98.4, coinsEarned: 50,
        isCompetitive: false, createdAt: serverTimestamp(),
      });
  });
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const entries = aliceDb.collection("users").doc(ALICE_UID).collection("progression_history");
  await assertSucceeds(entries.doc("entry_1").get());
  await assertFails(entries.doc("entry_2").set({ score: 999 }));
  await assertFails(entries.doc("entry_1").update({ score: 999999 }));
  await assertFails(entries.doc("entry_1").delete());
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

function makeValidRoom(roomCode, hostUid, mode = "BATTLE_1V1", maxPlayers = 2) {
  return {
    roomCode,
    hostUid,
    hostNickname: "Axel_Host",
    mode,
    status: "WAITING",
    trackId: "track_thunderstrike",
    trackTitle: "Thunderstrike Anthem",
    maxPlayers,
    memberUids: [hostUid],
    winnerUid: "",
    inviteMessage: "Entre na sala!",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function makeValidParticipant(roomCode, uid, nickname = "Rocker") {
  return {
    userId: uid,
    roomCode,
    nickname,
    instrument: "GUITAR",
    difficulty: "MEDIUM",
    ready: false,
    connectionState: "ONLINE",
    score: 0,
    combo: 0,
    rockMeter: 0.6,
    starPowerActive: false,
    lastHeartbeatMs: Date.now(),
    joinedAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

test("Multiplayer atomically creates a room and acoustic-guitar host participant", async () => {
  const roomCode = "ACOUSTIC-01";
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const roomRef = aliceDb.collection("multiplayer_rooms").doc(roomCode);
  const batch = aliceDb.batch();
  batch.set(roomRef, makeValidRoom(roomCode, ALICE_UID));
  batch.set(roomRef.collection("participants").doc(ALICE_UID), {
    ...makeValidParticipant(roomCode, ALICE_UID, "AliceAcoustic"),
    instrument: "ACOUSTIC_GUITAR",
  });
  await assertSucceeds(batch.commit());
  await assertSucceeds(roomRef.collection("participants").doc(ALICE_UID).get());
});

test("Multiplayer rooms: host create, exact-code join, participant sync, leave & terminal CLOSED lock", async () => {
  const roomCode = "ROCK-7782";
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  // 1. Unauthenticated caller denied
  await assertFails(unauthDb.collection("multiplayer_rooms").doc(roomCode).get());
  await assertFails(unauthDb.collection("multiplayer_rooms").doc(roomCode).set(makeValidRoom(roomCode, ALICE_UID)));

  // 2. Host Alice creates room and own participant record
  const aliceRoomRef = aliceDb.collection("multiplayer_rooms").doc(roomCode);
  await assertSucceeds(aliceRoomRef.set(makeValidRoom(roomCode, ALICE_UID)));
  await assertSucceeds(aliceRoomRef.collection("participants").doc(ALICE_UID).set(makeValidParticipant(roomCode, ALICE_UID, "AliceRock")));

  // 3. Open listing without membership filter denied; exact-code lookup allowed
  await assertFails(bobDb.collection("multiplayer_rooms").get());
  await assertSucceeds(bobDb.collection("multiplayer_rooms").doc(roomCode).get());

  // 4. Bob cannot create participant without joining parent memberUids first
  const bobRoomRef = bobDb.collection("multiplayer_rooms").doc(roomCode);
  await assertFails(bobRoomRef.collection("participants").doc(BOB_UID).set(makeValidParticipant(roomCode, BOB_UID, "BobRock")));

  // 5. Bob joins room cleanly (preserving Alice in memberUids) & creates own participant
  await assertSucceeds(
    bobRoomRef.update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(bobRoomRef.collection("participants").doc(BOB_UID).set(makeValidParticipant(roomCode, BOB_UID, "BobRock")));

  // 6. Bob updates own ready & score; cannot modify Alice's participant or spoof host settings
  await assertSucceeds(
    bobRoomRef.collection("participants").doc(BOB_UID).update({
      ready: true,
      score: 12400,
      combo: 28,
      rockMeter: 0.85,
      starPowerActive: true,
      lastHeartbeatMs: Date.now(),
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(bobRoomRef.collection("participants").doc(ALICE_UID).update({ score: 0 }));
  await assertFails(bobRoomRef.update({ status: "PLAYING", updatedAt: serverTimestamp() }));

  // 7. Third player Eve cannot exceed maxPlayers=2
  const eveDb = testEnv.authenticatedContext("eve_789").firestore();
  await assertFails(
    eveDb.collection("multiplayer_rooms").doc(roomCode).update({
      memberUids: [ALICE_UID, BOB_UID, "eve_789"],
      updatedAt: serverTimestamp(),
    })
  );

  // 8. Host starts match then closes room; updates rejected after CLOSED
  await assertSucceeds(aliceRoomRef.update({ status: "PLAYING", updatedAt: serverTimestamp() }));
  await assertSucceeds(aliceRoomRef.update({ status: "CLOSED", updatedAt: serverTimestamp() }));
  await assertFails(aliceRoomRef.update({ status: "WAITING", updatedAt: serverTimestamp() }));
});

