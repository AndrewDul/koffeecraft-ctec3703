package uk.ac.dmu.koffeecraft.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.dao.OrderDisplayItem
import uk.ac.dmu.koffeecraft.data.entities.AppNotification
import uk.ac.dmu.koffeecraft.data.repository.CustomerNotificationsRepository

data class CustomerNotificationsUiState(
    val items: List<AppNotification> = emptyList(),
    val detailsByOrderId: Map<Long, List<OrderDisplayItem>> = emptyMap(),
    val isEmpty: Boolean = true
)

class CustomerNotificationsViewModel(
    private val repository: CustomerNotificationsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CustomerNotificationsUiState())
    val state: StateFlow<CustomerNotificationsUiState> = _state

    private var observeJob: Job? = null

    fun start(customerId: Long) {
        if (observeJob != null) return

        observeJob = viewModelScope.launch {
            repository.observeCustomerNotifications(customerId).collect { data ->
                _state.update {
                    it.copy(
                        items = data.items,
                        detailsByOrderId = data.detailsByOrderId,
                        isEmpty = data.items.isEmpty()
                    )
                }
            }
        }
    }

    fun deleteNotification(notificationId: Long) {
        viewModelScope.launch {
            repository.deleteNotification(notificationId)
        }
    }

    fun openNotification(item: AppNotification) {
        if (item.isRead) return

        viewModelScope.launch {
            repository.markAsRead(item.notificationId)
        }
    }

    class Factory(
        private val repository: CustomerNotificationsRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CustomerNotificationsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CustomerNotificationsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}