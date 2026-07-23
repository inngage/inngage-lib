package br.com.inngage.sdk.internal.core

import br.com.inngage.sdk.internal.core.config.InngageProvider
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("InngageProvider")
class ConfigEnumTest {

    @Nested
    @DisplayName("InngageProvider.from()")
    inner class ProviderFrom {
        @Test fun `maps FCM`() = assertEquals(InngageProvider.FCM, InngageProvider.from("FCM"))
        @Test fun `maps GCM`() = assertEquals(InngageProvider.GCM, InngageProvider.from("GCM"))
        @Test fun `defaults to FCM for unknown`() = assertEquals(InngageProvider.FCM, InngageProvider.from("APNS"))
    }
}
