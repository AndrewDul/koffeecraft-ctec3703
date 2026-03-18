package uk.ac.dmu.koffeecraft.ui.admin.menu

import uk.ac.dmu.koffeecraft.data.entities.Product

data class AdminMenuUiState(
    val allProducts: List<Product> = emptyList(),
    val currentFilter: AdminMenuCategoryFilter = AdminMenuCategoryFilter.ALL,
    val isLoading: Boolean = true
) {
    val filteredProducts: List<Product>
        get() = allProducts.filter { currentFilter.matches(it) }
}