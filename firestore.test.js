const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated: cannot read rooms or users", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("rooms").get());
  await assertFails(unauth.collection("users").get());
});

test("Authenticated Alice: can create own profile", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      gamesPlayed: 0,
      gamesWon: 0,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated Bob: cannot create Alice's profile", async () => {
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bob.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Spoof",
      gamesPlayed: 0,
      gamesWon: 0,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated Alice: can create LudoRoom", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    alice.collection("rooms").doc("ROOM1").set({
      roomId: "ROOM1",
      name: "Alice's Room",
      hostId: ALICE_UID,
      status: "waiting",
      isPublic: true,
      maxPlayers: 4,
      playerIds: [ALICE_UID],
      players: [{ id: ALICE_UID, name: "Alice", color: "red", isHost: true }],
      turn: "red",
      dice: 0,
      rolled: false,
      winner: "",
      tokens: { red: [0, 0, 0, 0] },
      activeColors: ["red"],
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated Bob: can join Alice's waiting room", async () => {
  // Seed room
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("rooms").doc("ROOM1").set({
      roomId: "ROOM1",
      name: "Alice's Room",
      hostId: ALICE_UID,
      status: "waiting",
      isPublic: true,
      maxPlayers: 4,
      playerIds: [ALICE_UID],
      players: [{ id: ALICE_UID, name: "Alice", color: "red", isHost: true }],
      turn: "red",
      dice: 0,
      rolled: false,
      winner: "",
      tokens: { red: [0, 0, 0, 0] },
      activeColors: ["red"],
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bob.collection("rooms").doc("ROOM1").update({
      playerIds: [ALICE_UID, BOB_UID],
      players: [
        { id: ALICE_UID, name: "Alice", color: "red", isHost: true },
        { id: BOB_UID, name: "Bob", color: "green", isHost: false },
      ],
      tokens: { red: [0, 0, 0, 0], green: [0, 0, 0, 0] },
      activeColors: ["red", "green"],
      updatedAt: new Date(),
    })
  );
});

test("Authenticated Alice: can query public rooms", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("rooms").doc("ROOM1").set({
      roomId: "ROOM1",
      hostId: BOB_UID,
      status: "waiting",
      isPublic: true,
      maxPlayers: 4,
      playerIds: [BOB_UID],
      players: [{ id: BOB_UID, name: "Bob", color: "red", isHost: true }],
      turn: "red",
      dice: 0,
      rolled: false,
      tokens: { red: [0, 0, 0, 0] },
      activeColors: ["red"],
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    alice.collection("rooms").where("isPublic", "==", true).get()
  );
});
