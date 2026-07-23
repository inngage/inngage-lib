package br.com.inngage.sdk.internal.core.config

/**
 * Supported push notification providers.
 * Replaces InngageProvider.java.
 */
enum class InngageProvider(val value: String) {
    FCM(InngageConfig.PROVIDER_FCM),
    GCM(InngageConfig.PROVIDER_GCM);

    companion object {
        /** Returns the [InngageProvider] matching [value], defaulting to [FCM]. */
        fun from(value: String): InngageProvider =
            entries.firstOrNull { it.value == value } ?: FCM
    }
}

