package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.UUID

sealed class DeviceBindingResult {
    data class Success(val deviceId: String) : DeviceBindingResult()
    data class Conflict(
        val deviceId: String,
        val activeDeviceId: String,
        val message: String
    ) : DeviceBindingResult()
    data class Error(val message: String) : DeviceBindingResult()
}

object DeviceBindingManager {

    private const val PREFS_NAME = "secure_device_identity_prefs"
    private const val KEY_DEVICE_ID = "persistent_device_uuid"

    fun getOrCreateDeviceId(context: Context): String {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        if (deviceId.isNullOrBlank()) {
            deviceId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
        return deviceId
    }

    suspend fun registerDevice(
        context: Context,
        userEmail: String?,
        userId: String?,
        userTier: String = "FREE",
        userRole: String = "USER"
    ): DeviceBindingResult {
        val deviceId = getOrCreateDeviceId(context)
        val docKey = userEmail?.trim()?.lowercase() ?: userId
        if (docKey.isNullOrBlank()) {
            return DeviceBindingResult.Error("Aucun utilisateur connecté pour la liaison d'appareil.")
        }

        return try {
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(docKey)
            val snapshot = userRef.get().await()

            val maxDevices = if (userRole == "ADMIN" || userTier == "ULTRA") {
                Int.MAX_VALUE
            } else if (userTier == "PRO") {
                3
            } else {
                1
            }

            var currentDeviceList = emptyList<String>()
            var legacyActiveDeviceId = ""

            if (snapshot.exists()) {
                val deviceIds = snapshot.get("deviceIds") as? List<*>
                currentDeviceList = deviceIds?.filterIsInstance<String>() ?: emptyList()
                legacyActiveDeviceId = snapshot.getString("activeDeviceId") ?: ""
            }

            val baseList = if (currentDeviceList.isEmpty() && legacyActiveDeviceId.isNotEmpty()) {
                listOf(legacyActiveDeviceId)
            } else {
                currentDeviceList
            }

            if (deviceId in baseList) {
                return DeviceBindingResult.Success(deviceId)
            }

            if (baseList.size >= maxDevices) {
                val displayList = baseList.joinToString(", ")
                return DeviceBindingResult.Conflict(
                    deviceId = deviceId,
                    activeDeviceId = displayList,
                    message = when (userTier) {
                        "FREE" -> "La version FREE est strictement limitée à 1 seul appareil lié (Actuel : $displayList). Veuillez vous déconnecter ou passer à PRO/ULTRA."
                        "PRO" -> "La version PRO est limitée à 3 appareils liés maximum (Actuels : $displayList). Veuillez libérer un appareil ou passer à ULTRA."
                        else -> "Limite d'appareils atteinte ($maxDevices)."
                    }
                )
            }

            val newList = baseList + deviceId
            val payload = hashMapOf<String, Any?>(
                "activeDeviceId" to deviceId,
                "deviceIds" to newList,
                "userEmail" to userEmail,
                "userId" to userId,
                "userTier" to userTier,
                "userRole" to userRole,
                "lastDeviceRegistration" to System.currentTimeMillis()
            )

            userRef.set(payload, SetOptions.merge()).await()
            Log.d("DeviceBindingManager", "Appareil $deviceId lié avec succès au compte $docKey (Tier: $userTier)")
            DeviceBindingResult.Success(deviceId)
        } catch (e: Exception) {
            Log.e("DeviceBindingManager", "Erreur lors de la liaison de l'appareil: ${e.message}", e)
            DeviceBindingResult.Error("Erreur réseau ou Firestore: ${e.message}")
        }
    }

    suspend fun unbindDevice(
        context: Context,
        userEmail: String?,
        userId: String?
    ): Boolean {
        val docKey = userEmail?.trim()?.lowercase() ?: userId ?: return false
        return try {
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(docKey)
            val payload = hashMapOf<String, Any?>(
                "activeDeviceId" to "",
                "unboundAt" to System.currentTimeMillis()
            )
            userRef.set(payload, SetOptions.merge()).await()
            Log.d("DeviceBindingManager", "Appareil délié pour le compte $docKey")
            true
        } catch (e: Exception) {
            Log.e("DeviceBindingManager", "Échec de la dissociation de l'appareil: ${e.message}", e)
            false
        }
    }
}
