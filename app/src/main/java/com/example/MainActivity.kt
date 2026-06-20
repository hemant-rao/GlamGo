package com.example

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.example.data.LocationHelper
import com.example.ui.NikhatGlowViewModel
import com.example.ui.NikhatGlowMainShell
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  companion object {
    /** FCM notification channel id. Any code posting a push notification (e.g. a
     *  FirebaseMessagingService.onMessageReceived) MUST target this same id, and
     *  the channel must exist before the first notification is posted on API 26+. */
    const val FCM_CHANNEL_ID = "nikhatglow_default"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Create the default notification channel up-front. On Android 8+ (the app's
    // minSdk is 26) a channel must exist before any notification can be shown,
    // otherwise FCM-delivered notifications are silently dropped.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      if (manager.getNotificationChannel(FCM_CHANNEL_ID) == null) {
        manager.createNotificationChannel(
          NotificationChannel(
            FCM_CHANNEL_ID,
            "Nikhat Glow",
            NotificationManager.IMPORTANCE_HIGH
          ).apply {
            description = "Booking updates, chat messages and reminders."
          }
        )
      }
    }
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        androidx.compose.material3.Surface(
          modifier = Modifier.fillMaxSize(),
          color = androidx.compose.material3.MaterialTheme.colorScheme.background
        ) {
          val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<NikhatGlowViewModel>()
          // §687 — request location once on launch; on grant (or if already
          // granted) capture the device fix so "near me" discovery engages. The
          // app works fine if the user denies — discovery just isn't distance-sorted
          // and addresses fall back to manual entry.
          val permLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
          ) { granted ->
            if (granted.values.any { it }) viewModel.captureDeviceLocation()
          }
          LaunchedEffect(Unit) {
            // §703 — pull the admin-controlled app config on launch so feature
            // gates + role-based nav + policy copy reflect the server immediately.
            viewModel.loadAppConfig()
            // §710 P0-5 — register this device's FCM token (no-op if logged out) +
            // deep-link a cold-start that came from tapping a push.
            viewModel.registerFcmToken()
            intent?.getStringExtra("notif_booking_id")?.let { viewModel.openBookingFromPush(it) }
            if (LocationHelper.hasPermission(this@MainActivity)) {
              viewModel.captureDeviceLocation()
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
              }
            } else {
              val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.POST_NOTIFICATIONS)
              else
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
              permLauncher.launch(perms)
            }
          }
          NikhatGlowMainShell(viewModel = viewModel)
        }
      }
    }
  }
}
