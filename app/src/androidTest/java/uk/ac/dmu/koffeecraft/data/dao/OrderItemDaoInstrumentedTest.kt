package uk.ac.dmu.koffeecraft.data.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import uk.ac.dmu.koffeecraft.testsupport.BaseInstrumentedDatabaseTest
import uk.ac.dmu.koffeecraft.testsupport.TestSeedData

@RunWith(AndroidJUnit4::class)
class OrderItemDaoInstrumentedTest : BaseInstrumentedDatabaseTest() {

    @Test
    fun getReorderItems_returnsOnlyPaidAndAvailableProducts() = runBlocking {
        val customerId = TestSeedData.insertCustomer(
            db = db,
            email = "reorder@example.com"
        )

        val activeProduct = TestSeedData.insertProduct(
            db = db,
            name = "Flat White",
            productFamily = "COFFEE",
            price = 4.50,
            isActive = true
        )

        val inactiveProduct = TestSeedData.insertProduct(
            db = db,
            name = "Old Brownie",
            productFamily = "CAKE",
            price = 3.80,
            isActive = false
        )

        val rewardProduct = TestSeedData.insertProduct(
            db = db,
            name = "Reward Mug",
            productFamily = "MERCH",
            price = 12.99,
            isActive = true,
            rewardEnabled = true
        )

        val orderId = TestSeedData.insertOrder(
            db = db,
            customerId = customerId,
            totalAmount = 8.30
        )

        db.orderItemDao().insertAll(
            listOf(
                TestSeedData.buildOrderItem(
                    orderId = orderId,
                    productId = activeProduct.productId,
                    quantity = 2,
                    unitPrice = 4.15
                ),
                TestSeedData.buildOrderItem(
                    orderId = orderId,
                    productId = inactiveProduct.productId,
                    quantity = 1,
                    unitPrice = 3.80
                ),
                TestSeedData.buildOrderItem(
                    orderId = orderId,
                    productId = rewardProduct.productId,
                    quantity = 1,
                    unitPrice = 0.0
                )
            )
        )

        val reorderItems = db.orderItemDao().getReorderItems(orderId)

        assertEquals(1, reorderItems.size)
        assertEquals(activeProduct.productId, reorderItems.first().product.productId)
        assertEquals(2, reorderItems.first().quantity)
    }

    @Test
    fun feedbackQueries_andDisplayItems_returnExpectedSnapshotsAndNextItem() = runBlocking {
        val customerId = TestSeedData.insertCustomer(
            db = db,
            email = "feedbackitems@example.com"
        )

        val coffee = TestSeedData.insertProduct(
            db = db,
            name = "Flat White",
            productFamily = "COFFEE",
            price = 4.50
        )

        val cake = TestSeedData.insertProduct(
            db = db,
            name = "Cheesecake",
            productFamily = "CAKE",
            price = 5.50
        )

        val orderId = TestSeedData.insertOrder(
            db = db,
            customerId = customerId,
            totalAmount = 10.0
        )

        db.orderItemDao().insertAll(
            listOf(
                TestSeedData.buildOrderItem(
                    orderId = orderId,
                    productId = coffee.productId,
                    quantity = 1,
                    unitPrice = 4.50,
                    selectedOptionLabel = "Large",
                    selectedOptionSizeValue = 450,
                    selectedOptionSizeUnit = "ml",
                    selectedAddOnsSummary = "Extra shot",
                    estimatedCalories = 180,
                    productNameSnapshot = coffee.name,
                    productDescriptionSnapshot = coffee.description
                ),
                TestSeedData.buildOrderItem(
                    orderId = orderId,
                    productId = cake.productId,
                    quantity = 1,
                    unitPrice = 5.50,
                    productNameSnapshot = cake.name,
                    productDescriptionSnapshot = cake.description
                )
            )
        )

        val feedbackItems = db.orderItemDao().getFeedbackItemsForOrder(orderId)
        val firstOrderItemId = feedbackItems.first().orderItemId
        val secondOrderItemId = feedbackItems.last().orderItemId

        TestSeedData.insertFeedback(
            db = db,
            orderItemId = firstOrderItemId,
            customerId = customerId,
            rating = 5,
            comment = "Excellent"
        )

        val singleFeedbackItem = db.orderItemDao().getFeedbackItemByOrderItemId(firstOrderItemId)
        val nextUnreviewed = db.orderItemDao().getNextUnreviewedItem(orderId)
        val displayItems = db.orderItemDao().getDisplayItemsForOrder(orderId)

        assertEquals(2, feedbackItems.size)
        assertNotNull(singleFeedbackItem)
        assertEquals("Flat White", singleFeedbackItem?.productName)
        assertEquals(5, singleFeedbackItem?.rating)
        assertEquals("Excellent", singleFeedbackItem?.comment)
        assertTrue(singleFeedbackItem?.isCrafted == true)

        assertNotNull(nextUnreviewed)
        assertEquals(secondOrderItemId, nextUnreviewed?.orderItemId)

        assertEquals(2, displayItems.size)
        assertEquals("Flat White", displayItems.first().productName)
        assertTrue(displayItems.first().isCrafted)
    }
}