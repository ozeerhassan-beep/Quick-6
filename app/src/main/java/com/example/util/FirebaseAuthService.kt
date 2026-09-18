package com.example.util

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

sealed class GoogleAuthResult {
    data class Success(
        val email: String,
        val displayName: String,
        val photoUrl: String? = null,
        val idToken: String? = null
    ) : GoogleAuthResult()

    data class Failure(
        val errorMessage: String,
        val isCancelled: Boolean = false
    ) : GoogleAuthResult()
}

object FirebaseAuthService {
    const val DEFAULT_WEB_CLIENT_ID = "281205414143-l5137khb4cjcusghjg3thrl6ttqjk0m3.apps.googleusercontent.com"
    const val DEBUG_SHA1_FINGERPRINT = "A1:2D:52:65:5F:55:F6:17:A0:4E:DC:0A:14:26:1A:70:19:17:07:B5"

    fun getWebClientId(context: Context): String {
        return try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", "com.example")
                .takeIf { it != 0 }
                ?: context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) {
                val value = context.getString(resId)
                if (value.isNotBlank()) value else DEFAULT_WEB_CLIENT_ID
            } else DEFAULT_WEB_CLIENT_ID
        } catch (e: Exception) {
            DEFAULT_WEB_CLIENT_ID
        }
    }

    fun getAuth(): FirebaseAuth? {
        return try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w("FirebaseAuthService", "FirebaseAuth instance unavailable: ${e.message}")
            null
        }
    }

    suspend fun signInWithGoogle(
        context: Context,
        webClientId: String? = null
    ): GoogleAuthResult {
        val resolvedClientId = webClientId ?: getWebClientId(context)
        val auth = getAuth()
        val credentialManager = CredentialManager.create(context)

        // 1. Try dedicated GetSignInWithGoogleOption
        try {
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(resolvedClientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()
            val result = credentialManager.getCredential(context, request)
            val handleRes = handleSignInResult(result, auth)
            if (handleRes is GoogleAuthResult.Success) {
                return handleRes
            }
        } catch (e: GetCredentialCancellationException) {
            Log.i("FirebaseAuthService", "GetSignInWithGoogleOption cancelled, falling back to GetGoogleIdOption")
        } catch (e: Exception) {
            Log.w("FirebaseAuthService", "GetSignInWithGoogleOption failed (${e.message}), trying GetGoogleIdOption")
        }

        // 2. Fallback: GetGoogleIdOption with filterByAuthorizedAccounts = false
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(resolvedClientId)
                .setAutoSelectEnabled(false)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val result = credentialManager.getCredential(context, request)
            handleSignInResult(result, auth)
        } catch (e: GetCredentialCancellationException) {
            Log.i("FirebaseAuthService", "Google sign-in was cancelled by user")
            GoogleAuthResult.Failure("Connexion Google annulée par l'utilisateur", isCancelled = true)
        } catch (e: Exception) {
            Log.e("FirebaseAuthService", "Sign in failed: ${e.message}", e)
            val msg = e.localizedMessage ?: ""
            val isNoCredential = msg.contains("credential", ignoreCase = true) ||
                                 e.javaClass.simpleName.contains("NoCredential", ignoreCase = true)
            GoogleAuthResult.Failure(
                errorMessage = if (isNoCredential) "Aucun compte Google détecté ou pré-enregistré sur cet appareil." else (e.localizedMessage ?: "Échec de l'authentification Google"),
                isCancelled = isNoCredential
            )
        }
    }

    private suspend fun handleSignInResult(
        result: GetCredentialResponse,
        auth: FirebaseAuth?
    ): GoogleAuthResult {
        val credential = result.credential
        
        var idToken: String? = null
        var email: String? = null
        var displayName: String? = null
        var profilePictureUri: String? = null

        when (credential) {
            is GoogleIdTokenCredential -> {
                idToken = credential.idToken
                email = credential.id
                displayName = credential.displayName
                profilePictureUri = credential.profilePictureUri?.toString()
            }
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        idToken = googleIdTokenCredential.idToken
                        email = googleIdTokenCredential.id
                        displayName = googleIdTokenCredential.displayName
                        profilePictureUri = googleIdTokenCredential.profilePictureUri?.toString()
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e("FirebaseAuthService", "Failed to parse GoogleIdTokenCredential: ${e.message}")
                        return GoogleAuthResult.Failure("Échec du décodage de l'identifiant Google")
                    }
                }
            }
        }

        if (!email.isNullOrBlank()) {
            val formattedDisplayName = displayName ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }

            if (auth != null && !idToken.isNullOrBlank()) {
                try {
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(firebaseCredential).await()
                } catch (e: Exception) {
                    Log.w("FirebaseAuthService", "Firebase credential link warning: ${e.message}")
                }
            }

            return GoogleAuthResult.Success(
                email = email,
                displayName = formattedDisplayName,
                photoUrl = profilePictureUri,
                idToken = idToken
            )
        }

        return GoogleAuthResult.Failure("Type d'identifiant Google non reconnu")
    }

    fun getCurrentUser() = getAuth()?.currentUser

    fun signOut() {
        try {
            getAuth()?.signOut()
        } catch (e: Exception) {
            Log.w("FirebaseAuthService", "Sign out error: ${e.message}")
        }
    }
}
