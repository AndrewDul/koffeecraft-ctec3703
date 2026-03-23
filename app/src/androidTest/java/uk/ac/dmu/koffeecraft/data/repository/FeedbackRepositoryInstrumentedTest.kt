package uk.ac.dmu.koffeecraft.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uk.ac.dmu.koffeecraft.testsupport.BaseInstrumentedDatabaseTest
import uk.ac.dmu.koffeecraft.testsupport.TestSeedData

@RunWith(AndroidJUnit4::class)
class FeedbackRepositoryInstrumentedTest : BaseInstrumentedDatabaseTest() {

    private lateinit var repository: FeedbackRepository

    @Before
    fun setUpRepository() {
        repository = FeedbackRepository(db)
    }

    @Test
    fun saveFeedback_savesTrimmedClampedFeedback_andReturnsNextItem() = runBlocking {
        val customerId = TestSeedData.insertCustomer(db = db, email = "feedback@example.com")
        val productOne = TestSeedData.insertProduct(db = db, name = "Flat White")
        val productTwo = TestSeedData.insertProduct(db = db, name = "Cheesecake", productFamily = "CAKE", price = 5.50)

        val orderId = TestSeedData.insertOrder(
            db = db,
            customerId = customerId,
            totalAmount = 10.0
        )

        db.orderItemDao().insertAll(
            listOf(
                TestSeedData.buildOrderItem(
                    orderId = orderId,
                    productId = productOne.productId,
                    quantity = 1,
                    unitPrice = 4.50,
                    productNameSnapshot = productOne.name,
                    productDescriptionSnapshot = productOne.description
                ),
                TestSeedData.buildOrderItem(
                    orderId = orderId,
                    productId = productTwo.productId,
                    quantity = 1,
                    unitPrice = 5.50,
                    productNameSnapshot = productTwo.name,
                    productDescriptionSnapshot = productTwo.description
                )
            )
        )

        val feedbackItems = db.orderItemDao().getFeedbackItemsForOrder(orderId)
        val firstOrderItemId = feedbackItems[0].orderItemId
        val secondOrderItemId = feedbackItems[1].orderItemId

        val result = repository.saveFeedback(
            orderId = orderId,
            orderItemId = firstOrderItemId,
            customerId = customerId,
            rating = 7,
            comment = "  Great drink  "
        )

        assertTrue(result is ProductFeedbackSaveResult.NextItem)

        val nextResult = result as ProductFeedbackSaveResult.NextItem
        val storedFeedback = db.feedbackDao().getByOrderItemId(firstOrderItemId)

        assertEquals(secondOrderItemId, nextResult.nextItem.orderItemId)
        assertEquals("Thanks for your rating and feedback!", nextResult.message)

        assertEquals(5, storedFeedback?.rating)
        assertEquals("Great drink", storedFeedback?.comment)
    }

    @Test
    fun saveFeedback_returnsCompleted_forFinalItem_andBlankCommentUsesRatingMessage() = runBlocking {
        val customerId = TestSeedData.insertCustomer(db = db, email = "singlefeedback@example.com")
        val product = TestSeedData.insertProduct(db = db, name = "Latte")

        val orderId = TestSeedData.insertOrder(
            db = db,
            customerId = customerId,
            totalAmount = 4.20
        )

        db.orderItemDao().insertAll(
            listOf(
                TestSeedData.buildOrderItem(
                    orderId = orderId,
                    productId = product.productId,
                    quantity = 1,
                    unitPrice = 4.20,
                    productNameSnapshot = product.name,
                    productDescriptionSnapshot = product.description
                )
            )
        )

        val orderItemId = db.orderItemDao().getFeedbackItemsForOrder(orderId).single().orderItemId

        val result = repository.saveFeedback(
            orderId = orderId,
            orderItemId = orderItemId,
            customerId = customerId,
            rating = 4,
            comment = "   "
        )

        assertTrue(result is ProductFeedbackSaveResult.Completed)

        val completed = result as ProductFeedbackSaveResult.Completed
        val storedFeedback = db.feedbackDao().getByOrderItemId(orderItemId)

        assertEquals(orderId, completed.orderId)
        assertEquals("Thanks for your rating!", completed.message)
        assertEquals("", storedFeedback?.comment)
        assertEquals(4, storedFeedback?.rating)
    }

    @Test
    fun saveFeedback_returnsError_whenPurchasedItemCannotBeFound() = runBlocking {
        val customerId = TestSeedData.insertCustomer(db = db, email = "missingfeedback@example.com")

        val result = repository.saveFeedback(
            orderId = 999L,
            orderItemId = 999L,
            customerId = customerId,
            rating = 5,
            comment = "Nice"
        )

        assertTrue(result is ProductFeedbackSaveResult.Error)

        val error = result as ProductFeedbackSaveResult.Error
        assertEquals("This purchased product could not be found.", error.message)
    }
}