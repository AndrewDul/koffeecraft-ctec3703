package uk.ac.dmu.koffeecraft.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.entities.Order
import uk.ac.dmu.koffeecraft.data.repository.CustomerOrderDateFilter
import uk.ac.dmu.koffeecraft.data.repository.CustomerOrdersRepository

class CustomerOrdersViewModel(
    private val customerOrdersRepository: CustomerOrdersRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CustomerOrdersUiState())
    val state: StateFlow<CustomerOrdersUiState> = _state

    private val _effects = Channel<CustomerOrdersUiEffect>(Channel.Factory.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var customerId: Long? = null
    private var observeJob: Job? = null
    private var allOrders: List<Order> = emptyList()

    fun start(customerId: Long) {
        if (this.customerId == customerId && observeJob != null) return

        this.customerId = customerId
        observeJob?.cancel()

        observeJob = viewModelScope.launch {
            customerOrdersRepository.observeOrders(customerId).collectLatest { orders ->
                allOrders = orders
                refreshVisibleOrders()
            }
        }
    }

    fun selectFilter(filter: CustomerOrderDateFilter) {
        if (_state.value.selectedFilter == filter) return
        _state.value = _state.value.copy(selectedFilter = filter)
        refreshVisibleOrders()
    }

    fun hideOrder(orderId: Long) {
        val safeCustomerId = customerId ?: return
        customerOrdersRepository.hideOrder(
            customerId = safeCustomerId,
            orderId = orderId
        )
        refreshVisibleOrders()
    }

    fun orderAgain(orderId: Long) {
        viewModelScope.launch {
            val success = customerOrdersRepository.reorderToCart(orderId)
            if (success) {
                _effects.send(CustomerOrdersUiEffect.NavigateToCart)
            } else {
                _effects.send(
                    CustomerOrdersUiEffect.ShowMessage(
                        "These items are no longer available to reorder."
                    )
                )
            }
        }
    }

    private fun refreshVisibleOrders() {
        val safeCustomerId = customerId ?: return
        val selectedFilter = _state.value.selectedFilter

        viewModelScope.launch {
            val renderData = customerOrdersRepository.buildRenderData(
                customerId = safeCustomerId,
                allOrders = allOrders,
                filter = selectedFilter
            )

            _state.value = _state.value.copy(
                items = renderData.visibleOrders,
                detailsByOrderId = renderData.detailsByOrderId,
                feedbackSummaryByOrderId = renderData.feedbackByOrderId.mapValues { (_, counters) ->
                    OrderFeedbackSummary(
                        eligibleItemCount = counters.eligibleItemCount,
                        reviewedItemCount = counters.reviewedItemCount
                    )
                },
                emptyMessage = renderData.emptyMessage
            )
        }
    }
}