package br.com.inngage.sdk.internal.service.notificationstatus.domain

/**
 * Represents the FCM push payload received when a notification is tapped.
 *
 * Delivered to the consumer via [br.com.inngage.sdk.InngageClient.handleNotification]'s
 * `onNotificationClick` callback.
 *
 * @property notId           Inngage notification ID (used for the open callback).
 * @property id              Duplicate notification ID field present in some payloads.
 * @property title           Notification title.
 * @property body            Notification body text.
 * @property type            Link type: `"deep"` opens an external browser;
 *                           `"inapp"` opens a Chrome Custom Tab inside the app.
 *                           `null` when no navigation is required.
 * @property url             Destination URL associated with [type]. `null` when absent.
 * @property additionalData  Raw extra JSON string attached to the notification.
 */
data class NotificationPayload(
    val notId: String,
    val id: String,
    val title: String,
    val body: String,
    val type: String?,
    val url: String?,
    val additionalData: String?
)
