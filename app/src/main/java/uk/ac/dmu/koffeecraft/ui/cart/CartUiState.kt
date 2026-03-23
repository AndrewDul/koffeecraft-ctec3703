package uk.ac.dmu.koffeecraft.ui.cart

import uk.ac.dmu.koffeecraft.data.cart.CartItem

data class CartUiState(
    val items: List<CartItem> = emptyList(),
    val total: Double = 0.0,
    val beansToSpend: Int = 0,
    val isEmpty: Boolean = true
)