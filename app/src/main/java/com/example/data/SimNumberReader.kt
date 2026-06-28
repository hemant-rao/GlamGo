package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

/**
 * §774 — silent "is the SIM in THIS phone?" reader.
 *
 * The first-time phone-verification proof we trust most is: the number being
 * registered is the SIM physically in this device. §770 did that with Google's
 * Phone Number Hint (a one-tap chooser, no permission) — but on many Indian
 * carriers the number isn't exposed to Hint, so the chooser comes up empty and
 * the user is stranded on "skip / verify later".
 *
 * This reader closes that gap: with the READ_PHONE_NUMBERS permission we read the
 * device's OWN active SIM number(s) directly and silently — no tap, no chooser —
 * so when a SIM matches the typed number we can auto-verify with zero interaction.
 * It's the same "verify the user's own number" use-case the permission is meant
 * for (and Play approves). Everything here degrades gracefully: missing
 * permission, a carrier that hides the MSISDN, or any SecurityException simply
 * yields an empty list, and the caller falls back to the Hint chooser, then to
 * "verify later". It never throws.
 */
object SimNumberReader {

    /** Permissions we request. READ_PHONE_NUMBERS reads the SIM's own number;
     *  READ_PHONE_STATE is what lets us enumerate every active SIM (dual-SIM). */
    val PERMISSIONS = arrayOf(
        Manifest.permission.READ_PHONE_NUMBERS,
        Manifest.permission.READ_PHONE_STATE,
    )

    /** True once we can at least attempt a silent read (the number permission is
     *  granted). We don't hard-require READ_PHONE_STATE — the TelephonyManager
     *  fallback below still works for the default SIM without it. */
    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_PHONE_NUMBERS
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Every active SIM number we can read, as raw device strings (often E.164,
     * e.g. "+9198…"). The server's SIM rung normalises and matches against the
     * number being registered, so we don't normalise here. Returns an empty list
     * when nothing is readable — never throws.
     */
    @SuppressLint("MissingPermission", "HardwareIds")
    fun readSimNumbers(context: Context): List<String> {
        val out = LinkedHashSet<String>()

        // Primary: enumerate active subscriptions (covers dual-SIM). Needs
        // READ_PHONE_STATE to list subs + READ_PHONE_NUMBERS to read each number.
        runCatching {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                    as? SubscriptionManager
            val subs = sm?.activeSubscriptionInfoList ?: emptyList()
            for (info in subs) {
                val n = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    runCatching { sm?.getPhoneNumber(info.subscriptionId) }.getOrNull()
                } else {
                    @Suppress("DEPRECATION") info.number
                }
                if (!n.isNullOrBlank()) out.add(n.trim())
            }
        }

        // Fallback: the default line number (single-SIM / older devices where the
        // SubscriptionManager path returned nothing).
        runCatching {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            @Suppress("DEPRECATION", "MissingPermission")
            val n = tm?.line1Number
            if (!n.isNullOrBlank()) out.add(n.trim())
        }

        return out.toList()
    }
}
