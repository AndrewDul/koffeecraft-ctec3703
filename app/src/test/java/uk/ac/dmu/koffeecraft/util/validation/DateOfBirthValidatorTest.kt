package uk.ac.dmu.koffeecraft.util.validation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DateOfBirthValidatorTest {

    @Test
    fun isValid_returnsTrue_forCorrectFormat() {
        assertTrue(DateOfBirthValidator.isValid("2000-12-31"))
    }

    @Test
    fun isValid_trimsWhitespace_beforeValidation() {
        assertTrue(DateOfBirthValidator.isValid(" 2000-12-31 "))
    }

    @Test
    fun isValid_returnsFalse_forWrongSeparator() {
        assertFalse(DateOfBirthValidator.isValid("2000/12/31"))
    }

    @Test
    fun isValid_returnsFalse_whenMonthAndDayAreNotTwoDigits() {
        assertFalse(DateOfBirthValidator.isValid("2000-1-5"))
    }

    @Test
    fun isValid_returnsFalse_forAlphabeticInput() {
        assertFalse(DateOfBirthValidator.isValid("date-of-birth"))
    }

    @Test
    fun isValid_returnsFalse_forBlankInput() {
        assertFalse(DateOfBirthValidator.isValid(""))
    }
}