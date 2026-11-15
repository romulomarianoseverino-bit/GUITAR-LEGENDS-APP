package com.example.guitarlegends.data

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

sealed class GoogleCredentialResult {
    data class Success(val idToken: String, val displayName: String?, val email: String) : GoogleCredentialResult()
    object Cancelled : GoogleCredentialResult()
    data class Error(val message: String) : GoogleCredentialResult()
}

object GoogleSignInHelper {

    suspend fun requestGoogleSignIn(activityContext: Context): GoogleCredentialResult {
        val webClientId = runCatching {
            activityContext.getString(R.string.default_web_client_id)
        }.getOrNull().orEmpty()

        if (webClientId.isBlank()) {
            return GoogleCredentialResult.Error(
                "Configuração OAuth (default_web_client_id) não encontrada. Verifique o arquivo google-services.json."
            )
        }

        val credentialManager = CredentialManager.create(activityContext)
        val signInOption = GetSignInWithGoogleOption.Builder(webClientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        return try {
            val result = credentialManager.getCredential(
                context = activityContext,
                request = request
            )
            extractGoogleToken(result.credential)
        } catch (_: GetCredentialCancellationException) {
            GoogleCredentialResult.Cancelled
        } catch (_: NoCredentialException) {
            GoogleCredentialResult.Error(
                "Nenhuma conta Google encontrada neste aparelho. Adicione uma conta Google nas configurações do Android."
            )
        } catch (e: Exception) {
            Log.e("GoogleSignInHelper", "Falha no login Google via Credential Manager", e)
            GoogleCredentialResult.Error("Não foi possível concluir o login Google: ${e.message ?: "erro desconhecido"}")
        }
    }

    suspend fun attemptSilentSignIn(activityContext: Context): GoogleCredentialResult? {
        val webClientId = runCatching {
            activityContext.getString(R.string.default_web_client_id)
        }.getOrNull().orEmpty()
        if (webClientId.isBlank()) return null

        val credentialManager = CredentialManager.create(activityContext)
        val autoOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(autoOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context = activityContext, request = request)
            extractGoogleToken(result.credential)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun clearCredentials(context: Context) {
        runCatching {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        }
    }

    private fun extractGoogleToken(credential: androidx.credentials.Credential): GoogleCredentialResult {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
            return GoogleCredentialResult.Success(
                idToken = googleCred.idToken,
                displayName = googleCred.displayName,
                email = googleCred.id
            )
        }
        return GoogleCredentialResult.Error("Credencial retornada não é um token Google ID válido.")
    }
}
