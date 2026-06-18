package com.example.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * §687 — device location via Google Play Services FusedLocationProvider.
 *
 * Pre-§687 the app NEVER read real GPS: addresses were saved with a hardcoded
 * Bangalore coordinate and discovery never sent lat/lon, so "near me" silently
 * did nothing. This helper is the single source of a real device fix. It is
 * permission-aware (returns null when not granted) and never throws — callers
 * fall back to manual address entry / un-sorted discovery.
 */
object LocationHelper {

    fun hasPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Best-effort current location. Returns (lat, lon) or null if permission is
     * missing / location is off / the provider returns nothing. Uses a fresh
     * high-accuracy fix (falls back internally to last-known on the platform).
     */
    suspend fun current(context: Context): Pair<Double, Double>? {
        if (!hasPermission(context)) return null
        return try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val req = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .setMaxUpdateAgeMillis(60_000L)
                .build()
            @Suppress("MissingPermission")
            val loc: Location? = suspendCancellableCoroutine { cont ->
                client.getCurrentLocation(req, null)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            }
            loc?.let { it.latitude to it.longitude }
        } catch (_: Exception) {
            null
        }
    }
}
