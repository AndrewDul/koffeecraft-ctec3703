package uk.ac.dmu.koffeecraft.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.cart.CartManager
import uk.ac.dmu.koffeecraft.data.repository.CheckoutRepository
import uk.ac.dmu.koffeecraft.util.validation.CheckoutCardFormValidator
import uk.ac.dmu.koffeecraft.util.validation.CheckoutCardValidationResult

class CheckoutViewModel(
    private val checkoutRepository: CheckoutRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CheckoutUiState())
    val state: StateFlow<CheckoutUiState> = _state

    private val _effects = Channel<CheckoutUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var savedCardsJob: Job? = null
    private var startedCustomerId: Long? = null

    fun start(customerId: Long) {
        if (startedCustomerId == customerId && savedCardsJob != null) {
            return
        }

        startedCustomerId = customerId
        refreshCartSummary()

        savedCardsJob?.cancel()
        savedCardsJob = viewModelScope.launch {
            checkoutRepository.observeSavedCards(customerId).collect { cards ->
                val currentSelection = _state.value.selectedSavedCardId

                val nextSelection = when {
                    cards.isEmpty() -> null
                    currentSelection != null && cards.any { it.cardId == currentSelection } -> currentSelection
                    else -> cards.firstOrNull { it.isDefault }?.cardId ?: cards.firstOrNull()?.cardId
                }

                _state.value = _state.value.copy(
                    savedCards = cards,
                    selectedSavedCardId = nextSelection
                )
            }
        }
    }

    fun selectPaymentType(paymentType: String) {
        _state.value = _state.value.copy(
            paymentType = paymentType,
            cardValidation = CheckoutCardValidationResult()
        )
    }

    fun selectSavedCard(cardId: Long) {
        _state.value = _state.value.copy(
            selectedSavedCardId = cardId,
            cardValidation = CheckoutCardValidationResult()
        )
    }

    fun clearCardValidation() {
        if (_state.value.cardValidation != CheckoutCardValidationResult()) {
            _state.value = _state.value.copy(
                cardValidation = CheckoutCardValidationResult()
            )
        }
    }

    fun submitOrder(
        customerId: Long,
        cardNickname: String,
        cardholderName: String,
        cardNumber: String,
        expiryText: String,
        cvv: String,
        saveNewCardForFuture: Boolean
    ) {
        val items = CartManager.getItems()
        if (items.isEmpty()) {
            viewModelScope.launch {
                _effects.send(CheckoutUiEffect.ShowMessage("Cart is empty."))
            }
            return
        }

        val total = CartManager.total()
        val beansToSpend = CartManager.beansToSpend()
        val beansToEarn = CartManager.purchasedProductCountForBeans()
        val currentState = _state.value

        if (currentState.paymentType == "CARD") {
            val validation = CheckoutCardFormValidator.validate(
                holder = cardholderName,
                number = cardNumber,
                expiry = expiryText,
                cvv = cvv,
                selectedSavedCardId = currentState.selectedSavedCardId
            )

            if (!validation.isValid) {
                _state.value = currentState.copy(cardValidation = validation)

                if (validation.generalError != null) {
                    viewModelScope.launch {
                        _effects.send(CheckoutUiEffect.ShowMessage(validation.generalError))
                    }
                }
                return
            }
        }

        _state.value = currentState.copy(
            isSubmitting = true,
            cardValidation = CheckoutCardValidationResult()
        )

        viewModelScope.launch {
            val result = checkoutRepository.submitOrder(
                customerId = customerId,
                items = items,
                paymentType = _state.value.paymentType,
                totalAmount = total,
                beansToSpend = beansToSpend,
                beansToEarn = beansToEarn,
                saveNewCardForFuture = saveNewCardForFuture,
                cardNickname = cardNickname,
                cardholderName = cardholderName,
                cardNumber = cardNumber,
                expiryText = expiryText
            )

            when (result) {
                is CheckoutRepository.CheckoutSubmissionResult.Error -> {
                    _state.value = _state.value.copy(isSubmitting = false)
                    _effects.send(CheckoutUiEffect.ShowMessage(result.message))
                }

                is CheckoutRepository.CheckoutSubmissionResult.Success -> {
                    val paymentMessage = when (_state.value.paymentType) {
                        "APPLE_PAY" -> "Apple Pay payment successful."
                        "CASH" -> "Cash payment selected for collection."
                        else -> "Card payment successful."
                    }

                    CartManager.clear()

                    _state.value = _state.value.copy(
                        isSubmitting = false,
                        total = 0.0,
                        beansToSpend = 0,
                        cardValidation = CheckoutCardValidationResult()
                    )

                    _effects.send(
                        CheckoutUiEffect.CheckoutCompleted(
                            orderId = result.orderId,
                            paymentMessage = paymentMessage
                        )
                    )
                }
            }
        }
    }

    private fun refreshCartSummary() {
        _state.value = _state.value.copy(
            total = CartManager.total(),
            beansToSpend = CartManager.beansToSpend()
        )
    }
}