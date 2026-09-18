package com.example.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Service for synchronizing application settings between local storage (Room) and Cloud Firestore.
 * Supports exponential backoff, user-scoped and global documents, and real-time synchronization.
 */
object FirestoreSettingsService {
    private const val TAG = "FirestoreSettings"
    private const val COLLECTION_SETTINGS = "app_settings"
    private const val DOC_GLOBAL_SETTINGS = "current_settings"

    private fun getDb(): FirebaseFirestore? {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            try {
                val settings = FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
                firestore.firestoreSettings = settings
            } catch (ignored: Exception) {
                // Settings can only be configured once per process
            }
            firestore
        } catch (e: Exception) {
            Log.w(TAG, "Firestore unavailable: ${e.message}")
            null
        }
    }

    private fun sanitizeUserKey(userKey: String?): String {
        if (userKey.isNullOrBlank()) return "default_device"
        return userKey.trim().lowercase().replace(".", "_").replace("@", "_")
    }

    /**
     * Exponential backoff retry mechanism for network or transient quota issues.
     */
    private suspend fun <T> retryWithBackoff(
        maxRetries: Int = 3,
        initialDelayMs: Long = 400L,
        factor: Double = 2.0,
        actionName: String = "Settings Operation",
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMs
        var lastException: Exception? = null
        for (attempt in 1..maxRetries) {
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                val isTransient = e.message?.let {
                    it.contains("UNAVAILABLE", ignoreCase = true) ||
                    it.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                    it.contains("DEADLINE_EXCEEDED", ignoreCase = true) ||
                    it.contains("network", ignoreCase = true) ||
                    it.contains("timeout", ignoreCase = true)
                } ?: false

                if (attempt == maxRetries || !isTransient) {
                    Log.e(TAG, "$actionName failed at attempt $attempt: ${e.message}")
                    throw e
                }
                Log.w(TAG, "$actionName transient error on attempt $attempt: ${e.message}. Retrying in ${currentDelay}ms...")
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(4000L)
            }
        }
        throw lastException ?: Exception("$actionName failed after $maxRetries attempts")
    }

    /**
     * Converts AppSettingsEntity to a Firestore-compatible Map.
     */
    fun entityToMap(settings: AppSettingsEntity, userEmail: String?, deviceId: String? = null): Map<String, Any> {
        val sanitizedUser = sanitizeUserKey(userEmail)
        return hashMapOf(
            "id" to settings.id,
            "language" to settings.language,
            "themeMode" to settings.themeMode,
            "primaryColorTheme" to settings.primaryColorTheme,
            "fontStyleOption" to settings.fontStyleOption,
            "cardShapeOption" to settings.cardShapeOption,
            "layoutDensity" to settings.layoutDensity,
            "fontScaleOption" to settings.fontScaleOption,
            "isFirestoreAutoSyncEnabled" to settings.isFirestoreAutoSyncEnabled,
            "autoAddToCartOnScan" to settings.autoAddToCartOnScan,
            "continuousScanMode" to settings.continuousScanMode,
            "vibrateOnScan" to settings.vibrateOnScan,
            "isFeaturesEnabled" to settings.isFeaturesEnabled,
            "isPriceCompareEnabled" to settings.isPriceCompareEnabled,
            "isSmartCartEnabled" to settings.isSmartCartEnabled,
            "isSavingsAnalyticsEnabled" to settings.isSavingsAnalyticsEnabled,
            "isLoyaltyCardsEnabled" to settings.isLoyaltyCardsEnabled,
            "isAiShoppingListEnabled" to settings.isAiShoppingListEnabled,
            "isStoreRouteMapEnabled" to settings.isStoreRouteMapEnabled,
            "isBarcodeScannerEnabled" to settings.isBarcodeScannerEnabled,
            "isVoiceSearchEnabled" to settings.isVoiceSearchEnabled,
            "isPriceAlertsEnabled" to settings.isPriceAlertsEnabled,
            "hasCompletedWelcome" to settings.hasCompletedWelcome,
            "userEmail" to (userEmail ?: "guest"),
            "userId" to sanitizedUser,
            "deviceId" to (deviceId ?: "unknown_device"),
            "lastUpdated" to settings.lastUpdated,
            "syncedAt" to Date()
        )
    }

    /**
     * Parses Firestore document data into AppSettingsEntity.
     */
    fun mapToEntity(map: Map<String, Any?>): AppSettingsEntity {
        return AppSettingsEntity(
            id = 1,
            language = map["language"] as? String ?: "FRENCH",
            themeMode = map["themeMode"] as? String ?: "LIGHT",
            primaryColorTheme = map["primaryColorTheme"] as? String ?: "ROYAL_BLUE",
            fontStyleOption = map["fontStyleOption"] as? String ?: "SANS_SERIF",
            cardShapeOption = map["cardShapeOption"] as? String ?: "ROUNDED_SOFT",
            layoutDensity = map["layoutDensity"] as? String ?: "LIST",
            fontScaleOption = map["fontScaleOption"] as? String ?: "NORMAL",
            isFirestoreAutoSyncEnabled = map["isFirestoreAutoSyncEnabled"] as? Boolean ?: true,
            autoAddToCartOnScan = map["autoAddToCartOnScan"] as? Boolean ?: false,
            continuousScanMode = map["continuousScanMode"] as? Boolean ?: false,
            vibrateOnScan = map["vibrateOnScan"] as? Boolean ?: true,
            isFeaturesEnabled = map["isFeaturesEnabled"] as? Boolean ?: true,
            isPriceCompareEnabled = map["isPriceCompareEnabled"] as? Boolean ?: true,
            isSmartCartEnabled = map["isSmartCartEnabled"] as? Boolean ?: true,
            isSavingsAnalyticsEnabled = map["isSavingsAnalyticsEnabled"] as? Boolean ?: true,
            isLoyaltyCardsEnabled = map["isLoyaltyCardsEnabled"] as? Boolean ?: true,
            isAiShoppingListEnabled = map["isAiShoppingListEnabled"] as? Boolean ?: true,
            isStoreRouteMapEnabled = map["isStoreRouteMapEnabled"] as? Boolean ?: true,
            isBarcodeScannerEnabled = map["isBarcodeScannerEnabled"] as? Boolean ?: true,
            isVoiceSearchEnabled = map["isVoiceSearchEnabled"] as? Boolean ?: true,
            isPriceAlertsEnabled = map["isPriceAlertsEnabled"] as? Boolean ?: true,
            hasCompletedWelcome = map["hasCompletedWelcome"] as? Boolean ?: false,
            lastUpdated = (map["lastUpdated"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    /**
     * Saves app settings to Firestore with batch/merge writes and backoff retries.
     * Writes to both the user-scoped document and the device/global document for resilience.
     */
    suspend fun saveSettingsToFirestore(
        settings: AppSettingsEntity,
        userEmail: String? = null,
        deviceId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val db = getDb() ?: return@withContext false
            val data = entityToMap(settings, userEmail, deviceId)
            val sanitizedUser = sanitizeUserKey(userEmail)

            retryWithBackoff(actionName = "Save Settings to Firestore") {
                // 1. Write to global settings document
                db.collection(COLLECTION_SETTINGS)
                    .document(DOC_GLOBAL_SETTINGS)
                    .set(data, SetOptions.merge())
                    .await()

                // 2. If user is signed in or has key, write to user-specific document as well
                if (userEmail != null && userEmail.isNotBlank()) {
                    db.collection("users")
                        .document(sanitizedUser)
                        .collection("settings")
                        .document("preferences")
                        .set(data, SetOptions.merge())
                        .await()
                }
            }

            Log.d(TAG, "Settings successfully saved to Firestore (user=$sanitizedUser, lang=${settings.language}, theme=${settings.themeMode})")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save settings to Firestore: ${e.message}", e)
            false
        }
    }

    /**
     * Fetches saved settings from Firestore.
     * Prefers user-specific settings if available, otherwise falls back to global settings.
     */
    suspend fun fetchSettingsFromFirestore(
        userEmail: String? = null
    ): AppSettingsEntity? = withContext(Dispatchers.IO) {
        return@withContext try {
            val db = getDb() ?: return@withContext null
            val sanitizedUser = sanitizeUserKey(userEmail)

            // 1. Try user document if userEmail is valid
            if (userEmail != null && userEmail.isNotBlank()) {
                val userDoc = try {
                    db.collection("users")
                        .document(sanitizedUser)
                        .collection("settings")
                        .document("preferences")
                        .get()
                        .await()
                } catch (e: Exception) {
                    null
                }

                if (userDoc != null && userDoc.exists() && userDoc.data != null) {
                    Log.d(TAG, "Found user-specific settings for $sanitizedUser")
                    return@withContext mapToEntity(userDoc.data!!)
                }
            }

            // 2. Try global settings document
            val globalDoc = try {
                db.collection(COLLECTION_SETTINGS)
                    .document(DOC_GLOBAL_SETTINGS)
                    .get()
                    .await()
            } catch (e: Exception) {
                null
            }

            if (globalDoc != null && globalDoc.exists() && globalDoc.data != null) {
                Log.d(TAG, "Found global settings document in Firestore")
                return@withContext mapToEntity(globalDoc.data!!)
            }

            null
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching settings from Firestore: ${e.message}")
            null
        }
    }

    /**
     * Attaches a real-time listener to Firestore to sync settings across sessions/devices.
     */
    fun listenToFirestoreSettings(
        userEmail: String? = null,
        onUpdate: (AppSettingsEntity) -> Unit
    ): ListenerRegistration? {
        return try {
            val db = getDb() ?: return null
            val sanitizedUser = sanitizeUserKey(userEmail)

            val docRef = if (userEmail != null && userEmail.isNotBlank()) {
                db.collection("users").document(sanitizedUser).collection("settings").document("preferences")
            } else {
                db.collection(COLLECTION_SETTINGS).document(DOC_GLOBAL_SETTINGS)
            }

            docRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Firestore settings listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists() && snapshot.data != null) {
                    try {
                        val entity = mapToEntity(snapshot.data!!)
                        onUpdate(entity)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing updated settings: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to attach settings listener: ${e.message}")
            null
        }
    }
}
