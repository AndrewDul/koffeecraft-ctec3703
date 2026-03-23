package uk.ac.dmu.koffeecraft.data.repository

import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.AddOn
import uk.ac.dmu.koffeecraft.data.entities.Allergen
import uk.ac.dmu.koffeecraft.data.entities.CustomerFavouritePreset
import uk.ac.dmu.koffeecraft.data.entities.CustomerFavouritePresetAddOnCrossRef
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.entities.ProductOption

data class ProductCustomizationData(
    val product: Product,
    val options: List<ProductOption>,
    val addOns: List<AddOn>,
    val baseAllergens: List<Allergen>,
    val addOnAllergens: Map<Long, List<Allergen>>
)

sealed interface ProductCustomizationActionResult {
    data class Success(val message: String) : ProductCustomizationActionResult
    data class Error(val message: String) : ProductCustomizationActionResult
}

class ProductCustomizationRepository(
    private val db: KoffeeCraftDatabase,
    private val cartRepository: CartRepository
) {

    suspend fun loadData(productId: Long): ProductCustomizationData? {
        val product = db.productDao().getById(productId) ?: return null
        val options = db.productOptionDao().getForProduct(productId)
        val addOns = db.addOnDao().getActiveForProduct(productId)
        val baseAllergens = db.allergenDao().getForProduct(productId)
        val addOnAllergens = addOns.associate { addOn ->
            addOn.addOnId to db.allergenDao().getForAddOn(addOn.addOnId)
        }

        return ProductCustomizationData(
            product = product,
            options = options,
            addOns = addOns,
            baseAllergens = baseAllergens,
            addOnAllergens = addOnAllergens
        )
    }

    suspend fun savePreset(
        customerId: Long,
        product: Product,
        option: ProductOption,
        selectedAddOns: List<AddOn>,
        rewardMode: Boolean
    ): ProductCustomizationActionResult {
        val basePrice = if (rewardMode) 0.0 else product.price
        val totalPrice = basePrice + option.extraPrice + selectedAddOns.sumOf { it.price }
        val totalCalories = option.estimatedCalories + selectedAddOns.sumOf { it.estimatedCalories }

        val presetId = db.customerFavouritePresetDao().insertPreset(
            CustomerFavouritePreset(
                customerId = customerId,
                productId = product.productId,
                optionId = option.optionId,
                totalPriceSnapshot = totalPrice,
                totalCaloriesSnapshot = totalCalories
            )
        )

        if (selectedAddOns.isNotEmpty()) {
            db.customerFavouritePresetDao().insertAddOnRefs(
                selectedAddOns.map { addOn ->
                    CustomerFavouritePresetAddOnCrossRef(
                        presetId = presetId,
                        addOnId = addOn.addOnId
                    )
                }
            )
        }

        return ProductCustomizationActionResult.Success("Favourite combo saved.")
    }

    suspend fun addToCart(
        product: Product,
        option: ProductOption,
        selectedAddOns: List<AddOn>,
        rewardMode: Boolean,
        rewardType: String?,
        rewardBeansCost: Int
    ): ProductCustomizationActionResult {
        val hasMeaningfulCustomisation = !option.isDefault || selectedAddOns.isNotEmpty()

        if (rewardMode) {
            if (hasMeaningfulCustomisation) {
                cartRepository.addRewardCustomisedProduct(
                    sourceProduct = product,
                    rewardType = rewardType ?: "CUSTOM_REWARD",
                    beansCostPerUnit = rewardBeansCost,
                    option = option,
                    addOns = selectedAddOns
                )
            } else {
                cartRepository.addReward(
                    sourceProduct = product,
                    rewardType = rewardType ?: "CUSTOM_REWARD",
                    beansCostPerUnit = rewardBeansCost
                )
            }

            return ProductCustomizationActionResult.Success("Reward added to cart.")
        }

        cartRepository.addCustomisedProduct(
            product = product,
            option = option,
            addOns = selectedAddOns
        )

        return ProductCustomizationActionResult.Success("Product added to cart.")
    }
}