package uk.ac.dmu.koffeecraft.ui.admin.menu

import uk.ac.dmu.koffeecraft.data.entities.Allergen

data class AdminMenuProductAllergenSelectionUiState(
    val productId: Long? = null,
    val isLoading: Boolean = false,
    val allergens: List<Allergen> = emptyList(),
    val selectedIds: Set<Long> = emptySet()
)