package uk.ac.dmu.koffeecraft.ui.admin.home

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.dao.AdminFeedbackOverview
import uk.ac.dmu.koffeecraft.data.dao.ProductCommentInsight
import uk.ac.dmu.koffeecraft.data.dao.ProductFavouriteInsight
import uk.ac.dmu.koffeecraft.data.dao.ProductRatingInsight
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import java.util.Calendar
import java.util.Locale

class AdminHomeFragment : Fragment(R.layout.fragment_admin_home) {

    private lateinit var topRatedAdapter: AdminHomeCarouselAdapter
    private lateinit var mostLovedAdapter: AdminHomeCarouselAdapter
    private lateinit var mostCommentedAdapter: AdminHomeCarouselAdapter

    private lateinit var tvOrdersTodayValue: TextView
    private lateinit var tvRevenueTodayValue: TextView
    private lateinit var tvPendingOrdersValue: TextView
    private lateinit var tvAverageRatingValue: TextView
    private lateinit var tvPromoOptInRateValue: TextView
    private lateinit var tvActiveCustomersValue: TextView

    private lateinit var tvAvailableProductsValue: TextView
    private lateinit var tvDisabledProductsValue: TextView
    private lateinit var tvRewardEnabledProductsValue: TextView
    private lateinit var tvNewArrivalsValue: TextView

    private lateinit var tvAttentionPending: TextView
    private lateinit var tvAttentionDisabled: TextView
    private lateinit var tvAttentionHiddenFeedback: TextView
    private lateinit var tvAttentionPromoCoverage: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupQuickActions(view)
        setupCarousels(view)
        loadDashboard()
    }

    override fun onResume() {
        super.onResume()
        loadDashboard()
    }

    private fun bindViews(view: View) {
        tvOrdersTodayValue = view.findViewById(R.id.tvOrdersTodayValue)
        tvRevenueTodayValue = view.findViewById(R.id.tvRevenueTodayValue)
        tvPendingOrdersValue = view.findViewById(R.id.tvPendingOrdersValue)
        tvAverageRatingValue = view.findViewById(R.id.tvAverageRatingValue)
        tvPromoOptInRateValue = view.findViewById(R.id.tvPromoOptInRateValue)
        tvActiveCustomersValue = view.findViewById(R.id.tvActiveCustomersValue)

        tvAvailableProductsValue = view.findViewById(R.id.tvAvailableProductsValue)
        tvDisabledProductsValue = view.findViewById(R.id.tvDisabledProductsValue)
        tvRewardEnabledProductsValue = view.findViewById(R.id.tvRewardEnabledProductsValue)
        tvNewArrivalsValue = view.findViewById(R.id.tvNewArrivalsValue)

        tvAttentionPending = view.findViewById(R.id.tvAttentionPending)
        tvAttentionDisabled = view.findViewById(R.id.tvAttentionDisabled)
        tvAttentionHiddenFeedback = view.findViewById(R.id.tvAttentionHiddenFeedback)
        tvAttentionPromoCoverage = view.findViewById(R.id.tvAttentionPromoCoverage)
    }

    private fun setupQuickActions(view: View) {
        view.findViewById<View>(R.id.cardManageMenu).setOnClickListener {
            findNavController().navigate(R.id.adminMenuFragment)
        }

        view.findViewById<View>(R.id.cardViewOrders).setOnClickListener {
            findNavController().navigate(R.id.adminOrdersFragment)
        }

        view.findViewById<View>(R.id.cardSendPromo).setOnClickListener {
            findNavController().navigate(R.id.adminCampaignStudioFragment)
        }

        view.findViewById<View>(R.id.cardReviewFeedback).setOnClickListener {
            findNavController().navigate(R.id.adminFeedbackFragment)
        }

        view.findViewById<View>(R.id.cardManageCustomers).setOnClickListener {
            findNavController().navigate(R.id.adminManageCustomerAccountsFragment)
        }

        view.findViewById<View>(R.id.cardAdminSettings).setOnClickListener {
            findNavController().navigate(R.id.adminSettingsFragment)
        }
    }

    private fun setupCarousels(view: View) {
        topRatedAdapter = AdminHomeCarouselAdapter(emptyList())
        mostLovedAdapter = AdminHomeCarouselAdapter(emptyList())
        mostCommentedAdapter = AdminHomeCarouselAdapter(emptyList())

        attachCarousel(
            pager = view.findViewById(R.id.pagerTopRated),
            dotsContainer = view.findViewById(R.id.dotsTopRated),
            adapter = topRatedAdapter
        )

        attachCarousel(
            pager = view.findViewById(R.id.pagerMostLoved),
            dotsContainer = view.findViewById(R.id.dotsMostLoved),
            adapter = mostLovedAdapter
        )

        attachCarousel(
            pager = view.findViewById(R.id.pagerMostCommented),
            dotsContainer = view.findViewById(R.id.dotsMostCommented),
            adapter = mostCommentedAdapter
        )
    }

    private fun loadDashboard() {
        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)
        val (startOfDay, endOfDay) = buildTodayRange()

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
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

            val dashboard = AdminDashboardSnapshot(
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
                newArrivals = newArrivals
            )

            val topRatedItems = mapTopRated(topRated)
            val mostLovedItems = mapMostLoved(mostLoved)
            val mostCommentedItems = mapMostCommented(mostCommented)

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext

                bindKpis(dashboard)
                bindMenuHealth(dashboard)
                bindNeedsAttention(dashboard)

                submitCarousel(
                    adapter = topRatedAdapter,
                    pager = view?.findViewById(R.id.pagerTopRated),
                    dotsContainer = view?.findViewById(R.id.dotsTopRated),
                    items = topRatedItems
                )

                submitCarousel(
                    adapter = mostLovedAdapter,
                    pager = view?.findViewById(R.id.pagerMostLoved),
                    dotsContainer = view?.findViewById(R.id.dotsMostLoved),
                    items = mostLovedItems
                )

                submitCarousel(
                    adapter = mostCommentedAdapter,
                    pager = view?.findViewById(R.id.pagerMostCommented),
                    dotsContainer = view?.findViewById(R.id.dotsMostCommented),
                    items = mostCommentedItems
                )
            }
        }
    }

    private fun bindKpis(snapshot: AdminDashboardSnapshot) {
        tvOrdersTodayValue.text = snapshot.ordersToday.toString()
        tvRevenueTodayValue.text = formatCurrency(snapshot.revenueToday)
        tvPendingOrdersValue.text = snapshot.pendingOrders.toString()
        tvAverageRatingValue.text = formatAverage(snapshot.feedbackOverview)
        tvPromoOptInRateValue.text = formatPromoRate(
            totalCustomers = snapshot.totalCustomers,
            promoOptInCustomers = snapshot.promoOptInCustomers
        )
        tvActiveCustomersValue.text = snapshot.activeCustomers.toString()
    }

    private fun bindMenuHealth(snapshot: AdminDashboardSnapshot) {
        tvAvailableProductsValue.text = snapshot.activeProducts.toString()
        tvDisabledProductsValue.text = snapshot.disabledProducts.toString()
        tvRewardEnabledProductsValue.text = snapshot.rewardEnabledProducts.toString()
        tvNewArrivalsValue.text = snapshot.newArrivals.toString()
    }

    private fun bindNeedsAttention(snapshot: AdminDashboardSnapshot) {
        tvAttentionPending.text = if (snapshot.pendingOrders > 0) {
            "• ${snapshot.pendingOrders} orders are still waiting in the active queue."
        } else {
            "• No orders are currently waiting in the active queue."
        }

        tvAttentionDisabled.text = if (snapshot.disabledProducts > 0) {
            "• ${snapshot.disabledProducts} menu products are currently disabled."
        } else {
            "• All menu products are currently active."
        }

        tvAttentionHiddenFeedback.text = if (snapshot.feedbackOverview.hiddenComments > 0) {
            "• ${snapshot.feedbackOverview.hiddenComments} customer comments are currently hidden."
        } else {
            "• No customer comments are currently hidden."
        }

        tvAttentionPromoCoverage.text = if (snapshot.totalCustomers > 0) {
            "• ${formatPromoRate(snapshot.totalCustomers, snapshot.promoOptInCustomers)} of customers accept promotional messaging."
        } else {
            "• No customer accounts have been registered yet."
        }
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

    private fun formatAverage(overview: AdminFeedbackOverview): String {
        return if (overview.totalReviews <= 0) {
            "—"
        } else {
            String.format(Locale.UK, "%.1f", overview.overallAverage)
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

    private fun attachCarousel(
        pager: ViewPager2,
        dotsContainer: LinearLayout,
        adapter: AdminHomeCarouselAdapter
    ) {
        pager.adapter = adapter
        pager.offscreenPageLimit = 1

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateDots(
                    dotsContainer = dotsContainer,
                    totalDots = adapter.getRealItemCount(),
                    selectedIndex = adapter.getIndicatorPosition(position)
                )
            }
        })
    }

    private fun submitCarousel(
        adapter: AdminHomeCarouselAdapter,
        pager: ViewPager2?,
        dotsContainer: LinearLayout?,
        items: List<AdminHomeCarouselItem>
    ) {
        if (pager == null || dotsContainer == null) return

        adapter.submitList(items)
        createDots(dotsContainer, adapter.getRealItemCount())

        val initialPosition = adapter.getInitialPosition()
        pager.setCurrentItem(initialPosition, false)

        updateDots(
            dotsContainer = dotsContainer,
            totalDots = adapter.getRealItemCount(),
            selectedIndex = adapter.getIndicatorPosition(initialPosition)
        )
    }

    private fun createDots(dotsContainer: LinearLayout, count: Int) {
        dotsContainer.removeAllViews()

        repeat(count) {
            val dot = TextView(requireContext()).apply {
                text = "●"
                textSize = 15f
                alpha = 0.28f
                setTextColor(android.graphics.Color.parseColor("#8B6B4A"))
                setPadding(8, 0, 8, 0)
            }
            dotsContainer.addView(dot)
        }
    }

    private fun updateDots(dotsContainer: LinearLayout, totalDots: Int, selectedIndex: Int) {
        for (index in 0 until totalDots) {
            val dot = dotsContainer.getChildAt(index) as? TextView ?: continue
            dot.alpha = if (index == selectedIndex) 1f else 0.28f
        }
    }
}

private data class AdminDashboardSnapshot(
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
    val newArrivals: Int
)