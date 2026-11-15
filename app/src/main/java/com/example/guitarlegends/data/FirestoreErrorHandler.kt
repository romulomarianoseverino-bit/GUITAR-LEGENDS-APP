package com.example.guitarlegends.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject

enum class OperationType {
    CREATE, UPDATE, DELETE, LIST, GET, WRITE
}

data class FirestoreErrorInfo(
    val error: String,
    val operationType: OperationType,
    val path: String?,
    val uid: String?,
    val email: String?
)

fun handleFirestoreError(
    e: Exception,
    operationType: OperationType,
    path: String?,
    auth: FirebaseAuth?
): Nothing {
    val currentUser = auth?.currentUser
    val errorJson = JSONObject().apply {
        put("error", e.message ?: "Erro desconhecido no Firestore")
        put("operationType", operationType.name.lowercase())
        put("path", path ?: "")
        put("authInfo", JSONObject().apply {
            put("userId", currentUser?.uid ?: "")
            put("email", currentUser?.email ?: "")
            put("emailVerified", currentUser?.isEmailVerified ?: false)
            put("isAnonymous", currentUser?.isAnonymous ?: false)
        })
    }
    Log.e("FirestoreError", "Erro no Firestore [${operationType.name}] em $path: $errorJson", e)
    throw e
}
