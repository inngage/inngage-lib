package br.com.inngage.sdk.internal.service.subscription.domain

/**
 * Sealed failure type for the subscription service.
 * Services must never throw across boundaries — use this sealed class instead.
 */
internal sealed class SubscriptionFailure {
    data class ValidationError(val message: String) : SubscriptionFailure()
    data class TokenRetrievalError(val cause: Throwable) : SubscriptionFailure()
    data class NetworkError(val message: String, val cause: Throwable? = null) : SubscriptionFailure()
    data object LocationUnavailable : SubscriptionFailure()
}

