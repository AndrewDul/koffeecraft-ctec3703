package uk.ac.dmu.koffeecraft.ui.admin.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.querymodel.ProductCommentInsight
import uk.ac.dmu.koffeecraft.data.querymodel.ProductFavouriteInsight
import uk.ac.dmu.koffeecraft.data.querymodel.ProductRatingInsight
import uk.ac.dmu.koffeecraft.data.repository.AdminHomeDashboardData
import uk.ac.dmu.koffeecraft.data.repository.AdminHomeRepository
import java.util.Calendar
import java.util.Locale

data class AdminHomeUiState(
    val isLoading: Boolean = false,
    val ordersTodayValue: String = "0",
    val revenueTodayValue: String = "£0.00",
    val pendingOrdersValue: String = "0",
    val averageRatingValue: String = "—",
    val promoOptInRateValue: String = "—",
    val activeCustomersValue: String = "0",
    val availableProductsValue: String = "0",
    val disabledProductsValue: String = "0",
    val rewardEnabledProductsValue: String = "0",
    val newArrivalsValue: String = "0",
    val attentionPending: String = "• No orders are currently waiting in the active queue.",
    val attentionDisabled: String = "• All menu products are currently active.",
    val attentionHiddenFeedback: String = "• No customer comments are currently hidden.",
    val attentionPromoCoverage: String = "• No customer accounts have been registered yet.",
    val topRatedItems: List<AdminHomeCarouselItem> = emptyList(),
    val mostLovedItems: List<AdminHomeCarouselItem> = emptyList(),
    val mostCommentedItems: List<AdminHomeCarouselItem> = emptyList()
)

class AdminHomeViewModel(
    private val repository: AdminHomeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AdminHomeUiState())
    val state: StateFlow<AdminHomeUiState> = _state

    fun refreshDashboard() {
        _state.value = _state.value.copy(isLoading = true)

        viewModelScope.launch {
            val (startOfDay, endOfDay) = buildTodayRange()
            val dashboard = repository.loadDashboard(startOfDay, endOfDay)

            _state.value = mapToUiState(dashboard)
        }
    }

    private fun mapToUiState(data: AdminHomeDashboardData): AdminHomeUiState {
        return AdminHomeUiState(
            isLoading = false,
            ordersTodayValue = data.ordersToday.toString(),
            revenueTodayValue = formatCurrency(data.revenueToday),
            pendingOrdersValue = data.pendingOrders.toString(),
            averageRatingValue = formatAverage(data),
            promoOptInRateValue = formatPromoRate(
                totalCustomers = data.totalCustomers,
                promoOptInCustomers = data.promoOptInCustomers
            ),
            activeCustomersValue = data.activeCustomers.toString(),
            availableProductsValue = data.activeProducts.toString(),
            disabledProductsValue = data.disabledProducts.toString(),
            rewardEnabledProductsValue = data.rewardEnabledProducts.toString(),
            newArrivalsValue = data.newArrivals.toString(),
            attentionPending = if (data.pendingOrders > 0) {
                "• ${data.pendingOrders} orders are still waiting in the active queue."
            } else {
                "• No orders are currently waiting in the active queue."
            },
            attentionDisabled = if (data.disabledProducts > 0) {
                "• ${data.disabledProducts} menu products are currently disabled."
            } else {
                "• All menu products are currently active."
            },
            attentionHiddenFeedback = if (data.feedbackOverview.hiddenComments > 0) {
                "• ${data.feedbackOverview.hiddenComments} customer comments are currently hidden."
            } else {
                "• No customer comments are currently hidden."
            },
            attentionPromoCoverage = if (data.totalCustomers > 0) {
                "• ${formatPromoRate(data.totalCustomers, data.promoOptInCustomers)} of customers accept promotional messaging."
            } else {
                "• No customer accounts have been registered yet."
            },
            topRatedItems = mapTopRated(data.topRated),
            mostLovedItems = mapMostLoved(data.mostLoved),
            mostCommentedItems = mapMostCommented(data.mostCommented)
        )
    }

    private fun mapTopRated(items: List<ProductRatingInsight>): List<AdminHomeCarouselItem> {
        if (items.isEmpty()) {
            return listOf(
                AdminHomeCarouselItem(
                    rankLabel = "#",
                    productName = "No ratings yet",
                    primaryText = "Waiting for customer feedback",
                    secondaryText = "Top rated products will appear here"
                )
            )
        }

        return items.mapIndexed { index, item ->
            AdminHomeCarouselItem(
                rankLabel = "#${index + 1}",
                productName = item.productName,
                primaryText = "Average rating: ${formatRating(item.averageRating)} / 5",
                secondaryText = "${item.ratingCount} ratings"
            )
        }
    }

    private fun mapMostLoved(items: List<ProductFavouriteInsight>): List<AdminHomeCarouselItem> {
        if (items.isEmpty()) {
            return listOf(
                AdminHomeCarouselItem(
                    rankLabel = "#",
                    productName = "No favourites yet",
                    primaryText = "Waiting for customer hearts",
                    secondaryText = "Most loved products will appear here"
                )
            )
        }

        return items.mapIndexed { index, item ->
            AdminHomeCarouselItem(
                rankLabel = "#${index + 1}",
                productName = item.productName,
                primaryText = "${item.favouriteCount} hearts",
                secondaryText = "Loved products by customer favourites"
            )
        }
    }

    private fun mapMostCommented(items: List<ProductCommentInsight>): List<AdminHomeCarouselItem> {
        if (items.isEmpty()) {
            return listOf(
                AdminHomeCarouselItem(
                    rankLabel = "#",
                    productName = "No comments yet",
                    primaryText = "Waiting for written feedback",
                    secondaryText = "Most commented products will appear here"
                )
            )
        }

        return items.mapIndexed { index, item ->
            AdminHomeCarouselItem(
                rankLabel = "#${index + 1}",
                productName = item.productName,
                primaryText = "${item.commentCount} comments",
                secondaryText = "Average rating: ${formatRating(item.averageRating)} / 5"
            )
        }
    }

    private fun formatAverage(data: AdminHomeDashboardData): String {
        return if (data.feedbackOverview.totalReviews <= 0) {
            "—"
        } else {
            String.format(Locale.UK, "%.1f", data.feedbackOverview.overallAverage)
        }
    }

    private fun formatRating(value: Double): String {
        return String.format(Locale.UK, "%.1f", value)
    }

    private fun formatCurrency(value: Double): String {
        return String.format(Locale.UK, "£%.2f", value)
    }

    private fun formatPromoRate(totalCustomers: Int, promoOptInCustomers: Int): String {
        if (totalCustomers <= 0) return "—"
        val percentage = ((promoOptInCustomers.toDouble() / totalCustomers.toDouble()) * 100).toInt()
        return "$percentage%"
    }

    private fun buildTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val start = calendar.timeInMillis

        calendar.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        return start to calendar.timeInMillis
    }

    class Factory(
        private val repository: AdminHomeRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AdminHomeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AdminHomeViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}