package dev.woodos.launcher.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import dev.woodos.launcher.core.AppRef
import dev.woodos.launcher.core.Apps

/** Simplified notification model for the WoodOS Notification Center. */
data class WoodNotif(
    val key: String,
    val pkg: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val clearable: Boolean,
    val intent: PendingIntent?,
    val ref: AppRef?,
)

/**
 * NotificationListenerService feeding the WoodOS Notification Center.
 * Requires the user to grant notification access in system settings —
 * this is the only Android-supported way for a launcher to read notifications.
 */
class WoodNotificationListener : NotificationListenerService() {

    companion object {
        val notifications = mutableStateListOf<WoodNotif>()
        val history = mutableStateListOf<WoodNotif>()
        val connected = mutableStateOf(false)
        var listenerInstance: WoodNotificationListener? = null
            private set

        private val main = Handler(Looper.getMainLooper())

        fun isGranted(context: Context): Boolean =
            android.provider.Settings.Secure.getString(
                context.contentResolver, "enabled_notification_listeners"
            )?.contains(context.packageName) == true

        fun openAccessSettings(context: Context) {
            context.startActivity(
                Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }

        fun clearAll() {
            val l = listenerInstance ?: run {
                notifications.clear(); return
            }
            val snapshot = ArrayList(notifications)
            notifications.clear()
            snapshot.forEach {
                if (it.clearable) runCatching { l.cancelNotification(it.key) }
                history.add(0, it)
            }
            if (history.size > 100) history.removeRange(100, history.size)
        }

        fun dismiss(n: WoodNotif) {
            notifications.removeAll { it.key == n.key }
            runCatching { listenerInstance?.cancelNotification(n.key) }
            history.add(0, n)
            if (history.size > 100) history.removeRange(100, history.size)
        }

        fun open(context: Context, n: WoodNotif) {
            runCatching { n.intent?.send(context, 0, null) }
        }
    }

    override fun onListenerConnected() {
        listenerInstance = this
        connected.value = true
        refresh()
    }

    override fun onListenerDisconnected() {
        connected.value = false
        listenerInstance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        refreshOne(sbn, added = true)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        main.post { notifications.removeAll { it.key == sbn.key } }
    }

    private fun refresh() {
        val active = try { activeNotifications?.toList() ?: emptyList() } catch (_: Exception) { emptyList() }
        main.post {
            notifications.clear()
            active.filter { it.packageName != packageName }.forEach { mapToWood(it)?.let(notifications::add) }
            notifications.sortByDescending { it.timestamp }
        }
    }

    private fun refreshOne(sbn: StatusBarNotification, added: Boolean) {
        if (added) {
            val w = mapToWood(sbn) ?: return
            main.post {
                notifications.removeAll { it.key == w.key }
                notifications.add(0, w)
            }
        }
    }

    private fun mapToWood(sbn: StatusBarNotification): WoodNotif? {
        return try {
            val extras = sbn.notification.extras
            val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
            if (title.isBlank() && text.isBlank()) return null
            val pm = packageManager
            val appName = try {
                pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
            } catch (_: Exception) {
                sbn.packageName
            }
            val ref = Apps.apps.value.firstOrNull { it.ref.pkg == sbn.packageName }?.ref
            WoodNotif(
                key = sbn.key,
                pkg = sbn.packageName,
                appName = appName,
                title = title,
                text = text,
                timestamp = sbn.postTime,
                clearable = sbn.isClearable,
                intent = sbn.notification.contentIntent,
                ref = ref,
            )
        } catch (_: Exception) {
            null
        }
    }
}
