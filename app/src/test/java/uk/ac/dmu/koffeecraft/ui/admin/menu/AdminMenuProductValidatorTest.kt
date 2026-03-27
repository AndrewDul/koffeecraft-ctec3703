package uk.ac.dmu.koffeecraft.ui.admin.menu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminMenuProductValidatorTest {

    private val validator = AdminMenuProductValidator()

    @Test
    fun validate_returnsValidResult_forCorrectNonMerchProduct() {
        val result = validator.validate(
            AdminMenuProductFormData(
                name = "Flat White",
                description = "Smooth coffee with steamed milk",
                priceText = "4.50",
                productFamily = "DRINK",
                rewardEnabled = false,
                isNew = true,
                imageKey = null,
                customImagePath = null
            )
        )

        assertTrue(result.isValid)
        assertEquals(4.50, result.validatedPrice)
        assertNull(result.nameError)
        assertNull(result.descriptionError)
        assertNull(result.priceError)
        assertNull(result.generalMessage)
    }

    @Test
    fun validate_returnsNameError_whenNameIsBlank() {
        val result = validator.validate(
            AdminMenuProductFormData(
                name = " ",
                description = "Good coffee",
                priceText = "4.00",
                productFamily = "DRINK",
                rewardEnabled = false,
                isNew = true,
                imageKey = null,
                customImagePath = null
            )
        )

        assertFalse(result.isValid)
        assertEquals("Enter product name", result.nameError)
    }

    @Test
    fun validate_returnsDescriptionError_whenDescriptionIsBlank() {
        val result = validator.validate(
            AdminMenuProductFormData(
                name = "Latte",
                description = " ",
                priceText = "4.20",
                productFamily = "DRINK",
                rewardEnabled = false,
                isNew = true,
                imageKey = null,
                customImagePath = null
            )
        )

        assertFalse(result.isValid)
        assertEquals("Enter product description", result.descriptionError)
    }

    @Test
    fun validate_returnsPriceError_whenMenuProductPriceIsZero() {
        val result = validator.validate(
            AdminMenuProductFormData(
                name = "Latte",
                description = "Coffee with milk",
                priceText = "0",
                productFamily = "DRINK",
                rewardEnabled = false,
                isNew = true,
                imageKey = null,
                customImagePath = null
            )
        )

        assertFalse(result.isValid)
        assertEquals("Menu products must have a price above 0", result.priceError)
    }

    @Test
    fun validate_returnsGeneralMessage_whenProductFamilyIsBlank() {
        val result = validator.validate(
            AdminMenuProductFormData(
                name = "Latte",
                description = "Coffee with milk",
                priceText = "4.20",
                productFamily = " ",
                rewardEnabled = false,
                isNew = true,
                imageKey = null,
                customImagePath = null
            )
        )

        assertFalse(result.isValid)
        assertEquals("Choose a product family.", result.generalMessage)
    }

    @Test
    fun validate_returnsGeneralMessage_whenMerchIsNotRewardEnabled() {
        val result = validator.validate(
            AdminMenuProductFormData(
                name = "KoffeeCraft Mug",
                description = "Ceramic mug",
                priceText = "12.99",
                productFamily = "MERCH",
                rewardEnabled = false,
                isNew = true,
                imageKey = null,
                customImagePath = null
            )
        )

        assertFalse(result.isValid)
        assertEquals("Merch products should be reward-enabled.", result.generalMessage)
    }
}