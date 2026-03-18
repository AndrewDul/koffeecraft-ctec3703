package uk.ac.dmu.koffeecraft.ui.admin.menu

class AdminMenuProductValidator {

    fun validate(formData: AdminMenuProductFormData): AdminMenuProductValidationResult {
        val name = formData.name.trim()
        val description = formData.description.trim()
        val productFamily = formData.productFamily.trim()
        val price = formData.priceText.trim().toDoubleOrNull()

        var nameError: String? = null
        var descriptionError: String? = null
        var priceError: String? = null
        var generalMessage: String? = null

        if (name.isBlank()) {
            nameError = "Enter product name"
        }

        if (description.isBlank()) {
            descriptionError = "Enter product description"
        }

        if (productFamily.isBlank()) {
            generalMessage = "Choose a product family."
        }

        if (price == null || price < 0.0) {
            priceError = "Enter a valid price"
        } else {
            if (productFamily != "MERCH" && price <= 0.0) {
                priceError = "Menu products must have a price above 0"
            }

            if (productFamily == "MERCH" && !formData.rewardEnabled) {
                generalMessage = "Merch products should be reward-enabled."
            }
        }

        val validatedPrice = if (
            nameError == null &&
            descriptionError == null &&
            priceError == null &&
            generalMessage == null
        ) {
            price
        } else {
            null
        }

        return AdminMenuProductValidationResult(
            nameError = nameError,
            descriptionError = descriptionError,
            priceError = priceError,
            generalMessage = generalMessage,
            validatedPrice = validatedPrice
        )
    }
}