const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const ALICE_UID = "alice_user_123";
const BOB_UID = "bob_user_456";

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

test("Unauthenticated user: cannot read journeys", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("journeys").get());
});

test("Authenticated user: can create and read their own journey", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const journeyId = "JRN-20261015-A7F42";
  
  await assertSucceeds(
    aliceDb.collection("journeys").doc(journeyId).set({
      journeyId: journeyId,
      userId: ALICE_UID,
      vehicleNumber: "MH12AB1234",
      startLocation: "Pune",
      destination: "Mumbai",
      status: "ACTIVE",
      totalTollsDeducted: 0,
      tollsPassedCount: 0,
      servicesUsedCount: 0,
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("journeys").where("userId", "==", ALICE_UID).get()
  );
});

test("Cross-user access: Bob cannot read Alice's journey", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("journeys").doc("JRN-20261015-A7F42").set({
      journeyId: "JRN-20261015-A7F42",
      userId: ALICE_UID,
      vehicleNumber: "MH12AB1234",
      startLocation: "Pune",
      destination: "Mumbai",
      status: "ACTIVE",
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("journeys").doc("JRN-20261015-A7F42").get());
});

test("Authenticated user: can read toll plazas list", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("tolls").doc("TOLL-01").set({
      tollId: "TOLL-01",
      name: "Khedshivapur Plaza",
      highway: "NH-48",
      latitude: 18.3245,
      longitude: 73.8654,
      standardRate: 115,
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("tolls").doc("TOLL-01").get());
  await assertSucceeds(aliceDb.collection("tolls").get());
});

test("Wallet transaction: Alice can record transaction for her own account", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const txId = "TXN-987654";
  await assertSucceeds(
    aliceDb.collection("transactions").doc(txId).set({
      transactionId: txId,
      userId: ALICE_UID,
      type: "WALLET_TOPUP",
      amount: 500,
      title: "Wallet Recharge",
      status: "SUCCESS",
      createdAt: new Date(),
    })
  );
});

test("Toll deduction & PhonePe transfer: Alice can record simulated toll deduction and PhonePe transfer", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  
  // Create user profile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice",
      walletBalance: 500.0,
      phonePeNumber: "9876543210",
      createdAt: new Date(),
    })
  );

  // Toll deduction transaction
  const tollTxId = "TOLL-ABC12345";
  await assertSucceeds(
    aliceDb.collection("transactions").doc(tollTxId).set({
      transactionId: tollTxId,
      userId: ALICE_UID,
      journeyId: "JRN-20261015-A7F42",
      type: "TOLL_DEDUCTION",
      amount: 120.0,
      title: "Toll: Khalapur Plaza",
      description: "Simulated toll deduction",
      status: "SUCCESS",
      createdAt: new Date(),
    })
  );

  // User wallet balance deduction update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      walletBalance: 380.0,
      updatedAt: new Date(),
    })
  );

  // PhonePe direct transfer transaction
  const phonePeTxId = "PHONEPE-XYZ987";
  await assertSucceeds(
    aliceDb.collection("transactions").doc(phonePeTxId).set({
      transactionId: phonePeTxId,
      userId: ALICE_UID,
      type: "PHONEPE_TRANSFER",
      amount: 250.0,
      title: "PhonePe Transfer to 9876543210",
      description: "Direct UPI transfer to mobile number",
      phonePeNumber: "9876543210",
      status: "SUCCESS",
      createdAt: new Date(),
    })
  );
});

