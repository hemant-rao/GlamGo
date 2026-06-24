package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.remote.Mappers
import java.io.ByteArrayOutputStream

/**
 * §742 — shared service-image UI: a full-screen, pinch-to-zoom image viewer (used by
 * BOTH the customer service-detail gallery AND the partner service editor), plus a
 * partner-side multi-image editor (paste URL + upload from device). Images are either
 * http(s) URLs, relative /media URLs (resolved via [Mappers.absUrl]), or base64 `data:`
 * URLs (a device upload; Coil 2.x has no `data:` fetcher, so those are decoded to a
 * Bitmap — same approach as [SelfieProofImage]).
 */

private const val MAX_SERVICE_IMAGES = 8

/** Decode a (possibly base64 data:) url to a Bitmap, or null. */
@Composable
private fun rememberImageBitmap(dataUrl: String) = remember(dataUrl) {
    runCatching {
        val b64 = dataUrl.substringAfter(",", "")
        val bytes = Base64.decode(b64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}

/** A single image that can be pinch-zoomed + panned; double-tap toggles zoom. */
@Composable
private fun ZoomableImage(url: String, modifier: Modifier = Modifier) {
    var scale by remember(url) { mutableStateOf(1f) }
    var offset by remember(url) { mutableStateOf(Offset.Zero) }
    val resolved = Mappers.absUrl(url)

    val gestureMod = Modifier
        .fillMaxSize()
        .pointerInput(url) {
            detectTransformGestures { _, pan, zoom, _ ->
                scale = (scale * zoom).coerceIn(1f, 5f)
                offset = if (scale > 1f) offset + pan else Offset.Zero
            }
        }
        .pointerInput(url) {
            detectTapGestures(onDoubleTap = {
                if (scale > 1f) { scale = 1f; offset = Offset.Zero } else scale = 2.5f
            })
        }
    val layerMod = Modifier
        .fillMaxSize()
        .graphicsLayer(
            scaleX = scale, scaleY = scale,
            translationX = offset.x, translationY = offset.y,
        )

    Box(modifier = modifier.clipToBounds().then(gestureMod), contentAlignment = Alignment.Center) {
        if (resolved.startsWith("data:")) {
            val bmp = rememberImageBitmap(resolved)
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = layerMod,
                )
            }
        } else {
            AsyncImage(
                model = resolved,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = layerMod,
            )
        }
    }
}

/**
 * §742 — full-screen image viewer over a black backdrop. Steps through [images] with
 * arrows + an "n / N" counter when there is more than one, with a close button. Each
 * image is independently zoomable.
 */
@Composable
fun FullscreenImageViewer(images: List<String>, startIndex: Int = 0, onDismiss: () -> Unit) {
    if (images.isEmpty()) { onDismiss(); return }
    var index by remember { mutableStateOf(startIndex.coerceIn(0, images.size - 1)) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            ZoomableImage(images[index], modifier = Modifier.fillMaxSize())

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            ) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White) }

            if (images.size > 1) {
                IconButton(
                    onClick = { index = (index - 1 + images.size) % images.size },
                    modifier = Modifier.align(Alignment.CenterStart).padding(8.dp),
                ) { Icon(Icons.Default.ChevronLeft, contentDescription = "Previous", tint = Color.White) }
                IconButton(
                    onClick = { index = (index + 1) % images.size },
                    modifier = Modifier.align(Alignment.CenterEnd).padding(8.dp),
                ) { Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = Color.White) }
                Text(
                    text = "${index + 1} / ${images.size}",
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp),
                )
            }
        }
    }
}

/**
 * §742 — a horizontally-scrolling row of tappable image thumbnails. Tapping any opens
 * the [FullscreenImageViewer]. Renders relative/http urls via Coil and base64 `data:`
 * urls via a decoded Bitmap (reuses [SelfieProofImage]).
 */
@Composable
fun ServiceImageThumbRow(
    images: List<String>,
    modifier: Modifier = Modifier,
    thumbSize: Int = 120,
) {
    if (images.isEmpty()) return
    var viewerIndex by remember { mutableStateOf<Int?>(null) }
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        images.forEachIndexed { i, url ->
            SelfieProofImage(
                url = Mappers.absUrl(url),
                contentDescription = "Service photo",
                modifier = Modifier
                    .size(thumbSize.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { viewerIndex = i },
            )
        }
    }
    viewerIndex?.let { start ->
        FullscreenImageViewer(images = images, startIndex = start) { viewerIndex = null }
    }
}

/** Convert a picked gallery image Uri → a scaled base64 JPEG `data:` url the backend
 *  stores as-is (durable, no file storage). Mirrors the KYC selfie encode path. */
fun uriToJpegDataUrl(context: Context, uri: Uri, maxSide: Int = 1024, quality: Int = 70): String? =
    runCatching {
        val bmp = context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) }
            ?: return null
        val longest = maxOf(bmp.width, bmp.height)
        val scaled = if (longest > maxSide) {
            val ratio = maxSide.toFloat() / longest.toFloat()
            Bitmap.createScaledBitmap(
                bmp,
                (bmp.width * ratio).toInt().coerceAtLeast(1),
                (bmp.height * ratio).toInt().coerceAtLeast(1),
                true,
            )
        } else bmp
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }.getOrNull()

/**
 * §742 — partner-side editor for a service's photos: a thumbnail strip (tap to view
 * full-screen, with a remove badge), a "paste image URL" field, and an "Upload from
 * device" button that encodes the picked photo to a base64 `data:` url. Capped at
 * [MAX_SERVICE_IMAGES]. Emits the updated list via [onChange]; the caller sends it on
 * save (which re-enters admin approval).
 */
@Composable
fun ServiceImagesEditor(
    images: List<String>,
    onChange: (List<String>) -> Unit,
    accent: Color,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var urlInput by remember { mutableStateOf("") }
    var viewerIndex by remember { mutableStateOf<Int?>(null) }
    val full = images.size >= MAX_SERVICE_IMAGES

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null && images.size < MAX_SERVICE_IMAGES) {
            uriToJpegDataUrl(context, uri)?.let { onChange((images + it).take(MAX_SERVICE_IMAGES)) }
        }
    }

    Column {
        Text(
            "Service photos (${images.size}/$MAX_SERVICE_IMAGES)",
            fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold,
        )
        if (images.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                images.forEachIndexed { i, url ->
                    Box {
                        SelfieProofImage(
                            url = Mappers.absUrl(url),
                            contentDescription = "Service photo",
                            modifier = Modifier
                                .size(84.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewerIndex = i },
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .clickable { onChange(images.filterIndexed { idx, _ -> idx != i }) },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Default.Close, "Remove", tint = Color.White, modifier = Modifier.size(13.dp)) }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                placeholder = { Text("Paste image URL") },
                singleLine = true,
                enabled = !full,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    val u = urlInput.trim()
                    if (u.isNotEmpty() && !full) { onChange((images + u).take(MAX_SERVICE_IMAGES)); urlInput = "" }
                },
                enabled = urlInput.isNotBlank() && !full,
                colors = ButtonDefaults.buttonColors(containerColor = accent),
            ) { Icon(Icons.Default.Add, contentDescription = "Add URL", tint = Color.White) }
        }
        OutlinedButton(
            onClick = { if (!full) picker.launch("image/*") },
            enabled = !full,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        ) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null)
            Text("  Upload from device")
        }
        if (full) {
            Text(
                "Maximum $MAX_SERVICE_IMAGES photos.",
                fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp),
            )
        }
    }

    viewerIndex?.let { start ->
        FullscreenImageViewer(images = images, startIndex = start) { viewerIndex = null }
    }
}
