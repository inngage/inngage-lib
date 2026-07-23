package br.com.inngage.sdk.internal.service.inapp.application

import android.content.Intent
import android.util.Log
import br.com.inngage.sdk.internal.core.config.InngageConfig
import org.json.JSONObject

/**
 * Writes selected JSON push-data fields into Android [Intent] extras for the
 * notification tap action. Replaces IntentDataManager.java.
 */
internal object InAppIntentMapper {

    private val tag = InngageConfig.TAG_INAPP

    /**
     * Writes the fields from [data] JSON into [intent] extras.
     * Each field in [keys] is written only when present in [data].
     */
    fun writeToIntent(intent: Intent, data: String, keys: Array<String>) {
        runCatching {
            val json = JSONObject(data)
            keys.forEach { key ->
                if (json.has(key) && !json.isNull(key)) {
                    when {
                        json.optInt(key, Int.MIN_VALUE) != Int.MIN_VALUE ->
                            intent.putExtra(key, json.getInt(key))
                        json.optBoolean(key, false) ->
                            intent.putExtra(key, json.getBoolean(key))
                        else ->
                            intent.putExtra(key, json.getString(key))
                    }
                }
            }
        }.onFailure { Log.e(tag, "writeToIntent failed: ${it.message}") }
    }
}

