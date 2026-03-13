package uk.ac.dmu.koffeecraft.util.notifications

import android.content.Context
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.AppNotification

object CustomerNotificationManager {

    private fun buildTitle(status: String): String {
        return when (status) {
            "PREPARING" -> "Order update"
            "READY" -> "Ready for pickup"
            "COLLECTED" -> "Collected"
            else -> "Order update"
        }
    }

    private fun buildMessage(orderId: Long, status: String): String {
        return when (status) {
            "PREPARING" -> "Order #$orderId is now being prepared."
            "READY" -> "Order #$orderId is ready. You can collect it now."
            "COLLECTED" -> "Order #$orderId has been collected. Enjoy!"
            else -> "Order #$orderId status changed to $status."
        }
    }

    suspend fun createCustomerOrderStatusNotification(
        context: Context,
        db: KoffeeCraftDatabase,
        customerId: Long,
        orderId: Long,
        orderCreatedAt: Long,
        status: String
    ) {
        val title = buildTitle(status)
        val message = buildMessage(orderId, status)

        db.notificationDao().insert(
            AppNotification(
                recipientRole = "CUSTOMER",
                recipientCustomerId = customerId,
                title = title,
                message = message,
                notificationType = "CUSTOMER_ORDER_UPDATE",
                orderId = orderId,
                orderCreatedAt = orderCreatedAt,
                orderStatus = status,
                isRead = false
            )
        )

        NotificationHelper.showOrderNotification(
            context = context,
            title = title,
            message = message,
            notificationId = 500000 + (orderId % 50000).toInt()
        )
    }
}