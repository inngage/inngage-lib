package br.com.inngage.sdk.internal.service.notificationstatus.data

import br.com.inngage.sdk.internal.core.http.HttpClient
import br.com.inngage.sdk.internal.service.notificationstatus.domain.NotificationStatusRepository
import org.json.JSONObject

/** HTTP-backed implementation of [NotificationStatusRepository]. */
internal class NotificationStatusRepositoryImpl(
    private val httpClient: HttpClient = HttpClient()
) : NotificationStatusRepository {

    override suspend fun sendCallback(
        notificationId: String,
        appToken: String,
        endpoint: String
    ): Result<Unit> {
        val body = JSONObject().apply {
            put("id", notificationId)
            put("notid", notificationId)
            put("app_token", appToken)
        }
        val payload = JSONObject().put("notificationRequest", body)
        return httpClient.post(payload, endpoint).map { /* discard body */ }
    }
}

