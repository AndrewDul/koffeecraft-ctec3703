package uk.ac.dmu.koffeecraft.ui.admin.orders

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.dto.AdminOrderRow
import uk.ac.dmu.koffeecraft.util.notifications.AdminNotificationManager
import uk.ac.dmu.koffeecraft.util.notifications.CustomerNotificationManager
class AdminOrdersFragment : Fragment(R.layout.fragment_admin_orders) {

    private lateinit var adapter: AdminOrdersAdapter
    private lateinit var db: KoffeeCraftDatabase

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

    private var currentStatusFilter: String? = null
    private var currentSubmittedQuery: String = ""
    private var currentSearchMode: SearchMode = SearchMode.ORDER_ID
    private var sortDir: String = "DESC"

    private var collectJob: Job? = null

    private var currentRows: List<AdminOrderRow> = emptyList()
    private var expandedOrderId: Long? = null
    private var loadingOrderId: Long? = null
    private val detailCache = mutableMapOf<Long, AdminOrderDetailsUi>()

    private enum class SearchMode {
        ORDER_ID,
        CUSTOMER_ID
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        val rv = view.findViewById<RecyclerView>(R.id.rvAdminOrders)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        tvFeedSummary = view.findViewById(R.id.tvFeedSummary)

        bindFilterViews(view)
        setupFilterDefaults()

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.isNestedScrollingEnabled = false
        rv.setHasFixedSize(false)
        rv.itemAnimator = null
        rv.setHasFixedSize(false)
        rv.itemAnimator = null

        adapter = AdminOrdersAdapter(
            items = emptyList(),
            expandedOrderId = null,
            loadingOrderId = null,
            detailByOrderId = emptyMap(),
            onToggleExpand = { row -> handleExpandToggle(row) },
            onStatusAction = { row, targetStatus -> handleStatusAction(row, targetStatus) }
        )
        rv.adapter = adapter

        setupSearchModeClicks()
        setupFilterClicks()
        setupSearchActions()

        applySearchMode(SearchMode.ORDER_ID)
        startCollecting()
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

    private fun setupFilterDefaults() {
        currentStatusFilter = null
        currentSubmittedQuery = ""
        currentSearchMode = SearchMode.ORDER_ID
        sortDir = "DESC"
        updateFilterChipStyles()
    }

    private fun setupSearchModeClicks() {
        tvSearchByOrderId.setOnClickListener {
            applySearchMode(SearchMode.ORDER_ID)
        }

        tvSearchByCustomerId.setOnClickListener {
            applySearchMode(SearchMode.CUSTOMER_ID)
        }
    }

    private fun applySearchMode(mode: SearchMode) {
        currentSearchMode = mode

        setChipSelected(tvSearchByOrderId, mode == SearchMode.ORDER_ID)
        setChipSelected(tvSearchByCustomerId, mode == SearchMode.CUSTOMER_ID)

        when (mode) {
            SearchMode.ORDER_ID -> {
                tvSearchHint.text = "Find order by order ID"
                tilSearch.hint = "Enter order ID"
                etSearch.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }

            SearchMode.CUSTOMER_ID -> {
                tvSearchHint.text = "Find orders by customer ID"
                tilSearch.hint = "Enter customer ID"
                etSearch.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }
        }

        etSearch.setText("")
        currentSubmittedQuery = ""
        startCollecting()
    }

    private fun setupSearchActions() {
        btnFind.setOnClickListener {
            currentSubmittedQuery = etSearch.text?.toString()?.trim().orEmpty()
            startCollecting()
        }

        etSearch.setOnEditorActionListener { _, _, _ ->
            currentSubmittedQuery = etSearch.text?.toString()?.trim().orEmpty()
            startCollecting()
            true
        }
    }

    private fun setupFilterClicks() {
        tvStatusAll.setOnClickListener {
            currentStatusFilter = null
            updateFilterChipStyles()
            startCollecting()
        }

        tvStatusPlaced.setOnClickListener {
            currentStatusFilter = "PLACED"
            updateFilterChipStyles()
            startCollecting()
        }

        tvStatusPreparing.setOnClickListener {
            currentStatusFilter = "PREPARING"
            updateFilterChipStyles()
            startCollecting()
        }

        tvStatusReady.setOnClickListener {
            currentStatusFilter = "READY"
            updateFilterChipStyles()
            startCollecting()
        }

        tvStatusCollected.setOnClickListener {
            currentStatusFilter = "COLLECTED"
            updateFilterChipStyles()
            startCollecting()
        }

        tvSortNewest.setOnClickListener {
            sortDir = "DESC"
            updateFilterChipStyles()
            startCollecting()
        }

        tvSortOldest.setOnClickListener {
            sortDir = "ASC"
            updateFilterChipStyles()
            startCollecting()
        }
    }

    private fun updateFilterChipStyles() {
        setChipSelected(tvSearchByOrderId, currentSearchMode == SearchMode.ORDER_ID)
        setChipSelected(tvSearchByCustomerId, currentSearchMode == SearchMode.CUSTOMER_ID)

        setChipSelected(tvStatusAll, currentStatusFilter == null)
        setChipSelected(tvStatusPlaced, currentStatusFilter == "PLACED")
        setChipSelected(tvStatusPreparing, currentStatusFilter == "PREPARING")
        setChipSelected(tvStatusReady, currentStatusFilter == "READY")
        setChipSelected(tvStatusCollected, currentStatusFilter == "COLLECTED")

        setChipSelected(tvSortNewest, sortDir == "DESC")
        setChipSelected(tvSortOldest, sortDir == "ASC")
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

    private fun startCollecting() {
        collectJob?.cancel()

        collectJob = viewLifecycleOwner.lifecycleScope.launch {
            db.orderDao()
                .observeAdminOrdersFiltered(
                    status = currentStatusFilter,
                    query = currentSubmittedQuery,
                    searchMode = currentSearchMode.name,
                    sortDir = sortDir
                )
                .collect { rows ->
                    currentRows = rows

                    val expanded = expandedOrderId
                    if (expanded != null) {
                        val expandedRow = rows.firstOrNull { it.orderId == expanded }
                        if (expandedRow == null) {
                            expandedOrderId = null
                            loadingOrderId = null
                        } else {
                            val cached = detailCache[expanded]
                            if (cached != null && !cached.status.equals(expandedRow.status, ignoreCase = true)) {
                                detailCache.remove(expanded)
                                loadingOrderId = expanded
                                loadOrderDetails(expanded)
                            }
                        }
                    }

                    submitAdapterState()
                }
        }
    }

    private fun handleExpandToggle(row: AdminOrderRow) {
        if (expandedOrderId == row.orderId) {
            expandedOrderId = null
            loadingOrderId = null
            submitAdapterState()
            return
        }

        expandedOrderId = row.orderId

        if (detailCache.containsKey(row.orderId)) {
            loadingOrderId = null
            submitAdapterState()
        } else {
            loadingOrderId = row.orderId
            submitAdapterState()
            loadOrderDetails(row.orderId)
        }
    }

    private fun loadOrderDetails(orderId: Long) {
        viewLifecycleOwner.lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val order = db.orderDao().getById(orderId)
            val customer = db.customerDao().getInboxTargetByOrderId(orderId)
            val payment = db.paymentDao().getLatestForOrder(orderId)
            val displayItems = db.orderItemDao().getDisplayItemsForOrder(orderId)
            val feedbackItems = db.orderItemDao().getFeedbackItemsForOrder(orderId)

            if (order == null || customer == null) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    detailCache.remove(orderId)
                    if (expandedOrderId == orderId) {
                        loadingOrderId = null
                        submitAdapterState()
                    }
                }
                return@launch
            }

            val itemLines = displayItems.map { item ->
                AdminOrderLineUi(
                    productName = item.productName,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    selectedOptionLabel = item.selectedOptionLabel,
                    selectedOptionSizeValue = item.selectedOptionSizeValue,
                    selectedOptionSizeUnit = item.selectedOptionSizeUnit,
                    selectedAddOnsSummary = item.selectedAddOnsSummary,
                    estimatedCalories = item.estimatedCalories
                )
            }

            val details = AdminOrderDetailsUi(
                orderId = order.orderId,
                customerId = customer.customerId,
                customerName = "${customer.firstName} ${customer.lastName}".trim(),
                customerEmail = customer.email,
                promoEligible = customer.marketingInboxConsent,
                paymentType = payment?.paymentType ?: "UNKNOWN",
                totalAmount = order.totalAmount,
                createdAt = order.createdAt,
                status = order.status,
                feedbackWritten = feedbackItems.any { it.feedbackId != null },
                hasCraftedItems = itemLines.any { it.isCrafted },
                hasRewardItems = itemLines.any { it.isReward },
                items = itemLines
            )

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                detailCache[orderId] = details
                if (expandedOrderId == orderId) {
                    loadingOrderId = null
                    submitAdapterState()
                }
            }
        }
    }

    private fun handleStatusAction(
        row: AdminOrderRow,
        targetStatus: String
    ) {
        if (row.status == targetStatus) return

        val appContext = requireContext().applicationContext

        viewLifecycleOwner.lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            db.orderDao().updateStatus(row.orderId, targetStatus)

            val updatedOrder = db.orderDao().getById(row.orderId)

            if (updatedOrder != null) {
                CustomerNotificationManager.createCustomerOrderStatusNotification(
                    context = appContext,
                    db = db,
                    customerId = updatedOrder.customerId,
                    orderId = updatedOrder.orderId,
                    orderCreatedAt = updatedOrder.createdAt,
                    status = targetStatus
                )

                AdminNotificationManager.syncAdminOrderActionNotification(
                    context = appContext,
                    db = db,
                    orderId = updatedOrder.orderId,
                    orderCreatedAt = updatedOrder.createdAt,
                    orderStatus = targetStatus,
                    triggerPhoneNotificationForNewOnly = false
                )
            }

            detailCache.remove(row.orderId)

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (expandedOrderId == row.orderId) {
                    loadingOrderId = row.orderId
                    submitAdapterState()
                    loadOrderDetails(row.orderId)
                }
            }
        }
    }

    private fun submitAdapterState() {
        adapter.submitState(
            items = currentRows,
            expandedOrderId = expandedOrderId,
            loadingOrderId = loadingOrderId,
            detailByOrderId = detailCache
        )

        tvFeedSummary.text = if (currentRows.isEmpty()) {
            "No orders match the current search or filters"
        } else {
            "Showing ${currentRows.size} order${if (currentRows.size == 1) "" else "s"}"
        }

        tvEmpty.visibility = if (currentRows.isEmpty()) View.VISIBLE else View.GONE
    }
}

data class AdminOrderDetailsUi(
    val orderId: Long,
    val customerId: Long,
    val customerName: String,
    val customerEmail: String,
    val promoEligible: Boolean,
    val paymentType: String,
    val totalAmount: Double,
    val createdAt: Long,
    val status: String,
    val feedbackWritten: Boolean,
    val hasCraftedItems: Boolean,
    val hasRewardItems: Boolean,
    val items: List<AdminOrderLineUi>
)

data class AdminOrderLineUi(
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val selectedOptionLabel: String?,
    val selectedOptionSizeValue: Int?,
    val selectedOptionSizeUnit: String?,
    val selectedAddOnsSummary: String?,
    val estimatedCalories: Int?
) {
    val isCrafted: Boolean
        get() = !selectedAddOnsSummary.isNullOrBlank() ||
                (!selectedOptionLabel.isNullOrBlank() &&
                        selectedOptionSizeValue != null &&
                        !selectedOptionSizeUnit.isNullOrBlank())

    val isReward: Boolean
        get() = unitPrice <= 0.0
}