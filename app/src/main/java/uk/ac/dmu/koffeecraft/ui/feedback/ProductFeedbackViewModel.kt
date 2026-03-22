package uk.ac.dmu.koffeecraft.ui.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.dao.OrderFeedbackItem
import uk.ac.dmu.koffeecraft.data.repository.FeedbackRepository
import uk.ac.dmu.koffeecraft.data.repository.ProductFeedbackSaveResult

data class ProductFeedbackUiState(
    val title: String = "",
    val subtitle: String = "",
    val craftedVisible: Boolean = false,
    val rating: Float = 5f,
    val comment: String = "",
    val errorVisible: Boolean = false,
    val errorText: String = "",
    val saveEnabled: Boolean = true,
    val saveText: String = "Submit & Next"
)

class ProductFeedbackViewModel(
    private val repository: FeedbackRepository
) : ViewModel() {

    sealed interface UiEffect {
        data class ShowMessage(val message: String) : UiEffect
        data class NavigateNext(val orderId: Long, val orderItemId: Long) : UiEffect
        data class CompletedAll(val orderId: Long) : UiEffect
    }

    private val _state = MutableStateFlow(ProductFeedbackUiState())
    val state: StateFlow<ProductFeedbackUiState> = _state

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var orderId: Long = 0L
    private var orderItemId: Long = 0L
    private var customerId: Long? = null
    private var currentItem: OrderFeedbackItem? = null

    fun start(
        orderId: Long,
        orderItemId: Long,
        customerId: Long?
    ) {
        this.orderId = orderId
        this.orderItemId = orderItemId
        this.customerId = customerId

        if (customerId == null) {
            _state.value = ProductFeedbackUiState(
                errorVisible = true,
                errorText = "You are not logged in as a customer.",
                saveEnabled = false
            )
            return
        }

        viewModelScope.launch {
            val item = repository.loadFeedbackItem(orderItemId)

            if (item == null) {
                _state.value = ProductFeedbackUiState(
                    errorVisible = true,
                    errorText = "This purchased product could not be found.",
                    saveEnabled = false
                )
                return@launch
            }

            currentItem = item
            _state.value = ProductFeedbackUiState(
                title = item.productName,
                subtitle = "Order #${item.orderId} • Quantity: ${item.quantity}",
                craftedVisible = item.isCrafted,
                rating = (item.rating ?: 5).toFloat(),
                comment = item.comment.orEmpty(),
                errorVisible = false,
                errorText = "",
                saveEnabled = true,
                saveText = "Submit & Next"
            )
        }
    }

    fun updateRating(value: Float) {
        _state.value = _state.value.copy(rating = value)
    }

    fun updateComment(value: String) {
        _state.value = _state.value.copy(comment = value)
    }

    fun save() {
        val safeCustomerId = customerId
        if (safeCustomerId == null) {
            viewModelScope.launch {
                _effects.send(UiEffect.ShowMessage("You are not logged in as a customer."))
            }
            return
        }

        val rating = _state.value.rating.toInt().coerceIn(1, 5)
        val comment = _state.value.comment

        viewModelScope.launch {
            when (
                val result = repository.saveFeedback(
                    orderId = orderId,
                    orderItemId = orderItemId,
                    customerId = safeCustomerId,
                    rating = rating,
                    comment = comment
                )
            ) {
                is ProductFeedbackSaveResult.NextItem -> {
                    _effects.send(UiEffect.ShowMessage(result.message))
                    _effects.send(
                        UiEffect.NavigateNext(
                            orderId = result.nextItem.orderId,
                            orderItemId = result.nextItem.orderItemId
                        )
                    )
                }

                is ProductFeedbackSaveResult.Completed -> {
                    _effects.send(UiEffect.ShowMessage(result.message))
                    _effects.send(UiEffect.CompletedAll(result.orderId))
                }

                is ProductFeedbackSaveResult.Error -> {
                    _effects.send(UiEffect.ShowMessage(result.message))
                }
            }
        }
    }

    class Factory(
        private val repository: FeedbackRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProductFeedbackViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ProductFeedbackViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}