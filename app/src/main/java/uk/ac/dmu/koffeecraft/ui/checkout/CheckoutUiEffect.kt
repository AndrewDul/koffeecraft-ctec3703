package uk.ac.dmu.koffeecraft.ui.checkout

sealed interface CheckoutUiEffect {
    data class ShowMessage(val message: String) : CheckoutUiEffect
    data class CheckoutCompleted(
        val orderId: Long,
        val paymentMessage: String
    ) : CheckoutUiEffect
}