package uk.ac.dmu.koffeecraft.util.validation

data class CheckoutCardValidationResult(
    val holderError: String? = null,
    val numberError: String? = null,
    val expiryError: String? = null,
    val cvvError: String? = null,
    val generalError: String? = null
) {
    val isValid: Boolean
        get() = holderError == null &&
                numberError == null &&
                expiryError == null &&
                cvvError == null &&
                generalError == null
}