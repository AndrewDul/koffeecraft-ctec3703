package uk.ac.dmu.koffeecraft.ui.settings

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
import uk.ac.dmu.koffeecraft.data.entities.CustomerPaymentCard
import uk.ac.dmu.koffeecraft.data.repository.CustomerPaymentMethodsRepository
import uk.ac.dmu.koffeecraft.data.repository.SettingsActionResult

class CustomerPaymentMethodsViewModel(
    private val customerPaymentMethodsRepository: CustomerPaymentMethodsRepository
) : ViewModel() {

    data class UiState(
        val cards: List<CustomerPaymentCard> = emptyList()
    )

    sealed interface UiEffect {
        data class ShowMessage(val message: String) : UiEffect
        data object DismissAddCardDialog : UiEffect
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var observeJob: Job? = null
    private var startedCustomerId: Long? = null

    fun start(customerId: Long) {
        if (startedCustomerId == customerId && observeJob != null) return

        startedCustomerId = customerId
        observeJob?.cancel()

        observeJob = viewModelScope.launch {
            customerPaymentMethodsRepository.observeCards(customerId).collectLatest { cards ->
                _state.value = _state.value.copy(cards = cards)
            }
        }
    }

    fun addCard(
        customerId: Long,
        nickname: String,
        cardholderName: String,
        cardNumber: String,
        expiryText: String,
        setAsDefault: Boolean
    ) {
        viewModelScope.launch {
            when (val result = customerPaymentMethodsRepository.addCard(
                customerId = customerId,
                nickname = nickname,
                cardholderName = cardholderName,
                cardNumber = cardNumber,
                expiryText = expiryText,
                setAsDefault = setAsDefault
            )) {
                is SettingsActionResult.Success -> {
                    _effects.send(UiEffect.ShowMessage(result.message))
                    _effects.send(UiEffect.DismissAddCardDialog)
                }

                is SettingsActionResult.Error -> {
                    _effects.send(UiEffect.ShowMessage(result.message))
                }
            }
        }
    }

    fun setDefaultCard(
        customerId: Long,
        card: CustomerPaymentCard
    ) {
        viewModelScope.launch {
            when (val result = customerPaymentMethodsRepository.setDefaultCard(
                customerId = customerId,
                card = card
            )) {
                is SettingsActionResult.Success -> {
                    _effects.send(UiEffect.ShowMessage(result.message))
                }

                is SettingsActionResult.Error -> {
                    _effects.send(UiEffect.ShowMessage(result.message))
                }
            }
        }
    }

    fun deleteCard(
        customerId: Long,
        card: CustomerPaymentCard
    ) {
        viewModelScope.launch {
            when (val result = customerPaymentMethodsRepository.deleteCard(
                customerId = customerId,
                card = card
            )) {
                is SettingsActionResult.Success -> {
                    _effects.send(UiEffect.ShowMessage(result.message))
                }

                is SettingsActionResult.Error -> {
                    _effects.send(UiEffect.ShowMessage(result.message))
                }
            }
        }
    }

    class Factory(
        private val customerPaymentMethodsRepository: CustomerPaymentMethodsRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CustomerPaymentMethodsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CustomerPaymentMethodsViewModel(customerPaymentMethodsRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}