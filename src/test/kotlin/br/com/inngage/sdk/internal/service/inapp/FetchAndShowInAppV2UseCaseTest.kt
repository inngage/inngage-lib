package br.com.inngage.sdk.internal.service.inapp

import android.content.Context
import br.com.inngage.sdk.internal.service.inapp.application.FetchAndShowInAppV2UseCase
import br.com.inngage.sdk.internal.service.inapp.domain.InAppMessageV2
import br.com.inngage.sdk.internal.service.inapp.domain.InAppMessageV2Repository
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2Carousel
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2CarouselItem
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2Content
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2Media
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2Style
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("FetchAndShowInAppV2UseCase")
class FetchAndShowInAppV2UseCaseTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<InAppMessageV2Repository>()
    private val context    = mockk<Context>(relaxed = true)

    private val useCase = FetchAndShowInAppV2UseCase(
        context    = context,
        repository = repository,
        dispatcher = dispatcher
    )

    private fun validMessage() = InAppMessageV2(
        enabled = true,
        type    = "Banner",
        style   = InAppV2Style(),
        media   = InAppV2Media(
            InAppV2Carousel(
                isEnabled = true,
                items     = listOf(InAppV2CarouselItem(content = InAppV2Content(title = "Hello")))
            )
        )
    )

    @Nested
    @DisplayName("execute")
    inner class Execute {

        @Test
        fun `returns success when message is absent in response`() = runTest(dispatcher) {
            coEvery { repository.fetchMessage() } returns Result.success(null)

            val result = useCase.execute()

            assertTrue(result.isSuccess)
        }

        @Test
        fun `returns failure when fetch fails`() = runTest(dispatcher) {
            coEvery { repository.fetchMessage() } returns Result.failure(java.io.IOException("err"))

            val result = useCase.execute()

            assertTrue(result.isFailure)
        }

        @Test
        fun `returns failure when validation fails (disabled message)`() = runTest(dispatcher) {
            val disabled = validMessage().copy(enabled = false)
            coEvery { repository.fetchMessage() } returns Result.success(disabled)

            val result = useCase.execute()

            assertTrue(result.isFailure)
        }

        @Test
        fun `returns failure when validation fails (no content)`() = runTest(dispatcher) {
            val noContent = validMessage().copy(
                media = InAppV2Media(InAppV2Carousel(isEnabled = true, items = emptyList()))
            )
            coEvery { repository.fetchMessage() } returns Result.success(noContent)

            val result = useCase.execute()

            assertTrue(result.isFailure)
        }
    }
}
