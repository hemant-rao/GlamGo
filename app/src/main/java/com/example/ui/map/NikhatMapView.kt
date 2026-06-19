package com.example.ui.map

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import okhttp3.OkHttpClient
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.module.http.HttpRequestUtil
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/**
 * §690 — Nikhat Glow live tracking map. MapLibre GL engine + Ola vector tiles
 * (ported from the Solaris-Gemini OlaMapView). Renders the customer + partner as
 * markers and the route between them — MUTUAL live tracking. Uses NO Ola .aar SDK;
 * tiles render with the restricted tile key handed back by the geo gateway
 * (/api/geo/app-config), so the secret REST key never reaches the device.
 */

data class GeoPoint(val latitude: Double, val longitude: Double)

object NikhatMaps {
    const val DEFAULT_TILE_BASE = "https://api.olamaps.io"

    fun styleUrl(tileKey: String, dark: Boolean, tileBase: String = DEFAULT_TILE_BASE): String {
        val base = tileBase.trim().ifBlank { DEFAULT_TILE_BASE }.trimEnd('/')
        val style = if (dark) "default-dark-standard" else "default-light-standard"
        return "$base/tiles/vector/v1/styles/$style/style.json?api_key=$tileKey"
    }

    /** Decode a Google/Ola precision-5 encoded polyline into points. */
    fun decodePolyline(encoded: String): List<GeoPoint> {
        val poly = ArrayList<GeoPoint>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            poly.add(GeoPoint(lat / 1E5, lng / 1E5))
        }
        return poly
    }
}

private const val SRC_ROUTE = "ng-route-src"
private const val LYR_ROUTE = "ng-route-lyr"
private const val SRC_PARTNER = "ng-partner-src"
private const val LYR_PARTNER = "ng-partner-lyr"
private const val SRC_CUSTOMER = "ng-customer-src"
private const val LYR_CUSTOMER_HALO = "ng-customer-halo-lyr"
private const val LYR_CUSTOMER = "ng-customer-lyr"

private const val COLOR_PARTNER = "#C0334F"   // Crimson (partner)
private const val COLOR_CUSTOMER = "#3B82F6"  // blue (you)
private const val COLOR_ROUTE = "#C0334F"

@Volatile private var httpConfigured = false

private fun ensureOlaHttp(tileKey: String) {
    if (httpConfigured) return
    val client = OkHttpClient.Builder().addInterceptor { chain ->
        val req = chain.request()
        val url = req.url
        if (url.host.contains("olamaps.io") && url.queryParameter("api_key") == null) {
            val newUrl = url.newBuilder().addQueryParameter("api_key", tileKey).build()
            chain.proceed(req.newBuilder().url(newUrl).build())
        } else {
            chain.proceed(req)
        }
    }.build()
    HttpRequestUtil.setOkHttpClient(client)
    httpConfigured = true
}

@SuppressLint("MissingPermission")
@Composable
fun NikhatMapView(
    tileKey: String,
    modifier: Modifier = Modifier,
    tileBaseUrl: String = NikhatMaps.DEFAULT_TILE_BASE,
    customer: GeoPoint? = null,
    partner: GeoPoint? = null,
    route: List<GeoPoint>? = null,
    isDark: Boolean = isSystemInDarkTheme(),
    followCurrent: Boolean = false,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val mapView = remember {
        ensureOlaHttp(tileKey)
        MapLibre.getInstance(context.applicationContext)
        val options = MapLibreMapOptions()
        MapView(context, options)
    }

    DisposableEffect(lifecycleOwner) {
        mapView.onCreate(null)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    val mapRef = remember { mutableStateOf<MapLibreMap?>(null) }
    val styleRef = remember { mutableStateOf<Style?>(null) }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { mv ->
            mv.getMapAsync { map ->
                mapRef.value = map
                map.setStyle(Style.Builder().fromUri(NikhatMaps.styleUrl(tileKey, isDark, tileBaseUrl))) { style ->
                    styleRef.value = style
                    initLayers(style)
                    applyData(map, style, customer, partner, route, followCurrent, firstFit = true)
                }
            }
            mv
        },
        update = { _ ->
            val map = mapRef.value
            val style = styleRef.value
            if (map != null && style != null && style.isFullyLoaded) {
                applyData(map, style, customer, partner, route, followCurrent, firstFit = false)
            }
        },
    )
}

private fun initLayers(style: Style) {
    if (style.getSource(SRC_ROUTE) != null) return
    style.addSource(GeoJsonSource(SRC_ROUTE))
    style.addSource(GeoJsonSource(SRC_PARTNER))
    style.addSource(GeoJsonSource(SRC_CUSTOMER))

    style.addLayer(
        LineLayer(LYR_ROUTE, SRC_ROUTE).withProperties(
            PropertyFactory.lineColor(COLOR_ROUTE),
            PropertyFactory.lineWidth(5f),
            PropertyFactory.lineOpacity(0.85f),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
        )
    )
    style.addLayer(
        CircleLayer(LYR_PARTNER, SRC_PARTNER).withProperties(
            PropertyFactory.circleColor(COLOR_PARTNER),
            PropertyFactory.circleRadius(8f),
            PropertyFactory.circleStrokeColor("#FFFFFF"),
            PropertyFactory.circleStrokeWidth(2f),
        )
    )
    style.addLayer(
        CircleLayer(LYR_CUSTOMER_HALO, SRC_CUSTOMER).withProperties(
            PropertyFactory.circleColor(COLOR_CUSTOMER),
            PropertyFactory.circleRadius(16f),
            PropertyFactory.circleOpacity(0.18f),
        )
    )
    style.addLayer(
        CircleLayer(LYR_CUSTOMER, SRC_CUSTOMER).withProperties(
            PropertyFactory.circleColor(COLOR_CUSTOMER),
            PropertyFactory.circleRadius(6f),
            PropertyFactory.circleStrokeColor("#FFFFFF"),
            PropertyFactory.circleStrokeWidth(2f),
        )
    )
}

private fun applyData(
    map: MapLibreMap,
    style: Style,
    customer: GeoPoint?,
    partner: GeoPoint?,
    route: List<GeoPoint>?,
    followCurrent: Boolean,
    firstFit: Boolean,
) {
    (style.getSource(SRC_PARTNER) as? GeoJsonSource)?.setGeoJson(pointFeature(partner))
    (style.getSource(SRC_CUSTOMER) as? GeoJsonSource)?.setGeoJson(pointFeature(customer))
    (style.getSource(SRC_ROUTE) as? GeoJsonSource)?.setGeoJson(lineFeature(route))

    val all = listOfNotNull(customer, partner) + (route ?: emptyList())
    when {
        followCurrent && customer != null ->
            map.animateCamera(CameraUpdateFactory.newLatLng(LatLng(customer.latitude, customer.longitude)))
        firstFit && all.size >= 2 -> {
            val bounds = LatLngBounds.Builder().apply {
                all.forEach { include(LatLng(it.latitude, it.longitude)) }
            }.build()
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
        }
        firstFit && all.size == 1 -> {
            val p = all.first()
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(p.latitude, p.longitude), 13.0))
        }
    }
}

private fun pointFeature(p: GeoPoint?): FeatureCollection =
    if (p == null) FeatureCollection.fromFeatures(emptyArray<Feature>())
    else FeatureCollection.fromFeature(Feature.fromGeometry(Point.fromLngLat(p.longitude, p.latitude)))

private fun lineFeature(pts: List<GeoPoint>?): FeatureCollection {
    if (pts.isNullOrEmpty()) return FeatureCollection.fromFeatures(emptyArray<Feature>())
    val line = LineString.fromLngLats(pts.map { Point.fromLngLat(it.longitude, it.latitude) })
    return FeatureCollection.fromFeature(Feature.fromGeometry(line))
}
