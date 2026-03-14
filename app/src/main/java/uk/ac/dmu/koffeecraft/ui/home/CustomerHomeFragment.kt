package uk.ac.dmu.koffeecraft.ui.home

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.session.SessionManager

class CustomerHomeFragment : Fragment(R.layout.fragment_customer_home) {

    private lateinit var tvBeansValue: TextView
    private lateinit var tvBeansMeta: TextView
    private lateinit var cardBeansBalance: MaterialCardView
    private lateinit var cardRewardsSection: MaterialCardView

    private lateinit var rvRewardsPreview: RecyclerView
    private lateinit var rvNewArrivals: RecyclerView
    private lateinit var rvTopCoffees: RecyclerView
    private lateinit var rvTopCakes: RecyclerView

    private lateinit var tvRewardsEmpty: TextView
    private lateinit var tvNewArrivalsEmpty: TextView
    private lateinit var tvTopCoffeesEmpty: TextView
    private lateinit var tvTopCakesEmpty: TextView

    private lateinit var rewardsAdapter: CustomerHomeCarouselAdapter
    private lateinit var newArrivalsAdapter: CustomerHomeCarouselAdapter
    private lateinit var topCoffeesAdapter: CustomerHomeCarouselAdapter
    private lateinit var topCakesAdapter: CustomerHomeCarouselAdapter

    private val customerId: Long?
        get() = SessionManager.currentCustomerId

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvBeansValue = view.findViewById(R.id.tvBeansValue)
        tvBeansMeta = view.findViewById(R.id.tvBeansMeta)
        cardBeansBalance = view.findViewById(R.id.cardBeansBalance)
        cardRewardsSection = view.findViewById(R.id.cardRewardsSection)

        rvRewardsPreview = view.findViewById(R.id.rvRewardsPreview)
        rvNewArrivals = view.findViewById(R.id.rvNewArrivals)
        rvTopCoffees = view.findViewById(R.id.rvTopCoffees)
        rvTopCakes = view.findViewById(R.id.rvTopCakes)

        tvRewardsEmpty = view.findViewById(R.id.tvRewardsEmpty)
        tvNewArrivalsEmpty = view.findViewById(R.id.tvNewArrivalsEmpty)
        tvTopCoffeesEmpty = view.findViewById(R.id.tvTopCoffeesEmpty)
        tvTopCakesEmpty = view.findViewById(R.id.tvTopCakesEmpty)

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

        setupHorizontalRecycler(rvRewardsPreview, rewardsAdapter)
        setupHorizontalRecycler(rvNewArrivals, newArrivalsAdapter)
        setupHorizontalRecycler(rvTopCoffees, topCoffeesAdapter)
        setupHorizontalRecycler(rvTopCakes, topCakesAdapter)

        cardBeansBalance.setOnClickListener { openRewards() }
        cardRewardsSection.setOnClickListener { openRewards() }

        loadHomeContent()
    }

    override fun onResume() {
        super.onResume()
        loadHomeContent()
    }

    private fun setupHorizontalRecycler(
        recyclerView: RecyclerView,
        adapter: CustomerHomeCarouselAdapter
    ) {
        recyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = adapter
    }

    private fun loadHomeContent() {
        val cid = customerId ?: return
        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val customer = db.customerDao().getById(cid) ?: return@launch
            val rewardProducts = db.productDao().getRewardProducts()
            val newProducts = db.productDao().getActiveNewProducts()
            val topCoffees = db.feedbackDao().getTopRatedProductsByFamily("COFFEE", 3)
            val topCakes = db.feedbackDao().getTopRatedProductsByFamily("CAKE", 3)

            val rewardItems = buildRewardPreviewItems(customer.beansBalance, customer.nextBeansBonusThreshold, rewardProducts)
            val newArrivalItems = newProducts.map { product ->
                CustomerHomeCarouselItem(
                    title = product.name,
                    subtitle = product.description.ifBlank { "Freshly added to the KoffeeCraft collection." },
                    metaLine = if (product.isMerch) {
                        if (product.rewardEnabled) "Reward item" else "Merch item"
                    } else {
                        String.format(Locale.UK, "From £%.2f", product.price)
                    },
                    badgeLabel = "NEW"
                )
            }

            val topCoffeeItems = topCoffees.map { item ->
                CustomerHomeCarouselItem(
                    title = item.productName,
                    subtitle = item.productDescription.ifBlank { "Top-rated crafted coffee." },
                    metaLine = String.format(
                        Locale.UK,
                        "★ %.1f • %d ratings • From £%.2f",
                        item.averageRating,
                        item.ratingCount,
                        item.price
                    ),
                    badgeLabel = "COFFEE"
                )
            }

            val topCakeItems = topCakes.map { item ->
                CustomerHomeCarouselItem(
                    title = item.productName,
                    subtitle = item.productDescription.ifBlank { "Top-rated crafted cake." },
                    metaLine = String.format(
                        Locale.UK,
                        "★ %.1f • %d ratings • From £%.2f",
                        item.averageRating,
                        item.ratingCount,
                        item.price
                    ),
                    badgeLabel = "CAKE"
                )
            }

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext

                tvBeansValue.text = "${customer.beansBalance} beans"
                tvBeansMeta.text =
                    "Next bean booster unlocks at ${customer.nextBeansBonusThreshold} beans."

                rewardsAdapter.submitList(rewardItems)
                newArrivalsAdapter.submitList(newArrivalItems)
                topCoffeesAdapter.submitList(topCoffeeItems)
                topCakesAdapter.submitList(topCakeItems)

                tvRewardsEmpty.visibility = if (rewardItems.isEmpty()) View.VISIBLE else View.GONE
                rvRewardsPreview.visibility = if (rewardItems.isEmpty()) View.GONE else View.VISIBLE

                tvNewArrivalsEmpty.visibility = if (newArrivalItems.isEmpty()) View.VISIBLE else View.GONE
                rvNewArrivals.visibility = if (newArrivalItems.isEmpty()) View.GONE else View.VISIBLE

                tvTopCoffeesEmpty.visibility = if (topCoffeeItems.isEmpty()) View.VISIBLE else View.GONE
                rvTopCoffees.visibility = if (topCoffeeItems.isEmpty()) View.GONE else View.VISIBLE

                tvTopCakesEmpty.visibility = if (topCakeItems.isEmpty()) View.VISIBLE else View.GONE
                rvTopCakes.visibility = if (topCakeItems.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun buildRewardPreviewItems(
        beansBalance: Int,
        nextBeansBonusThreshold: Int,
        rewardProducts: List<Product>
    ): List<CustomerHomeCarouselItem> {
        val items = mutableListOf<CustomerHomeCarouselItem>()

        items += CustomerHomeCarouselItem(
            title = "5 Bean Booster",
            subtitle = "Claim 5 extra beans when you reach your next milestone.",
            metaLine = "Next milestone: $nextBeansBonusThreshold beans",
            badgeLabel = "REWARD"
        )

        items += CustomerHomeCarouselItem(
            title = "Free Coffee",
            subtitle = "Redeem a crafted coffee from the rewards screen.",
            metaLine = "15 beans",
            badgeLabel = if (beansBalance >= 15) "AVAILABLE" else "REWARD"
        )

        items += CustomerHomeCarouselItem(
            title = "Free Cake",
            subtitle = "Redeem a crafted cake from the rewards screen.",
            metaLine = "18 beans",
            badgeLabel = if (beansBalance >= 18) "AVAILABLE" else "REWARD"
        )

        rewardProducts.forEach { product ->
            val beansCost = when (product.name) {
                "KoffeeCraft Mug" -> 125
                "KoffeeCraft Teddy Bear" -> 250
                "1kg Crafted Coffee Beans" -> 370
                else -> 0
            }

            items += CustomerHomeCarouselItem(
                title = product.name,
                subtitle = product.description.ifBlank { "Special reward item available in the rewards screen." },
                metaLine = if (beansCost > 0) "$beansCost beans" else "Reward item",
                badgeLabel = if (beansCost > 0 && beansBalance >= beansCost) "AVAILABLE" else "REWARD"
            )
        }

        return items
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