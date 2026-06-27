package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.VerificationCapabilityDto
import com.example.data.remote.VerificationDto
import com.example.data.remote.VerificationNextActionDto
import com.example.data.remote.VerificationStepDto
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.DeepPlum
import com.example.ui.theme.OrderOrange
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.VedaDropRose

// §759 — Verification Center.
//
// A role-aware status dashboard backed by GET auth/verification (also embedded at
// profile.verification). Lists each verification step with a status pill, each
// capability ("can book" / "can get listed" …) with its allowed/blocked hint, and a
// prominent next_action card whose CTA routes into the EXISTING flows (KYC,
// Subscription, Business Location). The booking gate (403 PHONE_VERIFICATION_REQUIRED)
// routes the user here from VedaDropViewModel.maybeHandlePhoneGate().

/** Blocking-red used for required/rejected/expired pills. Matches the app's destructive
 *  accent without depending on the role-flipped MaterialTheme error tone. */
private val VerifyDangerRed = Color(0xFFEF4444)

private data class StatusVisual(val color: Color, val icon: ImageVector, val label: String)

private fun statusVisual(status: String): StatusVisual = when (status.lowercase()) {
    "verified", "active" -> StatusVisual(SuccessGreen, Icons.Filled.CheckCircle, "Verified")
    "pending" -> StatusVisual(OrderOrange, Icons.Filled.HourglassEmpty, "In review")
    "optional" -> StatusVisual(Color.Gray, Icons.Filled.RadioButtonUnchecked, "Optional")
    "rejected" -> StatusVisual(VerifyDangerRed, Icons.Filled.ErrorOutline, "Rejected")
    "expired" -> StatusVisual(VerifyDangerRed, Icons.Filled.ErrorOutline, "Expired")
    "required" -> StatusVisual(VerifyDangerRed, Icons.Filled.ErrorOutline, "Action needed")
    else -> StatusVisual(Color.Gray, Icons.Filled.RadioButtonUnchecked,
        status.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
}

/** Map a verification step/next-action key to an EXISTING destination screen, when one
 *  exists. `phone`/`email` have no standalone re-verify screen in-app (accounts are
 *  created only after both are verified during §757 registration), so they return null
 *  and the CTA simply re-checks status.
 *
 *  TODO(§759): if an existing-account phone RE-verify is ever needed, it can reuse the
 *  §758 SMS-OTP composable + Repository.registerPhoneRequestSms/registerPhoneVerify
 *  rungs — no new flow required here. */
private fun routeForStep(key: String): Screen? = when (key) {
    "kyc" -> Screen.PartnerKyc
    "subscription" -> Screen.PartnerSubscription
    "location" -> Screen.PartnerBusinessLocation
    else -> null   // phone / email — no standalone in-app re-verify destination
}

@Composable
fun VerificationCenterScreen(viewModel: VedaDropViewModel) {
    val verification by viewModel.verification.collectAsState()

    // Refresh on entry so KYC/subscription/location changes show without a re-login.
    LaunchedEffect(Unit) { viewModel.loadVerification() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header band.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(DeepPlum, DarkSlate)))
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.VerifiedUser, contentDescription = null,
                    tint = VedaDropRose, modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Verification Center",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White, fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Your account status & what unlocks next",
                        fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        val v = verification
        if (v == null) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(48.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = VedaDropRose) }
            return@Column
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Prominent next-action card (only when the backend says there's one).
            v.nextAction?.let { NextActionCard(it, viewModel) }

            // Steps.
            if (v.steps.isNotEmpty()) {
                Text(
                    "VERIFICATION STEPS",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold
                )
                v.steps.forEach { StepRow(it, viewModel) }
            }

            // Capabilities.
            if (v.capabilities.isNotEmpty()) {
                Text(
                    "WHAT YOU CAN DO",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                v.capabilities.forEach { CapabilityRow(it) }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NextActionCard(action: VerificationNextActionDto, viewModel: VedaDropViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("verification_next_action"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = VedaDropRose.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, VedaDropRose.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                action.title.ifBlank { "Next step" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
            )
            action.subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            }
            val dest = routeForStep(action.key)
            Button(
                onClick = {
                    if (dest != null) viewModel.currentScreen = dest
                    else viewModel.loadVerification()   // phone/email — re-check status
                },
                colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                modifier = Modifier.fillMaxWidth().testTag("verification_next_action_cta")
            ) {
                Text(
                    action.cta?.takeIf { it.isNotBlank() } ?: "Continue",
                    color = Color.Black, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false
                )
            }
        }
    }
}

@Composable
private fun StepRow(step: VerificationStepDto, viewModel: VedaDropViewModel) {
    val visual = statusVisual(step.status)
    val dest = routeForStep(step.key)
    val isDone = step.status.equals("verified", true) || step.status.equals("active", true)
    // Only blocking / actionable steps with a real destination are tappable.
    val tappable = dest != null && !isDone

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (tappable) Modifier.clickable { viewModel.currentScreen = dest!! } else Modifier)
            .testTag("verification_step_${step.key}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(visual.icon, contentDescription = null, tint = visual.color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        step.label.ifBlank { step.key },
                        fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (step.critical) {
                        Spacer(Modifier.width(6.dp))
                        Surface(color = VerifyDangerRed.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                "Required", color = VerifyDangerRed, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                step.help?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(it, fontSize = 11.sp, color = Color.Gray, lineHeight = 14.sp)
                }
            }
            Spacer(Modifier.width(8.dp))
            StatusPill(visual)
            if (tappable) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open",
                    tint = VedaDropRose, modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusPill(visual: StatusVisual) {
    Surface(color = visual.color.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(visual.icon, contentDescription = null, tint = visual.color, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(visual.label, color = visual.color, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
        }
    }
}

@Composable
private fun CapabilityRow(cap: VerificationCapabilityDto) {
    val color = if (cap.allowed) SuccessGreen else VerifyDangerRed
    val icon = if (cap.allowed) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                cap.label.ifBlank { cap.key },
                fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            // When blocked, the backend hint explains why; otherwise it's a confirmation.
            cap.hint?.takeIf { it.isNotBlank() }?.let {
                Text(it, fontSize = 11.sp, color = Color.Gray, lineHeight = 14.sp)
            }
        }
    }
}

/** Compact entry point shown on the customer profile + partner dashboard. Loads the
 *  snapshot lazily and shows a one-line status summary, routing into the full screen. */
@Composable
fun VerificationEntryCard(viewModel: VedaDropViewModel) {
    val verification by viewModel.verification.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadVerification() }

    val (summary, summaryColor) = summarise(verification)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.currentScreen = Screen.VerificationCenter }
            .testTag("verification_entry_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(summaryColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = summaryColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Verification Center", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(summary, fontSize = 11.sp, color = summaryColor, fontWeight = FontWeight.SemiBold)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
                tint = VedaDropRose, modifier = Modifier.size(18.dp))
        }
    }
}

/** One-line summary for the entry card. Uses next_action presence as the "needs work"
 *  signal (mirrors the backend's own intent). */
private fun summarise(v: VerificationDto?): Pair<String, Color> = when {
    v == null -> "Tap to review your account status" to VedaDropRose
    v.nextAction != null -> (v.nextAction.title.ifBlank { "Action needed to unlock features" }) to OrderOrange
    else -> "All verified" to SuccessGreen
}
