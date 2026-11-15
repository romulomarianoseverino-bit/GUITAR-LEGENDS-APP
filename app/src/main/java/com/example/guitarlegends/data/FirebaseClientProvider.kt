package com.example.guitarlegends.data

import android.content.Context
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseClientProvider {

    fun ensureInitialized(context: Context): FirebaseApp? {
        return runCatching {
            FirebaseApp.getApps(context).firstOrNull() ?: FirebaseApp.initializeApp(context)
        }.getOrNull()
    }

    fun getAuthOrNull(context: Context): FirebaseAuth? {
        val app = ensureInitialized(context) ?: return null
        return runCatching { FirebaseAuth.getInstance(app) }.getOrNull()
    }

    fun getFirestoreOrNull(context: Context): FirebaseFirestore? {
        val app = ensureInitialized(context) ?: return null
        return runCatching {
            val dbId = context.getString(R.string.firestore_database_id)
            FirebaseFirestore.getInstance(app, dbId)
        }.getOrNull()
    }
}
