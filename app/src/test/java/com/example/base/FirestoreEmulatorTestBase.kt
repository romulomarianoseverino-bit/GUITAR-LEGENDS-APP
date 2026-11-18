package com.example.base

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.firestore.MemoryCacheSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@LooperMode(LooperMode.Mode.PAUSED)
abstract class FirestoreEmulatorTestBase {

    protected lateinit var firestore: FirebaseFirestore
    protected lateinit var auth: FirebaseAuth
    protected lateinit var functions: FirebaseFunctions
    protected lateinit var databaseId: String

    @Before
    open fun setUpFirebase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resId = context.resources.getIdentifier("firestore_database_id", "string", context.packageName)
        databaseId = if (resId != 0) context.getString(resId) else "demo-database"

        val existingApp = runCatching { FirebaseApp.getInstance(TEST_APP_NAME) }.getOrNull()
        val app = existingApp ?: FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApplicationId("com.aistudio.applet.kpfcic")
                .setProjectId(PROJECT_ID)
                .setApiKey("fake-api-key-for-emulator")
                .build(),
            TEST_APP_NAME
        )

        firestore = FirebaseFirestore.getInstance(app, databaseId)
        auth = FirebaseAuth.getInstance(app)
        functions = FirebaseFunctions.getInstance(app)

        // As instâncias Firebase podem sobreviver entre classes Robolectric.
        // Garanta que cada serviço use os emuladores, mesmo quando o FirebaseApp já existe.
        if (existingApp == null) {
            firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
        }
        firestore.useEmulator(EMULATOR_HOST, FIRESTORE_PORT)
        auth.useEmulator(EMULATOR_HOST, AUTH_PORT)
        functions.useEmulator(EMULATOR_HOST, FUNCTIONS_PORT)
    }

    @After
    open fun tearDownFirebase() {
        auth.signOut()
    }

    // Helper exclusivo para testes locais contra o Auth Emulator.
    protected suspend fun signInTestUser(email: String): String = withContext(Dispatchers.IO) {
        withTimeout(AUTH_TIMEOUT_MS) {
            val result = try {
                auth.createUserWithEmailAndPassword(email, DEFAULT_PASSWORD).await()
            } catch (_: FirebaseAuthUserCollisionException) {
                auth.signInWithEmailAndPassword(email, DEFAULT_PASSWORD).await()
            }
            checkNotNull(result.user?.uid) { "User auth failed for $email" }
        }
    }

    protected fun Throwable.unwrapFirestoreException(): FirebaseFirestoreException? {
        var current: Throwable? = this
        while (current != null) {
            if (current is FirebaseFirestoreException) return current
            current = current.cause
        }
        return null
    }

    private companion object {
        const val TEST_APP_NAME = "guitar-legends-test"
        const val EMULATOR_HOST = "127.0.0.1"
        const val FIRESTORE_PORT = 8085
        const val AUTH_PORT = 9099
        const val FUNCTIONS_PORT = 5001
        const val PROJECT_ID = "demo-no-project"
        const val DEFAULT_PASSWORD = "password123"
        const val AUTH_TIMEOUT_MS = 20000L
    }
}
