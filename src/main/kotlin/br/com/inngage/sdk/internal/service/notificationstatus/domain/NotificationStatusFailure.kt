package br.com.inngage.sdk.internal.service.notificationstatus.domain

/** Sealed failure type for notification status callbacks. */
internal sealed class NotificationStatusFailure {
    data class NetworkError(val message: String, val cause: Throwable? = null) : NotificationStatusFailure()
}

