package uk.ac.dmu.koffeecraft.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import uk.ac.dmu.koffeecraft.data.entities.Payment

@Dao
interface PaymentDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(payment: Payment): Long

    @Query(
        """
        SELECT * FROM payments
        WHERE orderId = :orderId
        ORDER BY paymentDate DESC
        LIMIT 1
        """
    )
    suspend fun getLatestForOrder(orderId: Long): Payment?
}