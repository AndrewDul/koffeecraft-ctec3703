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
import uk.ac.dmu.koffeecraft.data.repository.SettingsActionResult
import uk.ac.dmu.koffeecraft.data.session.SessionRepository

class CustomerDeleteAccountViewModel(
    private val customerSettingsRepository: CustomerSettingsRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    data class UiState(
        val isDeleting: Boolean = false
    )

    sealed interface UiEffect {
        data class ShowMessage(val message: String) : UiEffect
        data object NavigateToLogout : UiEffect
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun deleteAccount(currentPassword: String) {
        val customerId = sessionRepository.currentCustomerId
        if (customerId == null) {
            viewModelScope.launch {
                _effects.send(UiEffect.ShowMessage("Please sign in first."))
            }
            return
        }

        _state.value = _state.value.copy(isDeleting = true)

        viewModelScope.launch {
            when (
                val result = customerSettingsRepository.deleteAccount(
                    customerId = customerId,
                    currentPassword = currentPassword
                )
            ) {
                is SettingsActionResult.Success -> {
                    _state.value = _state.value.copy(isDeleting = false)
                    _effects.send(UiEffect.ShowMessage(result.message))
                    _effects.send(UiEffect.NavigateToLogout)
                }

                is SettingsActionResult.Error -> {
                    _state.value = _state.value.copy(isDeleting = false)
                    _effects.send(UiEffect.ShowMessage(result.message))
                }
            }
        }
    }

    class Factory(
        private val customerSettingsRepository: CustomerSettingsRepository,
        private val sessionRepository: SessionRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CustomerDeleteAccountViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CustomerDeleteAccountViewModel(
                    customerSettingsRepository = customerSettingsRepository,
                    sessionRepository = sessionRepository
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}