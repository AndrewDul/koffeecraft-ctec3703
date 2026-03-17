package uk.ac.dmu.koffeecraft.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import uk.ac.dmu.koffeecraft.data.entities.AppNotification

@Dao
interface NotificationDao {

    @Query("""
        SELECT * FROM app_notifications
        WHERE recipientRole = 'ADMIN'
        ORDER BY isRead ASC, createdAt DESC
    """)
    fun observeAdminNotifications(): Flow<List<AppNotification>>

    @Query("""
        SELECT * FROM app_notifications
        WHERE recipientRole = 'CUSTOMER'
          AND recipientCustomerId = :customerId
        ORDER BY isRead ASC, createdAt DESC
    """)
    fun observeCustomerNotifications(customerId: Long): Flow<List<AppNotification>>

    @Query("""
        SELECT COUNT(*) FROM app_notifications
        WHERE recipientRole = 'ADMIN'
          AND isRead = 0
    """)
    fun observeUnreadAdminCount(): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM app_notifications
        WHERE recipientRole = 'CUSTOMER'
          AND recipientCustomerId = :customerId
          AND isRead = 0
    """)
    fun observeUnreadCustomerCount(customerId: Long): Flow<Int>

    @Query("""
        SELECT * FROM app_notifications
        WHERE recipientRole = 'ADMIN'
          AND notificationType = 'ADMIN_ORDER_ACTION'
          AND orderId = :orderId
        LIMIT 1
    """)
    suspend fun getAdminOrderActionNotification(orderId: Long): AppNotification?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: AppNotification): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<AppNotification>)

    @Query("""
        UPDATE app_notifications
        SET title = :title,
            message = :message,
            orderStatus = :orderStatus,
            isRead = :isRead
        WHERE notificationId = :notificationId
    """)
    suspend fun updateAdminOrderActionNotification(
        notificationId: Long,
        title: String,
        message: String,
        orderStatus: String,
        isRead: Boolean
    )

    @Query("""
        UPDATE app_notifications
        SET isRead = 1
        WHERE notificationId = :notificationId
    """)
    suspend fun markAsRead(notificationId: Long)

    @Query("""
        UPDATE app_notifications
        SET isRead = 1
        WHERE recipientRole = 'ADMIN'
    """)
    suspend fun markAllAdminAsRead()

    @Query("""
        UPDATE app_notifications
        SET isRead = 1
        WHERE recipientRole = 'CUSTOMER'
          AND recipientCustomerId = :customerId
    """)
    suspend fun markAllCustomerAsRead(customerId: Long)

    @Query("""
        UPDATE app_notifications
        SET isRead = 1
        WHERE recipientRole = 'CUSTOMER'
          AND recipientCustomerId = :customerId
          AND orderId = :orderId
    """)
    suspend fun markCustomerOrderNotificationsAsRead(
        customerId: Long,
        orderId: Long
    )

    @Query("""
        DELETE FROM app_notifications
        WHERE notificationId = :notificationId
    """)
    suspend fun deleteById(notificationId: Long)
}