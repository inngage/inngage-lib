package br.com.inngage.sdk.internal.platform.deviceinfo

import android.content.Context
import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.core.storage.PreferencesStorage
import java.util.UUID

/**
 * Provides the per-installation device UUID.
 *
 * A random [UUID] is generated once on first use and persisted in
 * [PreferencesStorage] under [InngageConfig.PREF_DEVICE_UUID]; every
 * subsequent call returns the same value, keeping the subscriber identity
 * stable across app launches.
 *
 * Replaces the legacy `DeviceIdProvider` (Wi-Fi MAC → IMEI → ANDROID_ID),
 * whose sources are unavailable on modern Android versions.
 */
internal class DeviceUuidProvider(context: Context) {

    private val storage = PreferencesStorage(context)

    /** Returns the persisted installation UUID, generating it on first use. */
    fun getUuid(): String =
        storage.getString(InngageConfig.PREF_DEVICE_UUID).ifBlank {
            UUID.randomUUID().toString().also {
                storage.putString(InngageConfig.PREF_DEVICE_UUID, it)
            }
        }
}
