package uk.ac.dmu.koffeecraft.ui.admin.orders

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.core.di.appContainer
import uk.ac.dmu.koffeecraft.data.repository.AdminOrderSearchMode
import uk.ac.dmu.koffeecraft.data.repository.AdminOrderSortDirection

class AdminOrdersFragment : Fragment(R.layout.fragment_admin_orders) {

    private lateinit var vm: AdminOrdersViewModel
    private lateinit var adapter: AdminOrdersAdapter

    private lateinit var tvEmpty: TextView
    private lateinit var tvFeedSummary: TextView

    private lateinit var tvSearchByOrderId: TextView
    private lateinit var tvSearchByCustomerId: TextView
    private lateinit var tvSearchHint: TextView
    private lateinit var tilSearch: TextInputLayout
    private lateinit var etSearch: TextInputEditText
    private lateinit var btnFind: MaterialButton

    private lateinit var tvStatusAll: TextView
    private lateinit var tvStatusPlaced: TextView
    private lateinit var tvStatusPreparing: TextView
    private lateinit var tvStatusReady: TextView
    private lateinit var tvStatusCollected: TextView

    private lateinit var tvSortNewest: TextView
    private lateinit var tvSortOldest: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vm = ViewModelProvider(
            this,
            AdminOrdersViewModel.Factory(appContainer.adminOrdersRepository)
        )[AdminOrdersViewModel::class.java]

        val rv = view.findViewById<RecyclerView>(R.id.rvAdminOrders)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        tvFeedSummary = view.findViewById(R.id.tvFeedSummary)

        bindFilterViews(view)

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.isNestedScrollingEnabled = false
        rv.setHasFixedSize(false)
        rv.itemAnimator = null

        adapter = AdminOrdersAdapter(
            items = emptyList(),
            expandedOrderId = null,
            loadingOrderId = null,
            detailByOrderId = emptyMap(),
            onToggleExpand = { row -> vm.toggleExpand(row) },
            onStatusAction = { row, targetStatus -> vm.updateStatus(row, targetStatus) }
        )
        rv.adapter = adapter

        setupSearchModeClicks()
        setupFilterClicks()
        setupSearchActions()

        viewLifecycleOwner.lifecycleScope.launch {
            vm.state.collect { state ->
                renderState(state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.effects.collect { effect ->
                when (effect) {
                    is AdminOrdersViewModel.UiEffect.ShowMessage -> {
                        android.widget.Toast.makeText(
                            requireContext(),
                            effect.message,
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun bindFilterViews(view: View) {
        tvSearchByOrderId = view.findViewById(R.id.tvSearchByOrderId)
        tvSearchByCustomerId = view.findViewById(R.id.tvSearchByCustomerId)
        tvSearchHint = view.findViewById(R.id.tvSearchHint)
        tilSearch = view.findViewById(R.id.tilSearch)
        etSearch = view.findViewById(R.id.etSearch)
        btnFind = view.findViewById(R.id.btnFind)

        tvStatusAll = view.findViewById(R.id.tvStatusAll)
        tvStatusPlaced = view.findViewById(R.id.tvStatusPlaced)
        tvStatusPreparing = view.findViewById(R.id.tvStatusPreparing)
        tvStatusReady = view.findViewById(R.id.tvStatusReady)
        tvStatusCollected = view.findViewById(R.id.tvStatusCollected)

        tvSortNewest = view.findViewById(R.id.tvSortNewest)
        tvSortOldest = view.findViewById(R.id.tvSortOldest)
    }

    private fun setupSearchModeClicks() {
        tvSearchByOrderId.setOnClickListener {
            vm.setSearchMode(AdminOrderSearchMode.ORDER_ID)
            etSearch.setText("")
        }

        tvSearchByCustomerId.setOnClickListener {
            vm.setSearchMode(AdminOrderSearchMode.CUSTOMER_ID)
            etSearch.setText("")
        }
    }

    private fun setupSearchActions() {
        btnFind.setOnClickListener {
            vm.submitSearch(etSearch.text?.toString().orEmpty())
        }

        etSearch.setOnEditorActionListener { _, _, _ ->
            vm.submitSearch(etSearch.text?.toString().orEmpty())
            true
        }
    }

    private fun setupFilterClicks() {
        tvStatusAll.setOnClickListener { vm.setStatusFilter(null) }
        tvStatusPlaced.setOnClickListener { vm.setStatusFilter("PLACED") }
        tvStatusPreparing.setOnClickListener { vm.setStatusFilter("PREPARING") }
        tvStatusReady.setOnClickListener { vm.setStatusFilter("READY") }
        tvStatusCollected.setOnClickListener { vm.setStatusFilter("COLLECTED") }

        tvSortNewest.setOnClickListener {
            vm.setSortDirection(AdminOrderSortDirection.DESC)
        }

        tvSortOldest.setOnClickListener {
            vm.setSortDirection(AdminOrderSortDirection.ASC)
        }
    }

    private fun renderState(state: AdminOrdersUiState) {
        renderSearchMode(state.currentSearchMode)
        renderFilterChipStyles(state)
        renderAdapterState(state)

        tvFeedSummary.text = state.summaryText
        tvEmpty.visibility = if (state.isEmpty) View.VISIBLE else View.GONE
    }

    private fun renderSearchMode(searchMode: AdminOrderSearchMode) {
        setChipSelected(tvSearchByOrderId, searchMode == AdminOrderSearchMode.ORDER_ID)
        setChipSelected(tvSearchByCustomerId, searchMode == AdminOrderSearchMode.CUSTOMER_ID)

        when (searchMode) {
            AdminOrderSearchMode.ORDER_ID -> {
                tvSearchHint.text = "Find order by order ID"
                tilSearch.hint = "Enter order ID"
                etSearch.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }

            AdminOrderSearchMode.CUSTOMER_ID -> {
                tvSearchHint.text = "Find orders by customer ID"
                tilSearch.hint = "Enter customer ID"
                etSearch.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }
        }
    }

    private fun renderFilterChipStyles(state: AdminOrdersUiState) {
        setChipSelected(tvStatusAll, state.currentStatusFilter == null)
        setChipSelected(tvStatusPlaced, state.currentStatusFilter == "PLACED")
        setChipSelected(tvStatusPreparing, state.currentStatusFilter == "PREPARING")
        setChipSelected(tvStatusReady, state.currentStatusFilter == "READY")
        setChipSelected(tvStatusCollected, state.currentStatusFilter == "COLLECTED")

        setChipSelected(tvSortNewest, state.sortDirection == AdminOrderSortDirection.DESC)
        setChipSelected(tvSortOldest, state.sortDirection == AdminOrderSortDirection.ASC)
    }

    private fun renderAdapterState(state: AdminOrdersUiState) {
        adapter.submitState(
            items = state.rows,
            expandedOrderId = state.expandedOrderId,
            loadingOrderId = state.loadingOrderId,
            detailByOrderId = state.detailByOrderId
        )
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
}