package uk.ac.dmu.koffeecraft.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uk.ac.dmu.koffeecraft.testsupport.BaseInstrumentedDatabaseTest
import uk.ac.dmu.koffeecraft.testsupport.TestSeedData

@RunWith(AndroidJUnit4::class)
class CheckoutRepositoryInstrumentedTest : BaseInstrumentedDatabaseTest() {

    private lateinit var repository: CheckoutRepository

    @Before
    fun setUpRepository() {
        repository = CheckoutRepository(db)
    }

    @Test
    fun submitOrder_createsOrderPaymentSavedCard_andUpdatesBeans() = runBlocking {
        val customerId = TestSeedData.insertCustomer(
            db = db,
            email = "checkout@example.com",
            beansBalance = 20
        )

        val product = TestSeedData.insertProduct(
            db = db,
            name = "Flat White",
            productFamily = "COFFEE",
            price = 4.50
        )

        val items = listOf(
            TestSeedData.buildCartItem(
                product = product,
                quantity = 2,
                unitPrice = 4.50
            )
        )

        val result = repository.submitOrder(
            customerId = customerId,
            items = items,
            paymentType = "CARD",
            totalAmount = 9.0,
            beansToSpend = 10,
            beansToEarn = 3,
            saveNewCardForFuture = true,
            cardNickname = "",
            cardholderName = "Andrew Dul",
            cardNumber = "4242 1111 2222 3333",
            expiryText = "12/29"
        )

        assertTrue(result is CheckoutRepository.CheckoutSubmissionResult.Success)

        val orderId = (result as CheckoutRepository.CheckoutSubmissionResult.Success).orderId
        val order = db.orderDao().getById(orderId)
        val payment = db.paymentDao().getLatestForOrder(orderId)
        val displayItems = db.orderItemDao().getDisplayItemsForOrder(orderId)
        val savedCards = db.customerPaymentCardDao().getAllForCustomer(customerId)
        val updatedCustomer = db.customerDao().getById(customerId)

        assertEquals("PLACED", order?.status)
        assertEquals("CARD", payment?.paymentType)
        assertEquals(9.0, payment?.amount ?: 0.0, 0.0001)

        assertEquals(1, displayItems.size)
        assertEquals(2, displayItems.first().quantity)
        assertEquals("Flat White", displayItems.first().productName)

        assertEquals(1, savedCards.size)
        assertEquals("VISA ending 3333", savedCards.first().nickname)
        assertTrue(savedCards.first().isDefault)

        assertEquals(13, updatedCustomer?.beansBalance)
        assertEquals(3, updatedCustomer?.beansBoosterProgress)
        assertEquals(0, updatedCustomer?.pendingBeansBoosters)
    }

    @Test
    fun submitOrder_returnsError_whenCustomerDoesNotHaveEnoughBeans() = runBlocking {
        val customerId = TestSeedData.insertCustomer(
            db = db,
            email = "beans@example.com",
            beansBalance = 2
        )

        val product = TestSeedData.insertProduct(
            db = db,
            name = "Latte",
            productFamily = "COFFEE",
            price = 4.20
        )

        val items = listOf(
            TestSeedData.buildCartItem(
                product = product,
                quantity = 1,
                unitPrice = 4.20
            )
        )

        val result = repository.submitOrder(
            customerId = customerId,
            items = items,
            paymentType = "CARD",
            totalAmount = 4.20,
            beansToSpend = 5,
            beansToEarn = 1,
            saveNewCardForFuture = true,
            cardNickname = "",
            cardholderName = "Andrew Dul",
            cardNumber = "4242 1111 2222 3333",
            expiryText = "12/29"
        )

        assertTrue(result is CheckoutRepository.CheckoutSubmissionResult.Error)

        val error = result as CheckoutRepository.CheckoutSubmissionResult.Error
        assertEquals(
            "You do not have enough beans for the selected rewards.",
            error.message
        )

        val cards = db.customerPaymentCardDao().getAllForCustomer(customerId)
        val customerOrders = db.orderDao().observeByCustomer(customerId).first()
        val unchangedCustomer = db.customerDao().getById(customerId)

        assertTrue(cards.isEmpty())
        assertTrue(customerOrders.isEmpty())
        assertEquals(2, unchangedCustomer?.beansBalance)
    }
}