package uk.ac.dmu.koffeecraft.util.validation

data class CustomerPersonalInfoValidationResult(
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val emailError: String? = null
) {
    val isValid: Boolean
        get() = firstNameError == null &&
                lastNameError == null &&
                emailError == null
}