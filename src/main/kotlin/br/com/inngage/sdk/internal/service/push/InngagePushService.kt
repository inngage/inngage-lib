package br.com.inngage.sdk.internal.service.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.annotation.NonNull
import androidx.core.app.NotificationCompat
import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.core.storage.PreferencesStorage
import br.com.inngage.sdk.internal.orchestration.NotificationRouter
import br.com.inngage.sdk.internal.service.push.domain.PushConfig
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Random

/**
 * Firebase push notification receiver.
 *
 * Replaces both `br.com.inngage.sdk.PushMessagingService` (root-level) and
 * `br.com.inngage.sdk.service.PushMessagingService` (the duplicate).
 *
 * Push config is provided via [NotificationRouter.pushConfig] — set before
 * [com.google.firebase.messaging.FirebaseMessagingService] is started.
 */
internal class InngagePushService : FirebaseMessagingService() {

    private val tag = InngageConfig.TAG
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(@NonNull token: String) {
        super.onNewToken(token)
        PreferencesStorage(applicationContext).putString(InngageConfig.PREF_FCM_TOKEN, token)
        Log.d(InngageConfig.TAG_FIREBASE, "FCM token refreshed and persisted")
        // Re-subscription is intentionally deferred to next app launch via SdkInitializer.
    }

    override fun onMessageReceived(@NonNull message: RemoteMessage) {
        super.onMessageReceived(message)
        if (message.data.isEmpty()) return

        runCatching {
            val json = JSONObject(message.data as Map<*, *>)
            Log.i(tag, "Push received: $json")
            handlePush(json)
        }.onFailure {
            Log.e(tag, "Error parsing push data: ${it.message}")
        }
    }

    private fun handlePush(json: JSONObject) {
        val config = NotificationRouter.pushConfig
        val intent = Intent().also { i ->
            config.targetActivity
                ?.takeIf { it.isNotBlank() }
                ?.let { i.setClassName(packageName, it) }
                ?: ActivityResolver.resolveMainActivity(i, packageManager, packageName)
        }

        NotificationRouter.routePush(applicationContext, intent, json)
        showNotification(intent, json, config)
    }

    private fun createChannel(config: PushConfig) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                config.channelId, config.channelName, NotificationManager.IMPORTANCE_HIGH
            ).apply { description = config.channelDescription }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(ch)
        }
    }

    private fun pendingIntent(intent: Intent, requestCode: Int): PendingIntent {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getActivity(this, requestCode, intent, flags)
    }

    private fun showNotification(intent: Intent, json: JSONObject, config: PushConfig) {
        createChannel(config)

        val title    = json.optString("title", "Nova notificação")
        val body     = json.optString("body", "")
        val imageUrl = json.optString("picture", "")
        val notifId  = Random().nextInt(1_000_000)
        val reqCode  = System.currentTimeMillis().toInt()
        val sound    = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(this, config.channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(sound)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSmallIcon(config.smallIcon)
            .setContentIntent(pendingIntent(intent, reqCode))

        // The SDK builds every notification itself (data messages), so Firebase
        // never applies the consumer's accent color — apply it here when set.
        if (config.notificationColor != PushConfig.COLOR_UNSET) {
            builder.color = config.notificationColor
        }

        if (imageUrl.isNotBlank()) {
            serviceScope.launch {
                loadBitmap(imageUrl)?.let { bmp ->
                    builder.setLargeIcon(bmp)
                    builder.setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(bmp)
                            .setSummaryText(body)
                    )
                    notify(notifId, builder)
                } ?: notify(notifId, builder)
            }
        } else {
            notify(notifId, builder)
        }
    }

    private fun notify(id: Int, builder: NotificationCompat.Builder) {
        (getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
            ?.notify(id, builder.build())
    }

    private fun loadBitmap(url: String): Bitmap? = runCatching {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.doInput = true
        conn.connect()
        BitmapFactory.decodeStream(conn.inputStream)
    }.getOrNull()
}

