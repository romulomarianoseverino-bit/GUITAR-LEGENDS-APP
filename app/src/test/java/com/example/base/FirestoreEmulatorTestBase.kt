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
import java.net.InetSocketAddress
import java.net.Socket
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

        // O GameViewModel usa FirebaseClientProvider e, portanto, a instância padrão.
        // Os testes devem autenticar essa mesma instância, não um FirebaseApp nomeado isolado.
        val existingApp = runCatching { FirebaseApp.getInstance() }.getOrNull()
        val app = existingApp ?: FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApplicationId("1:1234567890:android:abcdef123456")
                .setProjectId(PROJECT_ID)
                .setApiKey("fake-api-key-for-emulator")
                .build()
        )

        firestore = FirebaseFirestore.getInstance(app, databaseId)
        auth = FirebaseAuth.getInstance(app)
        functions = FirebaseFunctions.getInstance(app)

        if (existingApp == null) {
            firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
        }
        // Configura os emuladores mesmo quando FirebaseInitProvider já criou o app padrão.
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
        val emulatorReachability = runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(EMULATOR_HOST, AUTH_PORT), 3000)
                "reachable"
            }
        }.getOrElse { "unreachable: ${it.message ?: it.javaClass.simpleName}" }
        println(
            "[AUTH-TEST] emulator=$EMULATOR_HOST:$AUTH_PORT tcp=$emulatorReachability " +
                "app=${auth.app.name} project=${auth.app.options.projectId} " +
                "apiKeyPresent=${!auth.app.options.apiKey.isNullOrBlank()}"
        )
        withTimeout(AUTH_TIMEOUT_MS) {
            println("[AUTH-TEST] starting FirebaseAuth SDK create/sign-in request")
            val result = try {
                auth.createUserWithEmailAndPassword(email, DEFAULT_PASSWORD).await()
            } catch (_: FirebaseAuthUserCollisionException) {
                auth.signInWithEmailAndPassword(email, DEFAULT_PASSWORD).await()
            }
            println("[AUTH-TEST] FirebaseAuth SDK request completed")
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
        const val EMULATOR_HOST = "127.0.0.1"
        const val FIRESTORE_PORT = 8085
        const val AUTH_PORT = 9099
        const val FUNCTIONS_PORT = 5001
        const val PROJECT_ID = "guitar-legends-d7708"
        const val DEFAULT_PASSWORD = "password123"
        // O Auth Emulator pode levar mais tempo em CI; o limite não deve encerrar o teste prematuramente.
        const val AUTH_TIMEOUT_MS = 60000L
    }
}
