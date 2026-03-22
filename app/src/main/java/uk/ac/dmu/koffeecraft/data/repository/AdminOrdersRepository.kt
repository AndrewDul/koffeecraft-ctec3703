package uk.ac.dmu.koffeecraft.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.dto.AdminOrderRow
import uk.ac.dmu.koffeecraft.ui.admin.orders.AdminOrderDetailsUi
import uk.ac.dmu.koffeecraft.ui.admin.orders.AdminOrderLineUi
import uk.ac.dmu.koffeecraft.util.notifications.AdminNotificationManager
import uk.ac.dmu.koffeecraft.util.notifications.CustomerNotificationManager

enum class AdminOrderSearchMode {
    ORDER_ID,
    CUSTOMER_ID
}

enum class AdminOrderSortDirection {
    DESC,
    ASC
}

class AdminOrdersRepository(
    context: Context,
    private val db: KoffeeCraftDatabase
) {

    private val appContext = context.applicationContext

    fun observeOrders(
        status: String?,
        query: String,
        searchMode: AdminOrderSearchMode,
        sortDirection: AdminOrderSortDirection
    ): Flow<List<AdminOrderRow>> {
        return db.orderDao().observeAdminOrdersFiltered(
            status = status,
            query = query,
            searchMode = searchMode.name,
            sortDir = sortDirection.name
        )
    }

    suspend fun loadOrderDetails(orderId: Long): AdminOrderDetailsUi? {
        val order = db.orderDao().getById(orderId) ?: return null
        val customer = db.customerDao().getInboxTargetByOrderId(orderId) ?: return null
        val payment = db.paymentDao().getLatestForOrder(orderId)
        val displayItems = db.orderItemDao().getDisplayItemsForOrder(orderId)
        val feedbackItems = db.orderItemDao().getFeedbackItemsForOrder(orderId)

        val itemLines = displayItems.map { item ->
            AdminOrderLineUi(
                productName = item.productName,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                selectedOptionLabel = item.selectedOptionLabel,
                selectedOptionSizeValue = item.selectedOptionSizeValue,
                selectedOptionSizeUnit = item.selectedOptionSizeUnit,
                selectedAddOnsSummary = item.selectedAddOnsSummary,
                estimatedCalories = item.estimatedCalories
            )
        }

        return AdminOrderDetailsUi(
            orderId = order.orderId,
            customerId = customer.customerId,
            customerName = "${customer.firstName} ${customer.lastName}".trim(),
            customerEmail = customer.email,
            promoEligible = customer.marketingInboxConsent,
            paymentType = payment?.paymentType ?: "UNKNOWN",
            totalAmount = order.totalAmount,
            createdAt = order.createdAt,
            status = order.status,
            feedbackWritten = feedbackItems.any { it.feedbackId != null },
            hasCraftedItems = itemLines.any { it.isCrafted },
            hasRewardItems = itemLines.any { it.isReward },
            items = itemLines
        )
    }

    suspend fun updateOrderStatus(
        orderId: Long,
        targetStatus: String
    ): SettingsActionResult {
        val existing = db.orderDao().getById(orderId)
            ?: return SettingsActionResult.Error("Order could not be found.")

        if (existing.status == targetStatus) {
            return SettingsActionResult.Success("Order status is already $targetStatus.")
        }

        db.orderDao().updateStatus(orderId, targetStatus)

        val updatedOrder = db.orderDao().getById(orderId)
            ?: return SettingsActionResult.Error("Order was updated but could not be reloaded.")

        CustomerNotificationManager.createCustomerOrderStatusNotification(
            context = appContext,
            db = db,
            customerId = updatedOrder.customerId,
            orderId = updatedOrder.orderId,
            orderCreatedAt = updatedOrder.createdAt,
            status = targetStatus
        )

        AdminNotificationManager.syncAdminOrderActionNotification(
            context = appContext,
            db = db,
            orderId = updatedOrder.orderId,
            orderCreatedAt = updatedOrder.createdAt,
            orderStatus = targetStatus,
            triggerPhoneNotificationForNewOnly = false
        )

        return SettingsActionResult.Success(
            when (targetStatus) {
                "PREPARING" -> "Order moved to Preparing."
                "READY" -> "Order marked as Ready."
                "COLLECTED" -> "Order marked as Collected."
                else -> "Order status updated."
            }
        )
    }
}