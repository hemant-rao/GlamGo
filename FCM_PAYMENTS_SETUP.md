# Nikhat Glow — FCM + Razorpay activation (§710 P0-5 / P0-3)

Two launch-blockers need a one-time setup in your build environment. The backend is
already done and tested; the app code below either ships in this commit (FCM) or is
ready to paste (Razorpay UI). **Both need a Gradle sync + a run on a device/emulator
to verify — there is no Android toolchain on the machine that wrote this.**

---

## 1. FCM push (P0-5) — code already in this commit; you add the Firebase project files

Shipped in this commit: `NikhatGlowMessagingService` (receives + shows pushes, deep-links
to the booking), `firebase-messaging` dependency, manifest service + `POST_NOTIFICATIONS`,
token register-on-login / unregister-on-logout, and a cold-start push-tap → booking route.

**You add (Firebase Console → your project):**

1. Download **`google-services.json`** for the app's package and drop it in `app/`.
2. Add the Google Services Gradle plugin:
   - In the **root** `build.gradle.kts` (or `settings.gradle.kts` plugins block / version catalog):
     ```kotlin
     plugins {
       id("com.google.gms.google-services") version "4.4.2" apply false
     }
     ```
   - In **`app/build.gradle.kts`** `plugins { … }` add:
     ```kotlin
     id("com.google.gms.google-services")
     ```
3. Backend: set `NIKHATGLOW_FCM_ENABLED=1` and provide the FCM v1 service-account creds the
   backend's `notify()` already uses (`GLAMGO_FCM_*` / `NIKHATGLOW_FCM_*`).
4. Sync + run. On login the app registers its token; send a test push from the console or
   trigger a booking update — it should appear and, when tapped, open the booking.

Until `google-services.json` + the plugin are added, the app still compiles (the messaging
classes resolve from the dependency); `getToken()` is wrapped so it can't crash auth — push
just won't flow.

---

## 2. Razorpay ₹99 subscription (P0-3) — backend done; paste this app code

Backend (already shipped + tested): `POST /partner/subscription/checkout` (creates an order),
`POST /partner/subscription/verify` (verifies the signature, idempotent), and the public
`POST /webhooks/razorpay`. Activate the backend by setting env:
```
NIKHATGLOW_PAYMENTS_PROVIDER=razorpay
NIKHATGLOW_RAZORPAY_KEY_ID=rzp_live_xxxx
NIKHATGLOW_RAZORPAY_KEY_SECRET=xxxx
NIKHATGLOW_RAZORPAY_WEBHOOK_SECRET=xxxx     # also set on the Razorpay dashboard webhook
```
Dashboard webhook: add `…/api/nikhatglow/v1/webhooks/razorpay`, event **payment.captured**
(and optionally **order.paid**), with the same webhook secret.

### 2a. Dependency
`gradle/libs.versions.toml`:
```toml
razorpay-checkout = { group = "com.razorpay", name = "checkout", version = "1.6.40" }
```
`app/build.gradle.kts` dependencies:
```kotlin
implementation(libs.razorpay.checkout)
```

### 2b. DTOs (`data/remote/Dtos.kt`)
```kotlin
@JsonClass(generateAdapter = true)
data class CheckoutOrderResp(
    @Json(name = "order_id") val orderId: String,
    @Json(name = "amount_paise") val amountPaise: Long = 0,
    val currency: String = "INR",
    @Json(name = "key_id") val keyId: String,
    val name: String = "Nikhat Glow",
    val description: String = "₹99 monthly listing",
)

@JsonClass(generateAdapter = true)
data class VerifyPaymentReq(
    @Json(name = "razorpay_order_id") val razorpayOrderId: String,
    @Json(name = "razorpay_payment_id") val razorpayPaymentId: String,
    @Json(name = "razorpay_signature") val razorpaySignature: String,
)
```

### 2c. API (`data/remote/NikhatGlowApi.kt`)
```kotlin
@POST("partner/subscription/checkout")
suspend fun subscriptionCheckout(): CheckoutOrderResp

@POST("partner/subscription/verify")
suspend fun subscriptionVerify(@Body body: VerifyPaymentReq): SubscriptionDto   // reuse your sub DTO
```

### 2d. Repository
```kotlin
suspend fun subscriptionCheckout(): CheckoutOrderResp = api.subscriptionCheckout()
suspend fun subscriptionVerify(orderId: String, paymentId: String, signature: String) {
    api.subscriptionVerify(VerifyPaymentReq(orderId, paymentId, signature))
    refreshSubscription()   // your existing sub refresh
}
```

### 2e. ViewModel
```kotlin
var pendingCheckout by mutableStateOf<CheckoutOrderResp?>(null); private set
private var pendingOrderId: String? = null

fun startSubscriptionCheckout() {
    viewModelScope.launch {
        runCatching { repository.subscriptionCheckout() }
            .onSuccess { pendingOrderId = it.orderId; pendingCheckout = it }
            .onFailure { notify(friendly(it), isError = true) }
    }
}
fun checkoutConsumed() { pendingCheckout = null }

fun onPaymentSuccess(paymentId: String, signature: String) {
    val orderId = pendingOrderId ?: return
    viewModelScope.launch {
        runCatching { repository.subscriptionVerify(orderId, paymentId, signature) }
            .onSuccess { notify("Listing active — you're discoverable for 30 days") }
            .onFailure { notify(friendly(it), isError = true) }
        pendingOrderId = null
    }
}
fun onPaymentError(message: String) { notify(message, isError = true); pendingOrderId = null }
```

### 2f. MainActivity — implement the Razorpay callback + open Checkout when the VM asks
```kotlin
class MainActivity : ComponentActivity(), PaymentResultWithDataListener {
    private var vmRef: NikhatGlowViewModel? = null

    override fun onPaymentSuccess(razorpayPaymentId: String?, data: PaymentData?) {
        vmRef?.onPaymentSuccess(razorpayPaymentId ?: "", data?.signature ?: "")
    }
    override fun onPaymentError(code: Int, response: String?, data: PaymentData?) {
        vmRef?.onPaymentError(response ?: "Payment cancelled")
    }

    fun openRazorpay(order: CheckoutOrderResp, contact: String, email: String) {
        val co = Checkout()
        co.setKeyID(order.keyId)
        val opts = JSONObject().apply {
            put("name", order.name); put("description", order.description)
            put("currency", order.currency); put("amount", order.amountPaise)
            put("order_id", order.orderId)
            put("prefill", JSONObject().apply { put("contact", contact); put("email", email) })
        }
        co.open(this, opts)
    }
}
// in setContent { … }: capture the VM so callbacks can reach it
val viewModel = viewModel<NikhatGlowViewModel>().also { vmRef = it }
```

### 2g. SubscriptionScreen — trigger checkout, then hand the order to the Activity
```kotlin
val ctx = LocalContext.current
val pending = viewModel.pendingCheckout
LaunchedEffect(pending) {
    val order = pending ?: return@LaunchedEffect
    (ctx as? MainActivity)?.openRazorpay(order, contact = /* partner phone */ "", email = "")
    viewModel.checkoutConsumed()
}
// the "Subscribe ₹99 / Renew" button:
Button(onClick = { viewModel.startSubscriptionCheckout() }) { Text("Pay ₹99 / month") }
```
(Optionally call `Checkout.preload(applicationContext)` in `Application.onCreate` for a faster
first sheet.)

### Verify
Use a Razorpay **test** key first. Pay with a test card → the sheet returns success →
`/verify` flips the subscription to active (and the webhook confirms server-side). Replay is
idempotent (same payment id never double-extends). Then switch to the live key.
