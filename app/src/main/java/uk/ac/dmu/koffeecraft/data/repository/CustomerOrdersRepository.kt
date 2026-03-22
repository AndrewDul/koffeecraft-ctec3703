package uk.ac.dmu.koffeecraft.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import uk.ac.dmu.koffeecraft.data.cart.CartManager
import uk.ac.dmu.koffeecraft.data.dao.OrderDisplayItem
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.Order
import uk.ac.dmu.koffeecraft.data.settings.HiddenOrdersStore
import uk.ac.dmu.koffeecraft.util.notifications.AdminNotificationManager
import uk.ac.dmu.koffeecraft.util.orders.OrderSimulationManager
import java.util.Calendar

class CustomerOrdersRepository(
    context: Context,
    private val db: KoffeeCraftDatabase
) {

    private val appContext = context.applicationContext

    fun observeOrders(customerId: Long): Flow<List<Order>> {
        return db.orderDao().observeByCustomer(customerId)
    }

    fun observeOrder(orderId: Long): Flow<Order?> {
        return db.orderDao().observeById(orderId)
    }

    suspend fun buildRenderData(
        customerId: Long,
        allOrders: List<Order>,
        filter: CustomerOrderDateFilter
    ): CustomerOrdersRenderData {
        val hiddenOrderIds = HiddenOrdersStore.getHiddenOrderIds(appContext, customerId)

        val visibleOrders = allOrders
            .filterNot { order -> hiddenOrderIds.contains(order.orderId) }
            .filter { order -> matchesSelectedFilter(order.createdAt, filter) }

        val detailsAndFeedback = visibleOrders.associate { order ->
            val details = db.orderItemDao().getDisplayItemsForOrder(order.orderId)
            val feedbackItems = db.orderItemDao().getFeedbackItemsForOrder(order.orderId)
            val reviewedCount = feedbackItems.count { it.feedbackId != null }

            order.orderId to Pair(
                details,
                CustomerOrderFeedbackCounters(
                    eligibleItemCount = feedbackItems.size,
                    reviewedItemCount = reviewedCount
                )
            )
        }

        return CustomerOrdersRenderData(
            visibleOrders = visibleOrders,
            detailsByOrderId = detailsAndFeedback.mapValues { it.value.first },
            feedbackByOrderId = detailsAndFeedback.mapValues { it.value.second },
            emptyMessage = if (filter == CustomerOrderDateFilter.ALL) {
                "No orders yet."
            } else {
                "No orders found for this period."
            }
        )
    }

    fun hideOrder(customerId: Long, orderId: Long) {
        HiddenOrdersStore.hideOrder(
            context = appContext,
            customerId = customerId,
            orderId = orderId
        )
    }

    suspend fun reorderToCart(orderId: Long): Boolean {
        val reorderItems = db.orderItemDao().getReorderItems(orderId)
        if (reorderItems.isEmpty()) return false

        CartManager.clear()
        reorderItems.forEach { item ->
            CartManager.add(item.product, item.quantity)
        }
        return true
    }

    suspend fun loadOrderStatusSnapshot(orderId: Long): CustomerOrderStatusSnapshot {
        val payment = db.paymentDao().getLatestForOrder(orderId)
        val feedbackItems = db.orderItemDao().getFeedbackItemsForOrder(orderId)
        val reviewedCount = feedbackItems.count { it.feedbackId != null }
        val displayItems = db.orderItemDao().getDisplayItemsForOrder(orderId)

        return CustomerOrderStatusSnapshot(
            paymentType = payment?.paymentType ?: "UNKNOWN",
            itemsOrdered = displayItems.sumOf { it.quantity },
            feedbackCounters = CustomerOrderFeedbackCounters(
                eligibleItemCount = feedbackItems.size,
                reviewedItemCount = reviewedCount
            )
        )
    }

    fun startOrderSimulationIfNeeded(orderId: Long) {
        OrderSimulationManager.startIfNeeded(
            context = appContext,
            db = db,
            orderId = orderId
        )
    }

    suspend fun syncAdminOrderActionNotification(
        orderId: Long,
        orderCreatedAt: Long,
        orderStatus: String
    ) {
        AdminNotificationManager.syncAdminOrderActionNotification(
            context = appContext,
            db = db,
            orderId = orderId,
            orderCreatedAt = orderCreatedAt,
            orderStatus = orderStatus
        )
    }

    private fun matchesSelectedFilter(
        createdAt: Long,
        filter: CustomerOrderDateFilter
    ): Boolean {
        val startToday = startOfDay(0)
        val startYesterday = startOfDay(1)
        val startTwoDaysAgo = startOfDay(2)
        val startSevenDaysAgo = startOfDay(7)
        val startFourteenDaysAgo = startOfDay(14)

        return when (filter) {
            CustomerOrderDateFilter.ALL -> true
            CustomerOrderDateFilter.TODAY -> createdAt >= startToday
            CustomerOrderDateFilter.YESTERDAY -> createdAt in startYesterday until startToday
            CustomerOrderDateFilter.TWO_DAYS_AGO -> createdAt in startTwoDaysAgo until startYesterday
            CustomerOrderDateFilter.LAST_7_DAYS -> createdAt in startSevenDaysAgo until startTwoDaysAgo
            CustomerOrderDateFilter.LAST_14_DAYS -> createdAt in startFourteenDaysAgo until startSevenDaysAgo
            CustomerOrderDateFilter.EARLIER -> createdAt < startFourteenDaysAgo
        }
    }

    private fun startOfDay(daysAgo: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -daysAgo)
        }.timeInMillis
    }
}

enum class CustomerOrderDateFilter {
    ALL,
    TODAY,
    YESTERDAY,
    TWO_DAYS_AGO,
    LAST_7_DAYS,
    LAST_14_DAYS,
    EARLIER
}

data class CustomerOrderFeedbackCounters(
    val eligibleItemCount: Int,
    val reviewedItemCount: Int
)

data class CustomerOrdersRenderData(
    val visibleOrders: List<Order>,
    val detailsByOrderId: Map<Long, List<OrderDisplayItem>>,
    val feedbackByOrderId: Map<Long, CustomerOrderFeedbackCounters>,
    val emptyMessage: String
)

data class CustomerOrderStatusSnapshot(
    val paymentType: String,
    val itemsOrdered: Int,
    val feedbackCounters: CustomerOrderFeedbackCounters
)