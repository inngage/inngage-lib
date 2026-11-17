package br.com.inngage.sdk.internal.service.location.domain

/**
 * Sealed failure type for the location service.
 *
 * Extends [Exception] only so it can travel inside [kotlin.Result.failure];
 * callers match on the subtype to decide whether a retry makes sense.
 */
internal sealed class LocationFailure(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** A permanent failure: retrying with the same input would fail again. */
    sealed class Permanent(message: String) : LocationFailure(message)

    /** A transient failure (connectivity, 5xx): worth retrying later. */
    class Network(message: String, cause: Throwable? = null) : LocationFailure(message, cause)

    class InvalidAppToken(message: String) : Permanent(message)
    class InvalidCoordinates(message: String) : Permanent(message)
    /** No `identifier`, `registration` or `uuid` available — `subscribe()` never ran. */
    class MissingIdentity(message: String) : Permanent(message)
    /** HTTP 400 — the backend rejected the request body. */
    class BadRequest(message: String) : Permanent(message)
    /** HTTP 401 — invalid or expired `app_token`. */
    class Unauthorized(message: String) : Permanent(message)
    /** HTTP 404 — the subscriber does not exist in this app yet. */
    class SubscriberNotFound(message: String) : Permanent(message)
}
