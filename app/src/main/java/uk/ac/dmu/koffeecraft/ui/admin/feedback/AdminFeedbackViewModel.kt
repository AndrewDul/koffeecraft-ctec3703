package uk.ac.dmu.koffeecraft.ui.admin.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.repository.AdminFeedbackRepository
import java.util.Locale
import uk.ac.dmu.koffeecraft.data.querymodel.AdminFeedbackItem
import uk.ac.dmu.koffeecraft.data.querymodel.AdminFeedbackOverview
import uk.ac.dmu.koffeecraft.data.querymodel.AdminFeedbackRatingBreakdown
enum class RatingFilter { ALL, ONE, TWO, THREE, FOUR, FIVE }
enum class CommentFilter { ALL, WITH_COMMENT, WITHOUT_COMMENT }
enum class FeedbackCategoryFilter { ALL, COFFEE, CAKE }
enum class FeedbackSortMode { NEWEST, OLDEST }

data class AdminFeedbackUiState(
    val allItems: List<AdminFeedbackItem> = emptyList(),
    val filteredItems: List<AdminFeedbackItem> = emptyList(),
    val overview: AdminFeedbackOverview = AdminFeedbackOverview(
        overallAverage = 0.0,
        coffeeAverage = 0.0,
        cakeAverage = 0.0,
        totalReviews = 0,
        reviewsWithComments = 0,
        hiddenComments = 0
    ),
    val breakdown: List<AdminFeedbackRatingBreakdown> = emptyList(),
    val currentRatingFilter: RatingFilter = RatingFilter.ALL,
    val currentCommentFilter: CommentFilter = CommentFilter.ALL,
    val currentCategoryFilter: FeedbackCategoryFilter = FeedbackCategoryFilter.ALL,
    val currentSortMode: FeedbackSortMode = FeedbackSortMode.NEWEST,
    val feedCountText: String = "No reviews match the current filters",
    val showEmpty: Boolean = true,
    val overallAverageText: String = "—",
    val coffeeAverageText: String = "—",
    val cakeAverageText: String = "—",
    val totalReviewsText: String = "0",
    val commentsRateText: String = "0%",
    val hiddenCommentsText: String = "0",
    val progressFive: Int = 0,
    val progressFour: Int = 0,
    val progressThree: Int = 0,
    val progressTwo: Int = 0,
    val progressOne: Int = 0,
    val countFive: String = "0",
    val countFour: String = "0",
    val countThree: String = "0",
    val countTwo: String = "0",
    val countOne: String = "0"
)

class AdminFeedbackViewModel(
    private val repository: AdminFeedbackRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AdminFeedbackUiState())
    val state: StateFlow<AdminFeedbackUiState> = _state

    private var observeJob: Job? = null

    fun start() {
        if (observeJob != null) return

        observeJob = viewModelScope.launch {
            repository.observeFeedbackData().collect { data ->
                _state.update { current ->
                    publishState(
                        current.copy(
                            allItems = data.items,
                            overview = data.overview,
                            breakdown = data.breakdown
                        )
                    )
                }
            }
        }
    }

    fun setRatingFilter(filter: RatingFilter) {
        _state.update { current ->
            publishState(current.copy(currentRatingFilter = filter))
        }
    }

    fun setCommentFilter(filter: CommentFilter) {
        _state.update { current ->
            publishState(current.copy(currentCommentFilter = filter))
        }
    }

    fun setCategoryFilter(filter: FeedbackCategoryFilter) {
        _state.update { current ->
            publishState(current.copy(currentCategoryFilter = filter))
        }
    }

    fun setSortMode(sortMode: FeedbackSortMode) {
        _state.update { current ->
            publishState(current.copy(currentSortMode = sortMode))
        }
    }

    fun deleteFeedback(feedbackId: Long) {
        viewModelScope.launch {
            repository.deleteFeedback(feedbackId)
        }
    }

    fun toggleHideState(item: AdminFeedbackItem) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (item.isHidden) {
                repository.unhideFeedback(item.feedbackId, now)
            } else {
                repository.hideFeedback(item.feedbackId, now)
            }
        }
    }

    private fun publishState(current: AdminFeedbackUiState): AdminFeedbackUiState {
        val filtered = current.allItems.filter { item ->
            val matchesRating = when (current.currentRatingFilter) {
                RatingFilter.ALL -> true
                RatingFilter.ONE -> item.rating == 1
                RatingFilter.TWO -> item.rating == 2
                RatingFilter.THREE -> item.rating == 3
                RatingFilter.FOUR -> item.rating == 4
                RatingFilter.FIVE -> item.rating == 5
            }

            val hasComment = item.comment.isNotBlank()

            val matchesComment = when (current.currentCommentFilter) {
                CommentFilter.ALL -> true
                CommentFilter.WITH_COMMENT -> hasComment
                CommentFilter.WITHOUT_COMMENT -> !hasComment
            }

            val matchesCategory = when (current.currentCategoryFilter) {
                FeedbackCategoryFilter.ALL -> true
                FeedbackCategoryFilter.COFFEE -> item.productCategory.equals("COFFEE", ignoreCase = true)
                FeedbackCategoryFilter.CAKE -> item.productCategory.equals("CAKE", ignoreCase = true)
            }

            matchesRating && matchesComment && matchesCategory
        }

        val sorted = when (current.currentSortMode) {
            FeedbackSortMode.NEWEST -> filtered.sortedByDescending { it.updatedAt }
            FeedbackSortMode.OLDEST -> filtered.sortedBy { it.updatedAt }
        }

        val counts = current.breakdown.associate { it.rating to it.reviewCount }
        val totalReviews = current.overview.totalReviews
        val commentsPercent = if (totalReviews == 0) {
            0
        } else {
            ((current.overview.reviewsWithComments.toDouble() / totalReviews.toDouble()) * 100).toInt()
        }

        return current.copy(
            filteredItems = sorted,
            feedCountText = if (sorted.isEmpty()) {
                "No reviews match the current filters"
            } else {
                "Showing ${sorted.size} review${if (sorted.size == 1) "" else "s"}"
            },
            showEmpty = sorted.isEmpty(),
            overallAverageText = formatAverage(current.overview.overallAverage, totalReviews),
            coffeeAverageText = formatAverage(current.overview.coffeeAverage, totalReviews),
            cakeAverageText = formatAverage(current.overview.cakeAverage, totalReviews),
            totalReviewsText = totalReviews.toString(),
            commentsRateText = "$commentsPercent%",
            hiddenCommentsText = current.overview.hiddenComments.toString(),
            progressFive = percentage(counts[5] ?: 0, totalReviews),
            progressFour = percentage(counts[4] ?: 0, totalReviews),
            progressThree = percentage(counts[3] ?: 0, totalReviews),
            progressTwo = percentage(counts[2] ?: 0, totalReviews),
            progressOne = percentage(counts[1] ?: 0, totalReviews),
            countFive = (counts[5] ?: 0).toString(),
            countFour = (counts[4] ?: 0).toString(),
            countThree = (counts[3] ?: 0).toString(),
            countTwo = (counts[2] ?: 0).toString(),
            countOne = (counts[1] ?: 0).toString()
        )
    }

    private fun percentage(count: Int, total: Int): Int {
        return if (total == 0) 0 else ((count.toDouble() / total.toDouble()) * 100).toInt()
    }

    private fun formatAverage(value: Double, totalReviews: Int): String {
        return if (totalReviews == 0 || value == 0.0) {
            "—"
        } else {
            String.format(Locale.UK, "%.1f", value)
        }
    }

    class Factory(
        private val repository: AdminFeedbackRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AdminFeedbackViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AdminFeedbackViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}