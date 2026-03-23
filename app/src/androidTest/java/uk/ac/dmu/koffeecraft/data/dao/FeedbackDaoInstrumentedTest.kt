package uk.ac.dmu.koffeecraft.data.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import uk.ac.dmu.koffeecraft.testsupport.BaseInstrumentedDatabaseTest
import uk.ac.dmu.koffeecraft.testsupport.TestSeedData

@RunWith(AndroidJUnit4::class)
class FeedbackDaoInstrumentedTest : BaseInstrumentedDatabaseTest() {

    @Test
    fun getAdminFeedbackOverview_andBreakdown_returnExpectedAggregates() = runBlocking {
        val customerId = TestSeedData.insertCustomer(
            db = db,
            email = "overviewfeedback@example.com"
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

        val items = db.orderItemDao().getFeedbackItemsForOrder(orderId)

        TestSeedData.insertFeedback(
            db = db,
            orderItemId = items[0].orderItemId,
            customerId = customerId,
            rating = 5,
            comment = "Great coffee"
        )

        TestSeedData.insertFeedback(
            db = db,
            orderItemId = items[1].orderItemId,
            customerId = customerId,
            rating = 3,
            comment = ""
        )

        val overview = db.feedbackDao().getAdminFeedbackOverview()
        val breakdown = db.feedbackDao().getAdminFeedbackBreakdown()

        assertEquals(4.0, overview.overallAverage, 0.0001)
        assertEquals(5.0, overview.coffeeAverage, 0.0001)
        assertEquals(3.0, overview.cakeAverage, 0.0001)
        assertEquals(2, overview.totalReviews)
        assertEquals(1, overview.reviewsWithComments)
        assertEquals(0, overview.hiddenComments)

        assertEquals(2, breakdown.size)
        assertEquals(5, breakdown[0].rating)
        assertEquals(1, breakdown[0].reviewCount)
        assertEquals(3, breakdown[1].rating)
        assertEquals(1, breakdown[1].reviewCount)
    }

    @Test
    fun observeAllForAdmin_andHideUnhideComment_updateModerationFlags() = runBlocking {
        val customerId = TestSeedData.insertCustomer(
            db = db,
            email = "moderationfeedback@example.com"
        )

        val product = TestSeedData.insertProduct(
            db = db,
            name = "Latte",
            productFamily = "COFFEE",
            price = 4.20
        )

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

        TestSeedData.insertFeedback(
            db = db,
            orderItemId = orderItemId,
            customerId = customerId,
            rating = 4,
            comment = "Nice drink"
        )

        val adminRows = db.feedbackDao().observeAllForAdmin().first()
        val adminRow = adminRows.single()

        assertEquals("Latte", adminRow.productName)
        assertEquals("COFFEE", adminRow.productCategory)
        assertFalse(adminRow.isHidden)
        assertFalse(adminRow.isModerated)

        db.feedbackDao().hideComment(
            feedbackId = adminRow.feedbackId,
            updatedAt = 999L
        )

        val hidden = db.feedbackDao().getByOrderItemId(orderItemId)
        assertTrue(hidden?.isHidden == true)
        assertTrue(hidden?.isModerated == true)
        assertEquals(999L, hidden?.updatedAt)

        db.feedbackDao().unhideComment(
            feedbackId = adminRow.feedbackId,
            updatedAt = 1000L
        )

        val unhidden = db.feedbackDao().getByOrderItemId(orderItemId)
        assertFalse(unhidden?.isHidden ?: true)
        assertTrue(unhidden?.isModerated == true)
        assertEquals(1000L, unhidden?.updatedAt)
    }
}