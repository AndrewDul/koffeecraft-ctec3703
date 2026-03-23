package uk.ac.dmu.koffeecraft.data.repository

import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.Feedback
import uk.ac.dmu.koffeecraft.data.querymodel.OrderFeedbackItem
sealed interface ProductFeedbackSaveResult {
    data class NextItem(
        val nextItem: OrderFeedbackItem,
        val message: String
    ) : ProductFeedbackSaveResult

    data class Completed(
        val orderId: Long,
        val message: String
    ) : ProductFeedbackSaveResult

    data class Error(
        val message: String
    ) : ProductFeedbackSaveResult
}

class FeedbackRepository(
    private val db: KoffeeCraftDatabase
) {

    suspend fun loadFeedbackItems(orderId: Long): List<OrderFeedbackItem> {
        return db.orderItemDao().getFeedbackItemsForOrder(orderId)
    }

    suspend fun loadFeedbackItem(orderItemId: Long): OrderFeedbackItem? {
        return db.orderItemDao().getFeedbackItemByOrderItemId(orderItemId)
    }

    suspend fun saveFeedback(
        orderId: Long,
        orderItemId: Long,
        customerId: Long,
        rating: Int,
        comment: String
    ): ProductFeedbackSaveResult {
        val item = db.orderItemDao().getFeedbackItemByOrderItemId(orderItemId)
            ?: return ProductFeedbackSaveResult.Error("This purchased product could not be found.")

        val now = System.currentTimeMillis()
        val existing = db.feedbackDao().getByOrderItemId(item.orderItemId)

        db.feedbackDao().upsert(
            Feedback(
                feedbackId = existing?.feedbackId ?: 0,
                orderItemId = item.orderItemId,
                customerId = customerId,
                rating = rating.coerceIn(1, 5),
                comment = comment.trim(),
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
        )

        val nextItem = db.orderItemDao().getNextUnreviewedItem(orderId)

        val message = if (comment.isBlank()) {
            "Thanks for your rating!"
        } else {
            "Thanks for your rating and feedback!"
        }

        return if (nextItem != null) {
            ProductFeedbackSaveResult.NextItem(
                nextItem = nextItem,
                message = message
            )
        } else {
            ProductFeedbackSaveResult.Completed(
                orderId = orderId,
                message = message
            )
        }
    }
}