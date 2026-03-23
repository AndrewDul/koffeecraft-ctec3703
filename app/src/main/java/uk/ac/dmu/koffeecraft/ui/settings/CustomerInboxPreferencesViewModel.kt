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

class CustomerInboxPreferencesViewModel(
    private val customerSettingsRepository: CustomerSettingsRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    data class UiState(
        val marketingInboxConsent: Boolean = false,
        val isSaving: Boolean = false
    )

    sealed interface UiEffect {
        data class ShowMessage(val message: String) : UiEffect
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var startedCustomerId: Long? = null

    fun start() {
        val customerId = sessionRepository.currentCustomerId
        if (customerId == null) {
            viewModelScope.launch {
                _effects.send(UiEffect.ShowMessage("Please sign in first."))
            }
            return
        }

        if (startedCustomerId == customerId) return
        startedCustomerId = customerId

        viewModelScope.launch {
            val customer = customerSettingsRepository.getCustomer(customerId)
            if (customer == null) {
                _effects.send(UiEffect.ShowMessage("Customer account could not be found."))
                return@launch
            }

            _state.value = _state.value.copy(
                marketingInboxConsent = customer.marketingInboxConsent
            )
        }
    }

    fun setMarketingInboxConsent(enabled: Boolean) {
        _state.value = _state.value.copy(marketingInboxConsent = enabled)
    }

    fun save() {
        val customerId = sessionRepository.currentCustomerId
        if (customerId == null) {
            viewModelScope.launch {
                _effects.send(UiEffect.ShowMessage("Please sign in first."))
            }
            return
        }

        _state.value = _state.value.copy(isSaving = true)

        viewModelScope.launch {
            when (
                val result = customerSettingsRepository.updateMarketingInboxConsent(
                    customerId = customerId,
                    enabled = _state.value.marketingInboxConsent
                )
            ) {
                is SettingsActionResult.Success -> {
                    _state.value = _state.value.copy(isSaving = false)
                    _effects.send(UiEffect.ShowMessage(result.message))
                }

                is SettingsActionResult.Error -> {
                    _state.value = _state.value.copy(isSaving = false)
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
            if (modelClass.isAssignableFrom(CustomerInboxPreferencesViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CustomerInboxPreferencesViewModel(
                    customerSettingsRepository = customerSettingsRepository,
                    sessionRepository = sessionRepository
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}