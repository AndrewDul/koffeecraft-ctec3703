package uk.ac.dmu.koffeecraft.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.repository.AuthSessionLoginResult
import uk.ac.dmu.koffeecraft.data.repository.AuthSessionRepository

class LoginViewModel(
    private val repo: AuthSessionRepository
) : ViewModel() {

    sealed interface LoginSuccess {
        data class Customer(val customerId: Long) : LoginSuccess
        data class Admin(val adminId: Long) : LoginSuccess
    }

    data class UiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val loginSuccess: LoginSuccess? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    fun login(email: String, password: String) {
        if (_state.value.isLoading) return

        _state.value = UiState(isLoading = true)

        viewModelScope.launch {
            when (val result = repo.login(email, password.toCharArray())) {
                is AuthSessionLoginResult.Error -> {
                    _state.value = UiState(
                        isLoading = false,
                        error = result.message
                    )
                }

                is AuthSessionLoginResult.Admin -> {
                    _state.value = UiState(
                        isLoading = false,
                        loginSuccess = LoginSuccess.Admin(result.adminId)
                    )
                }

                is AuthSessionLoginResult.Customer -> {
                    _state.value = UiState(
                        isLoading = false,
                        loginSuccess = LoginSuccess.Customer(result.customerId)
                    )
                }
            }
        }
    }

    fun consumeLoginSuccess() {
        _state.value = _state.value.copy(loginSuccess = null)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}