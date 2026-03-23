package uk.ac.dmu.koffeecraft.ui.admin.menu

import uk.ac.dmu.koffeecraft.data.entities.ProductOption

data class AdminMenuProductOptionsUiState(
    val productId: Long? = null,
    val isLoading: Boolean = false,
    val options: List<ProductOption> = emptyList()
)