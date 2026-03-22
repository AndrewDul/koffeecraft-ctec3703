package uk.ac.dmu.koffeecraft.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.repository.CustomerSettingsRepository

data class CustomerSettingsUiState(
    val customerName: String = "",
    val customerEmail: String = "",
    val darkModeEnabled: Boolean = false,
    val profileMissing: Boolean = false
)

class CustomerSettingsViewModel(
    private val repository: CustomerSettingsRepository
) : ViewModel() {

    sealed interface UiEffect {
        data class ShowMessage(val message: String) : UiEffect
        data object NavigateToSignedOut : UiEffect
    }

    private val _state = MutableStateFlow(CustomerSettingsUiState())
    val state: StateFlow<CustomerSettingsUiState> = _state

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun load(customerId: Long?) {
        viewModelScope.launch {
            val data = repository.loadScreenData(customerId)

            val profileMissing = customerId != null &&
                    (data.customerName == null || data.customerEmail == null)

            _state.value = CustomerSettingsUiState(
                customerName = data.customerName ?: "",
                customerEmail = data.customerEmail ?: "",
                darkModeEnabled = data.darkModeEnabled,
                profileMissing = profileMissing
            )

            if (customerId == null) {
                _effects.send(UiEffect.ShowMessage("Please sign in first."))
            } else if (profileMissing) {
                _effects.send(UiEffect.ShowMessage("Customer profile could not be found."))
            }
        }
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        repository.setDarkModeEnabled(enabled)
        _state.value = _state.value.copy(darkModeEnabled = enabled)
    }

    fun signOut() {
        repository.signOut()
        viewModelScope.launch {
            _effects.send(UiEffect.NavigateToSignedOut)
        }
    }

    class Factory(
        private val repository: CustomerSettingsRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CustomerSettingsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CustomerSettingsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}