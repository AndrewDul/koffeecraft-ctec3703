package uk.ac.dmu.koffeecraft.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.cart.CartItem
import uk.ac.dmu.koffeecraft.data.cart.CartSnapshot
import uk.ac.dmu.koffeecraft.data.repository.CartRepository

class CartViewModel(
    private val cartRepository: CartRepository
) : ViewModel() {

    sealed interface UiEffect {
        data class ShowMessage(val message: String) : UiEffect
    }

    private val _state = MutableStateFlow(
        cartRepository.getCurrentCart().toUiState()
    )
    val state: StateFlow<CartUiState> = _state

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            val removedCount = cartRepository.removeUnavailableItems()
            if (removedCount > 0) {
                _effects.send(
                    UiEffect.ShowMessage("One or more unavailable items were removed from your cart.")
                )
            }
        }

        viewModelScope.launch {
            cartRepository.observeCart().collect { snapshot ->
                _state.value = snapshot.toUiState()
            }
        }
    }

    fun addOne(item: CartItem) {
        cartRepository.addExisting(item)
    }

    fun removeOne(lineKey: String) {
        cartRepository.removeOne(lineKey)
    }

    private fun CartSnapshot.toUiState(): CartUiState {
        return CartUiState(
            items = items,
            total = total,
            beansToSpend = beansToSpend,
            isEmpty = items.isEmpty()
        )
    }
}