package br.com.inngage.sdk.internal.service.notificationstatus.domain

/** Repository contract for sending a notification callback (open/impression). */
internal interface NotificationStatusRepository {
    /**
     * Sends a notification open callback to Inngage.
     * @return [Result.success] on HTTP 200, [Result.failure] otherwise.
     */
    suspend fun sendCallback(notificationId: String, appToken: String, endpoint: String): Result<Unit>
}

