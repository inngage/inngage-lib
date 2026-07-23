package br.com.inngage.sdk.internal.core.storage

import android.content.Context
import android.content.SharedPreferences

/**
 * Thin wrapper around [SharedPreferences].
 * Replaces AppPreferences.java.
 *
 * Always stores against [Context.applicationContext] — never against Activity context.
 */
internal class PreferencesStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("inngage_prefs", Context.MODE_PRIVATE)

    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getString(key: String, default: String = ""): String =
        prefs.getString(key, default) ?: default

    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean =
        prefs.getBoolean(key, default)

    fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    fun getInt(key: String, default: Int = 0): Int =
        prefs.getInt(key, default)

    fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    fun getLong(key: String, default: Long = 0L): Long =
        prefs.getLong(key, default)

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun contains(key: String): Boolean = prefs.contains(key)

    /** Removes all entries from this preferences store. */
    fun clear() {
        prefs.edit().clear().apply()
    }
}



