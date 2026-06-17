package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

data class QuoteBreakdown(
    val basePaise: Long,
    val distancePaise: Long,
    val surgePaise: Long,
    val couponDiscountPaise: Long,
    val walletDiscountPaise: Long,
    val taxPaise: Long,
    val totalPaise: Long,
    val couponMessage: String?
)

class GlamGoRepository(private val db: AppDatabase) {
    private val userDao = db.userDao()
    private val addressDao = db.addressDao()
    private val bookingDao = db.bookingDao()
    private val partnerServiceDao = db.partnerServiceDao()
    private val walletTransactionDao = db.walletTransactionDao()
    private val chatMessageDao = db.chatMessageDao()
    private val complaintDao = db.complaintDao()
    private val favoritePartnerDao = db.favoritePartnerDao()

    // Flows
    val activeUserFlow: Flow<UserEntity?> = userDao.getUserFlow()
    val addressesFlow: Flow<List<AddressEntity>> = addressDao.getAddressesFlow()
    val bookingsFlow: Flow<List<BookingEntity>> = bookingDao.getBookingsFlow()
    val partnerServicesFlow: Flow<List<PartnerServiceEntity>> = partnerServiceDao.getPartnerServicesFlow()
    val complaintsFlow: Flow<List<ComplaintEntity>> = complaintDao.getComplaintsFlow()
    val favoritePartnersFlow: Flow<List<FavoritePartnerEntity>> = favoritePartnerDao.getFavoritePartnersFlow()

    fun isFavoriteFlow(partnerId: String): Flow<Boolean> = favoritePartnerDao.isFavoriteFlow(partnerId)

    suspend fun toggleFavorite(partnerId: String) {
        val list = favoritePartnerDao.getFavoritePartnersFlow().firstOrNull() ?: emptyList()
        val exists = list.any { it.partnerId == partnerId }
        if (exists) {
            favoritePartnerDao.removeFavorite(partnerId)
        } else {
            favoritePartnerDao.addFavorite(FavoritePartnerEntity(partnerId))
        }
    }

    fun getMessagesFlow(bookingId: String): Flow<List<ChatMessageEntity>> = chatMessageDao.getMessagesFlow(bookingId)
    val allPreBookingMessagesFlow: Flow<List<ChatMessageEntity>> = chatMessageDao.getAllPreBookingMessagesFlow()
    fun getTransactionsFlow(role: String): Flow<List<WalletTransactionEntity>> = walletTransactionDao.getWalletTransactionsFlow(role)

    private suspend fun syncUserToFirestore() {
        val currentUser = userDao.getUserSync()
        if (currentUser != null) {
            GlamGoFirestoreManager.saveUserProfile(currentUser)
        }
    }

    // User operations
    suspend fun switchRole(newRole: String) {
        val currentUser = userDao.getUserSync()
        if (currentUser != null) {
            userDao.insertUser(currentUser.copy(role = newRole))
        } else {
            userDao.insertUser(
                UserEntity(
                    name = "Ananya Sharma",
                    email = "ananya.sharma@example.com",
                    role = newRole,
                    kycStatus = "not_started"
                )
            )
        }
        syncUserToFirestore()
    }

    suspend fun updateProfile(name: String, email: String, bio: String = "", experience: Int = 0) {
        val currentUser = userDao.getUserSync() ?: return
        userDao.insertUser(
            currentUser.copy(
                name = name,
                email = email,
                partnerBio = bio,
                partnerExperience = experience
            )
        )
        syncUserToFirestore()
    }

    suspend fun submitKyc(aadhaar: String, pan: String, selfieUrl: String) {
        val currentUser = userDao.getUserSync() ?: return
        userDao.insertUser(
            currentUser.copy(
                kycStatus = "under_review",
                aadhaarNo = aadhaar,
                panNo = pan,
                selfieUrl = selfieUrl
            )
        )
        syncUserToFirestore()
    }

    suspend fun approveKyc() {
        val currentUser = userDao.getUserSync() ?: return
        userDao.insertUser(currentUser.copy(kycStatus = "approved"))
        syncUserToFirestore()
    }

    suspend fun addWalletMoney(amountPaise: Long, role: String) {
        val currentUser = userDao.getUserSync() ?: return
        val newBalance = currentUser.walletBalancePaise + amountPaise
        userDao.updateWalletBalance(newBalance)

        walletTransactionDao.insertTransaction(
            WalletTransactionEntity(
                type = "credit",
                role = role,
                amountPaise = amountPaise,
                reason = "Wallet Top-up (Dev Payment)"
            )
        )
        syncUserToFirestore()
    }

    // Address operations
    suspend fun addAddress(label: String, line1: String, line2: String, city: String, pincode: String, lat: Double, lon: Double) {
        addressDao.insertAddress(
            AddressEntity(
                labelText = label,
                line1 = line1,
                line2 = line2,
                city = city,
                pincode = pincode,
                lat = lat,
                lon = lon
            )
        )
    }

    suspend fun deleteAddress(id: Long) {
        addressDao.deleteAddressById(id)
    }

    suspend fun setDefaultAddress(id: Long) {
        addressDao.clearDefaultAddress()
        addressDao.setDefaultAddress(id)
    }

    // Pricing calculation / Quote
    fun calculateQuote(
        pricePaise: Long,
        distanceKm: Double,
        couponCode: String?,
        useWallet: Boolean,
        walletBalancePaise: Long
    ): QuoteBreakdown {
        val basePaise = pricePaise
        val distancePaise = (distanceKm * 2000).toLong() // ₹20 per km (2000 paise)
        val surgePaise = if (System.currentTimeMillis() % 2 == 0L) 0L else 4900L // Occasional surge ₹49

        var couponDiscount = 0L
        var couponMsg: String? = null
        if (!couponCode.isNullOrBlank()) {
            if (couponCode.uppercase() == "GLAMNEW" || couponCode.uppercase() == "FIRST50") {
                couponDiscount = (basePaise * 0.15).toLong().coerceAtMost(15000) // 15% off up to ₹150 (15000 paise)
                couponMsg = "₹${couponDiscount / 100} off applied via $couponCode!"
            } else {
                couponMsg = "Invalid coupon code"
            }
        }

        val subtotal = basePaise + distancePaise + surgePaise - couponDiscount
        val taxPaise = (subtotal * 0.18).toLong() // 18% GST

        val preWalletTotal = subtotal + taxPaise
        val walletAmountUsed = if (useWallet) {
            preWalletTotal.coerceAtMost(walletBalancePaise)
        } else {
            0L
        }

        val finalTotal = (preWalletTotal - walletAmountUsed).coerceAtLeast(0L)

        return QuoteBreakdown(
            basePaise = basePaise,
            distancePaise = distancePaise,
            surgePaise = surgePaise,
            couponDiscountPaise = couponDiscount,
            walletDiscountPaise = walletAmountUsed,
            taxPaise = taxPaise,
            totalPaise = finalTotal,
            couponMessage = couponMsg
        )
    }

    // Booking actions
    suspend fun createBooking(
        service: Service,
        partner: Partner,
        slot: String,
        address: AddressEntity,
        quote: QuoteBreakdown,
        payViaWallet: Boolean
    ): BookingEntity {
        val bookingId = "GG-" + (1000 + (Math.random() * 9000).toInt())
        val booking = BookingEntity(
            id = bookingId,
            status = "pending",
            serviceId = service.id,
            serviceName = service.name,
            serviceImageUrl = service.imageUrl,
            categoryName = service.categoryId.replace("cat_", "").capitalize(),
            partnerId = partner.id,
            partnerName = partner.name,
            partnerAvatar = partner.avatarUrl,
            dateTimeSlot = slot,
            addressText = "${address.line1}, ${address.line2}, ${address.city} - ${address.pincode}",
            totalPaise = quote.totalPaise,
            paymentStatus = if (payViaWallet && quote.walletDiscountPaise > 0) "paid" else "pending",
            startOtp = (1000 + (Math.random() * 9000).toInt()).toString()
        )

        bookingDao.insertBooking(booking)

        // Adjust wallet balance if wallet was applied
        if (quote.walletDiscountPaise > 0) {
            val user = userDao.getUserSync()
            if (user != null) {
                val newBal = (user.walletBalancePaise - quote.walletDiscountPaise).coerceAtLeast(0L)
                userDao.updateWalletBalance(newBal)

                walletTransactionDao.insertTransaction(
                    WalletTransactionEntity(
                        type = "debit",
                        role = "customer",
                        amountPaise = quote.walletDiscountPaise,
                        reason = "Booking payment for $bookingId"
                    )
                )
            }
        }

        // Insert default welcome messages for chat
        chatMessageDao.insertMessage(
            ChatMessageEntity(
                bookingId = bookingId,
                senderRole = "partner",
                text = "Hello! Thanks for selecting me for your beauty session. I will confirm the slot shortly."
            )
        )

        return booking
    }

    suspend fun acceptBooking(id: String) {
        bookingDao.updateBookingStatus(id, "accepted")
        chatMessageDao.insertMessage(
            ChatMessageEntity(
                bookingId = id,
                senderRole = "partner",
                text = "Awesome, I have accepted your appointment! Looking forward to seeing you."
            )
        )
    }

    suspend fun rejectBooking(id: String) {
        bookingDao.updateBookingStatus(id, "rejected")
    }

    suspend fun startTravel(id: String) {
        bookingDao.updateBookingStatus(id, "partner_on_the_way")
        chatMessageDao.insertMessage(
            ChatMessageEntity(
                bookingId = id,
                senderRole = "partner",
                text = "Hello! I am on my way to your location. See you soon!"
            )
        )
    }

    suspend fun arriveLocation(id: String) {
        bookingDao.updateBookingStatus(id, "arrived")
    }

    suspend fun startJob(id: String) {
        bookingDao.updateBookingStatus(id, "started")
    }

    suspend fun completeJob(id: String, proofUrls: String) {
        val booking = bookingDao.getBookingById(id) ?: return
        bookingDao.updateBookingStatus(id, "completed")
        bookingDao.updateCompletionProofs(id, proofUrls)
        
        // Ensure paid
        bookingDao.updateBookingPaymentStatus(id, "paid")

        // Credit to Partner's wallet
        val user = userDao.getUserSync()
        if (user != null) {
            // Deduct platform commission of 15%
            val commission = (booking.totalPaise * 0.15).toLong()
            val earnings = booking.totalPaise - commission
            val newBal = user.walletBalancePaise + earnings
            userDao.updateWalletBalance(newBal)

            // Update completed jobs for stats
            userDao.insertUser(user.copy(
                walletBalancePaise = newBal,
                completedJobs = user.completedJobs + 1
            ))

            walletTransactionDao.insertTransaction(
                WalletTransactionEntity(
                    type = "credit",
                    role = "partner",
                    amountPaise = earnings,
                    reason = "Job earnings for $id (commission dec)"
                )
            )
            syncUserToFirestore()
        }
    }

    suspend fun cancelBooking(id: String, reason: String) {
        val booking = bookingDao.getBookingById(id) ?: return
        bookingDao.updateBookingStatus(id, "cancelled")
        
        // Refund if it was paid
        if (booking.paymentStatus == "paid") {
            bookingDao.updateBookingPaymentStatus(id, "refunded")
            val user = userDao.getUserSync()
            if (user != null) {
                val newBal = user.walletBalancePaise + booking.totalPaise
                userDao.updateWalletBalance(newBal)

                walletTransactionDao.insertTransaction(
                    WalletTransactionEntity(
                        type = "credit",
                        role = "customer",
                        amountPaise = booking.totalPaise,
                        reason = "Full Refund for Cancelled Booking $id"
                    )
                )
                syncUserToFirestore()
            }
        }
    }

    suspend fun addReview(id: String, rating: Int, comment: String) {
        bookingDao.addBookingReview(id, rating, comment)
    }

    // Chat operations
    suspend fun sendChatMessage(bookingId: String, senderRole: String, text: String, kind: String = "text", voiceDurationMs: Long = 0) {
        val chatMessage = ChatMessageEntity(
            bookingId = bookingId,
            senderRole = senderRole,
            text = text,
            kind = kind,
            voiceDurationMs = voiceDurationMs
        )
        chatMessageDao.insertMessage(chatMessage)
        GlamGoFirestoreManager.saveChatMessage(chatMessage)

        // Simple trigger dynamic automated response on behalf of the other agent
        if (senderRole == "customer") {
            kotlinx.coroutines.delay(1500)
            val replyMessage = ChatMessageEntity(
                bookingId = bookingId,
                senderRole = "partner",
                text = "Sure, got it! Thanks for telling me."
            )
            chatMessageDao.insertMessage(replyMessage)
            GlamGoFirestoreManager.saveChatMessage(replyMessage)
        }
    }

    // Partner Service operations
    suspend fun setServicePriceAndStatus(id: String, serviceId: String, name: String, category: String, pricePaise: Long, duration: Int, active: Boolean, productsUsed: String) {
        val partnerService = PartnerServiceEntity(
            id = id,
            serviceId = serviceId,
            name = name,
            categoryName = category,
            pricePaise = pricePaise,
            durationMin = duration,
            active = active,
            productsUsed = productsUsed
        )
        partnerServiceDao.insertPartnerService(partnerService)
        GlamGoFirestoreManager.savePartnerService(partnerService)
    }

    // Complaints
    suspend fun createComplaint(bookingId: String, subject: String, message: String) {
        complaintDao.insertComplaint(
            ComplaintEntity(
                bookingId = bookingId,
                subject = subject,
                message = message,
                status = "Pending"
            )
        )
    }
}
