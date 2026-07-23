package br.com.inngage.sdk.internal.service.push.domain

import androidx.annotation.DrawableRes

/**
 * Immutable configuration for push notification display.
 * Replaces the mutable singleton [br.com.inngage.sdk.InngagePushConfig].
 *
 * Because Inngage pushes arrive as FCM *data* messages, the SDK builds every
 * notification itself — Firebase never applies icon/color automatically, in
 * foreground or background. [smallIcon] and [notificationColor] are therefore
 * applied by the SDK to every notification it displays.
 *
 * @property smallIcon         Drawable used as the status-bar/small icon.
 * @property notificationColor Accent color (ARGB int, e.g. `Color.parseColor("#7043CC")`)
 *                             applied to the small icon and app name area.
 *                             [COLOR_UNSET] (default) keeps the system color.
 *
 * Use [Builder] to construct an instance.
 */
data class PushConfig(
    val channelId: String = "inngage_default_channel",
    val channelName: String = "Notificações",
    val channelDescription: String = "Canal padrão de notificações",
    @DrawableRes val smallIcon: Int = android.R.drawable.ic_dialog_info,
    val notificationColor: Int = COLOR_UNSET,
    val targetActivity: String? = null
) {
    companion object {
        /** Sentinel meaning "no accent color configured" — the system default is kept. */
        const val COLOR_UNSET = -1
    }

    class Builder {
        private var channelId: String = "inngage_default_channel"
        private var channelName: String = "Notificações"
        private var channelDescription: String = "Canal padrão de notificações"
        private var smallIcon: Int = android.R.drawable.ic_dialog_info
        private var notificationColor: Int = COLOR_UNSET
        private var targetActivity: String? = null

        fun channelId(id: String) = apply { channelId = id }
        fun channelName(name: String) = apply { channelName = name }
        fun channelDescription(desc: String) = apply { channelDescription = desc }
        fun smallIcon(@DrawableRes icon: Int) = apply { smallIcon = icon }
        fun notificationColor(color: Int) = apply { notificationColor = color }
        fun targetActivity(className: String) = apply { targetActivity = className }

        fun build() = PushConfig(
            channelId, channelName, channelDescription,
            smallIcon, notificationColor, targetActivity
        )
    }
}


