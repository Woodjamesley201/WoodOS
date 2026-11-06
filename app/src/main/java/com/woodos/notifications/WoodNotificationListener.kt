package com.woodos.notifications

import android.app.Notification
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Optional notification access. WoodOS never asks for this by itself;
 * the user can grant it from Settings so the home screen and WoodDesk
 * tray can show a notification count. The service does no work when
 * nothing is listening - it only recounts active notifications.
 */
class WoodNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refresh()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refresh()
    }

    private fun refresh() {
        activeNotifications?.let { activeCount = it.size }
    }

    override fun onListenerConnected() {
        activeNotifications?.let { activeCount = it.size }
    }

    companion object {
        /** Count of active notifications; read by Home / WoodDesk trays. */
        @Volatile
        var activeCount: Int = 0
            private set

        /** True when the user has granted WoodOS notification access. */
        fun isGranted(context: Context): Boolean {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            return flat.split(':').any {
                it.isNotBlank() && it.contains(context.packageName)
            }
        }

        /** Convenience: count to display, or null when access is not granted. */
        fun countOrNull(context: Context): Int? =
            if (isGranted(context)) activeCount else null
    }
}
