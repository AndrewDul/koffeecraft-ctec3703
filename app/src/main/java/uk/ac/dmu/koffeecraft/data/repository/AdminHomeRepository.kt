package uk.ac.dmu.koffeecraft.data.repository

import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.querymodel.AdminFeedbackOverview
import uk.ac.dmu.koffeecraft.data.querymodel.ProductCommentInsight
import uk.ac.dmu.koffeecraft.data.querymodel.ProductFavouriteInsight
import uk.ac.dmu.koffeecraft.data.querymodel.ProductRatingInsight
data class AdminHomeDashboardData(
    val ordersToday: Int,
    val revenueToday: Double,
    val pendingOrders: Int,
    val feedbackOverview: AdminFeedbackOverview,
    val activeCustomers: Int,
    val totalCustomers: Int,
    val promoOptInCustomers: Int,
    val activeProducts: Int,
    val disabledProducts: Int,
    val rewardEnabledProducts: Int,
    val newArrivals: Int,
    val topRated: List<ProductRatingInsight>,
    val mostLoved: List<ProductFavouriteInsight>,
    val mostCommented: List<ProductCommentInsight>
)

class AdminHomeRepository(
    private val db: KoffeeCraftDatabase
) {

    suspend fun loadDashboard(
        startOfDay: Long,
        endOfDay: Long
    ): AdminHomeDashboardData {
        val ordersToday = db.orderDao().countOrdersCreatedBetween(startOfDay, endOfDay)
        val revenueToday = db.orderDao().getRevenueCreatedBetween(startOfDay, endOfDay)
        val pendingOrders = db.orderDao().countPendingOrders()

        val feedbackOverview = db.feedbackDao().getAdminFeedbackOverview()
        val topRated = db.feedbackDao().getTopRatedProducts()
        val mostCommented = db.feedbackDao().getMostCommentedProducts()
        val mostLoved = db.favouriteDao().getTopFavouriteProducts()

        val activeCustomers = db.customerDao().countActiveCustomers()
        val totalCustomers = db.customerDao().countAllCustomers()
        val promoOptInCustomers = db.customerDao().countPromoOptInCustomers()

        val activeProducts = db.productDao().countActiveProducts()
        val disabledProducts = db.productDao().countDisabledProducts()
        val rewardEnabledProducts = db.productDao().countActiveRewardEnabledProducts()
        val newArrivals = db.productDao().countActiveNewProducts()

        return AdminHomeDashboardData(
            ordersToday = ordersToday,
            revenueToday = revenueToday,
            pendingOrders = pendingOrders,
            feedbackOverview = feedbackOverview,
            activeCustomers = activeCustomers,
            totalCustomers = totalCustomers,
            promoOptInCustomers = promoOptInCustomers,
            activeProducts = activeProducts,
            disabledProducts = disabledProducts,
            rewardEnabledProducts = rewardEnabledProducts,
            newArrivals = newArrivals,
            topRated = topRated,
            mostLoved = mostLoved,
            mostCommented = mostCommented
        )
    }
}