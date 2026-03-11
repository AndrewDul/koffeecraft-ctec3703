package uk.ac.dmu.koffeecraft.ui.admin.feedback

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.dao.AdminFeedbackItem
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase

class AdminFeedbackFragment : Fragment(R.layout.fragment_admin_feedback) {

    private lateinit var adapter: AdminFeedbackAdapter
    private lateinit var tvEmpty: TextView

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

        val rv = view.findViewById<RecyclerView>(R.id.rvAdminFeedback)
        val spinnerRatingFilter = view.findViewById<Spinner>(R.id.spinnerRatingFilter)
        val spinnerCommentFilter = view.findViewById<Spinner>(R.id.spinnerCommentFilter)
        val spinnerCategoryFilter = view.findViewById<Spinner>(R.id.spinnerCategoryFilter)
        val spinnerSortMode = view.findViewById<Spinner>(R.id.spinnerSortMode)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = AdminFeedbackAdapter(
            items = emptyList(),
            onDelete = { item -> showDeleteDialog(db, item) },
            onHideToggle = { item -> toggleHideState(db, item) }
        )
        rv.adapter = adapter

        setupRatingFilterSpinner(spinnerRatingFilter)
        setupCommentFilterSpinner(spinnerCommentFilter)
        setupCategoryFilterSpinner(spinnerCategoryFilter)
        setupSortSpinner(spinnerSortMode)

        viewLifecycleOwner.lifecycleScope.launch {
            db.feedbackDao().observeAllForAdmin().collect { items ->
                allFeedback = items
                applyFiltersAndSorting()
            }
        }
    }

    private fun setupRatingFilterSpinner(spinner: Spinner) {
        val options = listOf("All ratings", "1 star", "2 stars", "3 stars", "4 stars", "5 stars")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinner.adapter = spinnerAdapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentRatingFilter = when (position) {
                    1 -> RatingFilter.ONE
                    2 -> RatingFilter.TWO
                    3 -> RatingFilter.THREE
                    4 -> RatingFilter.FOUR
                    5 -> RatingFilter.FIVE
                    else -> RatingFilter.ALL
                }
                applyFiltersAndSorting()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun setupCommentFilterSpinner(spinner: Spinner) {
        val options = listOf("All comments", "With comment", "Without comment")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinner.adapter = spinnerAdapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentCommentFilter = when (position) {
                    1 -> CommentFilter.WITH_COMMENT
                    2 -> CommentFilter.WITHOUT_COMMENT
                    else -> CommentFilter.ALL
                }
                applyFiltersAndSorting()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun setupCategoryFilterSpinner(spinner: Spinner) {
        val options = listOf("All categories", "Coffee", "Cake")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinner.adapter = spinnerAdapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentCategoryFilter = when (position) {
                    1 -> CategoryFilter.COFFEE
                    2 -> CategoryFilter.CAKE
                    else -> CategoryFilter.ALL
                }
                applyFiltersAndSorting()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun setupSortSpinner(spinner: Spinner) {
        val options = listOf("Newest first", "Oldest first")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinner.adapter = spinnerAdapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentSortMode = if (position == 1) SortMode.OLDEST else SortMode.NEWEST
                applyFiltersAndSorting()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
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
        tvEmpty.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showDeleteDialog(db: KoffeeCraftDatabase, item: AdminFeedbackItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete feedback?")
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
