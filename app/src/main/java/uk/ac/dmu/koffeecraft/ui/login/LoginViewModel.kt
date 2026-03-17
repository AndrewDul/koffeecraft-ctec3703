package uk.ac.dmu.koffeecraft.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.repository.AuthRepository

class LoginViewModel(private val repo: AuthRepository) : ViewModel() {

    data class LoginSuccess(
        val userId: Long,
        val role: AuthRepository.UserRole
    )

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
                is AuthRepository.LoginResult.Success -> {
                    _state.value = UiState(
                        isLoading = false,
                        loginSuccess = LoginSuccess(
                            userId = result.userId,
                            role = result.role
                        )
                    )
                }

                is AuthRepository.LoginResult.Error -> {
                    _state.value = UiState(
                        isLoading = false,
                        error = result.message
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