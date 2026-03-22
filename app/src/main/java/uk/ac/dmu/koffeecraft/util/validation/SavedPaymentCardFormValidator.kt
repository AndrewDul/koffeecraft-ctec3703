package uk.ac.dmu.koffeecraft.util.validation

import uk.ac.dmu.koffeecraft.util.payment.PaymentCardValidator

object SavedPaymentCardFormValidator {

    fun validate(
        holder: String,
        number: String,
        expiry: String,
        cvv: String
    ): SavedPaymentCardValidationResult {
        val brand = PaymentCardValidator.detectBrand(number)

        val holderError = if (!PaymentCardValidator.isValidCardholderName(holder)) {
            "Enter the cardholder first and last name as shown on the card"
        } else {
            null
        }

        val numberError = PaymentCardValidator.explainInvalidCardNumber(number)

        val parsedExpiry = PaymentCardValidator.parseExpiry(expiry)
        val expiryError = if (
            parsedExpiry == null ||
            !PaymentCardValidator.isExpiryValid(parsedExpiry.first, parsedExpiry.second)
        ) {
            "Enter a valid future expiry date"
        } else {
            null
        }

        val cvvError = if (!PaymentCardValidator.isValidCvv(cvv, brand)) {
            if (brand == PaymentCardValidator.CardBrand.AMEX) {
                "AmEx uses a 4-digit security code"
            } else {
                "Enter a valid 3-digit security code"
            }
        } else {
            null
        }

        return SavedPaymentCardValidationResult(
            holderError = holderError,
            numberError = numberError,
            expiryError = expiryError,
            cvvError = cvvError
        )
    }
}