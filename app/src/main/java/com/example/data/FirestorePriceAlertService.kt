package com.example.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

object FirestorePriceAlertService {
    private const val TAG = "FirestorePriceAlerts"

    private fun getDb(): FirebaseFirestore? {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            try {
                val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
                firestore.firestoreSettings = settings
            } catch (ignored: Exception) {
                // Settings can only be set once before any operations
            }
            firestore
        } catch (e: Exception) {
            Log.w(TAG, "Firestore is unavailable: ${e.message}")
            null
        }
    }

    private fun sanitizeUserKey(userKey: String): String {
        return if (userKey.isBlank()) "guest_user" else userKey.replace(".", "_").replace("@", "_")
    }

    /**
     * Saves or updates a price alert document in Cloud Firestore under `price_alerts/{sanitizedUser}_{alertId}`
     * and in user sub-collection `users/{sanitizedUser}/price_alerts/{alertId}`.
     */
    suspend fun savePriceAlert(alert: PriceAlertEntity, userKey: String): String? {
        return try {
            val db = getDb() ?: return null
            val sanitizedUser = sanitizeUserKey(userKey)
            val docId = if (alert.firestoreId.isNotBlank()) alert.firestoreId else "alert_${UUID.randomUUID()}"
            val alertRef = db.collection("users").document(sanitizedUser).collection("price_alerts").document(docId)

            val alertData = hashMapOf(
                "alertId" to docId,
                "userEmail" to if (userKey.isBlank()) "guest_user" else userKey,
                "productId" to alert.productId,
                "catalogType" to alert.catalogType,
                "productName" to alert.productName,
                "category" to alert.category,
                "brand" to alert.brand,
                "unit" to alert.unit,
                "initialPrice" to alert.initialPrice,
                "targetPrice" to alert.targetPrice,
                "currentPrice" to alert.currentPrice,
                "isTriggered" to alert.isTriggered,
                "lastTriggeredPrice" to alert.lastTriggeredPrice,
                "lastNotifiedAt" to alert.lastNotifiedAt,
                "createdAt" to Date(alert.createdAt),
                "updatedAt" to Date()
            )

            alertRef.set(alertData, SetOptions.merge()).await()
            Log.d(TAG, "Price alert saved to Firestore: $docId for product ${alert.productName}")
            docId
        } catch (e: Exception) {
            Log.e(TAG, "Error saving price alert to Firestore: ${e.message}", e)
            null
        }
    }

    /**
     * Updates triggered status and current price on Firestore
     */
    suspend fun updateTriggeredStatus(
        firestoreDocId: String,
        userKey: String,
        isTriggered: Boolean,
        currentPrice: Double,
        lastTriggeredPrice: Double
    ): Boolean {
        return try {
            val db = getDb() ?: return false
            if (firestoreDocId.isBlank()) return false
            val sanitizedUser = sanitizeUserKey(userKey)
            val alertRef = db.collection("users").document(sanitizedUser).collection("price_alerts").document(firestoreDocId)

            val updateData = hashMapOf(
                "isTriggered" to isTriggered,
                "currentPrice" to currentPrice,
                "lastTriggeredPrice" to lastTriggeredPrice,
                "lastNotifiedAt" to System.currentTimeMillis(),
                "updatedAt" to Date()
            )

            alertRef.set(updateData, SetOptions.merge()).await()
            Log.d(TAG, "Updated triggered status in Firestore for alert $firestoreDocId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating alert status in Firestore: ${e.message}", e)
            false
        }
    }

    /**
     * Deletes a price alert from Firestore
     */
    suspend fun deletePriceAlert(firestoreDocId: String, userKey: String): Boolean {
        return try {
            val db = getDb() ?: return false
            if (firestoreDocId.isBlank()) return false
            val sanitizedUser = sanitizeUserKey(userKey)
            val alertRef = db.collection("users").document(sanitizedUser).collection("price_alerts").document(firestoreDocId)
            alertRef.delete().await()
            Log.d(TAG, "Price alert $firestoreDocId deleted from Firestore")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting price alert from Firestore: ${e.message}", e)
            false
        }
    }

    /**
     * Fetches all price alerts for the given user from Firestore
     */
    suspend fun fetchPriceAlerts(userKey: String): List<PriceAlertEntity> {
        return try {
            val db = getDb() ?: return emptyList()
            val sanitizedUser = sanitizeUserKey(userKey)
            val snapshot = db.collection("users").document(sanitizedUser).collection("price_alerts").get().await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    val alertId = doc.getString("alertId") ?: doc.id
                    val productId = (doc.getLong("productId") ?: 0L).toInt()
                    val catalogType = doc.getString("catalogType") ?: "ALL"
                    val productName = doc.getString("productName") ?: ""
                    val category = doc.getString("category") ?: ""
                    val brand = doc.getString("brand") ?: ""
                    val unit = doc.getString("unit") ?: ""
                    val initialPrice = doc.getDouble("initialPrice") ?: 0.0
                    val targetPrice = doc.getDouble("targetPrice") ?: 0.0
                    val currentPrice = doc.getDouble("currentPrice") ?: initialPrice
                    val isTriggered = doc.getBoolean("isTriggered") ?: false
                    val lastTriggeredPrice = doc.getDouble("lastTriggeredPrice") ?: 0.0
                    val lastNotifiedAt = doc.getLong("lastNotifiedAt") ?: 0L
                    val createdAt = doc.getDate("createdAt")?.time ?: System.currentTimeMillis()

                    PriceAlertEntity(
                        firestoreId = alertId,
                        productId = productId,
                        catalogType = catalogType,
                        productName = productName,
                        category = category,
                        brand = brand,
                        unit = unit,
                        initialPrice = initialPrice,
                        targetPrice = targetPrice,
                        currentPrice = currentPrice,
                        isTriggered = isTriggered,
                        lastTriggeredPrice = lastTriggeredPrice,
                        lastNotifiedAt = lastNotifiedAt,
                        createdAt = createdAt,
                        userEmail = userKey,
                        isSynced = true
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing alert document: ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching price alerts from Firestore: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Attaches a real-time Firestore snapshot listener to the user's price alerts collection
     */
    fun listenToPriceAlerts(
        userKey: String,
        onUpdate: (List<PriceAlertEntity>) -> Unit
    ): ListenerRegistration? {
        return try {
            val db = getDb() ?: return null
            val sanitizedUser = sanitizeUserKey(userKey)
            val alertsCollection = db.collection("users").document(sanitizedUser).collection("price_alerts")

            alertsCollection.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Firestore price alerts listener error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val alerts = snapshot.documents.mapNotNull { doc ->
                        try {
                            val alertId = doc.getString("alertId") ?: doc.id
                            val productId = (doc.getLong("productId") ?: 0L).toInt()
                            val catalogType = doc.getString("catalogType") ?: "ALL"
                            val productName = doc.getString("productName") ?: ""
                            val category = doc.getString("category") ?: ""
                            val brand = doc.getString("brand") ?: ""
                            val unit = doc.getString("unit") ?: ""
                            val initialPrice = doc.getDouble("initialPrice") ?: 0.0
                            val targetPrice = doc.getDouble("targetPrice") ?: 0.0
                            val currentPrice = doc.getDouble("currentPrice") ?: initialPrice
                            val isTriggered = doc.getBoolean("isTriggered") ?: false
                            val lastTriggeredPrice = doc.getDouble("lastTriggeredPrice") ?: 0.0
                            val lastNotifiedAt = doc.getLong("lastNotifiedAt") ?: 0L
                            val createdAt = doc.getDate("createdAt")?.time ?: System.currentTimeMillis()

                            PriceAlertEntity(
                                firestoreId = alertId,
                                productId = productId,
                                catalogType = catalogType,
                                productName = productName,
                                category = category,
                                brand = brand,
                                unit = unit,
                                initialPrice = initialPrice,
                                targetPrice = targetPrice,
                                currentPrice = currentPrice,
                                isTriggered = isTriggered,
                                lastTriggeredPrice = lastTriggeredPrice,
                                lastNotifiedAt = lastNotifiedAt,
                                createdAt = createdAt,
                                userEmail = userKey,
                                isSynced = true
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    onUpdate(alerts)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not attach Firestore listener: ${e.message}")
            null
        }
    }
}
