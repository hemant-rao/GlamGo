@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.DeepPlum
import com.example.ui.theme.VedaDropGold
import com.example.ui.theme.VedaDropRose
import com.example.ui.theme.vedaTextSecondary
import com.example.ui.theme.LightSage
import com.example.ui.theme.SoftCream
import com.example.ui.theme.LocalVedaDropPalette
import androidx.compose.ui.platform.LocalContext

/**
 * Partner ₹99/month listing subscription — the connector model's only revenue.
 * Shows trial/active status + days left, lets the partner subscribe / cancel,
 * and lists past payments. All data is server-backed (no hardcoded values).
 */
@Composable
fun PartnerSubscriptionScreen(viewModel: VedaDropViewModel) {
    val sub by viewModel.subscription.collectAsState()
    val payments by viewModel.subscriptionPayments.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadSubscription() }

    // §746 — when the VM has created a Razorpay order, open the Checkout sheet. It
    // needs the host Activity (MainActivity implements the PaymentResult callbacks
    // that route the outcome back to the VM for server-side verification).
    val context = LocalContext.current
    val pendingCheckout by viewModel.razorpayOpen.collectAsState()
    LaunchedEffect(pendingCheckout) {
        val c = pendingCheckout ?: return@LaunchedEffect
        val activity = context.findActivity()
        if (activity == null) {
            viewModel.onRazorpayResult(null, null, null, "Couldn't open the payment screen.")
            viewModel.consumeRazorpayOpen()
            return@LaunchedEffect
        }
        try {
            val checkout = com.razorpay.Checkout()
            checkout.setKeyID(c.keyId)
            val options = org.json.JSONObject().apply {
                put("name", c.name)
                put("description", c.description)
                put("currency", c.currency)
                put("order_id", c.orderId)
                put("amount", c.amountPaise)
                c.prefill?.let { pf ->
                    put("prefill", org.json.JSONObject().apply { pf.forEach { (k, v) -> put(k, v) } })
                }
                put("theme", org.json.JSONObject().apply { put("color", "#C2185B") })
            }
            checkout.open(activity, options)
        } catch (e: Exception) {
            viewModel.onRazorpayResult(null, null, null, "Couldn't open payment: ${e.message}")
        } finally {
            viewModel.consumeRazorpayOpen()
        }
    }

    val status = sub?.status ?: "none"
    val isActive = sub?.isActive == true
    val priceRupees = (sub?.pricePaise ?: 9900L) / 100

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // §740 — the app shell already shows "Premium Membership" + a back arrow, so the
        // inner back arrow + "Subscription" title were removed (no more double header).
        // This is now a compact price banner that follows the light/dark theme.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (LocalVedaDropPalette.current.isDark)
                        Brush.verticalGradient(listOf(DeepPlum, DarkSlate))
                    else Brush.verticalGradient(listOf(LightSage, SoftCream))
                )
                .padding(16.dp)
        ) {
            Column {
                Text("₹$priceRupees / month", color = VedaDropRose, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Stay discoverable and accept booking requests. You collect payment directly from customers — the platform never takes a cut.",
                    color = vedaTextSecondary, fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Status card
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Status", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        AssistChip(
                            onClick = {},
                            label = { Text(status.replaceFirstChar { it.uppercase() }) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isActive) VedaDropRose.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)
                            )
                        )
                    }
                    if ((sub?.daysLeft ?: 0) > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text("${sub?.daysLeft} day(s) remaining in the current period.", fontSize = 13.sp, color = vedaTextSecondary)
                    }
                    sub?.currentPeriodEnd?.let {
                        Spacer(Modifier.height(4.dp))
                        Text("Renews / ends: ${it.take(10)}", fontSize = 12.sp, color = vedaTextSecondary)
                    }
                    if (status == "trial") {
                        Spacer(Modifier.height(4.dp))
                        Text("You're on a free trial. Subscribe to keep your listing active afterwards.", fontSize = 12.sp, color = VedaDropRose)
                    }
                }
            }

            viewModel.subscriptionError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            // Actions
            if (isActive && sub?.autoRenew == true) {
                OutlinedButton(
                    onClick = { viewModel.cancelSubscriptionNow() },
                    enabled = !viewModel.subscriptionBusy,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    border = BorderStroke(1.dp, VedaDropRose),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VedaDropRose),
                ) { Text(if (viewModel.subscriptionBusy) "Please wait…" else "Cancel auto-renew", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
            } else {
                Button(
                    onClick = { viewModel.subscribeNow() },
                    enabled = !viewModel.subscriptionBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) { Text(if (viewModel.subscriptionBusy) "Please wait…" else "Subscribe ₹$priceRupees/mo", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
            }

            // Payment history
            if (payments.isNotEmpty()) {
                Text("Payment history", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                payments.forEach { p ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VedaDropRose, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("₹${p.amountPaise / 100}", fontWeight = FontWeight.SemiBold)
                                Text((p.at ?: p.periodStart ?: "").take(10), fontSize = 12.sp, color = vedaTextSecondary)
                            }
                            Text(p.status.replaceFirstChar { it.uppercase() }, fontSize = 12.sp, color = VedaDropRose)
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

/** §746 — unwrap the host Activity from a Compose Context so Razorpay Checkout can
 *  launch (and route its result back to MainActivity's PaymentResult callbacks). */
private tailrec fun android.content.Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}
