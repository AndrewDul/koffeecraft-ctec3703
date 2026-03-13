package uk.ac.dmu.koffeecraft.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.repository.AuthRepository

class RegisterViewModel(private val repo: AuthRepository) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val success: Boolean = false,
        val registeredCustomerId: Long? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    fun register(
        firstName: String,
        lastName: String,
        country: String,
        dateOfBirth: String,
        email: String,
        password: String,
        marketingInboxConsent: Boolean,
        termsAccepted: Boolean,
        privacyAccepted: Boolean
    ) {
        _state.value = UiState(isLoading = true)

        viewModelScope.launch {
            val result = repo.registerCustomer(
                firstName = firstName,
                lastName = lastName,
                country = country,
                dateOfBirth = dateOfBirth,
                email = email,
                password = password.toCharArray(),
                marketingInboxConsent = marketingInboxConsent,
                termsAccepted = termsAccepted,
                privacyAccepted = privacyAccepted
            )

            _state.value = when (result) {
                is AuthRepository.RegisterResult.Success -> {
                    UiState(
                        success = true,
                        registeredCustomerId = result.customerId
                    )
                }
                is AuthRepository.RegisterResult.Error -> UiState(error = result.message)
            }
        }
    }
}