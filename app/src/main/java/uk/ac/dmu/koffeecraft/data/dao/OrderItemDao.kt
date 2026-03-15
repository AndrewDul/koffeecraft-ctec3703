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
      AND oi.unitPrice > 0
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
        oi.selectedOptionLabel AS selectedOptionLabel,
        oi.selectedOptionSizeValue AS selectedOptionSizeValue,
        oi.selectedOptionSizeUnit AS selectedOptionSizeUnit,
        oi.selectedAddOnsSummary AS selectedAddOnsSummary,
        oi.estimatedCalories AS estimatedCalories,
        f.feedbackId AS feedbackId,
        f.rating AS rating,
        f.comment AS comment
    FROM order_items oi
    INNER JOIN products p ON p.productId = oi.productId
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
        p.name AS productName,
        p.description AS productDescription,
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
    INNER JOIN products p ON p.productId = oi.productId
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
        p.name AS productName,
        p.description AS productDescription,
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
    INNER JOIN products p ON p.productId = oi.productId
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
        p.name AS productName,
        oi.quantity AS quantity,
        oi.unitPrice AS unitPrice,
        oi.selectedOptionLabel AS selectedOptionLabel,
        oi.selectedOptionSizeValue AS selectedOptionSizeValue,
        oi.selectedOptionSizeUnit AS selectedOptionSizeUnit,
        oi.selectedAddOnsSummary AS selectedAddOnsSummary,
        oi.estimatedCalories AS estimatedCalories
    FROM order_items oi
    INNER JOIN products p ON p.productId = oi.productId
    WHERE oi.orderId = :orderId
    ORDER BY oi.orderItemId ASC
""")
    suspend fun getDisplayItemsForOrder(orderId: Long): List<OrderDisplayItem>
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
    val selectedOptionLabel: String?,
    val selectedOptionSizeValue: Int?,
    val selectedOptionSizeUnit: String?,
    val selectedAddOnsSummary: String?,
    val estimatedCalories: Int?,
    val feedbackId: Long?,
    val rating: Int?,
    val comment: String?
) {
    val isCrafted: Boolean
        get() = !selectedAddOnsSummary.isNullOrBlank() ||
                (!selectedOptionLabel.isNullOrBlank() &&
                        selectedOptionSizeValue != null &&
                        !selectedOptionSizeUnit.isNullOrBlank())
}

data class OrderDisplayItem(
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val selectedOptionLabel: String?,
    val selectedOptionSizeValue: Int?,
    val selectedOptionSizeUnit: String?,
    val selectedAddOnsSummary: String?,
    val estimatedCalories: Int?
) {
    val isCrafted: Boolean
        get() = !selectedAddOnsSummary.isNullOrBlank() ||
                (!selectedOptionLabel.isNullOrBlank() &&
                        selectedOptionSizeValue != null &&
                        !selectedOptionSizeUnit.isNullOrBlank())
}