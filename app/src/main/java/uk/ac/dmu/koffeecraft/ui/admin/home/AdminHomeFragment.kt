package uk.ac.dmu.koffeecraft.ui.admin.home

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.core.di.appContainer

class AdminHomeFragment : Fragment(R.layout.fragment_admin_home) {

    private lateinit var viewModel: AdminHomeViewModel

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

    private lateinit var pagerTopRated: ViewPager2
    private lateinit var pagerMostLoved: ViewPager2
    private lateinit var pagerMostCommented: ViewPager2
    private lateinit var dotsTopRated: LinearLayout
    private lateinit var dotsMostLoved: LinearLayout
    private lateinit var dotsMostCommented: LinearLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            AdminHomeViewModel.Factory(appContainer.adminHomeRepository)
        )[AdminHomeViewModel::class.java]

        bindViews(view)
        setupQuickActions(view)
        setupCarousels()
        observeState()

        viewModel.refreshDashboard()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDashboard()
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

        pagerTopRated = view.findViewById(R.id.pagerTopRated)
        pagerMostLoved = view.findViewById(R.id.pagerMostLoved)
        pagerMostCommented = view.findViewById(R.id.pagerMostCommented)

        dotsTopRated = view.findViewById(R.id.dotsTopRated)
        dotsMostLoved = view.findViewById(R.id.dotsMostLoved)
        dotsMostCommented = view.findViewById(R.id.dotsMostCommented)
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

    private fun setupCarousels() {
        topRatedAdapter = AdminHomeCarouselAdapter(emptyList())
        mostLovedAdapter = AdminHomeCarouselAdapter(emptyList())
        mostCommentedAdapter = AdminHomeCarouselAdapter(emptyList())

        attachCarousel(pagerTopRated, dotsTopRated, topRatedAdapter)
        attachCarousel(pagerMostLoved, dotsMostLoved, mostLovedAdapter)
        attachCarousel(pagerMostCommented, dotsMostCommented, mostCommentedAdapter)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                tvOrdersTodayValue.text = state.ordersTodayValue
                tvRevenueTodayValue.text = state.revenueTodayValue
                tvPendingOrdersValue.text = state.pendingOrdersValue
                tvAverageRatingValue.text = state.averageRatingValue
                tvPromoOptInRateValue.text = state.promoOptInRateValue
                tvActiveCustomersValue.text = state.activeCustomersValue

                tvAvailableProductsValue.text = state.availableProductsValue
                tvDisabledProductsValue.text = state.disabledProductsValue
                tvRewardEnabledProductsValue.text = state.rewardEnabledProductsValue
                tvNewArrivalsValue.text = state.newArrivalsValue

                tvAttentionPending.text = state.attentionPending
                tvAttentionDisabled.text = state.attentionDisabled
                tvAttentionHiddenFeedback.text = state.attentionHiddenFeedback
                tvAttentionPromoCoverage.text = state.attentionPromoCoverage

                submitCarousel(topRatedAdapter, pagerTopRated, dotsTopRated, state.topRatedItems)
                submitCarousel(mostLovedAdapter, pagerMostLoved, dotsMostLoved, state.mostLovedItems)
                submitCarousel(mostCommentedAdapter, pagerMostCommented, dotsMostCommented, state.mostCommentedItems)
            }
        }
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
        pager: ViewPager2,
        dotsContainer: LinearLayout,
        items: List<AdminHomeCarouselItem>
    ) {
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

        val dotColor = ContextCompat.getColor(requireContext(), R.color.kc_brand_primary)

        repeat(count) {
            val dot = TextView(requireContext()).apply {
                text = "●"
                textSize = 15f
                alpha = 0.28f
                setTextColor(dotColor)
                setPadding(8, 0, 8, 0)
            }
            dotsContainer.addView(dot)
        }
    }

    private fun updateDots(
        dotsContainer: LinearLayout,
        totalDots: Int,
        selectedIndex: Int
    ) {
        for (index in 0 until totalDots) {
            val dot = dotsContainer.getChildAt(index) as? TextView ?: continue
            dot.alpha = if (index == selectedIndex) 1f else 0.28f
        }
    }
}