package uk.ac.dmu.koffeecraft.ui.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.repository.CustomerFavouritesActionResult
import uk.ac.dmu.koffeecraft.data.repository.CustomerFavouritesRepository
import uk.ac.dmu.koffeecraft.data.session.SessionRepository
import uk.ac.dmu.koffeecraft.data.querymodel.CustomerFavouritePresetCard
import uk.ac.dmu.koffeecraft.data.querymodel.StandardFavouriteCard
data class CustomerFavouritesUiState(
    val presets: List<CustomerFavouritePresetCard> = emptyList(),
    val standardProducts: List<StandardFavouriteCard> = emptyList(),
    val showPresetSection: Boolean = false,
    val showStandardSection: Boolean = false,
    val showEmpty: Boolean = true
)

class CustomerFavouritesViewModel(
    private val repository: CustomerFavouritesRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    sealed interface UiEffect {
        data class ShowMessage(val message: String) : UiEffect
    }

    private val _state = MutableStateFlow(CustomerFavouritesUiState())
    val state: StateFlow<CustomerFavouritesUiState> = _state

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var observeJob: Job? = null
    private var startedCustomerId: Long? = null

    fun start() {
        val customerId = sessionRepository.currentCustomerId
        if (customerId == null) {
            viewModelScope.launch {
                _effects.send(UiEffect.ShowMessage("Please sign in first."))
            }
            return
        }

        if (observeJob != null && startedCustomerId == customerId) return

        startedCustomerId = customerId
        observeJob?.cancel()

        observeJob = viewModelScope.launch {
            repository.observeFavourites(customerId).collect { data ->
                _state.update {
                    it.copy(
                        presets = data.presets,
                        standardProducts = data.standardProducts,
                        showPresetSection = data.presets.isNotEmpty(),
                        showStandardSection = data.standardProducts.isNotEmpty(),
                        showEmpty = data.presets.isEmpty() && data.standardProducts.isEmpty()
                    )
                }
            }
        }
    }

    fun removePreset(presetId: Long) {
        viewModelScope.launch {
            when (val result = repository.removePreset(presetId)) {
                is CustomerFavouritesActionResult.Success -> {
                    _effects.send(UiEffect.ShowMessage(result.message))
                }

                is CustomerFavouritesActionResult.Error -> {
                    _effects.send(UiEffect.ShowMessage(result.message))
                }
            }
        }
    }

    fun removeStandardFavourite(productId: Long) {
        val customerId = sessionRepository.currentCustomerId
        if (customerId == null) {
            viewModelScope.launch {
                _effects.send(UiEffect.ShowMessage("Please sign in first."))
            }
            return
        }

        viewModelScope.launch {
            when (val result = repository.removeStandardFavourite(customerId, productId)) {
                is CustomerFavouritesActionResult.Success -> {
                    _effects.send(UiEffect.ShowMessage(result.message))
                }

                is CustomerFavouritesActionResult.Error -> {
                    _effects.send(UiEffect.ShowMessage(result.message))
                }
            }
        }
    }

    fun buyStandardFavouriteAgain(productId: Long) {
        viewModelScope.launch {
            when (val result = repository.buyStandardFavouriteAgain(productId)) {
                is CustomerFavouritesActionResult.Success -> {
                    _effects.send(UiEffect.ShowMessage(result.message))
                }

                is CustomerFavouritesActionResult.Error -> {
                    _effects.send(UiEffect.ShowMessage(result.message))
                }
            }
        }
    }

    fun buyPresetAgain(presetId: Long) {
        viewModelScope.launch {
            when (val result = repository.buyPresetAgain(presetId)) {
                is CustomerFavouritesActionResult.Success -> {
                    _effects.send(UiEffect.ShowMessage(result.message))
                }

                is CustomerFavouritesActionResult.Error -> {
                    _effects.send(UiEffect.ShowMessage(result.message))
                }
            }
        }
    }

    class Factory(
        private val repository: CustomerFavouritesRepository,
        private val sessionRepository: SessionRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CustomerFavouritesViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CustomerFavouritesViewModel(repository, sessionRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}