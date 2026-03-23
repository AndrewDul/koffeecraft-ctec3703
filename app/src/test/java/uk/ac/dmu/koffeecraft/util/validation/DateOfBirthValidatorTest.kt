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
    @Test
    fun isValid_returnsFalse_forImpossibleDate() {
        assertFalse(DateOfBirthValidator.isValid("2001-02-30"))
    }

    @Test
    fun isValid_returnsTrue_forValidLeapDay() {
        assertTrue(DateOfBirthValidator.isValid("2000-02-29"))
    }

    @Test
    fun isValid_returnsFalse_forFutureDate() {
        assertFalse(DateOfBirthValidator.isValid("2999-01-01"))
    }
}