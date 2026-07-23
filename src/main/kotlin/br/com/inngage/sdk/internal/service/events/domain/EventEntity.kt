package br.com.inngage.sdk.internal.service.events.domain

import org.json.JSONObject

/**
 * Domain entity representing a named SDK event.
 *
 * Exactly one of [identifier] / [registration] is expected to be non-null:
 * the user identifier when the consumer provided one, otherwise the FCM
 * registration token resolved internally by the SDK.
 *
 * [conversionValue] and [conversionNotid] are only sent to the API when
 * [conversionEvent] is `true`.
 */
internal data class EventEntity(
    val appToken: String,
    val eventName: String,
    val identifier: String? = null,
    val registration: String? = null,
    val eventValues: JSONObject? = null,
    val conversionValue: Any? = null,
    val conversionNotid: String? = null,
    val conversionEvent: Boolean = false
)
