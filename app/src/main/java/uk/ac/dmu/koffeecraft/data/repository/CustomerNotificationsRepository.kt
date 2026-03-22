package uk.ac.dmu.koffeecraft.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uk.ac.dmu.koffeecraft.data.dao.OrderDisplayItem
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.AppNotification

data class CustomerNotificationsData(
    val items: List<AppNotification>,
    val detailsByOrderId: Map<Long, List<OrderDisplayItem>>
)

class CustomerNotificationsRepository(
    private val db: KoffeeCraftDatabase
) {

    fun observeCustomerNotifications(customerId: Long): Flow<CustomerNotificationsData> {
        return db.notificationDao().observeCustomerNotifications(customerId).map { items ->
            val detailsByOrderId = items
                .mapNotNull { it.orderId }
                .distinct()
                .associateWith { orderId ->
                    db.orderItemDao().getDisplayItemsForOrder(orderId)
                }

            CustomerNotificationsData(
                items = items,
                detailsByOrderId = detailsByOrderId
            )
        }
    }

    suspend fun deleteNotification(notificationId: Long) {
        db.notificationDao().deleteById(notificationId)
    }

    suspend fun markAsRead(notificationId: Long) {
        db.notificationDao().markAsRead(notificationId)
    }
}