package br.com.inngage.sdk.internal.platform.firebase

import android.util.Log
import br.com.inngage.sdk.internal.core.config.InngageConfig
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Wraps [FirebaseMessaging] token retrieval as a Kotlin `suspend` function.
 *
 * Firebase is **never** coupled directly to business logic — all SDK services
 * receive a token `String` and have no knowledge of Firebase internals.
 *
 * Platform-specific — lives exclusively in `platform/firebase/`.
 */
internal class FirebaseTokenProvider {

    private val tag = InngageConfig.TAG_FIREBASE

    /**
     * Suspends until Firebase delivers a registration token.
     *
     * @throws [Exception] if Firebase fails to retrieve the token.
     */
    suspend fun getToken(): String = suspendCoroutine { cont ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                Log.d(tag, "FCM token obtained")
                cont.resume(token)
            }
            .addOnFailureListener { exception ->
                Log.e(tag, "Failed to obtain FCM token: ${exception.message}")
                cont.resumeWithException(exception)
            }
    }
}

