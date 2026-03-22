package uk.ac.dmu.koffeecraft.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.AppNotification
import uk.ac.dmu.koffeecraft.util.notifications.AdminNotificationManager
import java.util.Locale

data class AdminNotificationsData(
    val items: List<AppNotification>,
    val queueSummary: String
)

sealed interface AdminNotificationsActionResult {
    data object Success : AdminNotificationsActionResult
    data class Error(val message: String) : AdminNotificationsActionResult
}

class AdminNotificationsRepository(
    context: Context,
    private val db: KoffeeCraftDatabase
) {

    private val appContext = context.applicationContext

    fun observeAdminNotifications(): Flow<AdminNotificationsData> {
        return db.notificationDao().observeAdminNotifications().map { items ->
            AdminNotificationsData(
                items = items,
                queueSummary = buildQueueSummary(items)
            )
        }
    }

    suspend fun markAllAsRead() {
        db.notificationDao().markAllAdminAsRead()
    }

    suspend fun deleteNotification(item: AppNotification): AdminNotificationsActionResult {
        if (item.orderStatus != "COLLECTED") {
            return AdminNotificationsActionResult.Error(
                "Only collected notifications can be removed."
            )
        }

        db.notificationDao().deleteById(item.notificationId)
        return AdminNotificationsActionResult.Success
    }

    suspend fun advanceOrder(item: AppNotification): AdminNotificationsActionResult {
        val nextLabel = when (item.orderStatus?.uppercase(Locale.UK)) {
            "PLACED" -> "Preparing"
            "PREPARING" -> "Ready"
            "READY" -> "Collected"
            else -> null
        }

        if (nextLabel == null) {
            return AdminNotificationsActionResult.Error(
                "This notification has no next admin action."
            )
        }

        AdminNotificationManager.advanceOrderFromNotification(
            context = appContext,
            db = db,
            notification = item
        )

        return AdminNotificationsActionResult.Success
    }

    private fun buildQueueSummary(items: List<AppNotification>): String {
        if (items.isEmpty()) return "No admin notifications right now."

        val actionNeeded = items.count {
            it.notificationType == "ADMIN_ORDER_ACTION" &&
                    (it.orderStatus == "PLACED" ||
                            it.orderStatus == "PREPARING" ||
                            it.orderStatus == "READY")
        }

        val removable = items.count { it.orderStatus == "COLLECTED" }

        return buildString {
            append("${items.size} notification")
            if (items.size != 1) append("s")
            append(" in queue")

            if (actionNeeded > 0) {
                append(" • ")
                append("$actionNeeded need action")
            }

            if (removable > 0) {
                append(" • ")
                append("$removable ready to clear")
            }
        }
    }
}