package uk.ac.dmu.koffeecraft.ui.admin.home

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.dao.ProductCommentInsight
import uk.ac.dmu.koffeecraft.data.dao.ProductRatingInsight
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.settings.SimulationSettings

class AdminHomeFragment : Fragment(R.layout.fragment_admin_home) {

    private lateinit var bestRatedAdapter: AdminHomeCarouselAdapter
    private lateinit var lowestRatedAdapter: AdminHomeCarouselAdapter
    private lateinit var mostCommentedAdapter: AdminHomeCarouselAdapter
    private lateinit var leastCommentedAdapter: AdminHomeCarouselAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val simulationSwitch = view.findViewById<SwitchMaterial>(R.id.switchSimulation)
        val pagerBestRated = view.findViewById<ViewPager2>(R.id.pagerBestRated)
        val pagerLowestRated = view.findViewById<ViewPager2>(R.id.pagerLowestRated)
        val pagerMostCommented = view.findViewById<ViewPager2>(R.id.pagerMostCommented)
        val pagerLeastCommented = view.findViewById<ViewPager2>(R.id.pagerLeastCommented)

        val dotsBestRated = view.findViewById<LinearLayout>(R.id.dotsBestRated)
        val dotsLowestRated = view.findViewById<LinearLayout>(R.id.dotsLowestRated)
        val dotsMostCommented = view.findViewById<LinearLayout>(R.id.dotsMostCommented)
        val dotsLeastCommented = view.findViewById<LinearLayout>(R.id.dotsLeastCommented)

        simulationSwitch.isChecked = SimulationSettings.isEnabled(requireContext())
        simulationSwitch.setOnCheckedChangeListener { _, isChecked ->
            SimulationSettings.setEnabled(requireContext(), isChecked)
        }

        bestRatedAdapter = AdminHomeCarouselAdapter(emptyList())
        lowestRatedAdapter = AdminHomeCarouselAdapter(emptyList())
        mostCommentedAdapter = AdminHomeCarouselAdapter(emptyList())
        leastCommentedAdapter = AdminHomeCarouselAdapter(emptyList())

        attachCarousel(pagerBestRated, dotsBestRated, bestRatedAdapter)
        attachCarousel(pagerLowestRated, dotsLowestRated, lowestRatedAdapter)
        attachCarousel(pagerMostCommented, dotsMostCommented, mostCommentedAdapter)
        attachCarousel(pagerLeastCommented, dotsLeastCommented, leastCommentedAdapter)

        loadDashboard()
    }

    override fun onResume() {
        super.onResume()
        loadDashboard()
    }

    private fun loadDashboard() {
        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val bestRated = db.feedbackDao().getTopRatedProducts()
            val lowestRated = db.feedbackDao().getLowestRatedProducts()
            val mostCommented = db.feedbackDao().getMostCommentedProducts()
            val leastCommented = db.feedbackDao().getLeastCommentedProducts()

            val bestRatedItems = mapBestRated(bestRated)
            val lowestRatedItems = mapLowestRated(lowestRated)
            val mostCommentedItems = mapMostCommented(mostCommented)
            val leastCommentedItems = mapLeastCommented(leastCommented)

            withContext(Dispatchers.Main) {
                submitCarousel(bestRatedAdapter, view?.findViewById(R.id.pagerBestRated), view?.findViewById(R.id.dotsBestRated), bestRatedItems)
                submitCarousel(lowestRatedAdapter, view?.findViewById(R.id.pagerLowestRated), view?.findViewById(R.id.dotsLowestRated), lowestRatedItems)
                submitCarousel(mostCommentedAdapter, view?.findViewById(R.id.pagerMostCommented), view?.findViewById(R.id.dotsMostCommented), mostCommentedItems)
                submitCarousel(leastCommentedAdapter, view?.findViewById(R.id.pagerLeastCommented), view?.findViewById(R.id.dotsLeastCommented), leastCommentedItems)
            }
        }
    }

    private fun mapBestRated(items: List<ProductRatingInsight>): List<AdminHomeCarouselItem> {
        if (items.isEmpty()) {
            return listOf(
                AdminHomeCarouselItem(
                    rankLabel = "#",
                    productName = "No ratings yet",
                    primaryText = "Waiting for customer feedback",
                    secondaryText = ""
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

    private fun mapLowestRated(items: List<ProductRatingInsight>): List<AdminHomeCarouselItem> {
        if (items.isEmpty()) {
            return listOf(
                AdminHomeCarouselItem(
                    rankLabel = "#",
                    productName = "No ratings yet",
                    primaryText = "Waiting for customer feedback",
                    secondaryText = ""
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

    private fun mapMostCommented(items: List<ProductCommentInsight>): List<AdminHomeCarouselItem> {
        if (items.isEmpty()) {
            return listOf(
                AdminHomeCarouselItem(
                    rankLabel = "#",
                    productName = "No comments yet",
                    primaryText = "Waiting for customer comments",
                    secondaryText = ""
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

    private fun mapLeastCommented(items: List<ProductCommentInsight>): List<AdminHomeCarouselItem> {
        if (items.isEmpty()) {
            return listOf(
                AdminHomeCarouselItem(
                    rankLabel = "#",
                    productName = "No commented products yet",
                    primaryText = "At least one comment is required",
                    secondaryText = ""
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

    private fun formatRating(value: Double): String {
        return String.format(Locale.UK, "%.1f", value)
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
                textSize = 16f
                alpha = 0.3f
                setPadding(8, 0, 8, 0)
            }
            dotsContainer.addView(dot)
        }
    }

    private fun updateDots(dotsContainer: LinearLayout, totalDots: Int, selectedIndex: Int) {
        for (i in 0 until totalDots) {
            val dot = dotsContainer.getChildAt(i) as? TextView ?: continue
            dot.alpha = if (i == selectedIndex) 1f else 0.3f
        }
    }
}
