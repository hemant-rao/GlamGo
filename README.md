# Nikhat Glow – The Fragrance of Beauty

Nikhat Glow is a hyper-transparent, two-sided beauty marketplace built with **Jetpack Compose**, **Kotlin Coroutines / Flow**, **Firebase Firestore**, and local **Room Database** persistence. It empowers independent beauty professionals (Partners) to manage their digital micro-salons, while providing customers with a secure, premium doorstep service experience backed by authentic multi-dimensional reviews, verified brand kit transparency, and safe pre-booking negotiations.

The entire application compiles with a luxury dark cosmic theme: `DeepPlum` (#2A0845), `GlamRose` (#FF4A70), and `GlamGold` (#D4AF37) accents.

---

## ⚙️ 2026-06-19 — Connector-model rebuild (current behaviour)

The app was refactored to a pure **connector** marketplace. The notes below
override any older "wallet / escrow / AI stylist / Partner Mode" descriptions
further down this file:

- **No AI assistant, no wallet/escrow.** Removed entirely. In the connector
  model the customer pays the professional **directly** after the service — the
  platform never holds money. Cart/booking totals are shown as an estimate only.
- **Role chosen once at signup.** The login screen has a Customer / Partner
  selector; that choice fixes the session identity. There is **no in-app role
  switching / "Partner Mode" toggle** — to use the other role, log out and sign
  in again choosing it.
- **Role-based bottom navigation.**
  - Customer: **Explore · Cart · Bookings · Profile**
  - Partner: **Dashboard · Services · Requests · Account**
- **Customer flow:** search a service → choose a professional (or open a
  professional's profile and explore all their services) → **Add to cart**
  (single-partner, multi-service) → **Send booking request**.
- **Partner flow:** list services (pick from catalog + set own price) → receive
  booking **requests** → Accept / Reject → chat opens.
- **Phone privacy.** Phone numbers are **never shown** on profiles or cards. The
  in-app chat (opened after a partner accepts) is the only channel — a number is
  shared solely if a user types it into chat themselves.

---

## 🌌 Core Vision & Design Philosophy

Nikhat Glow steps away from standard interfaces to offer an exquisite, high-trust luxury salon experience:
* **Branded Theme**: Pure deep premium cosmos color accents, polished gold typography, and spacious layouts.
* **Edge-to-Edge Fluidity**: Dynamic layout wrapping with `WindowInsets` handling, proper safe zone padding, and fluid sizing adaptiveness.
* **Custom Launcher Emblem / Logo**: Completely redesigned vector launcher icons featuring blooming rose gradients, glistening star dust, and gold fragrance droplet elements supporting the *Fragrance of Beauty* identity.

---

## 🚀 Enhanced Marketplace Modules & Features

### 🛠 1. The Partner Ecosystem (Micro-Entrepreneurship)
* **Self-Onboarding & Catalog Builder**: Partners registers, uploads dynamic picture avatars, and establishes a highly customized "Digital Salon/Clinic" presence.
* **Dynamic Menus & Swiggy/Zomato-Style Pricing**: Partners can customize their service items (e.g., Bridal Grooming, Gel Manicure, Facials), control Swiggy/Zomato style open prices (paise-precision), and toggle activations.
* **Verified Product Kit Lists**: Supports premium product transparency. Professionals must configure which certified kit lists or brands they will utilize (e.g., L'Oréal Professional, O3+, organic floral oils).
* **Availability Control Engine**: Includes an interactive panel where therapists can:
    * Toggle **Active Status** (Online & Visible vs. Away Mode).
    * Configure **Service Bounds Radius** via active sliders (1 km to 30 km limits).
    * Set daily operating **Shift Hours Grid** (Standard, Extended, Late shift, or Late Night Luxe).
* **Comprehensive Provider Dashboard**: Dedicated hub to manage incoming job request queues, track lifetime clear balance earnings, monitor multi-dimensional ratings, and reply to client inquiries.

### 💖 2. The Customer Experience (Discovery & Trust)
* **Aesthetic Discovery Layer**: Search interface supporting search prompts, combined with brand filters (L'Oreal, Wella, Forest Essentials, O3+) and rating thresholds (Any Rating, 4.5+ Stars, 4.8+ Stars).
* **Pre-Booking Inquiry Corridor**: Safe pre-service discussion channel opening a direct connection between customer and partner before locking in slots. Customers can negotiate custom demands, check skin sensitivities, or verify physical kit seals.
* **Multi-Dimensional Review Engine**:
    * Post-service feedback is locked strictly behind completed transaction IDs to prevent fake reviews.
    * Users review professionals across three distinct parameters: **Technical Grooming Skill**, **Hygiene & Sanitation Care**, and **Product Authenticity & Seal Check**.
    * Scores are mathematically unified and structured transparently inside the partner database.
* **Favorites Shelf**: Instant hot-toggle bookmarking for chosen therapist specialists, displayed in a horizontal tray in the user dashboard.

### 💳 3. Secure Transacting & Wallet Escrow
* **Big GPT AI/Nikhat Glow Escrow**: Upon dispatching booking requests, funds are securely held in escrow until both the client and the therapist register the appointment as completed.
* **Instant Cancellation Refund**: Instantly returns funds to customer wallets on state cancellation, while automatically releasing escrow reserves to partner wallets when successfully finished.
* **Wallet Balance Tracking**: High-trust ledger documenting all platform payouts, top-ups, and cashback discounts.

### 🧠 4. Nikhat Glow AI Beauty Stylist
* Powered by Google's LLM engine to act as a personal fragrance palette advisor, skin therapist, and grooming stylist.
* Features responsive state animations and rapid interaction resets.

---

## 🛠 Tech Stack & Architecture

```
                                            [ Jetpack Compose UI ]
                                                       │
                                              [ Nikhat Glow VM ]
                                                       │
                                           [ Nikhat Glow Repository ]
                                         /                            \
              (Offline Cache / Local db)                               (Cloud Sync Layer)
           [ Room AppDatabase / DAOs ]                      [ GlamGoFirestoreManager ]
```

* **Jetpack Compose**: 100% Declarative UI with accessible components.
* **Room Database**: Secondary offline-first fallback handling all critical tables (`UserEntity`, `BookingEntity`, etc.).
* **Firebase Firestore Sync**: Syncs live marketplace tables instantly across customers and providers.
* **Kotlin Coroutines / Flow**: Asynchronous responsive states.

---

## 🎨 Resource Codes & UI Touchpoints

* **TestTags**: Outfitted with robust testing locators (e.g., `partner_availability_toggle`, `submit_triple_review_btn`, `review_input_comment`).
* **Accessibility**: Touch targets adhere strictly to the Material 3 standard constraint of **48dp x 48dp**.

---

Developed with ❤️ under modern Android design practices. For live deployments, place a valid `google-services.json` inside the gradle bundle.
