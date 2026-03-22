package uk.ac.dmu.koffeecraft.util.validation

object CustomerPersonalInfoValidator {

    fun validate(
        firstName: String,
        lastName: String,
        email: String
    ): CustomerPersonalInfoValidationResult {
        val cleanFirstName = firstName.trim()
        val cleanLastName = lastName.trim()
        val cleanEmail = email.trim()

        val firstNameError = if (cleanFirstName.isBlank()) {
            "Enter first name"
        } else {
            null
        }

        val lastNameError = if (cleanLastName.isBlank()) {
            "Enter last name"
        } else {
            null
        }

        val emailError = when {
            cleanEmail.isBlank() -> "Enter email"
            !EmailValidator.isValid(cleanEmail) -> "Enter a valid email"
            else -> null
        }

        return CustomerPersonalInfoValidationResult(
            firstNameError = firstNameError,
            lastNameError = lastNameError,
            emailError = emailError
        )
    }
}