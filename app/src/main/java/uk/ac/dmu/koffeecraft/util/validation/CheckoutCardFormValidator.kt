package uk.ac.dmu.koffeecraft.util.validation

import uk.ac.dmu.koffeecraft.util.payment.PaymentCardValidator

object CheckoutCardFormValidator {

    fun validate(
        holder: String,
        number: String,
        expiry: String,
        cvv: String,
        selectedSavedCardId: Long?
    ): CheckoutCardValidationResult {
        val hasStartedTypingNewCard = hasStartedTyping(
            holder = holder,
            number = number,
            expiry = expiry,
            cvv = cvv
        )

        if (!hasStartedTypingNewCard) {
            return if (selectedSavedCardId != null) {
                CheckoutCardValidationResult()
            } else {
                CheckoutCardValidationResult(
                    generalError = "Select a saved card or enter a new card."
                )
            }
        }

        val brand = PaymentCardValidator.detectBrand(number)

        var holderError: String? = null
        var numberError: String? = null
        var expiryError: String? = null
        var cvvError: String? = null

        if (!PaymentCardValidator.isValidCardholderName(holder)) {
            holderError = "Enter the cardholder first and last name as shown on the card"
        }

        numberError = PaymentCardValidator.explainInvalidCardNumber(number)

        val parsedExpiry = PaymentCardValidator.parseExpiry(expiry)
        if (parsedExpiry == null || !PaymentCardValidator.isExpiryValid(parsedExpiry.first, parsedExpiry.second)) {
            expiryError = "Enter a valid future expiry date"
        }

        if (!PaymentCardValidator.isValidCvv(cvv, brand)) {
            cvvError = if (brand == PaymentCardValidator.CardBrand.AMEX) {
                "AmEx uses a 4-digit security code"
            } else {
                "Enter a valid 3-digit security code"
            }
        }

        return CheckoutCardValidationResult(
            holderError = holderError,
            numberError = numberError,
            expiryError = expiryError,
            cvvError = cvvError
        )
    }

    fun hasStartedTyping(
        holder: String,
        number: String,
        expiry: String,
        cvv: String
    ): Boolean {
        return listOf(holder, number, expiry, cvv).any { it.isNotBlank() }
    }
}