package br.com.inngage.sdk.internal.platform.permission

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat

/**
 * Checks notification and location permission state.
 * Replaces the reflection-based `NotificationsUtils` inner class in InngageService.java
 * with the modern `NotificationManagerCompat` API.
 */
internal class PermissionChecker(private val context: Context) {

    /**
     * Returns `true` if the user has granted permission to show notifications.
     * Uses [NotificationManagerCompat] on API 19+ and falls back gracefully.
     */
    fun areNotificationsEnabled(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /**
     * Returns `true` if the app holds [android.Manifest.permission.ACCESS_FINE_LOCATION]
     * or [android.Manifest.permission.ACCESS_COARSE_LOCATION].
     */
    fun hasLocationPermission(): Boolean {
        val pm = context.packageManager
        val pkg = context.packageName
        val fine = pm.checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, pkg)
        val coarse = pm.checkPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION, pkg)
        return fine == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                coarse == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}

