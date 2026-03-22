package uk.ac.dmu.koffeecraft.ui.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.repository.RewardProductPickerRepository

data class RewardProductPickerUiState(
    val isLoading: Boolean = false,
    val items: List<Product> = emptyList()
)

class RewardProductPickerViewModel(
    private val repository: RewardProductPickerRepository
) : ViewModel() {

    sealed interface UiEffect {
        data class ShowMessage(val message: String) : UiEffect
    }

    private val _state = MutableStateFlow(RewardProductPickerUiState())
    val state: StateFlow<RewardProductPickerUiState> = _state

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun loadProducts(category: String) {
        _state.value = _state.value.copy(isLoading = true)

        viewModelScope.launch {
            val products = repository.loadProducts(category)

            if (products.isEmpty()) {
                _state.value = RewardProductPickerUiState(
                    isLoading = false,
                    items = emptyList()
                )
                _effects.send(
                    UiEffect.ShowMessage("No products are available in this category right now.")
                )
                return@launch
            }

            _state.value = RewardProductPickerUiState(
                isLoading = false,
                items = products
            )
        }
    }

    class Factory(
        private val repository: RewardProductPickerRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RewardProductPickerViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return RewardProductPickerViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}