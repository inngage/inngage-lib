package br.com.inngage.sdk.internal.platform.lifecycle

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Observes the whole-process lifecycle ([ProcessLifecycleOwner]) and invokes a
 * callback every time the app comes to the foreground.
 *
 * Registration is idempotent: calling [register] twice replaces the previous
 * callback instead of adding a second observer. Lifecycle registration must
 * happen on the main thread, so calls are posted there when needed.
 *
 * Platform-specific — lives exclusively in `platform/lifecycle/`.
 */
internal object AppForegroundObserver {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var onForeground: (() -> Unit)? = null

    private val observer = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            onForeground?.invoke()
        }
    }

    /**
     * Starts invoking [callback] on every foreground transition. If the app is
     * already in the foreground the callback fires immediately (the lifecycle
     * replays `ON_START` to new observers).
     */
    fun register(callback: () -> Unit) {
        runOnMain {
            val wasRegistered = onForeground != null
            onForeground = callback
            if (!wasRegistered) {
                ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
            } else if (isInForeground()) {
                callback()
            }
        }
    }

    fun unregister() {
        runOnMain {
            onForeground = null
            ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
        }
    }

    /** `true` while at least one Activity of the host app is started. */
    fun isInForeground(): Boolean =
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }
}
