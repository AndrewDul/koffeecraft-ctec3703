package uk.ac.dmu.koffeecraft.data.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import uk.ac.dmu.koffeecraft.data.entities.OrderItem
import uk.ac.dmu.koffeecraft.data.entities.Product

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

    @Query("""
        SELECT
            oi.orderItemId AS orderItemId,
            oi.orderId AS orderId,
            oi.productId AS productId,
            p.name AS productName,
            p.description AS productDescription,
            oi.quantity AS quantity,
            oi.unitPrice AS unitPrice,
            f.feedbackId AS feedbackId,
            f.rating AS rating,
            f.comment AS comment
        FROM order_items oi
        INNER JOIN products p ON p.productId = oi.productId
        LEFT JOIN feedback f ON f.orderItemId = oi.orderItemId
        WHERE oi.orderId = :orderId
        ORDER BY oi.orderItemId ASC
    """)
    suspend fun getFeedbackItemsForOrder(orderId: Long): List<OrderFeedbackItem>
    @Query("""
        
    SELECT
        oi.orderItemId AS orderItemId,
        oi.orderId AS orderId,
        oi.productId AS productId,
        p.name AS productName,
        p.description AS productDescription,
        oi.quantity AS quantity,
        oi.unitPrice AS unitPrice,
        f.feedbackId AS feedbackId,
        f.rating AS rating,
        f.comment AS comment
    FROM order_items oi
    INNER JOIN products p ON p.productId = oi.productId
    LEFT JOIN feedback f ON f.orderItemId = oi.orderItemId
    WHERE oi.orderItemId = :orderItemId
    LIMIT 1
""")
    suspend fun getFeedbackItemByOrderItemId(orderItemId: Long): OrderFeedbackItem?

    @Query("""
        SELECT
            oi.orderItemId AS orderItemId,
            oi.orderId AS orderId,
            oi.productId AS productId,
            p.name AS productName,
            p.description AS productDescription,
            oi.quantity AS quantity,
            oi.unitPrice AS unitPrice,
            f.feedbackId AS feedbackId,
            f.rating AS rating,
            f.comment AS comment
        FROM order_items oi
        INNER JOIN products p ON p.productId = oi.productId
        LEFT JOIN feedback f ON f.orderItemId = oi.orderItemId
        WHERE oi.orderId = :orderId
          AND f.feedbackId IS NULL
        ORDER BY oi.orderItemId ASC
        LIMIT 1
    """)
    suspend fun getNextUnreviewedItem(orderId: Long): OrderFeedbackItem?
}

data class ReorderItem(
    @Embedded val product: Product,
    val quantity: Int
)

data class OrderFeedbackItem(
    val orderItemId: Long,
    val orderId: Long,
    val productId: Long,
    val productName: String,
    val productDescription: String,
    val quantity: Int,
    val unitPrice: Double,
    val feedbackId: Long?,
    val rating: Int?,
    val comment: String?
)
