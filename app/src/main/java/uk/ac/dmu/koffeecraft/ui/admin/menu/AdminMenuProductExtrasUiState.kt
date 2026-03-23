package uk.ac.dmu.koffeecraft.ui.admin.menu

import uk.ac.dmu.koffeecraft.data.entities.AddOn

data class AdminMenuProductExtrasUiState(
    val productId: Long? = null,
    val isLoading: Boolean = false,
    val assignedExtras: List<AddOn> = emptyList(),
    val libraryExtras: List<AddOn> = emptyList()
)