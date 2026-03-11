package uk.ac.dmu.koffeecraft.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import uk.ac.dmu.koffeecraft.data.entities.Order
import uk.ac.dmu.koffeecraft.data.dto.AdminOrderRow
@Dao
interface OrderDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(order: Order): Long

    @Query("SELECT * FROM orders WHERE orderId = :orderId LIMIT 1")
    fun observeById(orderId: Long): Flow<Order?>

    @Query("UPDATE orders SET status = :status WHERE orderId = :orderId")
    suspend fun updateStatus(orderId: Long, status: String)

    @Query("SELECT * FROM orders WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun observeByCustomer(customerId: Long): Flow<List<Order>>

    @Query("""
    SELECT 
        o.orderId AS orderId,
        o.customerId AS customerId,
        c.email AS customerEmail,
        o.status AS status,
        o.totalAmount AS totalAmount,
        o.createdAt AS createdAt
    FROM orders o
    INNER JOIN customers c ON c.customerId = o.customerId
    WHERE (:status IS NULL OR o.status = :status)
    ORDER BY o.createdAt DESC
""")
    fun observeAdminOrders(status: String?): kotlinx.coroutines.flow.Flow<List<AdminOrderRow>>
}