package uk.ac.dmu.koffeecraft.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import uk.ac.dmu.koffeecraft.data.entities.Order

@Dao
interface OrderDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(order: Order): Long

    @Query("SELECT * FROM orders WHERE orderId = :orderId LIMIT 1")
    fun observeById(orderId: Long): Flow<Order?>

    @Query("UPDATE orders SET status = :status WHERE orderId = :orderId")
    suspend fun updateStatus(orderId: Long, status: String)
}