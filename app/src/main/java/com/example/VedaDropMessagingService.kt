package com.example

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.data.VedaDropRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * §710 P0-5 — receives FCM pushes while the app is backgrounded/killed and posts a
 * notification that deep-links into the referenced booking / complaint / offer when
 * tapped (MainActivity reads the `notif_*` extras and routes via openNotification).
 *
 * Requires `google-services.json` + the `com.google.gms.google-services` plugin to be
 * added before push actually flows — see FCM_PAYMENTS_SETUP.md. The class itself
 * compiles with the firebase-messaging dependency alone.
 */
class VedaDropMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // The token can rotate while the app is backgrounded. Cache it so the app
        // re-registers on next launch/login (registerFcmToken also reads the live
        // token, so this is a belt-and-suspenders for rotation).
        getSharedPreferences("nikhatglow_session", Context.MODE_PRIVATE)
            .edit().putString("pending_fcm_token", token).apply()

        // …but if the app stays backgrounded for a long time the cache is never
        // read and the backend keeps a stale token, so pushes stop reaching this
        // device until next launch. Register the rotated token NOW, best-effort,
        // off a short-lived IO coroutine. VedaDropRepository.registerDevice is the
        // same path the app uses (AuthInterceptor injects the saved Bearer token),
        // and it's an authed no-op when logged out — runCatching swallows the
        // resulting failure, so this never crashes.
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { VedaDropRepository(applicationContext).registerDevice(token) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data

        // §725 Batch-B — URGENT job alerts are SAFETY-critical: they ring the high-alert
        // alarm regardless of the "Push Reminders" toggle (the toggle is for routine
        // reminders, not an active customer waiting for a professional right now).
        // Data-only contract: {"type":"urgent_offer",...} starts the alarm;
        // {"type":"urgent_cleared",...} stops it.
        when (data["type"]) {
            "urgent_offer" -> {
                UrgentAlarmService.start(this)
                return
            }
            "urgent_cleared" -> {
                UrgentAlarmService.stop(this)
                return
            }
        }

        // §714 cpe-push-toggle-1 — honour the user's "Push Reminders" preference locally
        // even if a token is still registered server-side (the toggle also (de)registers).
        val remindersOn = getSharedPreferences("nikhatglow_prefs", Context.MODE_PRIVATE)
            .getBoolean("push_reminders_enabled", true)
        if (!remindersOn) return
        val title = message.notification?.title ?: data["title"] ?: "Veda Drop"
        val body = message.notification?.body ?: data["body"] ?: ""

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data["type"]?.let { putExtra("notif_type", it) }
            data["booking_id"]?.let { putExtra("notif_booking_id", it) }
            data["complaint_id"]?.let { putExtra("notif_complaint_id", it) }
            data["offer_id"]?.let { putExtra("notif_offer_id", it) }
        }
        val pending = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, MainActivity.FCM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .build()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(System.currentTimeMillis().toInt(), notification)
    }
}
