package uk.ac.dmu.koffeecraft.ui.admin.feedback

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.dao.AdminFeedbackItem
import uk.ac.dmu.koffeecraft.data.dao.AdminFeedbackOverview
import uk.ac.dmu.koffeecraft.data.dao.AdminFeedbackRatingBreakdown
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import java.util.Locale

class AdminFeedbackFragment : Fragment(R.layout.fragment_admin_feedback) {

    private lateinit var adapter: AdminFeedbackAdapter
    private lateinit var rvAdminFeedback: RecyclerView
    private lateinit var emptyStateCard: View
    private lateinit var tvFeedCount: TextView

    private lateinit var tvOverallAverageValue: TextView
    private lateinit var tvCoffeeAverageValue: TextView
    private lateinit var tvCakeAverageValue: TextView
    private lateinit var tvTotalReviewsValue: TextView
    private lateinit var tvCommentsRateValue: TextView
    private lateinit var tvHiddenCommentsValue: TextView

    private lateinit var progressFive: ProgressBar
    private lateinit var progressFour: ProgressBar
    private lateinit var progressThree: ProgressBar
    private lateinit var progressTwo: ProgressBar
    private lateinit var progressOne: ProgressBar

    private lateinit var tvCountFive: TextView
    private lateinit var tvCountFour: TextView
    private lateinit var tvCountThree: TextView
    private lateinit var tvCountTwo: TextView
    private lateinit var tvCountOne: TextView

    private lateinit var tvRatingAll: TextView
    private lateinit var tvRating1: TextView
    private lateinit var tvRating2: TextView
    private lateinit var tvRating3: TextView
    private lateinit var tvRating4: TextView
    private lateinit var tvRating5: TextView

    private lateinit var tvCommentAll: TextView
    private lateinit var tvCommentWith: TextView
    private lateinit var tvCommentWithout: TextView

    private lateinit var tvCategoryAll: TextView
    private lateinit var tvCategoryCoffee: TextView
    private lateinit var tvCategoryCake: TextView

    private lateinit var tvSortNewest: TextView
    private lateinit var tvSortOldest: TextView

    private var allFeedback: List<AdminFeedbackItem> = emptyList()

    private var currentRatingFilter: RatingFilter = RatingFilter.ALL
    private var currentCommentFilter: CommentFilter = CommentFilter.ALL
    private var currentCategoryFilter: CategoryFilter = CategoryFilter.ALL
    private var currentSortMode: SortMode = SortMode.NEWEST

    private enum class RatingFilter { ALL, ONE, TWO, THREE, FOUR, FIVE }
    private enum class CommentFilter { ALL, WITH_COMMENT, WITHOUT_COMMENT }
    private enum class CategoryFilter { ALL, COFFEE, CAKE }
    private enum class SortMode { NEWEST, OLDEST }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        rvAdminFeedback.layoutManager = LinearLayoutManager(requireContext())
        rvAdminFeedback.isNestedScrollingEnabled = false

        adapter = AdminFeedbackAdapter(
            items = emptyList(),
            onDelete = { item -> showDeleteDialog(db, item) },
            onHideToggle = { item -> toggleHideState(db, item) }
        )
        rvAdminFeedback.adapter = adapter

        setupFilterChips()
        updateFilterUi()

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            db.feedbackDao().observeAllForAdmin().collect { items ->
                val overview = db.feedbackDao().getAdminFeedbackOverview()
                val breakdown = db.feedbackDao().getAdminFeedbackBreakdown()

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    allFeedback = items
                    bindOverview(overview)
                    bindBreakdown(overview.totalReviews, breakdown)
                    applyFiltersAndSorting()
                }
            }
        }
    }

    private fun bindViews(view: View) {
        rvAdminFeedback = view.findViewById(R.id.rvAdminFeedback)
        emptyStateCard = view.findViewById(R.id.tvEmpty)
        tvFeedCount = view.findViewById(R.id.tvFeedCount)

        tvOverallAverageValue = view.findViewById(R.id.tvOverallAverageValue)
        tvCoffeeAverageValue = view.findViewById(R.id.tvCoffeeAverageValue)
        tvCakeAverageValue = view.findViewById(R.id.tvCakeAverageValue)
        tvTotalReviewsValue = view.findViewById(R.id.tvTotalReviewsValue)
        tvCommentsRateValue = view.findViewById(R.id.tvCommentsRateValue)
        tvHiddenCommentsValue = view.findViewById(R.id.tvHiddenCommentsValue)

        progressFive = view.findViewById(R.id.progressFive)
        progressFour = view.findViewById(R.id.progressFour)
        progressThree = view.findViewById(R.id.progressThree)
        progressTwo = view.findViewById(R.id.progressTwo)
        progressOne = view.findViewById(R.id.progressOne)

        tvCountFive = view.findViewById(R.id.tvCountFive)
        tvCountFour = view.findViewById(R.id.tvCountFour)
        tvCountThree = view.findViewById(R.id.tvCountThree)
        tvCountTwo = view.findViewById(R.id.tvCountTwo)
        tvCountOne = view.findViewById(R.id.tvCountOne)

        tvRatingAll = view.findViewById(R.id.tvRatingAll)
        tvRating1 = view.findViewById(R.id.tvRating1)
        tvRating2 = view.findViewById(R.id.tvRating2)
        tvRating3 = view.findViewById(R.id.tvRating3)
        tvRating4 = view.findViewById(R.id.tvRating4)
        tvRating5 = view.findViewById(R.id.tvRating5)

        tvCommentAll = view.findViewById(R.id.tvCommentAll)
        tvCommentWith = view.findViewById(R.id.tvCommentWith)
        tvCommentWithout = view.findViewById(R.id.tvCommentWithout)

        tvCategoryAll = view.findViewById(R.id.tvCategoryAll)
        tvCategoryCoffee = view.findViewById(R.id.tvCategoryCoffee)
        tvCategoryCake = view.findViewById(R.id.tvCategoryCake)

        tvSortNewest = view.findViewById(R.id.tvSortNewest)
        tvSortOldest = view.findViewById(R.id.tvSortOldest)
    }

    private fun setupFilterChips() {
        tvRatingAll.setOnClickListener {
            currentRatingFilter = RatingFilter.ALL
            updateFilterUi()
            applyFiltersAndSorting()
        }
        tvRating1.setOnClickListener {
            currentRatingFilter = RatingFilter.ONE
            updateFilterUi()
            applyFiltersAndSorting()
        }
        tvRating2.setOnClickListener {
            currentRatingFilter = RatingFilter.TWO
            updateFilterUi()
            applyFiltersAndSorting()
        }
        tvRating3.setOnClickListener {
            currentRatingFilter = RatingFilter.THREE
            updateFilterUi()
            applyFiltersAndSorting()
        }
        tvRating4.setOnClickListener {
            currentRatingFilter = RatingFilter.FOUR
            updateFilterUi()
            applyFiltersAndSorting()
        }
        tvRating5.setOnClickListener {
            currentRatingFilter = RatingFilter.FIVE
            updateFilterUi()
            applyFiltersAndSorting()
        }

        tvCommentAll.setOnClickListener {
            currentCommentFilter = CommentFilter.ALL
            updateFilterUi()
            applyFiltersAndSorting()
        }
        tvCommentWith.setOnClickListener {
            currentCommentFilter = CommentFilter.WITH_COMMENT
            updateFilterUi()
            applyFiltersAndSorting()
        }
        tvCommentWithout.setOnClickListener {
            currentCommentFilter = CommentFilter.WITHOUT_COMMENT
            updateFilterUi()
            applyFiltersAndSorting()
        }

        tvCategoryAll.setOnClickListener {
            currentCategoryFilter = CategoryFilter.ALL
            updateFilterUi()
            applyFiltersAndSorting()
        }
        tvCategoryCoffee.setOnClickListener {
            currentCategoryFilter = CategoryFilter.COFFEE
            updateFilterUi()
            applyFiltersAndSorting()
        }
        tvCategoryCake.setOnClickListener {
            currentCategoryFilter = CategoryFilter.CAKE
            updateFilterUi()
            applyFiltersAndSorting()
        }

        tvSortNewest.setOnClickListener {
            currentSortMode = SortMode.NEWEST
            updateFilterUi()
            applyFiltersAndSorting()
        }
        tvSortOldest.setOnClickListener {
            currentSortMode = SortMode.OLDEST
            updateFilterUi()
            applyFiltersAndSorting()
        }
    }

    private fun updateFilterUi() {
        setChipSelected(tvRatingAll, currentRatingFilter == RatingFilter.ALL)
        setChipSelected(tvRating1, currentRatingFilter == RatingFilter.ONE)
        setChipSelected(tvRating2, currentRatingFilter == RatingFilter.TWO)
        setChipSelected(tvRating3, currentRatingFilter == RatingFilter.THREE)
        setChipSelected(tvRating4, currentRatingFilter == RatingFilter.FOUR)
        setChipSelected(tvRating5, currentRatingFilter == RatingFilter.FIVE)

        setChipSelected(tvCommentAll, currentCommentFilter == CommentFilter.ALL)
        setChipSelected(tvCommentWith, currentCommentFilter == CommentFilter.WITH_COMMENT)
        setChipSelected(tvCommentWithout, currentCommentFilter == CommentFilter.WITHOUT_COMMENT)

        setChipSelected(tvCategoryAll, currentCategoryFilter == CategoryFilter.ALL)
        setChipSelected(tvCategoryCoffee, currentCategoryFilter == CategoryFilter.COFFEE)
        setChipSelected(tvCategoryCake, currentCategoryFilter == CategoryFilter.CAKE)

        setChipSelected(tvSortNewest, currentSortMode == SortMode.NEWEST)
        setChipSelected(tvSortOldest, currentSortMode == SortMode.OLDEST)
    }

    private fun setChipSelected(chip: TextView, isSelected: Boolean) {
        chip.setBackgroundResource(
            if (isSelected) R.drawable.bg_orders_filter_chip_selected
            else R.drawable.bg_orders_filter_chip
        )
        chip.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (isSelected) android.R.color.black else android.R.color.darker_gray
            )
        )
        chip.setTypeface(null, if (isSelected) Typeface.BOLD else Typeface.BOLD)
        chip.alpha = if (isSelected) 1f else 0.92f
    }

    private fun bindOverview(overview: AdminFeedbackOverview) {
        val commentPercent = if (overview.totalReviews == 0) {
            0
        } else {
            ((overview.reviewsWithComments.toDouble() / overview.totalReviews.toDouble()) * 100).toInt()
        }

        tvOverallAverageValue.text = formatAverage(overview.overallAverage, overview.totalReviews)
        tvCoffeeAverageValue.text = formatAverage(overview.coffeeAverage, overview.totalReviews)
        tvCakeAverageValue.text = formatAverage(overview.cakeAverage, overview.totalReviews)
        tvTotalReviewsValue.text = overview.totalReviews.toString()
        tvCommentsRateValue.text = "$commentPercent%"
        tvHiddenCommentsValue.text = overview.hiddenComments.toString()
    }

    private fun bindBreakdown(
        totalReviews: Int,
        breakdown: List<AdminFeedbackRatingBreakdown>
    ) {
        val counts = breakdown.associate { it.rating to it.reviewCount }

        bindBreakdownRow(progressFive, tvCountFive, counts[5] ?: 0, totalReviews)
        bindBreakdownRow(progressFour, tvCountFour, counts[4] ?: 0, totalReviews)
        bindBreakdownRow(progressThree, tvCountThree, counts[3] ?: 0, totalReviews)
        bindBreakdownRow(progressTwo, tvCountTwo, counts[2] ?: 0, totalReviews)
        bindBreakdownRow(progressOne, tvCountOne, counts[1] ?: 0, totalReviews)
    }

    private fun bindBreakdownRow(
        progressBar: ProgressBar,
        countView: TextView,
        count: Int,
        totalReviews: Int
    ) {
        val percentage = if (totalReviews == 0) 0 else ((count.toDouble() / totalReviews.toDouble()) * 100).toInt()
        progressBar.progress = percentage
        countView.text = count.toString()
    }

    private fun formatAverage(value: Double, totalReviews: Int): String {
        return if (totalReviews == 0 || value == 0.0) {
            "—"
        } else {
            String.format(Locale.UK, "%.1f", value)
        }
    }

    private fun applyFiltersAndSorting() {
        val filtered = allFeedback.filter { item ->
            val matchesRating = when (currentRatingFilter) {
                RatingFilter.ALL -> true
                RatingFilter.ONE -> item.rating == 1
                RatingFilter.TWO -> item.rating == 2
                RatingFilter.THREE -> item.rating == 3
                RatingFilter.FOUR -> item.rating == 4
                RatingFilter.FIVE -> item.rating == 5
            }

            val hasComment = item.comment.isNotBlank()

            val matchesComment = when (currentCommentFilter) {
                CommentFilter.ALL -> true
                CommentFilter.WITH_COMMENT -> hasComment
                CommentFilter.WITHOUT_COMMENT -> !hasComment
            }

            val matchesCategory = when (currentCategoryFilter) {
                CategoryFilter.ALL -> true
                CategoryFilter.COFFEE -> item.productCategory.equals("COFFEE", ignoreCase = true)
                CategoryFilter.CAKE -> item.productCategory.equals("CAKE", ignoreCase = true)
            }

            matchesRating && matchesComment && matchesCategory
        }

        val sorted = when (currentSortMode) {
            SortMode.NEWEST -> filtered.sortedByDescending { it.updatedAt }
            SortMode.OLDEST -> filtered.sortedBy { it.updatedAt }
        }

        adapter.submitList(sorted)
        tvFeedCount.text = if (sorted.isEmpty()) {
            "No reviews match the current filters"
        } else {
            "Showing ${sorted.size} review${if (sorted.size == 1) "" else "s"}"
        }

        emptyStateCard.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showDeleteDialog(db: KoffeeCraftDatabase, item: AdminFeedbackItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete review?")
            .setMessage("This will permanently remove the rating and comment for ${item.productName}.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    db.feedbackDao().deleteById(item.feedbackId)
                }
            }
            .show()
    }

    private fun toggleHideState(db: KoffeeCraftDatabase, item: AdminFeedbackItem) {
        if (item.comment.isBlank()) return

        val title = if (item.isHidden) "Unhide comment?" else "Hide comment?"
        val message = if (item.isHidden) {
            "This will make the comment visible again."
        } else {
            "This will hide the comment but keep the rating for statistics."
        }
        val action = if (item.isHidden) "Unhide" else "Hide"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton("Cancel", null)
            .setPositiveButton(action) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    val now = System.currentTimeMillis()
                    if (item.isHidden) {
                        db.feedbackDao().unhideComment(item.feedbackId, now)
                    } else {
                        db.feedbackDao().hideComment(item.feedbackId, now)
                    }
                }
            }
            .show()
    }
}