package uk.ac.dmu.koffeecraft.ui.orders

sealed interface CustomerOrdersUiEffect {
    data class ShowMessage(val message: String) : CustomerOrdersUiEffect
    data object NavigateToCart : CustomerOrdersUiEffect
}