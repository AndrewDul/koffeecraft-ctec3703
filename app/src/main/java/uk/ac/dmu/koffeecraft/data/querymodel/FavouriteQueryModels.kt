package uk.ac.dmu.koffeecraft.data.querymodel

data class CustomerFavouritePresetCard(
    val presetId: Long,
    val productId: Long,
    val productName: String,
    val productDescription: String,
    val productFamily: String,
    val imageKey: String?,
    val customImagePath: String?,
    val optionId: Long,
    val optionLabel: String,
    val optionSizeValue: Int,
    val optionSizeUnit: String,
    val totalPrice: Double,
    val totalCalories: Int,
    val addOnSummary: String?,
    val createdAt: Long
)

data class ProductFavouriteInsight(
    val productId: Long,
    val productName: String,
    val productFamily: String,
    val imageKey: String?,
    val customImagePath: String?,
    val favouriteCount: Int
)

data class HomeLovedProductInsight(
    val productId: Long,
    val productName: String,
    val productDescription: String,
    val productFamily: String,
    val price: Double,
    val imageKey: String?,
    val customImagePath: String?,
    val favouriteCount: Int
)

data class StandardFavouriteCard(
    val productId: Long,
    val name: String,
    val description: String,
    val productFamily: String,
    val price: Double,
    val isActive: Boolean,
    val imageKey: String?,
    val customImagePath: String?,
    val standardOptionLabel: String?,
    val standardSizeValue: Int?,
    val standardSizeUnit: String?,
    val standardCalories: Int?
) {
    val familyLabel: String
        get() = when {
            productFamily.equals("COFFEE", ignoreCase = true) -> "Coffee"
            productFamily.equals("CAKE", ignoreCase = true) -> "Cake"
            productFamily.equals("MERCH", ignoreCase = true) -> "Merch"
            else -> productFamily.replaceFirstChar { it.uppercase() }
        }

    val standardSizeText: String
        get() {
            val label = standardOptionLabel?.takeIf { it.isNotBlank() }
            val sizeValue = standardSizeValue
            val sizeUnit = standardSizeUnit?.takeIf { it.isNotBlank() }?.lowercase()

            return when {
                label != null && sizeValue != null && sizeUnit != null -> "$label • ${sizeValue}${sizeUnit}"
                label != null -> label
                sizeValue != null && sizeUnit != null -> "${sizeValue}${sizeUnit}"
                else -> "Not set"
            }
        }

    val standardCaloriesText: String
        get() = standardCalories?.let { "$it kcal" } ?: "Not set"
}