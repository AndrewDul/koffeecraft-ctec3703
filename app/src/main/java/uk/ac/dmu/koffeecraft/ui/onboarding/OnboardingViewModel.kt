package uk.ac.dmu.koffeecraft.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.repository.OnboardingFinishResult
import uk.ac.dmu.koffeecraft.data.repository.OnboardingRepository

data class OnboardingUiState(
    val isLoading: Boolean = false,
    val promoConsentChoice: Boolean = false,
    val currentIndex: Int = 0
)

class OnboardingViewModel(
    private val repository: OnboardingRepository
) : ViewModel() {

    sealed interface UiEffect {
        data class ShowMessage(val message: String) : UiEffect
        data object NavigateToHome : UiEffect
    }

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun start(customerId: Long) {
        _state.value = _state.value.copy(isLoading = true)

        viewModelScope.launch {
            val data = repository.loadInitialData(customerId)
            if (data == null) {
                _state.value = _state.value.copy(isLoading = false)
                _effects.send(UiEffect.ShowMessage("Customer account could not be found."))
                return@launch
            }

            _state.value = _state.value.copy(
                isLoading = false,
                promoConsentChoice = data.marketingInboxConsent
            )
        }
    }

    fun setPromoConsentChoice(value: Boolean) {
        _state.value = _state.value.copy(promoConsentChoice = value)
    }

    fun nextPage(lastIndex: Int) {
        val current = _state.value.currentIndex
        if (current < lastIndex) {
            _state.value = _state.value.copy(currentIndex = current + 1)
        }
    }

    fun finishOnboarding(customerId: Long) {
        _state.value = _state.value.copy(isLoading = true)

        viewModelScope.launch {
            when (
                val result = repository.finishOnboarding(
                    customerId = customerId,
                    promoConsentChoice = _state.value.promoConsentChoice
                )
            ) {
                OnboardingFinishResult.Success -> {
                    _state.value = _state.value.copy(isLoading = false)
                    _effects.send(UiEffect.NavigateToHome)
                }

                is OnboardingFinishResult.Error -> {
                    _state.value = _state.value.copy(isLoading = false)
                    _effects.send(UiEffect.ShowMessage(result.message))
                }
            }
        }
    }

    class Factory(
        private val repository: OnboardingRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return OnboardingViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}