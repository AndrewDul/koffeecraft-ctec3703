package uk.ac.dmu.koffeecraft.ui.admin.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.dao.AdminAccountTarget
import uk.ac.dmu.koffeecraft.data.dao.CustomerAccountTarget
import uk.ac.dmu.koffeecraft.data.repository.AdminAccountsRepository
import uk.ac.dmu.koffeecraft.data.repository.SettingsActionResult

enum class ManagedAccountType {
    CUSTOMERS,
    ADMINS
}

enum class CustomerSearchMode {
    ORDER_ID,
    CUSTOMER_ID
}

enum class AdminSearchMode {
    ADMIN_ID,
    EMAIL,
    USERNAME
}

data class AdminManageAccountsUiState(
    val accountType: ManagedAccountType = ManagedAccountType.CUSTOMERS,
    val customerSearchMode: CustomerSearchMode = CustomerSearchMode.ORDER_ID,
    val adminSearchMode: AdminSearchMode = AdminSearchMode.ADMIN_ID,
    val selectedCustomer: CustomerAccountTarget? = null,
    val selectedAdmin: AdminAccountTarget? = null,
    val searchError: String? = null,
    val isWorking: Boolean = false
)

class AdminManageCustomerAccountsViewModel(
    private val adminAccountsRepository: AdminAccountsRepository,
    private val currentAdminId: Long?
) : ViewModel() {

    sealed interface UiEffect {
        data class ShowMessage(val message: String) : UiEffect
    }

    private val _state = MutableStateFlow(AdminManageAccountsUiState())
    val state: StateFlow<AdminManageAccountsUiState> = _state

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun setAccountType(accountType: ManagedAccountType) {
        if (_state.value.accountType == accountType) return

        _state.value = _state.value.copy(
            accountType = accountType,
            selectedCustomer = null,
            selectedAdmin = null,
            searchError = null,
            customerSearchMode = CustomerSearchMode.ORDER_ID,
            adminSearchMode = AdminSearchMode.ADMIN_ID
        )
    }

    fun setCustomerSearchMode(searchMode: CustomerSearchMode) {
        if (_state.value.customerSearchMode == searchMode) return

        _state.value = _state.value.copy(
            customerSearchMode = searchMode,
            selectedCustomer = null,
            selectedAdmin = null,
            searchError = null
        )
    }

    fun setAdminSearchMode(searchMode: AdminSearchMode) {
        if (_state.value.adminSearchMode == searchMode) return

        _state.value = _state.value.copy(
            adminSearchMode = searchMode,
            selectedCustomer = null,
            selectedAdmin = null,
            searchError = null
        )
    }

    fun searchAccount(rawValue: String) {
        val input = rawValue.trim()
        if (input.isBlank()) {
            viewModelScope.launch {
                _effects.send(UiEffect.ShowMessage("Enter a search value first."))
            }
            return
        }

        val currentState = _state.value

        _state.value = currentState.copy(
            isWorking = true,
            searchError = null,
            selectedCustomer = null,
            selectedAdmin = null
        )

        viewModelScope.launch {
            when (currentState.accountType) {
                ManagedAccountType.CUSTOMERS -> searchCustomer(input, currentState.customerSearchMode)
                ManagedAccountType.ADMINS -> searchAdmin(input, currentState.adminSearchMode)
            }
        }
    }

    fun updateSelectedCustomerStatus(isActive: Boolean) {
        val selectedCustomer = _state.value.selectedCustomer ?: return

        _state.value = _state.value.copy(isWorking = true)

        viewModelScope.launch {
            when (
                val result = adminAccountsRepository.updateCustomerStatus(
                    customerId = selectedCustomer.customerId,
                    isActive = isActive
                )
            ) {
                is SettingsActionResult.Success -> {
                    val refreshed = adminAccountsRepository.findCustomerByCustomerId(selectedCustomer.customerId)
                    _state.value = _state.value.copy(
                        selectedCustomer = refreshed,
                        selectedAdmin = null,
                        searchError = null,
                        isWorking = false
                    )
                    _effects.send(UiEffect.ShowMessage(result.message))
                }

                is SettingsActionResult.Error -> {
                    _state.value = _state.value.copy(isWorking = false)
                    _effects.send(UiEffect.ShowMessage(result.message))
                }
            }
        }
    }

    fun updateSelectedAdminStatus(isActive: Boolean) {
        val selectedAdmin = _state.value.selectedAdmin ?: return

        _state.value = _state.value.copy(isWorking = true)

        viewModelScope.launch {
            when (
                val result = adminAccountsRepository.updateAdminStatus(
                    adminId = selectedAdmin.adminId,
                    isActive = isActive,
                    currentAdminId = currentAdminId
                )
            ) {
                is SettingsActionResult.Success -> {
                    val refreshed = adminAccountsRepository.findAdminByAdminId(selectedAdmin.adminId)
                    _state.value = _state.value.copy(
                        selectedAdmin = refreshed,
                        selectedCustomer = null,
                        searchError = null,
                        isWorking = false
                    )
                    _effects.send(UiEffect.ShowMessage(result.message))
                }

                is SettingsActionResult.Error -> {
                    _state.value = _state.value.copy(isWorking = false)
                    _effects.send(UiEffect.ShowMessage(result.message))
                }
            }
        }
    }

    fun deleteSelectedCustomer() {
        val selectedCustomer = _state.value.selectedCustomer ?: return

        _state.value = _state.value.copy(isWorking = true)

        viewModelScope.launch {
            when (
                val result = adminAccountsRepository.deleteCustomerPermanently(
                    selectedCustomer.customerId
                )
            ) {
                is SettingsActionResult.Success -> {
                    _state.value = _state.value.copy(
                        selectedCustomer = null,
                        selectedAdmin = null,
                        searchError = null,
                        isWorking = false
                    )
                    _effects.send(UiEffect.ShowMessage(result.message))
                }

                is SettingsActionResult.Error -> {
                    _state.value = _state.value.copy(isWorking = false)
                    _effects.send(UiEffect.ShowMessage(result.message))
                }
            }
        }
    }

    fun resetSelectedAdminPassword(
        newPassword: String,
        confirmPassword: String
    ) {
        val selectedAdmin = _state.value.selectedAdmin ?: return

        _state.value = _state.value.copy(isWorking = true)

        viewModelScope.launch {
            when (
                val result = adminAccountsRepository.resetAdminPassword(
                    adminId = selectedAdmin.adminId,
                    newPassword = newPassword,
                    confirmPassword = confirmPassword
                )
            ) {
                is SettingsActionResult.Success -> {
                    _state.value = _state.value.copy(isWorking = false)
                    _effects.send(UiEffect.ShowMessage(result.message))
                }

                is SettingsActionResult.Error -> {
                    _state.value = _state.value.copy(isWorking = false)
                    _effects.send(UiEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private suspend fun searchCustomer(
        input: String,
        searchMode: CustomerSearchMode
    ) {
        val result = when (searchMode) {
            CustomerSearchMode.ORDER_ID -> {
                val numeric = input.toLongOrNull()
                if (numeric == null) {
                    _state.value = _state.value.copy(
                        isWorking = false,
                        searchError = "Enter a valid numeric value."
                    )
                    return
                }
                adminAccountsRepository.findCustomerByOrderId(numeric)
            }

            CustomerSearchMode.CUSTOMER_ID -> {
                val numeric = input.toLongOrNull()
                if (numeric == null) {
                    _state.value = _state.value.copy(
                        isWorking = false,
                        searchError = "Enter a valid numeric value."
                    )
                    return
                }
                adminAccountsRepository.findCustomerByCustomerId(numeric)
            }
        }

        _state.value = _state.value.copy(
            selectedCustomer = result,
            selectedAdmin = null,
            searchError = if (result == null) "No matching customer found." else null,
            isWorking = false
        )
    }

    private suspend fun searchAdmin(
        input: String,
        searchMode: AdminSearchMode
    ) {
        val result = when (searchMode) {
            AdminSearchMode.ADMIN_ID -> {
                val numeric = input.toLongOrNull()
                if (numeric == null) {
                    _state.value = _state.value.copy(
                        isWorking = false,
                        searchError = "Enter a valid numeric admin ID."
                    )
                    return
                }
                adminAccountsRepository.findAdminByAdminId(numeric)
            }

            AdminSearchMode.EMAIL -> adminAccountsRepository.findAdminByEmail(input)
            AdminSearchMode.USERNAME -> adminAccountsRepository.findAdminByUsername(input)
        }

        _state.value = _state.value.copy(
            selectedCustomer = null,
            selectedAdmin = result,
            searchError = if (result == null) "No matching admin found." else null,
            isWorking = false
        )
    }

    class Factory(
        private val adminAccountsRepository: AdminAccountsRepository,
        private val currentAdminId: Long?
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AdminManageCustomerAccountsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AdminManageCustomerAccountsViewModel(
                    adminAccountsRepository = adminAccountsRepository,
                    currentAdminId = currentAdminId
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}