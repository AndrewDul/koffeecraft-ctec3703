package uk.ac.dmu.koffeecraft.util.notifications

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.AppNotification

object AdminNotificationManager {

    private fun isTrackedStatus(status: String): Boolean {
        return status == "PLACED" ||
                status == "PREPARING" ||
                status == "READY" ||
                status == "COLLECTED"
    }

    private fun nextStatus(status: String): String? {
        return when (status) {
            "PLACED" -> "PREPARING"
            "PREPARING" -> "READY"
            "READY" -> "COLLECTED"
            else -> null
        }
    }

    private fun buildTitle(status: String): String {
        return when (status) {
            "PLACED" -> "New order needs action"
            "PREPARING" -> "Order ready for next step"
            "READY" -> "Order ready to collect"
            "COLLECTED" -> "Order collected"
            else -> "Order update"
        }
    }

    private fun buildMessage(orderId: Long, orderCreatedAt: Long, status: String): String {
        val formattedTime = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.UK)
            .format(Date(orderCreatedAt))

        return "Order #$orderId • Created: $formattedTime • Current status: $status"
    }

    suspend fun syncAdminOrderActionNotification(
        context: Context,
        db: KoffeeCraftDatabase,
        orderId: Long,
        orderCreatedAt: Long,
        orderStatus: String,
        triggerPhoneNotificationForNewOnly: Boolean = true
    ) {
        val dao = db.notificationDao()

        if (!isTrackedStatus(orderStatus)) {
            dao.getAdminOrderActionNotification(orderId)?.let {
                dao.deleteById(it.notificationId)
            }
            return
        }

        val existing = dao.getAdminOrderActionNotification(orderId)
        val title = buildTitle(orderStatus)
        val message = buildMessage(orderId, orderCreatedAt, orderStatus)

        if (existing == null) {
            dao.insert(
                AppNotification(
                    recipientRole = "ADMIN",
                    recipientCustomerId = null,
                    title = title,
                    message = message,
                    notificationType = "ADMIN_ORDER_ACTION",
                    orderId = orderId,
                    orderCreatedAt = orderCreatedAt,
                    orderStatus = orderStatus,
                    isRead = false
                )
            )

            if (triggerPhoneNotificationForNewOnly) {
                NotificationHelper.showOrderNotification(
                    context = context,
                    title = title,
                    message = message,
                    notificationId = 400000 + (orderId % 50000).toInt()
                )
            }
        } else {
            dao.updateAdminOrderActionNotification(
                notificationId = existing.notificationId,
                title = title,
                message = message,
                orderStatus = orderStatus,
                isRead = existing.isRead
            )
        }
    }

    suspend fun advanceOrderFromNotification(
        context: Context,
        db: KoffeeCraftDatabase,
        notification: AppNotification
    ) {
        val orderId = notification.orderId ?: return
        val currentStatus = notification.orderStatus ?: return
        val orderCreatedAt = notification.orderCreatedAt ?: System.currentTimeMillis()
        val next = nextStatus(currentStatus) ?: return

        db.orderDao().updateStatus(orderId, next)

        val order = db.orderDao().getById(orderId)
        if (order != null) {
            CustomerNotificationManager.createCustomerOrderStatusNotification(
                context = context,
                db = db,
                customerId = order.customerId,
                orderId = order.orderId,
                orderCreatedAt = order.createdAt,
                status = next
            )
        }

        syncAdminOrderActionNotification(
            context = context,
            db = db,
            orderId = orderId,
            orderCreatedAt = orderCreatedAt,
            orderStatus = next,
            triggerPhoneNotificationForNewOnly = false
        )

        db.notificationDao().markAsRead(notification.notificationId)
    }
}