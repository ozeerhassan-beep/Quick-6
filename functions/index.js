const functions = require("firebase-functions");
const admin = require("firebase-admin");

if (!admin.apps.length) {
  admin.initializeApp();
}

/**
 * HTTPS Callable Cloud Function: registerDevice
 * Enforces single-device-per-user authentication and authorization policy.
 */
exports.registerDevice = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "User must be authenticated to register a device."
    );
  }

  const userId = context.auth.uid;
  const deviceId = data.deviceId;

  if (!deviceId || typeof deviceId !== "string") {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "The function must be called with a valid 'deviceId' string."
    );
  }

  const userRef = admin.firestore().collection("users").doc(userId);
  const doc = await userRef.get();

  if (doc.exists) {
    const activeDeviceId = doc.data().activeDeviceId;
    if (activeDeviceId && activeDeviceId !== deviceId) {
      throw new functions.https.HttpsError(
        "already-exists",
        "This account is already linked to another device. Please log out on that device first."
      );
    }
  }

  await userRef.set(
    {
      activeDeviceId: deviceId,
      lastLoginTime: admin.firestore.FieldValue.serverTimestamp(),
      lastUpdated: Date.now()
    },
    { merge: true }
  );

  return { success: true, activeDeviceId: deviceId };
});
