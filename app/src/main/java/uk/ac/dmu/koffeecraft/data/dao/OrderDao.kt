package uk.ac.dmu.koffeecraft.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import uk.ac.dmu.koffeecraft.data.dto.AdminOrderRow
import uk.ac.dmu.koffeecraft.data.entities.Order

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

    @Query(
        """
        SELECT 
            o.orderId AS orderId,
            o.customerId AS customerId,
            c.firstName AS customerFirstName,
            c.lastName AS customerLastName,
            c.email AS customerEmail,
            o.status AS status,
            o.totalAmount AS totalAmount,
            o.createdAt AS createdAt,
            COALESCE((
                SELECT SUM(oi.quantity)
                FROM order_items oi
                WHERE oi.orderId = o.orderId
            ), 0) AS itemCount,
            COALESCE((
                SELECT COUNT(*)
                FROM order_items oi
                WHERE oi.orderId = o.orderId
                  AND (
                    (oi.selectedAddOnsSummary IS NOT NULL AND TRIM(oi.selectedAddOnsSummary) != '')
                    OR
                    (
                        oi.selectedOptionLabel IS NOT NULL
                        AND TRIM(oi.selectedOptionLabel) != ''
                        AND oi.selectedOptionSizeValue IS NOT NULL
                        AND oi.selectedOptionSizeUnit IS NOT NULL
                        AND TRIM(oi.selectedOptionSizeUnit) != ''
                    )
                  )
            ), 0) AS craftedLineCount,
            COALESCE((
                SELECT COUNT(*)
                FROM order_items oi
                WHERE oi.orderId = o.orderId
                  AND oi.unitPrice <= 0
            ), 0) AS rewardLineCount
        FROM orders o
        INNER JOIN customers c ON c.customerId = o.customerId
        WHERE (:status IS NULL OR o.status = :status)
        ORDER BY o.createdAt DESC
        """
    )
    fun observeAdminOrders(status: String?): Flow<List<AdminOrderRow>>

    @Query(
        """
        SELECT 
            o.orderId AS orderId,
            o.customerId AS customerId,
            c.firstName AS customerFirstName,
            c.lastName AS customerLastName,
            c.email AS customerEmail,
            o.status AS status,
            o.totalAmount AS totalAmount,
            o.createdAt AS createdAt,
            COALESCE((
                SELECT SUM(oi.quantity)
                FROM order_items oi
                WHERE oi.orderId = o.orderId
            ), 0) AS itemCount,
            COALESCE((
                SELECT COUNT(*)
                FROM order_items oi
                WHERE oi.orderId = o.orderId
                  AND (
                    (oi.selectedAddOnsSummary IS NOT NULL AND TRIM(oi.selectedAddOnsSummary) != '')
                    OR
                    (
                        oi.selectedOptionLabel IS NOT NULL
                        AND TRIM(oi.selectedOptionLabel) != ''
                        AND oi.selectedOptionSizeValue IS NOT NULL
                        AND oi.selectedOptionSizeUnit IS NOT NULL
                        AND TRIM(oi.selectedOptionSizeUnit) != ''
                    )
                  )
            ), 0) AS craftedLineCount,
            COALESCE((
                SELECT COUNT(*)
                FROM order_items oi
                WHERE oi.orderId = o.orderId
                  AND oi.unitPrice <= 0
            ), 0) AS rewardLineCount
        FROM orders o
        INNER JOIN customers c ON c.customerId = o.customerId
        WHERE (:status IS NULL OR o.status = :status)
          AND (
            :query IS NULL OR :query = '' OR
            (:searchMode = 'ORDER_ID' AND CAST(o.orderId AS TEXT) = :query) OR
            (:searchMode = 'CUSTOMER_ID' AND CAST(o.customerId AS TEXT) = :query)
          )
        ORDER BY
          CASE WHEN :sortDir = 'ASC' THEN o.createdAt END ASC,
          CASE WHEN :sortDir = 'DESC' THEN o.createdAt END DESC
        """
    )
    fun observeAdminOrdersFiltered(
        status: String?,
        query: String?,
        searchMode: String,
        sortDir: String
    ): Flow<List<AdminOrderRow>>


    @Query("SELECT * FROM orders WHERE orderId = :orderId LIMIT 1")
    suspend fun getById(orderId: Long): Order?

    @Query(
        """
        SELECT COUNT(*) FROM orders
        WHERE createdAt BETWEEN :startOfDay AND :endOfDay
        """
    )
    suspend fun countOrdersCreatedBetween(startOfDay: Long, endOfDay: Long): Int

    @Query(
        """
        SELECT COALESCE(SUM(totalAmount), 0) FROM orders
        WHERE createdAt BETWEEN :startOfDay AND :endOfDay
          AND status != 'CANCELLED'
        """
    )
    suspend fun getRevenueCreatedBetween(startOfDay: Long, endOfDay: Long): Double

    @Query(
        """
        SELECT COUNT(*) FROM orders
        WHERE status IN ('PLACED', 'PREPARING', 'READY')
        """
    )
    suspend fun countPendingOrders(): Int
}