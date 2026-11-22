package com.jusu.aicallassistant.calls

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Detects WhatsApp call notifications. When WhatsApp exposes an explicit
 * Decline/Reject notification action, JUSU mirrors that action in its own
 * notification so the user can invoke WhatsApp's own PendingIntent.
 *
 * This does not access call audio or attempt to control WhatsApp outside
 * the action explicitly supplied by WhatsApp in its notification.
 */
class WhatsAppCallNotificationListener : NotificationListenerService() {

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            CHANNEL_ID,
            "WhatsApp call controls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Optional decline shortcut for detected WhatsApp calls"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        if (packageName != WHATSAPP_PACKAGE && packageName != WHATSAPP_BUSINESS_PACKAGE) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val categoryIsCall = notification.category == Notification.CATEGORY_CALL
        val textLooksLikeCall = CALL_PATTERN.containsMatchIn(title) ||
            CALL_PATTERN.containsMatchIn(text)

        if (!categoryIsCall && !textLooksLikeCall) return

        val safeLabel = title.takeIf { it.isNotBlank() }?.take(80) ?: "WhatsApp call"
        val appLabel = if (packageName == WHATSAPP_BUSINESS_PACKAGE) "WhatsApp Business" else "WhatsApp"
        val declineAction = notification.actions
            ?.firstOrNull { action ->
                val actionTitle = action.title?.toString().orEmpty()
                DECLINE_PATTERN.matches(actionTitle.trim())
            }

        getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit()
            .putString(KEY_LAST_LABEL, safeLabel)
            .putLong(KEY_LAST_TIME, System.currentTimeMillis())
            .putString(KEY_LAST_APP, appLabel)
            .putBoolean(KEY_DECLINE_AVAILABLE, declineAction?.actionIntent != null)
            .apply()

        // Only expose a decline shortcut when WhatsApp itself supplies one.
        val pendingIntent = declineAction?.actionIntent ?: return
        val shortcut = Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
            "Decline WhatsApp call",
            pendingIntent
        ).build()

        val jusuNotification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_call_missed)
            .setContentTitle("Incoming $appLabel call")
            .setContentText("$safeLabel · Use the action to decline")
            .setCategory(Notification.CATEGORY_CALL)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .addAction(shortcut)
            .build()

        getSystemService(NotificationManager::class.java).notify(WHATSAPP_NOTIFICATION_ID, jusuNotification)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName == WHATSAPP_PACKAGE || sbn.packageName == WHATSAPP_BUSINESS_PACKAGE) {
            getSystemService(NotificationManager::class.java).cancel(WHATSAPP_NOTIFICATION_ID)
            getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit()
                .putBoolean(KEY_DECLINE_AVAILABLE, false)
                .apply()
        }
    }

    companion object {
        const val PREFERENCES = "jusu_whatsapp_call_detection"
        const val KEY_LAST_LABEL = "last_call_label"
        const val KEY_LAST_TIME = "last_call_time"
        const val KEY_LAST_APP = "last_call_app"
        const val KEY_DECLINE_AVAILABLE = "decline_action_available"

        private const val WHATSAPP_PACKAGE = "com.whatsapp"
        private const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"
        private const val CHANNEL_ID = "jusu_whatsapp_call_controls"
        private const val WHATSAPP_NOTIFICATION_ID = 72041
        private val CALL_PATTERN = Regex(
            "(incoming|outgoing|voice|video)\\s+(whatsapp\\s+)?call|call\\s+(from|with)",
            RegexOption.IGNORE_CASE
        )
        private val DECLINE_PATTERN = Regex(
            "(decline|reject)(\\s+call)?",
            RegexOption.IGNORE_CASE
        )
    }
}
