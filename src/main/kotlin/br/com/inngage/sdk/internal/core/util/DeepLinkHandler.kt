package br.com.inngage.sdk.internal.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

/**
 * Handles deep-link and in-app browser navigation triggered by push notifications.
 * Extracts the `web()` and `deep()` functions from InngageUtils.java.
 */
internal object DeepLinkHandler {

    /**
     * Opens [url] in a Chrome Custom Tab.
     *
     * @param context Must be [Context.applicationContext] to avoid Activity leaks.
     */
    fun openInBrowser(context: Context, url: String) {
        if (url.isBlank()) return
        val intent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        intent.launchUrl(context, Uri.parse(url))
    }

    /**
     * Fires an [Intent.ACTION_VIEW] deep-link for [url].
     *
     * @param context Must be called with [Intent.FLAG_ACTIVITY_NEW_TASK] when launched
     *                from a non-Activity context.
     */
    fun openDeepLink(context: Context, url: String) {
        if (url.isBlank()) return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** Returns `true` if [url] is a non-null, non-empty string. */
    fun isValid(url: String?): Boolean = !url.isNullOrBlank()
}

