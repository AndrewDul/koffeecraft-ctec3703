package uk.ac.dmu.koffeecraft.data.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import uk.ac.dmu.koffeecraft.data.entities.OrderItem
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.querymodel.OrderDisplayItem
import uk.ac.dmu.koffeecraft.data.querymodel.OrderFeedbackItem
import uk.ac.dmu.koffeecraft.data.querymodel.ReorderItem
@Dao
interface OrderItemDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<OrderItem>)

    @Query("""
SELECT p.*, oi.quantity AS quantity
FROM order_items oi
INNER JOIN products p ON p.productId = oi.productId
WHERE oi.orderId = :orderId
  AND oi.unitPrice > 0
  AND p.isAvailable = 1
""")
    suspend fun getReorderItems(orderId: Long): List<ReorderItem>

    @Query("""
SELECT
    oi.orderItemId AS orderItemId,
    oi.orderId AS orderId,
    oi.productId AS productId,
    COALESCE(oi.productNameSnapshot, p.name, 'Removed product') AS productName,
    COALESCE(oi.productDescriptionSnapshot, p.description, '') AS productDescription,
    oi.quantity AS quantity,
    oi.unitPrice AS unitPrice,
    oi.selectedOptionLabel AS selectedOptionLabel,
    oi.selectedOptionSizeValue AS selectedOptionSizeValue,
    oi.selectedOptionSizeUnit AS selectedOptionSizeUnit,
    oi.selectedAddOnsSummary AS selectedAddOnsSummary,
    oi.estimatedCalories AS estimatedCalories,
    f.feedbackId AS feedbackId,
    f.rating AS rating,
    f.comment AS comment
FROM order_items oi
LEFT JOIN products p ON p.productId = oi.productId
LEFT JOIN feedback f ON f.orderItemId = oi.orderItemId
WHERE oi.orderId = :orderId
  AND oi.unitPrice > 0
ORDER BY oi.orderItemId ASC
""")
    suspend fun getFeedbackItemsForOrder(orderId: Long): List<OrderFeedbackItem>

    @Query("""
SELECT
    oi.orderItemId AS orderItemId,
    oi.orderId AS orderId,
    oi.productId AS productId,
    COALESCE(oi.productNameSnapshot, p.name, 'Removed product') AS productName,
    COALESCE(oi.productDescriptionSnapshot, p.description, '') AS productDescription,
    oi.quantity AS quantity,
    oi.unitPrice AS unitPrice,
    oi.selectedOptionLabel AS selectedOptionLabel,
    oi.selectedOptionSizeValue AS selectedOptionSizeValue,
    oi.selectedOptionSizeUnit AS selectedOptionSizeUnit,
    oi.selectedAddOnsSummary AS selectedAddOnsSummary,
    oi.estimatedCalories AS estimatedCalories,
    f.feedbackId AS feedbackId,
    f.rating AS rating,
    f.comment AS comment
FROM order_items oi
LEFT JOIN products p ON p.productId = oi.productId
LEFT JOIN feedback f ON f.orderItemId = oi.orderItemId
WHERE oi.orderItemId = :orderItemId
  AND oi.unitPrice > 0
LIMIT 1
""")
    suspend fun getFeedbackItemByOrderItemId(orderItemId: Long): OrderFeedbackItem?

    @Query("""
SELECT
    oi.orderItemId AS orderItemId,
    oi.orderId AS orderId,
    oi.productId AS productId,
    COALESCE(oi.productNameSnapshot, p.name, 'Removed product') AS productName,
    COALESCE(oi.productDescriptionSnapshot, p.description, '') AS productDescription,
    oi.quantity AS quantity,
    oi.unitPrice AS unitPrice,
    oi.selectedOptionLabel AS selectedOptionLabel,
    oi.selectedOptionSizeValue AS selectedOptionSizeValue,
    oi.selectedOptionSizeUnit AS selectedOptionSizeUnit,
    oi.selectedAddOnsSummary AS selectedAddOnsSummary,
    oi.estimatedCalories AS estimatedCalories,
    f.feedbackId AS feedbackId,
    f.rating AS rating,
    f.comment AS comment
FROM order_items oi
LEFT JOIN products p ON p.productId = oi.productId
LEFT JOIN feedback f ON f.orderItemId = oi.orderItemId
WHERE oi.orderId = :orderId
  AND oi.unitPrice > 0
  AND f.feedbackId IS NULL
ORDER BY oi.orderItemId ASC
LIMIT 1
""")
    suspend fun getNextUnreviewedItem(orderId: Long): OrderFeedbackItem?

    @Query("""
SELECT
    COALESCE(oi.productNameSnapshot, p.name, 'Removed product') AS productName,
    oi.quantity AS quantity,
    oi.unitPrice AS unitPrice,
    oi.selectedOptionLabel AS selectedOptionLabel,
    oi.selectedOptionSizeValue AS selectedOptionSizeValue,
    oi.selectedOptionSizeUnit AS selectedOptionSizeUnit,
    oi.selectedAddOnsSummary AS selectedAddOnsSummary,
    oi.estimatedCalories AS estimatedCalories
FROM order_items oi
LEFT JOIN products p ON p.productId = oi.productId
WHERE oi.orderId = :orderId
ORDER BY oi.orderItemId ASC
""")
    suspend fun getDisplayItemsForOrder(orderId: Long): List<OrderDisplayItem>
}
