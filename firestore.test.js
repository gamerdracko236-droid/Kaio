const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_sertaneja";
const BOB_UID = "bob_cangaceiro";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validSavePayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    slotLabel: "Salvamento Automático",
    heroName: "Kauan",
    hatStyle: "VAQUEIRO",
    outfitStyle: "GIBAO_COURO",
    bladeColor: "BRASA_VIVA",
    activeStyle: "CHAMA",
    level: 1,
    xp: 0,
    coins: 50,
    currentChapter: 1,
    currentRegionId: "vila_das_almas",
    skillPoints: 1,
    attackBonus: 0,
    defenseBonus: 0,
    energyBonus: 0,
    weaponUpgrade: 1,
    playTimeSeconds: 120,
    unlockedRegions: ["vila_das_almas"],
    completedQuests: [],
    unlockedSkills: ["chama_1"],
    unlockedAchievements: [],
    inventoryEntries: ["pocao:3", "rapadura:2"],
    createdAt: now,
    updatedAt: now,
    ...overrides,
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

test("Unauthenticated user cannot read or write save slots", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("save_slots").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("save_slots")
      .doc("autosave")
      .set(validSavePayload(ALICE_UID))
  );
});

test("Owner can create, list, update, and delete valid save slots", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const slotRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("save_slots")
    .doc("autosave");

  await assertSucceeds(slotRef.set(validSavePayload(ALICE_UID)));

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("save_slots").get()
  );

  await assertSucceeds(
    slotRef.update({
      level: 3,
      xp: 180,
      coins: 120,
      currentChapter: 2,
      currentRegionId: "caatinga_profunda",
      unlockedRegions: ["vila_das_almas", "caatinga_profunda"],
      completedQuests: ["q_vila_1"],
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(slotRef.delete());
});

test("Cross-user access is strictly denied", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("save_slots")
      .doc("slot_1")
      .set(validSavePayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("save_slots").get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("save_slots")
      .doc("slot_1")
      .get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("save_slots")
      .doc("slot_1")
      .update({ coins: 999999, updatedAt: new Date(Date.now() - 500) })
  );
});

test("Invalid fields, @DocumentId id injection, immutable createdAt mutation, and out-of-bounds values fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const slotsCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("save_slots");

  // Extra @DocumentId field must fail
  await assertFails(
    slotsCol.doc("slot_1").set(validSavePayload(ALICE_UID, { id: "slot_1" }))
  );

  // Invalid enum must fail
  await assertFails(
    slotsCol
      .doc("slot_2")
      .set(validSavePayload(ALICE_UID, { activeStyle: "INVALIDO" }))
  );

  // Out-of-bounds level must fail
  await assertFails(
    slotsCol.doc("slot_3").set(validSavePayload(ALICE_UID, { level: 999 }))
  );

  // Future timestamp must fail
  await assertFails(
    slotsCol
      .doc("slot_4")
      .set(
        validSavePayload(ALICE_UID, {
          createdAt: new Date(Date.now() + 60000),
          updatedAt: new Date(Date.now() + 60000),
        })
      )
  );

  // Valid create, then reject immutable createdAt update
  await assertSucceeds(slotsCol.doc("autosave").set(validSavePayload(ALICE_UID)));
  await assertFails(
    slotsCol.doc("autosave").update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );
});
