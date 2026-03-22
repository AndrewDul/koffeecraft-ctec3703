package uk.ac.dmu.koffeecraft.data.repository

sealed interface SettingsActionResult {
    data class Success(val message: String) : SettingsActionResult
    data class Error(val message: String) : SettingsActionResult
}