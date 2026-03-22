package uk.ac.dmu.koffeecraft.ui.admin.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.dto.AdminOrderRow
import uk.ac.dmu.koffeecraft.data.repository.AdminOrderSearchMode
import uk.ac.dmu.koffeecraft.data.repository.AdminOrderSortDirection
import uk.ac.dmu.koffeecraft.data.repository.AdminOrdersRepository
import uk.ac.dmu.koffeecraft.data.repository.SettingsActionResult

data class AdminOrdersUiState(
    val rows: List<AdminOrderRow> = emptyList(),
    val expandedOrderId: Long? = null,
    val loadingOrderId: Long? = null,
    val detailByOrderId: Map<Long, AdminOrderDetailsUi> = emptyMap(),
    val currentStatusFilter: String? = null,
    val currentSubmittedQuery: String = "",
    val currentSearchMode: AdminOrderSearchMode = AdminOrderSearchMode.ORDER_ID,
    val sortDirection: AdminOrderSortDirection = AdminOrderSortDirection.DESC,
    val summaryText: String = "No orders match the current search or filters",
    val isEmpty: Boolean = true
)

class AdminOrdersViewModel(
    private val adminOrdersRepository: AdminOrdersRepository
) : ViewModel() {

    sealed interface UiEffect {
        data class ShowMessage(val message: String) : UiEffect
    }

    private val _state = MutableStateFlow(AdminOrdersUiState())
    val state: StateFlow<AdminOrdersUiState> = _state

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var collectJob: Job? = null
    private val detailCache = mutableMapOf<Long, AdminOrderDetailsUi>()
    private var currentRows: List<AdminOrderRow> = emptyList()

    init {
        restartCollection()
    }

    fun setSearchMode(searchMode: AdminOrderSearchMode) {
        if (_state.value.currentSearchMode == searchMode) return

        _state.value = _state.value.copy(
            currentSearchMode = searchMode,
            currentSubmittedQuery = "",
            expandedOrderId = null,
            loadingOrderId = null
        )
        restartCollection()
    }

    fun submitSearch(query: String) {
        _state.value = _state.value.copy(
            currentSubmittedQuery = query.trim(),
            expandedOrderId = null,
            loadingOrderId = null
        )
        restartCollection()
    }

    fun setStatusFilter(status: String?) {
        if (_state.value.currentStatusFilter == status) return

        _state.value = _state.value.copy(
            currentStatusFilter = status,
            expandedOrderId = null,
            loadingOrderId = null
        )
        restartCollection()
    }

    fun setSortDirection(sortDirection: AdminOrderSortDirection) {
        if (_state.value.sortDirection == sortDirection) return

        _state.value = _state.value.copy(
            sortDirection = sortDirection,
            expandedOrderId = null,
            loadingOrderId = null
        )
        restartCollection()
    }

    fun toggleExpand(row: AdminOrderRow) {
        if (_state.value.expandedOrderId == row.orderId) {
            _state.value = _state.value.copy(
                expandedOrderId = null,
                loadingOrderId = null
            )
            publishState()
            return
        }

        _state.value = _state.value.copy(
            expandedOrderId = row.orderId
        )

        if (detailCache.containsKey(row.orderId)) {
            _state.value = _state.value.copy(loadingOrderId = null)
            publishState()
        } else {
            _state.value = _state.value.copy(loadingOrderId = row.orderId)
            publishState()
            loadOrderDetails(row.orderId)
        }
    }

    fun updateStatus(
        row: AdminOrderRow,
        targetStatus: String
    ) {
        if (row.status == targetStatus) return

        viewModelScope.launch {
            when (val result = adminOrdersRepository.updateOrderStatus(row.orderId, targetStatus)) {
                is SettingsActionResult.Success -> {
                    detailCache.remove(row.orderId)
                    _effects.send(UiEffect.ShowMessage(result.message))

                    if (_state.value.expandedOrderId == row.orderId) {
                        _state.value = _state.value.copy(loadingOrderId = row.orderId)
                        publishState()
                        loadOrderDetails(row.orderId)
                    }
                }

                is SettingsActionResult.Error -> {
                    _effects.send(UiEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun restartCollection() {
        collectJob?.cancel()

        collectJob = viewModelScope.launch {
            adminOrdersRepository.observeOrders(
                status = _state.value.currentStatusFilter,
                query = _state.value.currentSubmittedQuery,
                searchMode = _state.value.currentSearchMode,
                sortDirection = _state.value.sortDirection
            ).collectLatest { rows ->
                currentRows = rows

                val expanded = _state.value.expandedOrderId
                if (expanded != null) {
                    val expandedRow = rows.firstOrNull { it.orderId == expanded }
                    if (expandedRow == null) {
                        _state.value = _state.value.copy(
                            expandedOrderId = null,
                            loadingOrderId = null
                        )
                    } else {
                        val cached = detailCache[expanded]
                        if (cached != null && !cached.status.equals(expandedRow.status, ignoreCase = true)) {
                            detailCache.remove(expanded)
                            _state.value = _state.value.copy(loadingOrderId = expanded)
                            publishState()
                            loadOrderDetails(expanded)
                        }
                    }
                }

                publishState()
            }
        }
    }

    private fun loadOrderDetails(orderId: Long) {
        viewModelScope.launch {
            val details = adminOrdersRepository.loadOrderDetails(orderId)

            if (details == null) {
                detailCache.remove(orderId)
                if (_state.value.expandedOrderId == orderId) {
                    _state.value = _state.value.copy(loadingOrderId = null)
                    publishState()
                }
                return@launch
            }

            detailCache[orderId] = details
            if (_state.value.expandedOrderId == orderId) {
                _state.value = _state.value.copy(loadingOrderId = null)
                publishState()
            }
        }
    }

    private fun publishState() {
        _state.value = _state.value.copy(
            rows = currentRows,
            detailByOrderId = detailCache.toMap(),
            summaryText = if (currentRows.isEmpty()) {
                "No orders match the current search or filters"
            } else {
                "Showing ${currentRows.size} order${if (currentRows.size == 1) "" else "s"}"
            },
            isEmpty = currentRows.isEmpty()
        )
    }

    class Factory(
        private val adminOrdersRepository: AdminOrdersRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AdminOrdersViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AdminOrdersViewModel(adminOrdersRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}