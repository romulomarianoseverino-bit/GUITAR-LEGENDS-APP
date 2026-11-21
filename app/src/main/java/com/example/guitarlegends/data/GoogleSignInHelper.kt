package com.example.guitarlegends.data

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
    data class Error(val message: String) : GoogleCredentialResult()
}

object GoogleSignInHelper {

    // Credential Manager precisa da Activity em primeiro plano para abrir a interface
    // de seleção e receber corretamente o resultado da conta escolhida.
    private fun Context.findActivity(): Activity? {
        var current: Context = this
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return current as? Activity
    }

    // Google Services generates this resource when a valid google-services.json is present.
    // Resolve it dynamically so CI builds without private Firebase configuration still compile.
    private fun getWebClientId(context: Context): String {
        val resourceId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        return if (resourceId != 0) {
            runCatching { context.getString(resourceId) }.getOrNull().orEmpty()
        } else {
            ""
        }
    }

    suspend fun requestGoogleSignIn(activityContext: Context): GoogleCredentialResult {
        val activity = activityContext.findActivity()
            ?: return GoogleCredentialResult.Error("Não foi possível iniciar o login: a tela do aplicativo não está ativa. Feche e abra o Guitar Legends novamente.")
        val webClientId = getWebClientId(activity)

        if (webClientId.isBlank()) {
            return GoogleCredentialResult.Error(
                "Configuração OAuth (default_web_client_id) não encontrada. Verifique o arquivo google-services.json."
            )
        }

        val credentialManager = CredentialManager.create(activity)
        val signInOption = GetSignInWithGoogleOption.Builder(webClientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        return try {
            val result = credentialManager.getCredential(
                context = activity,
                request = request
            )
            extractGoogleToken(result.credential)
        } catch (e: GetCredentialCancellationException) {
            // Credential Manager can report this both for a user dismissal and
            // for an authorization flow rejected by OAuth configuration (for example,
            // an unregistered signing-certificate SHA-1). Do not label every case
            // as a deliberate user cancellation.
            Log.w(
                "GoogleSignInHelper",
                "Google authorization was not completed; verify OAuth client and APK signing SHA-1 if unexpected.",
                e
            )
            GoogleCredentialResult.Error(
                "A autorização do Google não foi concluída. Isso pode acontecer se o login foi fechado ou se a assinatura deste APK (SHA-1) não está cadastrada no Firebase. Se você não cancelou, é necessário conferir a chave usada para assinar o APK."
            )
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
        val activity = activityContext.findActivity() ?: return null
        val webClientId = getWebClientId(activity)
        if (webClientId.isBlank()) return null

        val credentialManager = CredentialManager.create(activity)
        val autoOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(autoOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context = activity, request = request)
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
