package br.com.inngage.sdk.internal.service.location

import android.content.Context
import android.content.SharedPreferences
import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.core.storage.PreferencesStorage
import br.com.inngage.sdk.internal.service.location.data.PreferencesLocationCache
import br.com.inngage.sdk.internal.service.location.domain.Coordinates
import br.com.inngage.sdk.internal.service.location.domain.SentLocation
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("PreferencesLocationCache")
class PreferencesLocationCacheTest {

    private val editor  = mockk<SharedPreferences.Editor>(relaxed = true)
    private val prefs   = mockk<SharedPreferences>(relaxed = true)
    private val context = mockk<Context>()
    private lateinit var cache: PreferencesLocationCache

    @BeforeEach
    fun setUp() {
        every { context.applicationContext } returns context
        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putLong(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        cache = PreferencesLocationCache(PreferencesStorage(context))
    }

    @Test
    fun `returns null when nothing stored`() {
        every { prefs.getString(any(), any()) } returns ""
        every { prefs.getLong(any(), any()) } returns -1L
        assertNull(cache.lastSent())
    }

    @Test
    fun `returns null when stored values are corrupt`() {
        every { prefs.getString(InngageConfig.PREF_LAST_LOCATION_LAT, any()) } returns "abc"
        every { prefs.getString(InngageConfig.PREF_LAST_LOCATION_LONG, any()) } returns "-46.6"
        every { prefs.getLong(any(), any()) } returns 10L
        assertNull(cache.lastSent())
    }

    @Test
    fun `round-trips a stored location`() {
        every { prefs.getString(InngageConfig.PREF_LAST_LOCATION_LAT, any()) } returns "-23.5505"
        every { prefs.getString(InngageConfig.PREF_LAST_LOCATION_LONG, any()) } returns "-46.6333"
        every { prefs.getLong(InngageConfig.PREF_LAST_LOCATION_AT, any()) } returns 42L

        assertEquals(SentLocation(Coordinates(-23.5505, -46.6333), 42L), cache.lastSent())
    }

    @Test
    fun `saves all three keys`() {
        cache.saveLastSent(SentLocation(Coordinates(1.5, -2.5), 99L))
        verify { editor.putString(InngageConfig.PREF_LAST_LOCATION_LAT, "1.5") }
        verify { editor.putString(InngageConfig.PREF_LAST_LOCATION_LONG, "-2.5") }
        verify { editor.putLong(InngageConfig.PREF_LAST_LOCATION_AT, 99L) }
    }

    @Test
    fun `clear removes all three keys`() {
        cache.clear()
        verify { editor.remove(InngageConfig.PREF_LAST_LOCATION_LAT) }
        verify { editor.remove(InngageConfig.PREF_LAST_LOCATION_LONG) }
        verify { editor.remove(InngageConfig.PREF_LAST_LOCATION_AT) }
    }
}
