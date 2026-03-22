package uk.ac.dmu.koffeecraft.ui.admin.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.entities.AppNotification
import uk.ac.dmu.koffeecraft.data.repository.AdminNotificationsActionResult
import uk.ac.dmu.koffeecraft.data.repository.AdminNotificationsRepository

data class AdminNotificationsUiState(
    val items: List<AppNotification> = emptyList(),
    val queueSummary: String = "No admin notifications right now.",
    val isEmpty: Boolean = true
)

class AdminNotificationsViewModel(
    private val repository: AdminNotificationsRepository
) : ViewModel() {

    sealed interface UiEffect {
        data class ShowMessage(val message: String) : UiEffect
    }

    private val _state = MutableStateFlow(AdminNotificationsUiState())
    val state: StateFlow<AdminNotificationsUiState> = _state

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var observeJob: Job? = null
    private var markReadJobStarted = false

    fun start() {
        if (!markReadJobStarted) {
            markReadJobStarted = true
            viewModelScope.launch {
                repository.markAllAsRead()
            }
        }

        if (observeJob != null) return

        observeJob = viewModelScope.launch {
            repository.observeAdminNotifications().collect { data ->
                _state.update {
                    it.copy(
                        items = data.items,
                        queueSummary = data.queueSummary,
                        isEmpty = data.items.isEmpty()
                    )
                }
            }
        }
    }

    fun deleteNotification(item: AppNotification) {
        viewModelScope.launch {
            when (val result = repository.deleteNotification(item)) {
                AdminNotificationsActionResult.Success -> Unit
                is AdminNotificationsActionResult.Error -> {
                    _effects.send(UiEffect.ShowMessage(result.message))
                }
            }
        }
    }

    fun advanceOrder(item: AppNotification) {
        viewModelScope.launch {
            when (val result = repository.advanceOrder(item)) {
                AdminNotificationsActionResult.Success -> Unit
                is AdminNotificationsActionResult.Error -> {
                    _effects.send(UiEffect.ShowMessage(result.message))
                }
            }
        }
    }

    class Factory(
        private val repository: AdminNotificationsRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AdminNotificationsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AdminNotificationsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}