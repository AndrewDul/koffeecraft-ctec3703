package uk.ac.dmu.koffeecraft.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import uk.ac.dmu.koffeecraft.data.entities.OrderItem
import androidx.room.Embedded
import uk.ac.dmu.koffeecraft.data.entities.Product
import androidx.room.Query
@Dao
interface OrderItemDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<OrderItem>)

    @Query("""
    SELECT p.*, oi.quantity AS quantity
    FROM order_items oi
    INNER JOIN products p ON p.productId = oi.productId
    WHERE oi.orderId = :orderId
""")
    suspend fun getReorderItems(orderId: Long): List<ReorderItem>
}
data class ReorderItem(
    @Embedded val product: Product,
    val quantity: Int
)