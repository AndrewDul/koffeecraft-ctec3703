package uk.ac.dmu.koffeecraft.ui.admin.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.repository.AdminAccountsRepository
import uk.ac.dmu.koffeecraft.data.repository.AdminCreateAccountResult

class AdminCreateAccountViewModel(
    private val adminAccountsRepository: AdminAccountsRepository
) : ViewModel() {

    data class UiState(
        val isSubmitting: Boolean = false,
        val emailAlreadyUsed: Boolean = false,
        val usernameAlreadyUsed: Boolean = false
    )

    sealed interface UiEffect {
        data object ClearForm : UiEffect
        data class ShowMessage(val message: String) : UiEffect
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun submitCreateAdmin(
        fullName: String,
        email: String,
        phone: String,
        username: String,
        password: String,
        isActive: Boolean
    ) {
        if (_state.value.isSubmitting) return

        _state.value = _state.value.copy(
            isSubmitting = true,
            emailAlreadyUsed = false,
            usernameAlreadyUsed = false
        )

        viewModelScope.launch {
            when (
                val result = adminAccountsRepository.createAdminAccount(
                    fullName = fullName,
                    email = email,
                    phone = phone,
                    username = username,
                    password = password,
                    isActive = isActive
                )
            ) {
                AdminCreateAccountResult.Success -> {
                    _state.value = UiState()
                    _effects.send(UiEffect.ClearForm)
                    _effects.send(UiEffect.ShowMessage("Admin account created successfully."))
                }

                is AdminCreateAccountResult.DuplicateFields -> {
                    _state.value = UiState(
                        isSubmitting = false,
                        emailAlreadyUsed = result.emailTaken,
                        usernameAlreadyUsed = result.usernameTaken
                    )
                }

                is AdminCreateAccountResult.Error -> {
                    _state.value = _state.value.copy(isSubmitting = false)
                    _effects.send(UiEffect.ShowMessage(result.message))
                }
            }
        }
    }

    fun clearDuplicateErrors() {
        val currentState = _state.value
        if (!currentState.emailAlreadyUsed && !currentState.usernameAlreadyUsed) return

        _state.value = currentState.copy(
            emailAlreadyUsed = false,
            usernameAlreadyUsed = false
        )
    }

    class Factory(
        private val adminAccountsRepository: AdminAccountsRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AdminCreateAccountViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AdminCreateAccountViewModel(adminAccountsRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}