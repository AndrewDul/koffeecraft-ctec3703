package uk.ac.dmu.koffeecraft.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.repository.AuthRepository
import uk.ac.dmu.koffeecraft.data.session.SessionManager
class LoginViewModel(private val repo: AuthRepository) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val navigateToMenu: Boolean = false,
        val navigateToAdmin: Boolean = false
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    fun login(email: String, password: String) {
        _state.value = UiState(isLoading = true)

        viewModelScope.launch {
            val result = repo.login(email, password.toCharArray())

            _state.value = when (result) {
                is AuthRepository.LoginResult.AdminSuccess -> {
                    SessionManager.setAdmin()
                    UiState(navigateToAdmin = true)
                }
                is AuthRepository.LoginResult.CustomerSuccess -> {
                    SessionManager.setCustomer(result.customerId)
                    UiState(navigateToMenu = true)
                }
                is AuthRepository.LoginResult.Error ->
                    UiState(error = result.message)
            }
        }
    }

    fun consumeNavigation() {
        _state.value = _state.value.copy(navigateToMenu = false, navigateToAdmin = false)
    }
}