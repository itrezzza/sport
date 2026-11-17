const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const serverTimestampFor = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ROOM_ID = "match_derby_1";

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

test("Unauthenticated user cannot read or list match chat messages", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("match_rooms").doc(ROOM_ID).collection("messages").get());
  await assertFails(unauthDb.collection("match_rooms").doc(ROOM_ID).collection("messages").doc("msg_1").get());
});

test("Unauthenticated user cannot send chat messages", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("match_rooms").doc(ROOM_ID).collection("messages").doc("msg_1").set({
      roomId: ROOM_ID,
      userId: ALICE_UID,
      senderName: "علی",
      text: "سلام",
      createdAt: serverTimestampFor(unauthDb),
    })
  );
});

test("Authenticated user can send and list messages in valid match room", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("match_rooms").doc(ROOM_ID).collection("messages").doc("msg_1").set({
      roomId: ROOM_ID,
      userId: ALICE_UID,
      senderName: "علی",
      text: "چه گلی زد!",
      createdAt: serverTimestampFor(aliceDb),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("match_rooms").doc(ROOM_ID).collection("messages").orderBy("createdAt").limitToLast(50).get()
  );
});

test("Authenticated user cannot spoof another userId or inject ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("match_rooms").doc(ROOM_ID).collection("messages").doc("msg_spoof").set({
      roomId: ROOM_ID,
      userId: BOB_UID,
      senderName: "علی",
      text: "پیام جعلی",
      createdAt: serverTimestampFor(aliceDb),
    })
  );

  await assertFails(
    aliceDb.collection("match_rooms").doc(ROOM_ID).collection("messages").doc("msg_ghost").set({
      roomId: ROOM_ID,
      userId: ALICE_UID,
      senderName: "علی",
      text: "پیام با فیلد اضافه",
      isAdmin: true,
      createdAt: serverTimestampFor(aliceDb),
    })
  );
});

test("Authenticated user cannot send oversized or empty message text or mismatched roomId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("match_rooms").doc(ROOM_ID).collection("messages").doc("msg_empty").set({
      roomId: ROOM_ID,
      userId: ALICE_UID,
      senderName: "علی",
      text: "",
      createdAt: serverTimestampFor(aliceDb),
    })
  );

  await assertFails(
    aliceDb.collection("match_rooms").doc(ROOM_ID).collection("messages").doc("msg_wrong_room").set({
      roomId: "other_room",
      userId: ALICE_UID,
      senderName: "علی",
      text: "سلام",
      createdAt: serverTimestampFor(aliceDb),
    })
  );

  await assertFails(
    aliceDb.collection("match_rooms").doc(ROOM_ID).collection("messages").doc("msg_too_long").set({
      roomId: ROOM_ID,
      userId: ALICE_UID,
      senderName: "علی",
      text: "آ".repeat(301),
      createdAt: serverTimestampFor(aliceDb),
    })
  );
});

test("Chat messages are immutable on update and only owner can delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const refAlice = aliceDb.collection("match_rooms").doc(ROOM_ID).collection("messages").doc("msg_immutable");
  await assertSucceeds(
    refAlice.set({
      roomId: ROOM_ID,
      userId: ALICE_UID,
      senderName: "علی",
      text: "نبرد عالی",
      createdAt: serverTimestampFor(aliceDb),
    })
  );

  await assertFails(refAlice.update({ text: "ویرایش" }));
  await assertFails(bobDb.collection("match_rooms").doc(ROOM_ID).collection("messages").doc("msg_immutable").delete());
  await assertSucceeds(refAlice.delete());
});
