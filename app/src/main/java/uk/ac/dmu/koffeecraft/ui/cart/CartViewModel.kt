package uk.ac.dmu.koffeecraft.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.cart.CartItem
import uk.ac.dmu.koffeecraft.data.cart.CartSnapshot
import uk.ac.dmu.koffeecraft.data.repository.CartRepository

class CartViewModel(
    private val cartRepository: CartRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        cartRepository.getCurrentCart().toUiState()
    )
    val state: StateFlow<CartUiState> = _state

    init {
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