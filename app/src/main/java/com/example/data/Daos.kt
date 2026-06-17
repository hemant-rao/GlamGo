package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserFlow(id: String = "me"): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserSync(id: String = "me"): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("UPDATE users SET walletBalancePaise = :balance WHERE id = :id")
    suspend fun updateWalletBalance(balance: Long, id: String = "me")

    @Query("UPDATE users SET kycStatus = :status, aadhaarNo = :aadhaar, panNo = :pan, selfieUrl = :selfie WHERE id = :id")
    suspend fun updateKyc(status: String, aadhaar: String, pan: String, selfie: String, id: String = "me")
}

@Dao
interface AddressDao {
    @Query("SELECT * FROM addresses ORDER BY isDefault DESC, id DESC")
    fun getAddressesFlow(): Flow<List<AddressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddress(address: AddressEntity)

    @Update
    suspend fun updateAddress(address: AddressEntity)

    @Query("DELETE FROM addresses WHERE id = :id")
    suspend fun deleteAddressById(id: Long)

    @Query("UPDATE addresses SET isDefault = 0")
    suspend fun clearDefaultAddress()

    @Query("UPDATE addresses SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultAddress(id: Long)
}

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings ORDER BY createdAt DESC")
    fun getBookingsFlow(): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE id = :id LIMIT 1")
    fun getBookingFlowById(id: String): Flow<BookingEntity?>

    @Query("SELECT * FROM bookings WHERE id = :id LIMIT 1")
    suspend fun getBookingById(id: String): BookingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: BookingEntity)

    @Query("UPDATE bookings SET status = :status WHERE id = :id")
    suspend fun updateBookingStatus(id: String, status: String)

    @Query("UPDATE bookings SET paymentStatus = :payStatus WHERE id = :id")
    suspend fun updateBookingPaymentStatus(id: String, payStatus: String)

    @Query("UPDATE bookings SET reviewRating = :rating, reviewComment = :comment WHERE id = :id")
    suspend fun addBookingReview(id: String, rating: Int, comment: String)

    @Query("UPDATE bookings SET completionProofUrls = :proofUrls WHERE id = :id")
    suspend fun updateCompletionProofs(id: String, proofUrls: String)
}

@Dao
interface PartnerServiceDao {
    @Query("SELECT * FROM partner_services")
    fun getPartnerServicesFlow(): Flow<List<PartnerServiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartnerService(service: PartnerServiceEntity)

    @Delete
    suspend fun deletePartnerService(service: PartnerServiceEntity)
    
    @Query("UPDATE partner_services SET pricePaise = :price, active = :active WHERE id = :id")
    suspend fun updateServicePriceAndStatus(id: String, price: Long, active: Boolean)
}

@Dao
interface WalletTransactionDao {
    @Query("SELECT * FROM wallet_transactions WHERE role = :role ORDER BY at DESC")
    fun getWalletTransactionsFlow(role: String): Flow<List<WalletTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: WalletTransactionEntity)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE bookingId = :bookingId ORDER BY timestamp ASC")
    fun getMessagesFlow(bookingId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE bookingId LIKE 'pre_%' ORDER BY timestamp DESC")
    fun getAllPreBookingMessagesFlow(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)
}

@Dao
interface ComplaintDao {
    @Query("SELECT * FROM complaints ORDER BY createdAt DESC")
    fun getComplaintsFlow(): Flow<List<ComplaintEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaint(complaint: ComplaintEntity)
}

@Dao
interface FavoritePartnerDao {
    @Query("SELECT * FROM favorite_partners")
    fun getFavoritePartnersFlow(): Flow<List<FavoritePartnerEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_partners WHERE partnerId = :partnerId)")
    fun isFavoriteFlow(partnerId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoritePartnerEntity)

    @Query("DELETE FROM favorite_partners WHERE partnerId = :partnerId")
    suspend fun removeFavorite(partnerId: String)
}
