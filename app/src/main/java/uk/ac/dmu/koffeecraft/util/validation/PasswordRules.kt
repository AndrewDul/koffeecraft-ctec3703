package uk.ac.dmu.koffeecraft.util.validation

data class PasswordRules(
    val hasUppercase: Boolean,
    val hasLowercase: Boolean,
    val hasDigit: Boolean,
    val hasSpecial: Boolean,
    val hasMinLength: Boolean
) {
    val isValid: Boolean
        get() = hasUppercase && hasLowercase && hasDigit && hasSpecial && hasMinLength
}