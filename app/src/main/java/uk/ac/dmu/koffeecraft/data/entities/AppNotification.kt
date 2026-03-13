package uk.ac.dmu.koffeecraft.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_notifications",
    indices = [
        Index(value = ["recipientRole"]),
        Index(value = ["recipientCustomerId"]),
        Index(value = ["isRead"]),
        Index(value = ["createdAt"]),
        Index(value = ["orderId"])
    ]
)
data class AppNotification(
    @PrimaryKey(autoGenerate = true) val notificationId: Long = 0,
    val recipientRole: String, // "ADMIN" or "CUSTOMER"
    val recipientCustomerId: Long? = null,
    val title: String,
    val message: String,
    val notificationType: String, // "ADMIN_ORDER_ACTION" or "CUSTOMER_ORDER_UPDATE"
    val orderId: Long? = null,
    val orderCreatedAt: Long? = null,
    val orderStatus: String? = null,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)