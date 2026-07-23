package br.com.inngage.sdk.internal.platform.appinfo

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import br.com.inngage.sdk.internal.core.config.InngageConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Domain model for app installation metadata.
 * Replaces AppInfo.java.
 */
internal data class AppInfo(
    val installationDate: String,
    val updateDate: String,
    val versionName: String
)

/**
 * Reads app metadata from the system [PackageManager].
 * Platform-specific — lives exclusively in `platform/`.
 */
internal class AppInfoProvider(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /** Returns [AppInfo] for the current application package. */
    fun getAppInfo(): AppInfo {
        val packageName = context.packageName
        return try {
            val pm = context.packageManager
            val info = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            AppInfo(
                installationDate = dateFormat.format(Date(info.firstInstallTime)),
                updateDate = dateFormat.format(Date(info.lastUpdateTime)),
                versionName = info.versionName ?: ""
            )
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(InngageConfig.TAG, "Failed to read app info: ${e.message}")
            AppInfo("", "", "")
        }
    }
}

