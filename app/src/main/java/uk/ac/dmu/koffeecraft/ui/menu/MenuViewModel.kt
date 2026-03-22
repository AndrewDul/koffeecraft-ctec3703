package uk.ac.dmu.koffeecraft.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.repository.MenuRepository

class MenuViewModel(
    private val menuRepository: MenuRepository
) : ViewModel() {

    data class UiState(
        val category: String = "COFFEE",
        val products: List<Product> = emptyList()
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private var categoryJob: Job? = null

    fun setCategory(category: String) {
        _state.value = _state.value.copy(category = category)
        observeCategory(category)
    }

    fun start() {
        observeCategory(_state.value.category)
    }

    private fun observeCategory(category: String) {
        categoryJob?.cancel()
        categoryJob = viewModelScope.launch {
            menuRepository.observeProductsByCategory(category).collect { products ->
                _state.value = _state.value.copy(products = products)
            }
        }
    }
}