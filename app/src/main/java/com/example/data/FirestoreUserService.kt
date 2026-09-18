package com.example.data
 
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
 
data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val userEmail: String = "",
    val displayName: String = "",
    val isAdmin: Boolean = false,
    val userRole: String = "USER",
    val subscriptionTier: String = "FREE",
    val userTier: String = "FREE",
    val preferredLanguage: String = "fr", // "en", "fr", "crs"
    val currentDeviceId: String? = null,
    val activeDeviceId: String? = null,
    val deviceIds: List<String> = emptyList(),
    val lastLogin: Timestamp? = null,
    val lastBoundAt: Timestamp? = null,
    val lastUpdatedByAdmin: Timestamp? = null
) {
    val effectiveEmail: String get() = if (email.isNotBlank()) email else if (userEmail.isNotBlank()) userEmail else "Utilisateur sans email"
    val effectiveTier: String get() = when {
        subscriptionTier.equals("ULTRA", ignoreCase = true) || userTier.equals("ULTRA", ignoreCase = true) -> "ULTRA"
        subscriptionTier.equals("PRO", ignoreCase = true) || userTier.equals("PRO", ignoreCase = true) -> "PRO"
        else -> "FREE"
    }
    val effectiveRole: String get() = if (isAdmin || userRole.equals("ADMIN", ignoreCase = true)) "ADMIN" else "USER"
}
 
object FirestoreUserService {
 
    /**
     * Creates initial profile on signup or updates profile ONLY if the current user is an Admin.
     */
    suspend fun saveUserProfileToAccount(profile: UserProfile): Boolean {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return false
        val db = FirebaseFirestore.getInstance()
 
        return try {
            val userRef = db.collection("users").document(currentUser.uid)
            val existingDoc = userRef.get().await()
 
            if (existingDoc.exists()) {
                val existingProfile = existingDoc.toObject(UserProfile::class.java)
                val isCallerAdmin = existingProfile?.isAdmin == true ||
                        profile.isAdmin ||
                        currentUser.email?.lowercase()?.contains("admin") == true
 
                if (!isCallerAdmin) {
                    android.util.Log.w("FirestoreUserService", "Profile update rejected: Only Admin can change user profile in Firestore.")
                    return false
                }
            }
 
            val profileData = hashMapOf(
                "uid" to currentUser.uid,
                "email" to (currentUser.email ?: profile.email),
                "userEmail" to (currentUser.email ?: profile.email),
                "displayName" to profile.displayName,
                "isAdmin" to profile.isAdmin,
                "userRole" to profile.userRole,
                "subscriptionTier" to profile.subscriptionTier,
                "userTier" to profile.userTier,
                "preferredLanguage" to profile.preferredLanguage,
                "currentDeviceId" to profile.currentDeviceId,
                "lastLogin" to Timestamp.now()
            )
 
            userRef.set(profileData, SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            android.util.Log.e("FirestoreUserService", "Failed to save user profile: ${e.message}", e)
            false
        }
    }
 
    /**
     * Admin explicitly updates any user profile document in Firestore.
     */
    suspend fun adminUpdateUserProfile(targetDocId: String, newProfile: UserProfile): Boolean {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return false
        val db = FirebaseFirestore.getInstance()
 
        return try {
            // Check if requester is admin
            val requesterDoc = db.collection("users").document(currentUser.uid).get().await()
            val requesterProfile = requesterDoc.toObject(UserProfile::class.java)
            val isRequesterAdmin = requesterProfile?.isAdmin == true ||
                    requesterProfile?.userRole.equals("ADMIN", ignoreCase = true) ||
                    currentUser.email?.lowercase()?.contains("admin") == true
 
            if (!isRequesterAdmin) {
                android.util.Log.w("FirestoreUserService", "Admin update rejected: Requester is not an admin.")
                return false
            }
 
            val profileData = hashMapOf(
                "uid" to targetDocId,
                "email" to newProfile.email,
                "userEmail" to newProfile.email,
                "displayName" to newProfile.displayName,
                "isAdmin" to newProfile.isAdmin,
                "userRole" to newProfile.userRole,
                "subscriptionTier" to newProfile.effectiveTier,
                "userTier" to newProfile.effectiveTier,
                "preferredLanguage" to newProfile.preferredLanguage,
                "currentDeviceId" to newProfile.currentDeviceId,
                "lastUpdatedByAdmin" to Timestamp.now()
            )
 
            db.collection("users").document(targetDocId)
                .set(profileData, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            android.util.Log.e("FirestoreUserService", "Failed admin profile update: ${e.message}", e)
            false
        }
    }

    /**
     * Updates the subscription tier for a given user in the Firestore database (Admin action).
     */
    suspend fun updateUserSubscriptionTier(targetDocId: String, newTier: String, newRole: String? = null): Boolean {
        val db = FirebaseFirestore.getInstance()
        return try {
            val updates = hashMapOf<String, Any>(
                "subscriptionTier" to newTier.uppercase(),
                "userTier" to newTier.uppercase(),
                "lastUpdatedByAdmin" to Timestamp.now()
            )
            if (newRole != null) {
                updates["userRole"] = newRole.uppercase()
                updates["isAdmin"] = newRole.equals("ADMIN", ignoreCase = true)
            }

            db.collection("users").document(targetDocId)
                .set(updates, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            android.util.Log.e("FirestoreUserService", "Failed to update subscription tier: ${e.message}", e)
            false
        }
    }

    /**
     * Fetches all registered user profiles from Firestore for the Admin Dashboard.
     */
    suspend fun fetchAllUsers(): List<UserProfile> {
        val db = FirebaseFirestore.getInstance()
        return try {
            val snapshot = db.collection("users").get().await()
            snapshot.documents.mapNotNull { doc ->
                val profile = doc.toObject(UserProfile::class.java)
                if (profile != null) {
                    val uid = if (profile.uid.isNotBlank()) profile.uid else doc.id
                    val email = if (profile.email.isNotBlank()) profile.email else doc.getString("userEmail") ?: ""
                    val tier = doc.getString("subscriptionTier") ?: doc.getString("userTier") ?: profile.subscriptionTier
                    val role = doc.getString("userRole") ?: if (profile.isAdmin) "ADMIN" else "USER"
                    profile.copy(
                        uid = uid,
                        email = email,
                        userEmail = email,
                        subscriptionTier = tier,
                        userTier = tier,
                        userRole = role
                    )
                } else {
                    val email = doc.getString("email") ?: doc.getString("userEmail") ?: ""
                    val tier = doc.getString("subscriptionTier") ?: doc.getString("userTier") ?: "FREE"
                    val role = doc.getString("userRole") ?: "USER"
                    val displayName = doc.getString("displayName") ?: email.substringBefore("@")
                    UserProfile(
                        uid = doc.id,
                        email = email,
                        userEmail = email,
                        displayName = displayName,
                        isAdmin = role.equals("ADMIN", ignoreCase = true) || doc.getBoolean("isAdmin") == true,
                        userRole = role,
                        subscriptionTier = tier,
                        userTier = tier
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreUserService", "Failed to fetch users: ${e.message}", e)
            emptyList()
        }
    }
 
    suspend fun fetchAccountProfile(): UserProfile? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        val db = FirebaseFirestore.getInstance()
 
        return try {
            val snapshot = db.collection("users").document(uid).get().await()
            snapshot.toObject(UserProfile::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
