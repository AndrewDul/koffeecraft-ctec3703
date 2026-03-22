package uk.ac.dmu.koffeecraft.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.entities.InboxMessage
import uk.ac.dmu.koffeecraft.data.repository.CustomerInboxRepository

enum class InboxFilter {
    ALL,
    READ,
    UNREAD,
    PROMO,
    IMPORTANT,
    SERVICE
}

data class CustomerInboxUiState(
    val allItems: List<InboxMessage> = emptyList(),
    val filteredItems: List<InboxMessage> = emptyList(),
    val currentFilter: InboxFilter = InboxFilter.ALL,
    val isEmpty: Boolean = true,
    val emptyMessage: String = "No messages yet."
)

class CustomerInboxViewModel(
    private val repository: CustomerInboxRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CustomerInboxUiState())
    val state: StateFlow<CustomerInboxUiState> = _state

    private var observeJob: Job? = null

    fun start(customerId: Long, launchInboxMessageId: Long?) {
        if (observeJob != null) return

        if (launchInboxMessageId != null && launchInboxMessageId > 0L) {
            viewModelScope.launch {
                repository.markAsRead(launchInboxMessageId)
            }
        }

        observeJob = viewModelScope.launch {
            repository.observeInbox(customerId).collect { items ->
                _state.update { current ->
                    val filtered = filterItems(items, current.currentFilter)
                    current.copy(
                        allItems = items,
                        filteredItems = filtered,
                        isEmpty = filtered.isEmpty(),
                        emptyMessage = if (items.isEmpty()) {
                            "No messages yet."
                        } else {
                            "No messages match this filter."
                        }
                    )
                }
            }
        }
    }

    fun setFilter(filter: InboxFilter) {
        _state.update { current ->
            val filtered = filterItems(current.allItems, filter)
            current.copy(
                currentFilter = filter,
                filteredItems = filtered,
                isEmpty = filtered.isEmpty(),
                emptyMessage = if (current.allItems.isEmpty()) {
                    "No messages yet."
                } else {
                    "No messages match this filter."
                }
            )
        }
    }

    fun deleteMessage(inboxMessageId: Long) {
        viewModelScope.launch {
            repository.deleteMessage(inboxMessageId)
        }
    }

    fun openMessage(item: InboxMessage) {
        if (item.isRead) return

        viewModelScope.launch {
            repository.markAsRead(item.inboxMessageId)
        }
    }

    private fun filterItems(
        items: List<InboxMessage>,
        filter: InboxFilter
    ): List<InboxMessage> {
        return when (filter) {
            InboxFilter.ALL -> items
            InboxFilter.READ -> items.filter { it.isRead }
            InboxFilter.UNREAD -> items.filter { !it.isRead }
            InboxFilter.PROMO -> items.filter { it.deliveryType.startsWith("PROMO") }
            InboxFilter.IMPORTANT -> items.filter { it.deliveryType.startsWith("IMPORTANT") }
            InboxFilter.SERVICE -> items.filter { it.deliveryType.startsWith("SERVICE") }
        }
    }

    class Factory(
        private val repository: CustomerInboxRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CustomerInboxViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CustomerInboxViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}