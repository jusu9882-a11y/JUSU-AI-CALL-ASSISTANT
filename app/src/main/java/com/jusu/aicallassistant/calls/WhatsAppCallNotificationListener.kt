package com.jusu.aicallassistant.calls

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.Notification
import android.content.SharedPreferences

/**
 * Observes WhatsApp call notifications only. Android does not expose WhatsApp's
 * private VoIP call controls or audio to this listener.
 */
class WhatsAppCallNotificationListener : NotificationListenerService() {

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

        // Store only a short call label and timestamp; never persist notification text.
        val safeLabel = title.takeIf { it.isNotBlank() }?.take(80) ?: "WhatsApp call"
        getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit()
            .putString(KEY_LAST_LABEL, safeLabel)
            .putLong(KEY_LAST_TIME, System.currentTimeMillis())
            .putString(KEY_LAST_APP, if (packageName == WHATSAPP_BUSINESS_PACKAGE) "WhatsApp Business" else "WhatsApp")
            .apply()
    }

    companion object {
        const val PREFERENCES = "jusu_whatsapp_call_detection"
        const val KEY_LAST_LABEL = "last_call_label"
        const val KEY_LAST_TIME = "last_call_time"
        const val KEY_LAST_APP = "last_call_app"

        private const val WHATSAPP_PACKAGE = "com.whatsapp"
        private const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"
        private val CALL_PATTERN = Regex(
            "(incoming|outgoing|voice|video)\\s+(whatsapp\\s+)?call|call\\s+(from|with)",
            RegexOption.IGNORE_CASE
        )
    }
}
