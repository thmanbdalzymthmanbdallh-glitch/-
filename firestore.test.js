const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_seller_123";
const BOB_UID = "bob_buyer_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const pastTime = () => new Date(Date.now() - 5000);

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

// 1. Unauthenticated bounds
test("Unauthenticated user: cannot read products, orders, or users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("products").where("isPublished", "==", true).get());
  await assertFails(unauthDb.collection("orders").where("buyerId", "==", ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

// 2. User Profile PII isolation & Shadow update protection
test("User Profile: owner can create and read, cross-user read fails, ghost field fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastTime();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "سارة أحمد",
      storeName: "متجر سارة",
      phone: "0501234567",
      defaultAddress: "الرياض، حي العليا",
      currency: "SAR",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  // Shadow update with ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      storeName: "متجر معدل",
      isAdmin: true,
      updatedAt: pastTime(),
    })
  );
});

// 3. Products creation, spoofing prevention, and query alignment
test("Products: seller creates valid product, rejects spoofing and unfiltered queries", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastTime();

  // Valid create
  await assertSucceeds(
    aliceDb.collection("products").doc("prod_1").set({
      sellerId: ALICE_UID,
      sellerName: "متجر سارة",
      title: "سماعة لاسلكية فاخرة",
      description: "صوت نقي وعزل ضوضاء",
      category: "electronics",
      price: 299.0,
      currency: "SAR",
      stock: 10,
      salesCount: 0,
      isPublished: true,
      imagePreset: "electronics",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Spoof sellerId
  await assertFails(
    bobDb.collection("products").doc("prod_spoof").set({
      sellerId: ALICE_UID,
      sellerName: "مزيف",
      title: "منتج مزيف",
      description: "وصف",
      category: "electronics",
      price: 100.0,
      currency: "SAR",
      stock: 5,
      salesCount: 0,
      isPublished: true,
      imagePreset: "electronics",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Unfiltered list query MUST fail (rules are not filters)
  await assertFails(bobDb.collection("products").get());

  // Filtered published list query MUST succeed
  await assertSucceeds(bobDb.collection("products").where("isPublished", "==", true).get());

  // Filtered seller list query MUST succeed for owner
  await assertSucceeds(aliceDb.collection("products").where("sellerId", "==", ALICE_UID).get());
});

// 4. Buyer cannot edit product price, but can decrement stock on order
test("Products: buyer cannot tamper with price, can decrement stock with matching salesCount delta", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastTime();

  await assertSucceeds(
    aliceDb.collection("products").doc("prod_stock").set({
      sellerId: ALICE_UID,
      sellerName: "متجر سارة",
      title: "عطر شرقي فاخر",
      description: "ثبات عالي",
      category: "beauty",
      price: 180.0,
      currency: "SAR",
      stock: 5,
      salesCount: 0,
      isPublished: true,
      imagePreset: "perfume",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Buyer tries to tamper with price -> FAIL
  await assertFails(
    bobDb.collection("products").doc("prod_stock").update({
      price: 1.0,
      updatedAt: pastTime(),
    })
  );

  // Buyer decrements stock by 2 and increments salesCount by 2 -> SUCCEED
  await assertSucceeds(
    bobDb.collection("products").doc("prod_stock").update({
      stock: 3,
      salesCount: 2,
      updatedAt: pastTime(),
    })
  );

  // Buyer tries to set negative stock -> FAIL
  await assertFails(
    bobDb.collection("products").doc("prod_stock").update({
      stock: -1,
      salesCount: 6,
      updatedAt: pastTime(),
    })
  );
});

// 5. Orders: creation validation, buyer/seller isolation, status workflow & terminal lock
test("Orders: enforces product existence, status lifecycle, and terminal state lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = pastTime();

  await assertSucceeds(
    aliceDb.collection("products").doc("prod_order").set({
      sellerId: ALICE_UID,
      sellerName: "متجر سارة",
      title: "حقيبة جلد طبيعي",
      description: "تصميم أنيق",
      category: "fashion",
      price: 250.0,
      currency: "SAR",
      stock: 8,
      salesCount: 0,
      isPublished: true,
      imagePreset: "fashion",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Orphan order pointing to non-existent product -> FAIL
  await assertFails(
    bobDb.collection("orders").doc("order_orphan").set({
      buyerId: BOB_UID,
      sellerId: ALICE_UID,
      sellerName: "متجر سارة",
      productId: "does_not_exist",
      productTitle: "منتج وهمي",
      quantity: 1,
      unitPrice: 250.0,
      totalPrice: 250.0,
      currency: "SAR",
      buyerName: "خالد",
      buyerPhone: "0551112222",
      shippingAddress: "جدة، حي الشاطئ",
      status: "pending",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid order creation
  await assertSucceeds(
    bobDb.collection("orders").doc("order_1").set({
      buyerId: BOB_UID,
      sellerId: ALICE_UID,
      sellerName: "متجر سارة",
      productId: "prod_order",
      productTitle: "حقيبة جلد طبيعي",
      quantity: 1,
      unitPrice: 250.0,
      totalPrice: 250.0,
      currency: "SAR",
      buyerName: "خالد",
      buyerPhone: "0551112222",
      shippingAddress: "جدة، حي الشاطئ",
      status: "pending",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Third party Charlie cannot read Bob/Alice's order -> FAIL
  await assertFails(charlieDb.collection("orders").doc("order_1").get());
  await assertFails(charlieDb.collection("orders").where("buyerId", "==", BOB_UID).get());

  // Seller Alice confirms, ships, and delivers
  await assertSucceeds(
    aliceDb.collection("orders").doc("order_1").update({ status: "confirmed", updatedAt: pastTime() })
  );
  // Buyer cannot cancel after confirmed
  await assertFails(
    bobDb.collection("orders").doc("order_1").update({ status: "cancelled", updatedAt: pastTime() })
  );
  await assertSucceeds(
    aliceDb.collection("orders").doc("order_1").update({ status: "delivered", updatedAt: pastTime() })
  );

  // Terminal state lock: once delivered, seller cannot change status
  await assertFails(
    aliceDb.collection("orders").doc("order_1").update({ status: "cancelled", updatedAt: pastTime() })
  );
  // Cannot delete orders
  await assertFails(aliceDb.collection("orders").doc("order_1").delete());
});

// 6. Cart subcollection isolation
test("Cart: owner-scoped CRUD works and cross-user fails", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastTime();

  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("cart").doc("prod_1").set({
      userId: BOB_UID,
      productId: "prod_1",
      sellerId: ALICE_UID,
      sellerName: "متجر سارة",
      productTitle: "سماعة لاسلكية فاخرة",
      unitPrice: 299.0,
      currency: "SAR",
      quantity: 2,
      imagePreset: "electronics",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(bobDb.collection("users").doc(BOB_UID).collection("cart").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("cart").get());
});

// 7. Discount codes validation, creator ownership, and buyer redemption bounds
test("Discounts: creator CRUD, buyer validation & single-increment redemption, prevents tampering", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = pastTime();

  await assertFails(unauthDb.collection("discounts").doc("SAVE20").get());

  await assertSucceeds(
    aliceDb.collection("discounts").doc("SAVE20").set({
      code: "SAVE20",
      creatorId: ALICE_UID,
      type: "percentage",
      value: 20,
      minOrderAmount: 50,
      usageCount: 0,
      maxUses: 10,
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Buyer Bob can read SAVE20 and increment usageCount by 1
  await assertSucceeds(bobDb.collection("discounts").doc("SAVE20").get());
  await assertSucceeds(
    bobDb.collection("discounts").doc("SAVE20").update({
      usageCount: 1,
      updatedAt: pastTime(),
    })
  );

  // Buyer Bob cannot tamper with discount value
  await assertFails(
    bobDb.collection("discounts").doc("SAVE20").update({
      value: 100,
      updatedAt: pastTime(),
    })
  );

  // Buyer Bob cannot delete Alice's discount
  await assertFails(bobDb.collection("discounts").doc("SAVE20").delete());
});

// 8. Wishlist collection isolation & schema validation
test("Wishlist: user-scoped CRUD works and cross-user read/write fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastTime();

  const docId = `${BOB_UID}_prod_1`;
  await assertSucceeds(
    bobDb.collection("wishlist").doc(docId).set({
      userId: BOB_UID,
      productId: "prod_1",
      sellerId: ALICE_UID,
      sellerName: "متجر سارة",
      productTitle: "سماعة لاسلكية فاخرة",
      category: "electronics",
      price: 299.0,
      currency: "SAR",
      imagePreset: "electronics",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Owner Bob can list filtered by userId
  await assertSucceeds(bobDb.collection("wishlist").where("userId", "==", BOB_UID).get());

  // Unfiltered list or cross-user read/delete fails
  await assertFails(aliceDb.collection("wishlist").get());
  await assertFails(aliceDb.collection("wishlist").doc(docId).get());
  await assertFails(aliceDb.collection("wishlist").doc(docId).delete());

  // Owner Bob can delete
  await assertSucceeds(bobDb.collection("wishlist").doc(docId).delete());
});

// 9. Product Price History subcollection immutability & seller ownership
test("PriceHistory: seller-only immutable create and authenticated reads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastTime();

  await assertSucceeds(
    aliceDb.collection("products").doc("prod_hist").set({
      sellerId: ALICE_UID,
      sellerName: "متجر سارة",
      title: "ساعة ذكية",
      description: "ساعة ذكية متعددة المزايا",
      category: "electronics",
      price: 450.0,
      currency: "SAR",
      stock: 5,
      salesCount: 0,
      isPublished: true,
      imagePreset: "electronics",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Seller Alice can record a price history entry
  await assertSucceeds(
    aliceDb.collection("products").doc("prod_hist").collection("priceHistory").doc("h1").set({
      productId: "prod_hist",
      sellerId: ALICE_UID,
      price: 450.0,
      currency: "SAR",
      recordedAt: ts,
    })
  );

  // Buyer Bob can read price history, but cannot write or tamper with it
  await assertSucceeds(bobDb.collection("products").doc("prod_hist").collection("priceHistory").get());
  await assertFails(
    bobDb.collection("products").doc("prod_hist").collection("priceHistory").doc("h2").set({
      productId: "prod_hist",
      sellerId: BOB_UID,
      price: 10.0,
      currency: "SAR",
      recordedAt: ts,
    })
  );

  // History entries are immutable
  await assertFails(
    aliceDb.collection("products").doc("prod_hist").collection("priceHistory").doc("h1").update({ price: 300.0 })
  );
  await assertFails(
    aliceDb.collection("products").doc("prod_hist").collection("priceHistory").doc("h1").delete()
  );
});
