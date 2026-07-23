package br.com.inngage.sdk.internal.core.error

/**
 * Top-level sealed hierarchy for all SDK failures.
 * Each service module extends this with its own sub-sealed class.
 */
internal sealed class InngageFailure {
    /** A network or HTTP-level error. */
    data class NetworkError(val message: String, val cause: Throwable? = null) : InngageFailure()

    /** An error caused by invalid input from the SDK consumer. */
    data class ValidationError(val message: String) : InngageFailure()

    /** An unexpected internal SDK error. */
    data class UnknownError(val message: String, val cause: Throwable? = null) : InngageFailure()
}

