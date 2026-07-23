package br.com.inngage.sdk.internal.service.push

import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

/**
 * Resolves the host app's launcher Activity to use as the push tap target.
 * Replaces `br.com.inngage.sdk.service.PackageManagerActivities`.
 */
internal object ActivityResolver {

    private const val TAG = "Inngage-Push"

    /**
     * Sets the component on [intent] to the first Activity whose name ends
     * with `.MainActivity`, or the first declared Activity as a fallback.
     */
    fun resolveMainActivity(intent: Intent, packageManager: PackageManager, packageName: String) {
        try {
            val activities = packageManager
                .getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
                .activities ?: return

            val target = activities.firstOrNull { it.name.endsWith(".MainActivity") }
                ?: activities.firstOrNull()
                ?: return

            intent.setClassName(packageName, target.name)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Package not found while resolving main activity: $packageName", e)
        }
    }
}

