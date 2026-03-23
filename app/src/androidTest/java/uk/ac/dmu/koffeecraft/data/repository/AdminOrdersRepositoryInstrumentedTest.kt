package uk.ac.dmu.koffeecraft.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uk.ac.dmu.koffeecraft.testsupport.BaseInstrumentedDatabaseTest
import uk.ac.dmu.koffeecraft.testsupport.TestSeedData

@RunWith(AndroidJUnit4::class)
class AdminOrdersRepositoryInstrumentedTest : BaseInstrumentedDatabaseTest() {

    private lateinit var repository: AdminOrdersRepository

    @Before
    fun setUpRepository() {
        repository = AdminOrdersRepository(context, db)
    }

    @Test
    fun loadOrderDetails_returnsAggregatedOrderState() = runBlocking {
        val customerId = TestSeedData.insertCustomer(
            db = db,
            email = "adminorders@example.com",
            marketingInboxConsent = true
        )

        val product = TestSeedData.insertProduct(
            db = db,
            name = "Flat White",
            productFamily = "COFFEE",
            price = 4.50
        )

        val orderId = TestSeedData.insertOrder(
            db = db,
            customerId = customerId,
            status = "PLACED",
            totalAmount = 8.50,
            createdAt = 1_700_000_000_000L
        )

        db.orderItemDao().insertAll(
            listOf(
                TestSeedData.buildOrderItem(
                    orderId = orderId,
                    productId = product.productId,
                    quantity = 1,
                    unitPrice = 4.50,
                    selectedOptionLabel = "Large",
                    selectedOptionSizeValue = 450,
                    selectedOptionSizeUnit = "ml",
                    productNameSnapshot = product.name,
                    productDescriptionSnapshot = product.description
                ),
                TestSeedData.buildOrderItem(
                    orderId = orderId,
                    productId = product.productId,
                    quantity = 1,
                    unitPrice = 0.0,
                    productNameSnapshot = "Reward: ${product.name}",
                    productDescriptionSnapshot = product.description
                )
            )
        )

        TestSeedData.insertPayment(
            db = db,
            orderId = orderId,
            paymentType = "CARD",
            amount = 8.50
        )

        val paidItemId = db.orderItemDao().getFeedbackItemsForOrder(orderId).single().orderItemId
        TestSeedData.insertFeedback(
            db = db,
            orderItemId = paidItemId,
            customerId = customerId,
            rating = 5,
            comment = "Excellent"
        )

        val details = repository.loadOrderDetails(orderId)

        assertNotNull(details)
        assertEquals(orderId, details!!.orderId)
        assertEquals(customerId, details.customerId)
        assertEquals("CARD", details.paymentType)
        assertTrue(details.promoEligible)
        assertTrue(details.feedbackWritten)
        assertTrue(details.hasCraftedItems)
        assertTrue(details.hasRewardItems)
        assertEquals(2, details.items.size)
        assertTrue(details.items.any { it.isCrafted })
        assertTrue(details.items.any { it.isReward })
    }

    @Test
    fun updateOrderStatus_updatesOrder_andCreatesCustomerAndAdminNotifications() = runBlocking {
        val customerId = TestSeedData.insertCustomer(
            db = db,
            email = "notify@example.com"
        )

        val orderId = TestSeedData.insertOrder(
            db = db,
            customerId = customerId,
            status = "PLACED",
            totalAmount = 6.0,
            createdAt = 1_700_000_000_000L
        )

        val result = repository.updateOrderStatus(
            orderId = orderId,
            targetStatus = "PREPARING"
        )

        assertTrue(result is SettingsActionResult.Success)
        assertEquals(
            "Order moved to Preparing.",
            (result as SettingsActionResult.Success).message
        )

        val updatedOrder = db.orderDao().getById(orderId)
        val customerNotifications = db.notificationDao().observeCustomerNotifications(customerId).first()
        val adminNotifications = db.notificationDao().observeAdminNotifications().first()

        assertEquals("PREPARING", updatedOrder?.status)
        assertEquals(1, customerNotifications.size)
        assertEquals(1, adminNotifications.size)

        assertEquals("CUSTOMER_ORDER_UPDATE", customerNotifications.first().notificationType)
        assertEquals("PREPARING", customerNotifications.first().orderStatus)
        assertEquals(orderId, customerNotifications.first().orderId)

        assertEquals("ADMIN_ORDER_ACTION", adminNotifications.first().notificationType)
        assertEquals("PREPARING", adminNotifications.first().orderStatus)
        assertEquals(orderId, adminNotifications.first().orderId)
    }

    @Test
    fun observeOrders_filtersByStatusAndCustomerId() = runBlocking {
        val customerOneId = TestSeedData.insertCustomer(
            db = db,
            email = "customer1@example.com"
        )
        val customerTwoId = TestSeedData.insertCustomer(
            db = db,
            email = "customer2@example.com"
        )

        val matchingOrderId = TestSeedData.insertOrder(
            db = db,
            customerId = customerOneId,
            status = "READY",
            totalAmount = 5.0,
            createdAt = 1000L
        )

        TestSeedData.insertOrder(
            db = db,
            customerId = customerTwoId,
            status = "PLACED",
            totalAmount = 7.0,
            createdAt = 2000L
        )

        val rows = repository.observeOrders(
            status = "READY",
            query = customerOneId.toString(),
            searchMode = AdminOrderSearchMode.CUSTOMER_ID,
            sortDirection = AdminOrderSortDirection.DESC
        ).first()

        assertEquals(1, rows.size)
        assertEquals(matchingOrderId, rows.first().orderId)
        assertEquals(customerOneId, rows.first().customerId)
        assertEquals("READY", rows.first().status)
    }
}