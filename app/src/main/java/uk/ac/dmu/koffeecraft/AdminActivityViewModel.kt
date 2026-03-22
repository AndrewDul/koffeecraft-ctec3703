package uk.ac.dmu.koffeecraft

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.repository.AdminActivityRepository

data class AdminActivityUiState(
    val showNotificationBadge: Boolean = false,
    val notificationBadgeText: String = ""
)

class AdminActivityViewModel(
    private val repository: AdminActivityRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AdminActivityUiState())
    val state: StateFlow<AdminActivityUiState> = _state

    private var badgeJob: Job? = null

    fun start() {
        if (badgeJob != null) return

        badgeJob = viewModelScope.launch {
            repository.observeAdminBadge().collect { badgeData ->
                _state.value = AdminActivityUiState(
                    showNotificationBadge = badgeData.showBadge,
                    notificationBadgeText = badgeData.badgeText
                )
            }
        }
    }

    class Factory(
        private val repository: AdminActivityRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AdminActivityViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AdminActivityViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}