package uk.ac.dmu.koffeecraft.util.validation

data class SavedPaymentCardValidationResult(
    val holderError: String? = null,
    val numberError: String? = null,
    val expiryError: String? = null,
    val cvvError: String? = null
) {
    val isValid: Boolean
        get() = holderError == null &&
                numberError == null &&
                expiryError == null &&
                cvvError == null
}