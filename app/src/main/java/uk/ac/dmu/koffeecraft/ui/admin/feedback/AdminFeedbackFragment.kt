package uk.ac.dmu.koffeecraft.ui.admin.feedback

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.core.di.appContainer
import uk.ac.dmu.koffeecraft.data.querymodel.AdminFeedbackItem
class AdminFeedbackFragment : Fragment(R.layout.fragment_admin_feedback) {

    private lateinit var viewModel: AdminFeedbackViewModel
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            AdminFeedbackViewModel.Factory(appContainer.adminFeedbackRepository)
        )[AdminFeedbackViewModel::class.java]

        bindViews(view)

        rvAdminFeedback.layoutManager = LinearLayoutManager(requireContext())
        rvAdminFeedback.isNestedScrollingEnabled = false

        adapter = AdminFeedbackAdapter(
            items = emptyList(),
            onDelete = { item -> showDeleteDialog(item) },
            onHideToggle = { item -> toggleHideState(item) }
        )
        rvAdminFeedback.adapter = adapter

        setupFilterChips()
        observeState()

        viewModel.start()
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
        tvRatingAll.setOnClickListener { viewModel.setRatingFilter(RatingFilter.ALL) }
        tvRating1.setOnClickListener { viewModel.setRatingFilter(RatingFilter.ONE) }
        tvRating2.setOnClickListener { viewModel.setRatingFilter(RatingFilter.TWO) }
        tvRating3.setOnClickListener { viewModel.setRatingFilter(RatingFilter.THREE) }
        tvRating4.setOnClickListener { viewModel.setRatingFilter(RatingFilter.FOUR) }
        tvRating5.setOnClickListener { viewModel.setRatingFilter(RatingFilter.FIVE) }

        tvCommentAll.setOnClickListener { viewModel.setCommentFilter(CommentFilter.ALL) }
        tvCommentWith.setOnClickListener { viewModel.setCommentFilter(CommentFilter.WITH_COMMENT) }
        tvCommentWithout.setOnClickListener { viewModel.setCommentFilter(CommentFilter.WITHOUT_COMMENT) }

        tvCategoryAll.setOnClickListener { viewModel.setCategoryFilter(FeedbackCategoryFilter.ALL) }
        tvCategoryCoffee.setOnClickListener { viewModel.setCategoryFilter(FeedbackCategoryFilter.COFFEE) }
        tvCategoryCake.setOnClickListener { viewModel.setCategoryFilter(FeedbackCategoryFilter.CAKE) }

        tvSortNewest.setOnClickListener { viewModel.setSortMode(FeedbackSortMode.NEWEST) }
        tvSortOldest.setOnClickListener { viewModel.setSortMode(FeedbackSortMode.OLDEST) }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                bindState(state)
            }
        }
    }

    private fun bindState(state: AdminFeedbackUiState) {
        setChipSelected(tvRatingAll, state.currentRatingFilter == RatingFilter.ALL)
        setChipSelected(tvRating1, state.currentRatingFilter == RatingFilter.ONE)
        setChipSelected(tvRating2, state.currentRatingFilter == RatingFilter.TWO)
        setChipSelected(tvRating3, state.currentRatingFilter == RatingFilter.THREE)
        setChipSelected(tvRating4, state.currentRatingFilter == RatingFilter.FOUR)
        setChipSelected(tvRating5, state.currentRatingFilter == RatingFilter.FIVE)

        setChipSelected(tvCommentAll, state.currentCommentFilter == CommentFilter.ALL)
        setChipSelected(tvCommentWith, state.currentCommentFilter == CommentFilter.WITH_COMMENT)
        setChipSelected(tvCommentWithout, state.currentCommentFilter == CommentFilter.WITHOUT_COMMENT)

        setChipSelected(tvCategoryAll, state.currentCategoryFilter == FeedbackCategoryFilter.ALL)
        setChipSelected(tvCategoryCoffee, state.currentCategoryFilter == FeedbackCategoryFilter.COFFEE)
        setChipSelected(tvCategoryCake, state.currentCategoryFilter == FeedbackCategoryFilter.CAKE)

        setChipSelected(tvSortNewest, state.currentSortMode == FeedbackSortMode.NEWEST)
        setChipSelected(tvSortOldest, state.currentSortMode == FeedbackSortMode.OLDEST)

        tvOverallAverageValue.text = state.overallAverageText
        tvCoffeeAverageValue.text = state.coffeeAverageText
        tvCakeAverageValue.text = state.cakeAverageText
        tvTotalReviewsValue.text = state.totalReviewsText
        tvCommentsRateValue.text = state.commentsRateText
        tvHiddenCommentsValue.text = state.hiddenCommentsText

        progressFive.progress = state.progressFive
        progressFour.progress = state.progressFour
        progressThree.progress = state.progressThree
        progressTwo.progress = state.progressTwo
        progressOne.progress = state.progressOne

        tvCountFive.text = state.countFive
        tvCountFour.text = state.countFour
        tvCountThree.text = state.countThree
        tvCountTwo.text = state.countTwo
        tvCountOne.text = state.countOne

        adapter.submitList(state.filteredItems)
        tvFeedCount.text = state.feedCountText
        emptyStateCard.visibility = if (state.showEmpty) View.VISIBLE else View.GONE
    }

    private fun setChipSelected(chip: TextView, isSelected: Boolean) {
        chip.setBackgroundResource(
            if (isSelected) R.drawable.bg_orders_filter_chip_selected
            else R.drawable.bg_orders_filter_chip
        )
        chip.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (isSelected) R.color.kc_text_primary else R.color.kc_text_secondary
            )
        )
        chip.setTypeface(null, Typeface.BOLD)
        chip.alpha = if (isSelected) 1f else 0.92f
    }

    private fun showDeleteDialog(item: AdminFeedbackItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete review?")
            .setMessage("This will permanently remove the rating and comment for ${item.productName}.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteFeedback(item.feedbackId)
            }
            .show()
    }

    private fun toggleHideState(item: AdminFeedbackItem) {
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
                viewModel.toggleHideState(item)
            }
            .show()
    }
}