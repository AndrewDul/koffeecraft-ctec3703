package uk.ac.dmu.koffeecraft.ui.admin.menu

import uk.ac.dmu.koffeecraft.data.entities.Product

enum class AdminMenuCategoryFilter {
    ALL,
    COFFEE,
    CAKE,
    MERCH;

    fun matches(product: Product): Boolean {
        return when (this) {
            ALL -> true
            COFFEE -> product.isCoffee
            CAKE -> product.isCake
            MERCH -> product.isMerch
        }
    }
}