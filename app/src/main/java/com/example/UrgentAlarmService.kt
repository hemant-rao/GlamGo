package com.example

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * §725 Batch-B (founder: "alarm jaisi ringtone baje jab tak partner view/accept na kare").
 *
 * A foreground service that loops an ALARM-stream sound and posts a full-screen-intent
 * notification (so the screen lights up even when locked) the moment an URGENT job
 * offer arrives. It keeps ringing until the partner VIEWS the Rescue Board, claims a
 * job, or the booking is cleared/taken (urgent_cleared push / poll).
 *
 * Driven from two places:
 *   - [VedaDropMessagingService] on a data-only FCM push ({"type":"urgent_offer"}).
 *   - the foreground 20s offers poll in the ViewModel (works even with FCM cred-blocked).
 *
 * Stopped via [stop] (called from the Rescue Board composable on display, on accept /
 * claim, and on an urgent_cleared push) or simply by stopping the service.
 */
class UrgentAlarmService : Service() {

    companion object {
        const val ACTION_START = "com.example.action.URGENT_ALARM_START"
        const val ACTION_STOP = "com.example.action.URGENT_ALARM_STOP"

        /** A stable id so repeated starts replace (not stack) the foreground notification. */
        private const val URGENT_NOTIFICATION_ID = 920_725

        /** Track running state cheaply so callers can avoid redundant start/stop intents. */
        @Volatile
        var isRinging: Boolean = false
            private set

        /** Start (or refresh) the looping alarm + full-screen notification. */
        fun start(context: Context) {
            val intent = Intent(context, UrgentAlarmService::class.java).apply { action = ACTION_START }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /** Stop the alarm + dismiss the notification + tear down the service. */
        fun stop(context: Context) {
            // If it isn't running, sending ACTION_STOP would (on O+) start the service
            // just to stop it — skip that needless churn.
            if (!isRinging) return
            val intent = Intent(context, UrgentAlarmService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
        }
    }

    private var player: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopAlarm()
                stopForegroundCompat()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                // ACTION_START (or a system restart). Post the foreground notification
                // FIRST (must happen within ~5s of startForegroundService) then ring.
                // On Android 12+ a background FGS start can throw
                // ForegroundServiceStartNotAllowedException if no exemption applies; we
                // still want the alarm + a (non-FGS) high-importance notification, so
                // fall back gracefully instead of crashing.
                val notification = buildNotification()
                val startedForeground = runCatching {
                    startForeground(URGENT_NOTIFICATION_ID, notification)
                }.isSuccess
                if (!startedForeground) {
                    runCatching {
                        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                            .notify(URGENT_NOTIFICATION_ID, notification)
                    }
                }
                startAlarm()
                isRinging = true
            }
        }
        // Do NOT auto-restart with a null intent — we don't want a phantom alarm after
        // the system kills + recreates the service post-dismissal.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }

    // ── Alarm sound (looping, ALARM stream so it overrides media/ring volume) ──────
    private fun startAlarm() {
        if (player != null || fallbackRingtone != null) return
        val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return
        val mp = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            isLooping = true
        }
        val ok = runCatching {
            mp.setDataSource(this@UrgentAlarmService, alarmUri)
            mp.setOnPreparedListener { it.start() }
            mp.prepareAsync()
        }.isSuccess
        if (ok) {
            player = mp
        } else {
            // MediaPlayer set-up failed → release it and fall back to a RingtoneManager
            // TYPE_ALARM stream so the partner still hears the alert.
            runCatching { mp.release() }
            player = null
            playFallbackRingtone()
        }
    }

    private var fallbackRingtone: android.media.Ringtone? = null

    private fun playFallbackRingtone() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) ?: return
        fallbackRingtone = RingtoneManager.getRingtone(applicationContext, uri)?.apply {
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isLooping = true
            runCatching { play() }
        }
    }

    private fun stopAlarm() {
        isRinging = false
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        runCatching { fallbackRingtone?.stop() }
        fallbackRingtone = null
    }

    @Suppress("DEPRECATION")
    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
        // Belt-and-suspenders: explicitly drop the notification.
        (getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
            ?.cancel(URGENT_NOTIFICATION_ID)
    }

    // ── Full-screen notification that opens the Rescue Board (Open Jobs) ───────────
    private fun buildNotification(): Notification {
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("notif_type", "urgent_offer")
            putExtra("open_urgent_offers", true)
        }
        val pending = PendingIntent.getActivity(
            this, 1, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, MainActivity.URGENT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Urgent job — respond now")
            .setContentText("A customer needs a professional right away. Tap to view and claim.")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pending)
            // Full-screen intent — lights the screen / heads-up even when locked.
            .setFullScreenIntent(pending, true)
            .build()
    }
}
