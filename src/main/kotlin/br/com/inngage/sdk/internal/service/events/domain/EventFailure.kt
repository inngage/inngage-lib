package br.com.inngage.sdk.internal.service.events.domain

/** Sealed failure type for the events service. */
internal sealed class EventFailure {
    data class ValidationError(val message: String) : EventFailure()
    data class NetworkError(val message: String, val cause: Throwable? = null) : EventFailure()
}

