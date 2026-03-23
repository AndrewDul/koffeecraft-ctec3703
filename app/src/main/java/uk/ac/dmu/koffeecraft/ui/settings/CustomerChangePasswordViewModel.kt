package uk.ac.dmu.koffeecraft.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.repository.CustomerSettingsRepository
import uk.ac.dmu.koffeecraft.data.repository.SettingsActionResult
import uk.ac.dmu.koffeecraft.data.session.SessionRepository

class CustomerChangePasswordViewModel(
    private val customerSettingsRepository: CustomerSettingsRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    data class UiState(
        val isSaving: Boolean = false
    )

    sealed interface UiEffect {
        data class ShowMessage(val message: String) : UiEffect
        data object ClearForm : UiEffect
        data object MarkCurrentPasswordIncorrect : UiEffect
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun changePassword(
        currentPassword: String,
        newPassword: String
    ) {
        val customerId = sessionRepository.currentCustomerId
        if (customerId == null) {
            viewModelScope.launch {
                _effects.send(UiEffect.ShowMessage("Please sign in first."))
            }
            return
        }

        _state.value = _state.value.copy(isSaving = true)

        viewModelScope.launch {
            when (
                val result = customerSettingsRepository.changePassword(
                    customerId = customerId,
                    currentPassword = currentPassword,
                    newPassword = newPassword
                )
            ) {
                is SettingsActionResult.Success -> {
                    _state.value = _state.value.copy(isSaving = false)
                    _effects.send(UiEffect.ShowMessage(result.message))
                    _effects.send(UiEffect.ClearForm)
                }

                is SettingsActionResult.Error -> {
                    _state.value = _state.value.copy(isSaving = false)

                    if (result.message == "Current password is incorrect.") {
                        _effects.send(UiEffect.MarkCurrentPasswordIncorrect)
                    } else {
                        _effects.send(UiEffect.ShowMessage(result.message))
                    }
                }
            }
        }
    }

    class Factory(
        private val customerSettingsRepository: CustomerSettingsRepository,
        private val sessionRepository: SessionRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CustomerChangePasswordViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CustomerChangePasswordViewModel(
                    customerSettingsRepository = customerSettingsRepository,
                    sessionRepository = sessionRepository
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}