package uk.ac.dmu.koffeecraft.ui.checkout

import uk.ac.dmu.koffeecraft.data.entities.CustomerPaymentCard
import uk.ac.dmu.koffeecraft.util.validation.CheckoutCardValidationResult

data class CheckoutUiState(
    val total: Double = 0.0,
    val beansToSpend: Int = 0,
    val paymentType: String = "CARD",
    val selectedSavedCardId: Long? = null,
    val savedCards: List<CustomerPaymentCard> = emptyList(),
    val isSubmitting: Boolean = false,
    val isCartEmpty: Boolean = true,
    val cardValidation: CheckoutCardValidationResult = CheckoutCardValidationResult()
)