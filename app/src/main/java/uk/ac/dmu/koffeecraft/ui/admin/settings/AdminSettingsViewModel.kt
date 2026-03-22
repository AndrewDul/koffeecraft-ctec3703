package uk.ac.dmu.koffeecraft.ui.admin.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.repository.AdminSettingsRepository

data class AdminSettingsUiState(
    val adminName: String = "",
    val adminEmail: String = "",
    val simulationEnabled: Boolean = false,
    val darkModeEnabled: Boolean = false,
    val profileMissing: Boolean = false
)

class AdminSettingsViewModel(
    private val repository: AdminSettingsRepository
) : ViewModel() {

    sealed interface UiEffect {
        data class ShowMessage(val message: String) : UiEffect
        data object NavigateToSignedOut : UiEffect
    }

    private val _state = MutableStateFlow(AdminSettingsUiState())
    val state: StateFlow<AdminSettingsUiState> = _state

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun load(adminId: Long?) {
        viewModelScope.launch {
            val data = repository.loadScreenData(adminId)

            val profileMissing = adminId != null && (data.adminName == null || data.adminEmail == null)

            _state.value = AdminSettingsUiState(
                adminName = data.adminName ?: "",
                adminEmail = data.adminEmail ?: "",
                simulationEnabled = data.simulationEnabled,
                darkModeEnabled = data.darkModeEnabled,
                profileMissing = profileMissing
            )

            if (profileMissing) {
                _effects.send(UiEffect.ShowMessage("Admin profile could not be found."))
            }
        }
    }

    fun setSimulationEnabled(enabled: Boolean) {
        repository.setSimulationEnabled(enabled)
        _state.value = _state.value.copy(simulationEnabled = enabled)
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        repository.setDarkModeEnabled(enabled)
        _state.value = _state.value.copy(darkModeEnabled = enabled)
    }

    fun signOut() {
        repository.signOut()
        viewModelScope.launch {
            _effects.send(UiEffect.NavigateToSignedOut)
        }
    }

    class Factory(
        private val repository: AdminSettingsRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AdminSettingsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AdminSettingsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}