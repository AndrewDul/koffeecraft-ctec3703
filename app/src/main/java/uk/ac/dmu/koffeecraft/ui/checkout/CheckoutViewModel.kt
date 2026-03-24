package uk.ac.dmu.koffeecraft.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.repository.CartRepository
import uk.ac.dmu.koffeecraft.data.repository.CheckoutRepository
import uk.ac.dmu.koffeecraft.data.session.SessionRepository
import uk.ac.dmu.koffeecraft.util.validation.CheckoutCardFormValidator
import uk.ac.dmu.koffeecraft.util.validation.CheckoutCardValidationResult

class CheckoutViewModel(
    private val checkoutRepository: CheckoutRepository,
    private val cartRepository: CartRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        CheckoutUiState().copy(
            total = cartRepository.getCurrentCart().total,
            beansToSpend = cartRepository.getCurrentCart().beansToSpend,
            isCartEmpty = cartRepository.getCurrentCart().items.isEmpty()
        )
    )
    val state: StateFlow<CheckoutUiState> = _state

    private val _effects = Channel<CheckoutUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var savedCardsJob: Job? = null
    private var startedCustomerId: Long? = null

    init {
        viewModelScope.launch {
            cartRepository.observeCart().collect { snapshot ->
                _state.value = _state.value.copy(
                    total = snapshot.total,
                    beansToSpend = snapshot.beansToSpend,
                    isCartEmpty = snapshot.items.isEmpty()
                )
            }
        }
    }

    fun start() {
        val customerId = sessionRepository.currentCustomerId
        if (customerId == null) {
            viewModelScope.launch {
                _effects.send(CheckoutUiEffect.ShowMessage("Not logged in as customer."))
            }
            return
        }

        if (startedCustomerId == customerId && savedCardsJob != null) {
            return
        }

        startedCustomerId = customerId

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
        cardNickname: String,
        cardholderName: String,
        cardNumber: String,
        expiryText: String,
        cvv: String,
        saveNewCardForFuture: Boolean
    ) {
        val customerId = sessionRepository.currentCustomerId
        if (customerId == null) {
            viewModelScope.launch {
                _effects.send(CheckoutUiEffect.ShowMessage("Not logged in as customer."))
            }
            return
        }

        viewModelScope.launch {
            val removedCount = cartRepository.removeUnavailableItems()
            if (removedCount > 0) {
                val refreshedCart = cartRepository.getCurrentCart()

                _state.value = _state.value.copy(
                    total = refreshedCart.total,
                    beansToSpend = refreshedCart.beansToSpend,
                    isCartEmpty = refreshedCart.items.isEmpty()
                )

                _effects.send(
                    CheckoutUiEffect.ShowMessage(
                        "One or more unavailable items were removed from your cart."
                    )
                )
                return@launch
            }

            val cartSnapshot = cartRepository.getCurrentCart()
            if (cartSnapshot.items.isEmpty()) {
                _state.value = _state.value.copy(
                    total = cartSnapshot.total,
                    beansToSpend = cartSnapshot.beansToSpend,
                    isCartEmpty = true
                )
                _effects.send(CheckoutUiEffect.ShowMessage("Cart is empty."))
                return@launch
            }

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
                        _effects.send(CheckoutUiEffect.ShowMessage(validation.generalError))
                    }
                    return@launch
                }
            }

            _state.value = currentState.copy(
                isSubmitting = true,
                cardValidation = CheckoutCardValidationResult()
            )

            val result = checkoutRepository.submitOrder(
                customerId = customerId,
                items = cartSnapshot.items,
                paymentType = _state.value.paymentType,
                totalAmount = cartSnapshot.total,
                beansToSpend = cartSnapshot.beansToSpend,
                beansToEarn = cartSnapshot.purchasedProductCountForBeans,
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

                    cartRepository.clear()

                    _state.value = _state.value.copy(
                        isSubmitting = false,
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
}