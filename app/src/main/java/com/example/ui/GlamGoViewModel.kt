package com.example.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class Screen {
    object CustomerHome : Screen()
    data class CategoryDetail(val category: Category) : Screen()
    data class ServiceDetail(val service: Service) : Screen()
    data class PartnerSelect(val service: Service) : Screen()
    data class BookingConfirm(val service: Service, val partner: Partner) : Screen(), RouteWithParams
    data class BookingDetail(val bookingId: String) : Screen(), RouteWithParams
    object Wallet : Screen()
    object ComplaintsList : Screen()
    object AIChat : Screen()
    
    object CustomerProfile : Screen()
    object ServiceBookingForm : Screen()
    
    // Partner screens
    object PartnerDashboard : Screen()
    object PartnerKyc : Screen()
    object PartnerServices : Screen()
    object PartnerEarnings : Screen()
    
    // Pre-booking messaging
    data class PreBookingChat(val service: Service, val partner: Partner) : Screen()
}

interface RouteWithParams

class GlamGoViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    val repository = GlamGoRepository(db)

    // State flows from Room
    val activeUser = repository.activeUserFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val addresses = repository.addressesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val bookings = repository.bookingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val partnerServices = repository.partnerServicesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val complaints = repository.complaintsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val preBookingInquiries = repository.allPreBookingMessagesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoritePartners = repository.favoritePartnersFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun isFavorite(partnerId: String): Flow<Boolean> = repository.isFavoriteFlow(partnerId)

    fun toggleFavorite(partnerId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(partnerId)
        }
    }

    // Navigation and transient UI states
    var currentScreen by mutableStateOf<Screen>(Screen.CustomerHome)
    var onboardingComplete by mutableStateOf(true)

    var pushRemindersEnabled by mutableStateOf(getPushRemindersPref())
        private set

    private fun getPushRemindersPref(): Boolean {
        val prefs = getApplication<Application>().getSharedPreferences("glamgo_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("push_reminders_enabled", true)
    }

    fun updatePushReminders(enabled: Boolean) {
        pushRemindersEnabled = enabled
        val prefs = getApplication<Application>().getSharedPreferences("glamgo_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("push_reminders_enabled", enabled).apply()
    }

    // Selected items for booking flow cache
    var selectedSlot by mutableStateOf("Tomorrow, 10:00 AM - 11:30 AM")
    var couponCode by mutableStateOf("")
    var applyWalletDiscount by mutableStateOf(true)
    var quoteBreakdown by mutableStateOf<QuoteBreakdown?>(null)

    // AI Chat list
    private val _aiMessages = MutableStateFlow<List<Pair<String, Boolean>>>(
        listOf("Hello! I am your GlamGo Beauty AI assistant. Ask me for custom style suggestions, haircut advice, facials, or body spa recommendations!" to false)
    )
    val aiMessages = _aiMessages.asStateFlow()
    var aiLoading by mutableStateOf(false)

    // Active Chat in Booking Detail
    fun getMessagesForBooking(bookingId: String): Flow<List<ChatMessageEntity>> {
        return repository.getMessagesFlow(bookingId)
    }

    // Initialize/sync database user profile
    init {
        viewModelScope.launch {
            // Check if profile exists, otherwise create
            val user = db.userDao().getUserSync()
            if (user == null) {
                db.userDao().insertUser(
                    UserEntity(
                        name = "Ananya Sharma",
                        email = "ananya.sharma@example.com",
                        role = "customer",
                        kycStatus = "not_started",
                        walletBalancePaise = 500000 // ₹5000
                    )
                )
            }
        }
    }

    fun switchRole(newRole: String) {
        viewModelScope.launch {
            repository.switchRole(newRole)
            if (newRole == "customer") {
                currentScreen = Screen.CustomerHome
            } else {
                currentScreen = Screen.PartnerDashboard
            }
        }
    }

    fun updateProfile(name: String, email: String, bio: String = "", experience: Int = 0) {
        viewModelScope.launch {
            repository.updateProfile(name, email, bio, experience)
        }
    }

    fun addWalletMoney(amountPaise: Long, role: String) {
        viewModelScope.launch {
            repository.addWalletMoney(amountPaise, role)
        }
    }

    // Address
    fun addNewAddress(label: String, line1: String, line2: String, city: String, pincode: String) {
        viewModelScope.launch {
            repository.addAddress(label, line1, line2, city, pincode, 12.9716, 77.5946)
        }
    }

    fun deleteAddress(id: Long) {
        viewModelScope.launch {
            repository.deleteAddress(id)
        }
    }

    fun setDefaultAddress(id: Long) {
        viewModelScope.launch {
            repository.setDefaultAddress(id)
        }
    }

    // Quote Recalculation
    fun updateBookingQuote(service: Service, partner: Partner, customPricePaise: Long? = null) {
        val userVal = activeUser.value
        val userBalance = userVal?.walletBalancePaise ?: 0L
        quoteBreakdown = repository.calculateQuote(
            pricePaise = customPricePaise ?: service.pricePaise,
            distanceKm = partner.distanceKm,
            couponCode = couponCode,
            useWallet = applyWalletDiscount,
            walletBalancePaise = userBalance
        )
    }

    // Creating Booking
    fun confirmAndBook(service: Service, partner: Partner, address: AddressEntity) {
        val quote = quoteBreakdown ?: return
        viewModelScope.launch {
            val booking = repository.createBooking(
                service = service,
                partner = partner,
                slot = selectedSlot,
                address = address,
                quote = quote,
                payViaWallet = applyWalletDiscount
            )
            // Navigate to Booking detail
            currentScreen = Screen.BookingDetail(booking.id)

            // Auto advance state simulator after some seconds so user can see it react
            simulateBookingStateProgression(booking.id)
        }
    }

    fun bookDirectlyFromForm(
        service: Service,
        slot: String,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            val partner = GlamMockDataSource.partners.firstOrNull { it.servicesOffered.contains(service.id) }
                ?: GlamMockDataSource.partners.first()
            
            val currentAddresses = addresses.value
            val address = currentAddresses.firstOrNull { it.isDefault }
                ?: currentAddresses.firstOrNull()
                ?: AddressEntity(
                    labelText = "Default",
                    line1 = "123 Elegance Lane",
                    line2 = "Penthouse Suite",
                    city = "Bengaluru",
                    pincode = "560001",
                    isDefault = true,
                    lat = 12.9716,
                    lon = 77.5946
                )
            
            val basePrice = service.pricePaise
            val taxPrice = ((service.pricePaise) * 0.18).toLong()
            val total = basePrice + taxPrice
            
            val quote = QuoteBreakdown(
                basePaise = basePrice,
                distancePaise = 0L,
                surgePaise = 0L,
                couponDiscountPaise = 0L,
                walletDiscountPaise = 0L,
                taxPaise = taxPrice,
                totalPaise = total,
                couponMessage = null
            )
            
            val booking = repository.createBooking(
                service = service,
                partner = partner,
                slot = slot,
                address = address,
                quote = quote,
                payViaWallet = false
            )
            
            currentScreen = Screen.BookingDetail(booking.id)
            simulateBookingStateProgression(booking.id)
            onSuccess(booking.id)
        }
    }

    private fun simulateBookingStateProgression(bookingId: String) {
        viewModelScope.launch {
            // First delay to accept job (Wait 6 seconds)
            delay(5000)
            val currentBooking = db.bookingDao().getBookingById(bookingId)
            if (currentBooking != null && currentBooking.status == "pending") {
                repository.acceptBooking(bookingId)
                
                // Wait another 7 seconds to depart
                delay(6000)
                repository.startTravel(bookingId)

                // Wait another 7 seconds to arrive
                delay(6000)
                repository.arriveLocation(bookingId)
            }
        }
    }

    // Chat
    fun sendChatMessage(bookingId: String, senderRole: String, text: String) {
        viewModelScope.launch {
            repository.sendChatMessage(bookingId, senderRole, text)
        }
    }

    // KYC
    fun submitKyc(aadhaar: String, pan: String) {
        viewModelScope.launch {
            repository.submitKyc(aadhaar, pan, "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format")
            // Simulate admin approval 7 seconds later
            delay(6000)
            repository.approveKyc()
        }
    }

    // Service pricing changes (Partner)
    fun setPartnerServicePrice(serviceId: String, name: String, category: String, pricePaise: Long, active: Boolean, productsUsed: String) {
        viewModelScope.launch {
            repository.setServicePriceAndStatus(
                id = "me_$serviceId",
                serviceId = serviceId,
                name = name,
                category = category,
                pricePaise = pricePaise,
                duration = 45,
                active = active,
                productsUsed = productsUsed
            )
        }
    }

    // Review
    fun submitBookingReview(bookingId: String, rating: Int, comment: String) {
        viewModelScope.launch {
            repository.addReview(bookingId, rating, comment)
        }
    }

    // Complaint
    fun submitComplaint(bookingId: String, subject: String, message: String) {
        viewModelScope.launch {
            repository.createComplaint(bookingId, subject, message)
        }
    }

    // AI Support Response
    fun sendAiMessage(message: String) {
        if (message.isBlank()) return
        val currentLog = _aiMessages.value.toMutableList()
        currentLog.add(message to true)
        _aiMessages.value = currentLog

        aiLoading = true
        viewModelScope.launch {
            val response = GeminiAssistant.generateBeautyAdvise(message)
            val updatedLog = _aiMessages.value.toMutableList()
            updatedLog.add(response to false)
            _aiMessages.value = updatedLog
            aiLoading = false
        }
    }

    fun clearAiLog() {
        _aiMessages.value = listOf(
            "Hello! I am your GlamGo Beauty AI assistant. Ask me for custom style suggestions, haircut advice, facials, or body spa recommendations!" to false
        )
    }
}
