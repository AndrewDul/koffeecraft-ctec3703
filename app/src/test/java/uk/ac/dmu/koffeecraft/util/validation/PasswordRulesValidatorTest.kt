package uk.ac.dmu.koffeecraft.util.validation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordRulesValidatorTest {

    @Test
    fun describe_returnsAllRulesTrue_forStrongPassword() {
        val rules = PasswordRulesValidator.describe("Strong1!")

        assertTrue(rules.hasUppercase)
        assertTrue(rules.hasLowercase)
        assertTrue(rules.hasDigit)
        assertTrue(rules.hasSpecial)
        assertTrue(rules.hasMinLength)
        assertTrue(rules.isValid)
    }

    @Test
    fun describe_returnsExpectedFlags_forWeakPassword() {
        val rules = PasswordRulesValidator.describe("abcdefg")

        assertFalse(rules.hasUppercase)
        assertTrue(rules.hasLowercase)
        assertFalse(rules.hasDigit)
        assertFalse(rules.hasSpecial)
        assertFalse(rules.hasMinLength)
        assertFalse(rules.isValid)
    }

    @Test
    fun isValid_returnsTrue_forValidPassword() {
        assertTrue(PasswordRulesValidator.isValid("Coffee123!"))
    }

    @Test
    fun isValid_returnsFalse_forTooShortPassword() {
        assertFalse(PasswordRulesValidator.isValid("A1!bc"))
    }

    @Test
    fun isValid_returnsFalse_whenMissingUppercase() {
        assertFalse(PasswordRulesValidator.isValid("coffee123!"))
    }

    @Test
    fun isValid_returnsFalse_forEmptyPassword() {
        assertFalse(PasswordRulesValidator.isValid(""))
    }
}