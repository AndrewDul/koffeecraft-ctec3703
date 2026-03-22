package uk.ac.dmu.koffeecraft.ui.admin.menu

import uk.ac.dmu.koffeecraft.data.entities.Product

sealed interface AdminMenuUiEvent {
    data class Message(val text: String) : AdminMenuUiEvent
    data class ProductValidationFailed(
        val result: AdminMenuProductValidationResult
    ) : AdminMenuUiEvent
    data class ProductSaved(
        val product: Product,
        val created: Boolean
    ) : AdminMenuUiEvent
}