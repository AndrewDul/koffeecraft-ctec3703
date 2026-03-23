package uk.ac.dmu.koffeecraft.util.validation

object PasswordRulesValidator {

    fun describe(password: String): PasswordRules {
        return PasswordRules(
            hasUppercase = password.any { it.isUpperCase() },
            hasLowercase = password.any { it.isLowerCase() },
            hasDigit = password.any { it.isDigit() },
            hasSpecial = password.any { !it.isLetterOrDigit() && !it.isWhitespace() },
            hasMinLength = password.length >= 8
        )
    }

    fun isValid(password: String): Boolean {
        return describe(password).isValid
    }
}