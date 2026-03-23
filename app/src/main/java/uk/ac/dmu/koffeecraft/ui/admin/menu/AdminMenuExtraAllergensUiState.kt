package uk.ac.dmu.koffeecraft.ui.admin.menu

data class AdminMenuExtraAllergensUiState(
    val productId: Long? = null,
    val isLoading: Boolean = false,
    val entries: List<AdminMenuExtraAllergenEntryUiModel> = emptyList()
)