package uk.ac.dmu.koffeecraft.util.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class PaymentCardValidatorTest {

    @Test
    fun extractDigits_removesSpacesAndSymbols() {
        val result = PaymentCardValidator.extractDigits("4242-1111 2222 3333")

        assertEquals("4242111122223333", result)
    }

    @Test
    fun formatCardNumberInput_groupsDigitsInBlocksOfFour() {
        val result = PaymentCardValidator.formatCardNumberInput("4242111122223333")

        assertEquals("4242 1111 2222 3333", result)
    }

    @Test
    fun detectBrand_identifiesSupportedCardBrands() {
        assertEquals(
            PaymentCardValidator.CardBrand.VISA,
            PaymentCardValidator.detectBrand("4242 1111 2222 3333")
        )

        assertEquals(
            PaymentCardValidator.CardBrand.MASTERCARD,
            PaymentCardValidator.detectBrand("5555 1111 2222 3333")
        )

        assertEquals(
            PaymentCardValidator.CardBrand.AMEX,
            PaymentCardValidator.detectBrand("3412 3456 7890 1234")
        )

        assertEquals(
            PaymentCardValidator.CardBrand.MAESTRO,
            PaymentCardValidator.detectBrand("5600 1111 2222 3333")
        )
    }

    @Test
    fun explainInvalidCardNumber_returnsNull_forExactly16Digits() {
        val result = PaymentCardValidator.explainInvalidCardNumber("4242 1111 2222 3333")

        assertNull(result)
    }

    @Test
    fun explainInvalidCardNumber_returnsError_forTooShortNumber() {
        val result = PaymentCardValidator.explainInvalidCardNumber("4242 1111")

        assertEquals("Card number is too short. Enter exactly 16 digits.", result)
    }

    @Test
    fun parseExpiry_returnsMonthAndYear_forValidExpiryText() {
        val result = PaymentCardValidator.parseExpiry("08/29")

        assertEquals(8, result?.first)
        assertEquals(2029, result?.second)
    }

    @Test
    fun isExpiryValid_returnsTrue_forCurrentOrFutureMonth() {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentYear = calendar.get(Calendar.YEAR)

        assertTrue(PaymentCardValidator.isExpiryValid(currentMonth, currentYear))
    }

    @Test
    fun isExpiryValid_returnsFalse_forInvalidMonth() {
        assertFalse(PaymentCardValidator.isExpiryValid(13, 2030))
    }

    @Test
    fun isValidCvv_usesFourDigitsForAmex() {
        assertTrue(PaymentCardValidator.isValidCvv("1234", PaymentCardValidator.CardBrand.AMEX))
        assertFalse(PaymentCardValidator.isValidCvv("123", PaymentCardValidator.CardBrand.AMEX))
    }

    @Test
    fun isValidCvv_usesThreeDigitsForNonAmex() {
        assertTrue(PaymentCardValidator.isValidCvv("123", PaymentCardValidator.CardBrand.VISA))
        assertFalse(PaymentCardValidator.isValidCvv("1234", PaymentCardValidator.CardBrand.VISA))
    }

    @Test
    fun buildMaskedNumber_returnsMaskedRepresentation() {
        val result = PaymentCardValidator.buildMaskedNumber("4242 1111 2222 3333")

        assertEquals("•••• •••• •••• 3333", result)
    }

    @Test
    fun defaultNickname_buildsNameFromBrandAndLastFourDigits() {
        val result = PaymentCardValidator.defaultNickname(
            brand = PaymentCardValidator.CardBrand.VISA,
            last4 = "3333"
        )

        assertEquals("VISA ending 3333", result)
    }
}