package uk.ac.dmu.koffeecraft.ui.orders

import uk.ac.dmu.koffeecraft.data.dao.OrderDisplayItem
import uk.ac.dmu.koffeecraft.data.entities.Order
import uk.ac.dmu.koffeecraft.data.repository.CustomerOrderDateFilter

data class CustomerOrdersUiState(
    val items: List<Order> = emptyList(),
    val detailsByOrderId: Map<Long, List<OrderDisplayItem>> = emptyMap(),
    val feedbackSummaryByOrderId: Map<Long, OrderFeedbackSummary> = emptyMap(),
    val selectedFilter: CustomerOrderDateFilter = CustomerOrderDateFilter.ALL,
    val emptyMessage: String = "No orders yet."
)