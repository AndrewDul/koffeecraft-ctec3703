package uk.ac.dmu.koffeecraft.data.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import uk.ac.dmu.koffeecraft.data.entities.AppNotification
import uk.ac.dmu.koffeecraft.testsupport.BaseInstrumentedDatabaseTest
import uk.ac.dmu.koffeecraft.testsupport.TestSeedData

@RunWith(AndroidJUnit4::class)
class NotificationDaoInstrumentedTest : BaseInstrumentedDatabaseTest() {

    @Test
    fun observeUnreadCounts_andGetAdminOrderActionNotification_returnExpectedResults() = runBlocking {
        val customerId = TestSeedData.insertCustomer(
            db = db,
            email = "notifications@example.com"
        )

        val orderId = TestSeedData.insertOrder(
            db = db,
            customerId = customerId,
            status = "PLACED",
            totalAmount = 6.0
        )

        db.notificationDao().insertAll(
            listOf(
                AppNotification(
                    recipientRole = "ADMIN",
                    title = "Order action",
                    message = "Order moved to Preparing.",
                    notificationType = "ADMIN_ORDER_ACTION",
                    orderId = orderId,
                    orderCreatedAt = 1_700_000_000_000L,
                    orderStatus = "PREPARING",
                    isRead = false,
                    createdAt = 1000L
                ),
                AppNotification(
                    recipientRole = "ADMIN",
                    title = "Older admin item",
                    message = "Already read",
                    notificationType = "ADMIN_ORDER_ACTION",
                    orderId = orderId,
                    orderCreatedAt = 1_700_000_000_000L,
                    orderStatus = "PLACED",
                    isRead = true,
                    createdAt = 900L
                ),
                AppNotification(
                    recipientRole = "CUSTOMER",
                    recipientCustomerId = customerId,
                    title = "Order update",
                    message = "Your order is preparing.",
                    notificationType = "CUSTOMER_ORDER_UPDATE",
                    orderId = orderId,
                    orderCreatedAt = 1_700_000_000_000L,
                    orderStatus = "PREPARING",
                    isRead = false,
                    createdAt = 1100L
                )
            )
        )

        val unreadAdminCount = db.notificationDao().observeUnreadAdminCount().first()
        val unreadCustomerCount = db.notificationDao().observeUnreadCustomerCount(customerId).first()
        val adminOrderNotification = db.notificationDao().getAdminOrderActionNotification(orderId)

        assertEquals(1, unreadAdminCount)
        assertEquals(1, unreadCustomerCount)
        assertNotNull(adminOrderNotification)
        assertEquals("ADMIN_ORDER_ACTION", adminOrderNotification?.notificationType)
        assertEquals("PREPARING", adminOrderNotification?.orderStatus)
    }

    @Test
    fun markReadMethods_updateOnlyTargetedNotifications() = runBlocking {
        val customerId = TestSeedData.insertCustomer(
            db = db,
            email = "notifications2@example.com"
        )

        val orderId = TestSeedData.insertOrder(
            db = db,
            customerId = customerId,
            status = "READY",
            totalAmount = 7.0
        )

        val customerNotificationId = db.notificationDao().insert(
            AppNotification(
                recipientRole = "CUSTOMER",
                recipientCustomerId = customerId,
                title = "Order ready",
                message = "Your order is ready.",
                notificationType = "CUSTOMER_ORDER_UPDATE",
                orderId = orderId,
                orderCreatedAt = 1_700_000_000_000L,
                orderStatus = "READY",
                isRead = false,
                createdAt = 1000L
            )
        )

        db.notificationDao().insert(
            AppNotification(
                recipientRole = "CUSTOMER",
                recipientCustomerId = customerId,
                title = "Another order message",
                message = "Collected",
                notificationType = "CUSTOMER_ORDER_UPDATE",
                orderId = orderId,
                orderCreatedAt = 1_700_000_000_000L,
                orderStatus = "COLLECTED",
                isRead = false,
                createdAt = 1100L
            )
        )

        db.notificationDao().insert(
            AppNotification(
                recipientRole = "ADMIN",
                title = "Admin action",
                message = "Admin notification",
                notificationType = "ADMIN_ORDER_ACTION",
                orderId = orderId,
                orderCreatedAt = 1_700_000_000_000L,
                orderStatus = "READY",
                isRead = false,
                createdAt = 1200L
            )
        )

        db.notificationDao().markAsRead(customerNotificationId)
        db.notificationDao().markCustomerOrderNotificationsAsRead(customerId, orderId)
        db.notificationDao().markAllAdminAsRead()

        val customerNotifications = db.notificationDao().observeCustomerNotifications(customerId).first()
        val adminNotifications = db.notificationDao().observeAdminNotifications().first()
        val unreadAdminCount = db.notificationDao().observeUnreadAdminCount().first()
        val unreadCustomerCount = db.notificationDao().observeUnreadCustomerCount(customerId).first()

        assertTrue(customerNotifications.all { it.isRead })
        assertTrue(adminNotifications.all { it.isRead })
        assertEquals(0, unreadAdminCount)
        assertEquals(0, unreadCustomerCount)
    }
}