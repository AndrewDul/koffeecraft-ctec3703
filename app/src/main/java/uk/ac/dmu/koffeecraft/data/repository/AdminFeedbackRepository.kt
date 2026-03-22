package uk.ac.dmu.koffeecraft.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uk.ac.dmu.koffeecraft.data.dao.AdminFeedbackItem
import uk.ac.dmu.koffeecraft.data.dao.AdminFeedbackOverview
import uk.ac.dmu.koffeecraft.data.dao.AdminFeedbackRatingBreakdown
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase

data class AdminFeedbackData(
    val items: List<AdminFeedbackItem>,
    val overview: AdminFeedbackOverview,
    val breakdown: List<AdminFeedbackRatingBreakdown>
)

class AdminFeedbackRepository(
    private val db: KoffeeCraftDatabase
) {

    fun observeFeedbackData(): Flow<AdminFeedbackData> {
        return db.feedbackDao().observeAllForAdmin().map { items ->
            AdminFeedbackData(
                items = items,
                overview = db.feedbackDao().getAdminFeedbackOverview(),
                breakdown = db.feedbackDao().getAdminFeedbackBreakdown()
            )
        }
    }

    suspend fun deleteFeedback(feedbackId: Long) {
        db.feedbackDao().deleteById(feedbackId)
    }

    suspend fun hideFeedback(feedbackId: Long, updatedAt: Long) {
        db.feedbackDao().hideComment(feedbackId, updatedAt)
    }

    suspend fun unhideFeedback(feedbackId: Long, updatedAt: Long) {
        db.feedbackDao().unhideComment(feedbackId, updatedAt)
    }
}