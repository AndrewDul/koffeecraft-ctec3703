package uk.ac.dmu.koffeecraft.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import uk.ac.dmu.koffeecraft.data.entities.CustomerPaymentCard

@Dao
interface CustomerPaymentCardDao {

    @Query(
        """
        SELECT * FROM customer_payment_cards
        WHERE customerId = :customerId
        ORDER BY isDefault DESC, createdAt DESC
        """
    )
    fun observeForCustomer(customerId: Long): Flow<List<CustomerPaymentCard>>

    @Query(
        """
        SELECT * FROM customer_payment_cards
        WHERE customerId = :customerId
        ORDER BY isDefault DESC, createdAt DESC
        """
    )
    suspend fun getAllForCustomer(customerId: Long): List<CustomerPaymentCard>

    @Query(
        """
        SELECT * FROM customer_payment_cards
        WHERE customerId = :customerId
          AND isDefault = 1
        LIMIT 1
        """
    )
    suspend fun getDefaultForCustomer(customerId: Long): CustomerPaymentCard?

    @Query(
        """
        SELECT * FROM customer_payment_cards
        WHERE customerId = :customerId
        ORDER BY createdAt DESC
        LIMIT 1
        """
    )
    suspend fun getMostRecentForCustomer(customerId: Long): CustomerPaymentCard?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(card: CustomerPaymentCard): Long

    @Query(
        """
        UPDATE customer_payment_cards
        SET isDefault = 0
        WHERE customerId = :customerId
        """
    )
    suspend fun clearDefaultForCustomer(customerId: Long)

    @Query(
        """
        UPDATE customer_payment_cards
        SET isDefault = 1
        WHERE customerId = :customerId
          AND cardId = :cardId
        """
    )
    suspend fun setDefault(cardId: Long, customerId: Long)

    @Query(
        """
        UPDATE customer_payment_cards
        SET nickname = :nickname
        WHERE cardId = :cardId
          AND customerId = :customerId
        """
    )
    suspend fun updateNickname(cardId: Long, customerId: Long, nickname: String)

    @Query(
        """
        DELETE FROM customer_payment_cards
        WHERE cardId = :cardId
          AND customerId = :customerId
        """
    )
    suspend fun deleteByIdAndCustomer(cardId: Long, customerId: Long)
}