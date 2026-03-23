package uk.ac.dmu.koffeecraft.util.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CheckoutCardFormValidatorTest {

    private fun futureExpiry(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, 2)
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR) % 100
        return "%02d/%02d".format(month, year)
    }

    @Test
    fun validate_returnsGeneralError_whenNoSavedCardAndNoNewCardData() {
        val result = CheckoutCardFormValidator.validate(
            holder = "",
            number = "",
            expiry = "",
            cvv = "",
            selectedSavedCardId = null
        )

        assertFalse(result.isValid)
        assertEquals("Select a saved card or enter a new card.", result.generalError)
    }

    @Test
    fun validate_returnsValid_whenSavedCardIsSelectedAndNoTypingStarted() {
        val result = CheckoutCardFormValidator.validate(
            holder = "",
            number = "",
            expiry = "",
            cvv = "",
            selectedSavedCardId = 10L
        )

        assertTrue(result.isValid)
        assertNull(result.generalError)
    }

    @Test
    fun validate_returnsValid_forCorrectNewVisaCardData() {
        val result = CheckoutCardFormValidator.validate(
            holder = "Andrew Dul",
            number = "4242 1111 2222 3333",
            expiry = futureExpiry(),
            cvv = "123",
            selectedSavedCardId = null
        )

        assertTrue(result.isValid)
        assertNull(result.holderError)
        assertNull(result.numberError)
        assertNull(result.expiryError)
        assertNull(result.cvvError)
    }

    @Test
    fun validate_returnsAmexSpecificError_forThreeDigitAmexCvv() {
        val result = CheckoutCardFormValidator.validate(
            holder = "Andrew Dul",
            number = "3412 3456 7890 1234",
            expiry = futureExpiry(),
            cvv = "123",
            selectedSavedCardId = null
        )

        assertFalse(result.isValid)
        assertEquals("AmEx uses a 4-digit security code", result.cvvError)
    }

    @Test
    fun validate_returnsFieldErrors_forInvalidNewCardData() {
        val result = CheckoutCardFormValidator.validate(
            holder = "Andrew",
            number = "1234",
            expiry = "01/20",
            cvv = "9",
            selectedSavedCardId = null
        )

        assertFalse(result.isValid)
        assertEquals(
            "Enter the cardholder first and last name as shown on the card",
            result.holderError
        )
        assertEquals(
            "Card number is too short. Enter exactly 16 digits.",
            result.numberError
        )
        assertEquals("Enter a valid future expiry date", result.expiryError)
        assertEquals("Enter a valid 3-digit security code", result.cvvError)
    }

    @Test
    fun hasStartedTyping_returnsFalse_whenAllFieldsAreBlank() {
        val result = CheckoutCardFormValidator.hasStartedTyping(
            holder = "",
            number = "",
            expiry = "",
            cvv = ""
        )

        assertFalse(result)
    }

    @Test
    fun hasStartedTyping_returnsTrue_whenAnyFieldContainsText() {
        val result = CheckoutCardFormValidator.hasStartedTyping(
            holder = "",
            number = "4",
            expiry = "",
            cvv = ""
        )

        assertTrue(result)
    }
}