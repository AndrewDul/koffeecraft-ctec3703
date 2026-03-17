package uk.ac.dmu.koffeecraft.util.payment

import java.util.Calendar

object PaymentCardValidator {

    enum class CardBrand(val displayName: String) {
        VISA("VISA"),
        MASTERCARD("Mastercard"),
        AMEX("AmEx"),
        MAESTRO("Maestro"),
        UNKNOWN("Card")
    }

    fun extractDigits(value: String): String {
        return value.filter { it.isDigit() }
    }

    fun formatCardNumberInput(value: String): String {
        val digits = extractDigits(value).take(16)
        return digits.chunked(4).joinToString(" ")
    }

    fun formatExpiryInput(value: String): String {
        val digits = extractDigits(value).take(4)
        return when {
            digits.length <= 2 -> digits
            else -> "${digits.take(2)}/${digits.drop(2)}"
        }
    }

    fun detectBrand(cardNumber: String): CardBrand {
        val digits = extractDigits(cardNumber)

        return when {
            digits.startsWith("4") -> CardBrand.VISA
            digits.matches(Regex("^(5[1-5]|2[2-7]).*")) -> CardBrand.MASTERCARD
            digits.startsWith("34") || digits.startsWith("37") -> CardBrand.AMEX
            digits.matches(Regex("^(50|56|57|58|6).*")) -> CardBrand.MAESTRO
            else -> CardBrand.UNKNOWN
        }
    }

    fun isValidCardholderName(name: String): Boolean {
        val clean = name.trim()
        return clean.length >= 5 &&
                clean.contains(" ") &&
                clean.all { it.isLetter() || it == ' ' || it == '-' || it == '\'' }
    }

    fun isValidCardNumber(cardNumber: String): Boolean {
        val digits = extractDigits(cardNumber)
        return digits.length == 16
    }

    fun explainInvalidCardNumber(cardNumber: String): String? {
        val digits = extractDigits(cardNumber)

        return when {
            digits.isBlank() -> "Enter a card number."
            digits.length < 16 -> "Card number is too short. Enter exactly 16 digits."
            digits.length > 16 -> "Card number is too long. Enter exactly 16 digits."
            else -> null
        }
    }

    fun parseExpiry(expiryText: String): Pair<Int, Int>? {
        val digits = extractDigits(expiryText)
        if (digits.length != 4) return null

        val month = digits.take(2).toIntOrNull() ?: return null
        val yearShort = digits.drop(2).toIntOrNull() ?: return null

        return month to (2000 + yearShort)
    }

    fun isExpiryValid(month: Int, year: Int): Boolean {
        if (month !in 1..12) return false

        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1

        return year > currentYear || (year == currentYear && month >= currentMonth)
    }

    fun isValidCvv(cvv: String, brand: CardBrand): Boolean {
        val digits = extractDigits(cvv)
        return if (brand == CardBrand.AMEX) {
            digits.length == 4
        } else {
            digits.length == 3
        }
    }

    fun buildMaskedNumber(cardNumber: String): String {
        val digits = extractDigits(cardNumber)
        val last4 = digits.takeLast(4).padStart(4, '•')
        return "•••• •••• •••• $last4"
    }

    fun buildPreviewNumber(cardNumber: String): String {
        val formatted = formatCardNumberInput(cardNumber)
        return if (formatted.isBlank()) {
            "•••• •••• •••• ••••"
        } else {
            formatted
        }
    }

    fun defaultNickname(brand: CardBrand, last4: String): String {
        return "${brand.displayName} ending $last4"
    }

    fun demoCardExamplesText(): String {
        return "Enter any 16 digits for this demo card, for example 0000 0000 0000 0000"
    }
}