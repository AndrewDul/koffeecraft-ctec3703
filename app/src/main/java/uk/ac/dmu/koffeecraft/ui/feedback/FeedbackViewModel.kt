package uk.ac.dmu.koffeecraft.ui.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.dao.OrderFeedbackItem
import uk.ac.dmu.koffeecraft.data.repository.FeedbackRepository

data class FeedbackUiState(
    val items: List<OrderFeedbackItem> = emptyList(),
    val isEmpty: Boolean = true
)

class FeedbackViewModel(
    private val repository: FeedbackRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FeedbackUiState())
    val state: StateFlow<FeedbackUiState> = _state

    fun load(orderId: Long) {
        viewModelScope.launch {
            val items = repository.loadFeedbackItems(orderId)
            _state.value = FeedbackUiState(
                items = items,
                isEmpty = items.isEmpty()
            )
        }
    }

    class Factory(
        private val repository: FeedbackRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FeedbackViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return FeedbackViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}