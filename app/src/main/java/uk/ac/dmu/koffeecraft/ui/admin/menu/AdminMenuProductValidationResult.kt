package uk.ac.dmu.koffeecraft.ui.admin.menu

data class AdminMenuProductValidationResult(
    val nameError: String? = null,
    val descriptionError: String? = null,
    val priceError: String? = null,
    val generalMessage: String? = null,
    val validatedPrice: Double? = null
) {
    val isValid: Boolean
        get() = nameError == null &&
                descriptionError == null &&
                priceError == null &&
                generalMessage == null &&
                validatedPrice != null
}