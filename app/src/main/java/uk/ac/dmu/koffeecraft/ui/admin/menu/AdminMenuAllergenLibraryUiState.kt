package uk.ac.dmu.koffeecraft.ui.admin.menu

import uk.ac.dmu.koffeecraft.data.entities.Allergen

data class AdminMenuAllergenLibraryUiState(
    val isLoading: Boolean = false,
    val allergens: List<Allergen> = emptyList()
)