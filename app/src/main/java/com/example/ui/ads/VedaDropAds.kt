package com.example.ui.ads

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.remote.AdsConfig
import com.example.ui.VedaDropViewModel
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * §756 — config-driven AdMob ads for Veda Drop.
 *
 * Everything here is gated on the role-trimmed `/config.ads` slice the backend serves
 * (admin master switch + per-role gate + per-placement config). When ads are off or the
 * role isn't allowed, the server sends `{enabled:false}` and these composables/helpers
 * render / do nothing. Ad-unit ids come from the admin; when none is set (or test mode
 * is on) we fall back to Google's official TEST units so the founder can verify the
 * wiring without risking policy strikes on real units.
 */

// Google's official sample/test ad unit ids — safe to ship; only ever serve test ads.
private const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"

private fun AdsConfig.placementEnabled(placement: String): Boolean {
    if (!enabled) return false
    val p = placements[placement] ?: return false
    return p.enabled
}

private fun AdsConfig.unitFor(format: String, testUnit: String): String {
    val real = adUnits[format].orEmpty().trim()
    // Use the configured unit only when it's set AND we're not in test mode; otherwise
    // serve Google's test unit so we never accidentally request a real ad too early.
    return if (real.isNotEmpty() && !testMode) real else testUnit
}

/**
 * A banner ad for a named placement (e.g. "home", "service_detail", "booking_list").
 * Renders nothing unless the admin has enabled ads for this role + placement. Safe to
 * drop into any Composable column — it sizes to a standard adaptive banner.
 */
@Composable
fun VedaDropBannerAd(
    placement: String,
    viewModel: VedaDropViewModel,
    modifier: Modifier = Modifier,
) {
    val cfg by viewModel.appConfig.collectAsState()
    val ads = cfg?.ads ?: return
    if (!ads.placementEnabled(placement)) return
    // Banners cover the "banner" + "native" formats here (a real native renderer would
    // need a template); interstitial placements use maybeShowInterstitial() instead.
    val fmt = ads.placements[placement]?.format ?: "banner"
    if (fmt == "interstitial") return

    val context = LocalContext.current
    val unitId = remember(ads, placement) { ads.unitFor("banner", TEST_BANNER) }

    AndroidView(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = unitId
                runCatching { loadAd(AdRequest.Builder().build()) }
            }
        },
    )
}

/**
 * §756 — interstitial pacing. The frequency caps ("interstitial every N actions",
 * "min interval", "max per session") live in the admin; this single per-process
 * controller enforces them so full-screen ads never spam the user. Call
 * [onAction] at natural break points (e.g. a completed booking) and it shows an
 * interstitial only when the config + caps allow it.
 */
object VedaDropInterstitials {
    private var actionCount = 0
    private var shownThisSession = 0
    private var lastShownAtMs = 0L
    private var loaded: InterstitialAd? = null
    private var loading = false

    fun onAction(activity: Activity, cfg: AdsConfig?, placement: String = "post_booking") {
        val ads = cfg ?: return
        if (!ads.placementEnabled(placement)) return
        if (ads.placements[placement]?.format != "interstitial") return
        val freq = ads.frequency
        actionCount += 1
        if (freq.maxPerSession in 1..shownThisSession) return
        if (freq.interstitialEveryNActions > 0 && actionCount % freq.interstitialEveryNActions != 0) {
            preload(activity, ads)  // warm up for the next eligible action
            return
        }
        val now = System.currentTimeMillis()
        if (freq.minIntervalSec > 0 && now - lastShownAtMs < freq.minIntervalSec * 1000L) return

        val ready = loaded
        if (ready != null) {
            loaded = null
            shownThisSession += 1
            lastShownAtMs = now
            runCatching { ready.show(activity) }
            preload(activity, ads)
        } else {
            preload(activity, ads)  // not ready yet — load for next time, show nothing now
        }
    }

    private fun preload(context: Context, ads: AdsConfig) {
        if (loading || loaded != null) return
        loading = true
        val unitId = ads.unitFor("interstitial", TEST_INTERSTITIAL)
        runCatching {
            InterstitialAd.load(
                context, unitId, AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) { loaded = ad; loading = false }
                    override fun onAdFailedToLoad(error: LoadAdError) { loaded = null; loading = false }
                },
            )
        }.onFailure { loading = false }
    }
}
