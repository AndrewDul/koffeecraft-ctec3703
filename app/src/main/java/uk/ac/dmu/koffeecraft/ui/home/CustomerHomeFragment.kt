package uk.ac.dmu.koffeecraft.ui.home

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.core.di.appContainer

class CustomerHomeFragment : Fragment(R.layout.fragment_customer_home) {

    private lateinit var viewModel: CustomerHomeViewModel

    private lateinit var tvBeansValue: TextView
    private lateinit var tvBeansMeta: TextView
    private lateinit var tvBeansProgressValue: TextView
    private lateinit var progressBeansHome: LinearProgressIndicator
    private lateinit var cardBeansBalance: MaterialCardView
    private lateinit var cardRewardsSection: MaterialCardView

    private lateinit var rvRewardsPreview: RecyclerView
    private lateinit var rvNewArrivals: RecyclerView
    private lateinit var rvTopCoffees: RecyclerView
    private lateinit var rvTopCakes: RecyclerView
    private lateinit var rvMostLoved: RecyclerView

    private lateinit var tvRewardsEmpty: TextView
    private lateinit var tvNewArrivalsEmpty: TextView
    private lateinit var tvTopCoffeesEmpty: TextView
    private lateinit var tvTopCakesEmpty: TextView
    private lateinit var tvMostLovedEmpty: TextView

    private lateinit var rewardsAdapter: CustomerHomeCarouselAdapter
    private lateinit var newArrivalsAdapter: CustomerHomeCarouselAdapter
    private lateinit var topCoffeesAdapter: CustomerHomeCarouselAdapter
    private lateinit var topCakesAdapter: CustomerHomeCarouselAdapter
    private lateinit var mostLovedAdapter: CustomerHomeCarouselAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            CustomerHomeViewModel.Factory(
                repository = appContainer.customerHomeRepository,
                sessionRepository = appContainer.sessionRepository
            )
        )[CustomerHomeViewModel::class.java]

        tvBeansValue = view.findViewById(R.id.tvBeansValue)
        tvBeansMeta = view.findViewById(R.id.tvBeansMeta)
        tvBeansProgressValue = view.findViewById(R.id.tvBeansProgressValue)
        progressBeansHome = view.findViewById(R.id.progressBeansHome)
        cardBeansBalance = view.findViewById(R.id.cardBeansBalance)
        cardRewardsSection = view.findViewById(R.id.cardRewardsSection)

        rvRewardsPreview = view.findViewById(R.id.rvRewardsPreview)
        rvNewArrivals = view.findViewById(R.id.rvNewArrivals)
        rvTopCoffees = view.findViewById(R.id.rvTopCoffees)
        rvTopCakes = view.findViewById(R.id.rvTopCakes)
        rvMostLoved = view.findViewById(R.id.rvMostLoved)

        tvRewardsEmpty = view.findViewById(R.id.tvRewardsEmpty)
        tvNewArrivalsEmpty = view.findViewById(R.id.tvNewArrivalsEmpty)
        tvTopCoffeesEmpty = view.findViewById(R.id.tvTopCoffeesEmpty)
        tvTopCakesEmpty = view.findViewById(R.id.tvTopCakesEmpty)
        tvMostLovedEmpty = view.findViewById(R.id.tvMostLovedEmpty)

        rewardsAdapter = CustomerHomeCarouselAdapter(emptyList()) {
            openRewards()
        }

        newArrivalsAdapter = CustomerHomeCarouselAdapter(emptyList()) {
            openMenu()
        }

        topCoffeesAdapter = CustomerHomeCarouselAdapter(emptyList()) {
            openMenu()
        }

        topCakesAdapter = CustomerHomeCarouselAdapter(emptyList()) {
            openMenu()
        }

        mostLovedAdapter = CustomerHomeCarouselAdapter(emptyList()) {
            openMenu()
        }

        setupHorizontalRecycler(rvRewardsPreview, rewardsAdapter)
        setupHorizontalRecycler(rvNewArrivals, newArrivalsAdapter)
        setupHorizontalRecycler(rvTopCoffees, topCoffeesAdapter)
        setupHorizontalRecycler(rvTopCakes, topCakesAdapter)
        setupHorizontalRecycler(rvMostLoved, mostLovedAdapter)

        cardBeansBalance.setOnClickListener { openRewards() }
        cardRewardsSection.setOnClickListener { openRewards() }

        observeState()
        viewModel.refresh()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                tvBeansValue.text = state.beansValue
                tvBeansMeta.text = state.beansMeta
                tvBeansProgressValue.text = state.beansProgressValue

                progressBeansHome.max = 10
                progressBeansHome.progress = state.beansProgress

                rewardsAdapter.submitList(state.rewardItems)
                newArrivalsAdapter.submitList(state.newArrivalItems)
                topCoffeesAdapter.submitList(state.topCoffeeItems)
                topCakesAdapter.submitList(state.topCakeItems)
                mostLovedAdapter.submitList(state.mostLovedItems)

                tvRewardsEmpty.visibility = if (state.rewardItems.isEmpty()) View.VISIBLE else View.GONE
                rvRewardsPreview.visibility = if (state.rewardItems.isEmpty()) View.GONE else View.VISIBLE

                tvNewArrivalsEmpty.visibility = if (state.newArrivalItems.isEmpty()) View.VISIBLE else View.GONE
                rvNewArrivals.visibility = if (state.newArrivalItems.isEmpty()) View.GONE else View.VISIBLE

                tvTopCoffeesEmpty.visibility = if (state.topCoffeeItems.isEmpty()) View.VISIBLE else View.GONE
                rvTopCoffees.visibility = if (state.topCoffeeItems.isEmpty()) View.GONE else View.VISIBLE

                tvTopCakesEmpty.visibility = if (state.topCakeItems.isEmpty()) View.VISIBLE else View.GONE
                rvTopCakes.visibility = if (state.topCakeItems.isEmpty()) View.GONE else View.VISIBLE

                tvMostLovedEmpty.visibility = if (state.mostLovedItems.isEmpty()) View.VISIBLE else View.GONE
                rvMostLoved.visibility = if (state.mostLovedItems.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun setupHorizontalRecycler(
        recyclerView: RecyclerView,
        adapter: CustomerHomeCarouselAdapter
    ) {
        recyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = adapter
    }

    private fun openRewards() {
        if (findNavController().currentDestination?.id == R.id.customerRewardsFragment) return
        findNavController().navigate(R.id.customerRewardsFragment)
    }

    private fun openMenu() {
        if (findNavController().currentDestination?.id == R.id.menuFragment) return
        findNavController().navigate(R.id.menuFragment)
    }
}