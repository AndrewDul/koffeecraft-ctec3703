package uk.ac.dmu.koffeecraft.data.repository

import kotlinx.coroutines.flow.Flow
import uk.ac.dmu.koffeecraft.data.dao.AddOnDao
import uk.ac.dmu.koffeecraft.data.dao.AllergenDao
import uk.ac.dmu.koffeecraft.data.dao.CustomerFavouritePresetDao
import uk.ac.dmu.koffeecraft.data.dao.FavouriteDao
import uk.ac.dmu.koffeecraft.data.dao.ProductDao
import uk.ac.dmu.koffeecraft.data.dao.ProductOptionDao
import uk.ac.dmu.koffeecraft.data.entities.AddOn
import uk.ac.dmu.koffeecraft.data.entities.CustomerFavouritePreset
import uk.ac.dmu.koffeecraft.data.entities.CustomerFavouritePresetAddOnCrossRef
import uk.ac.dmu.koffeecraft.data.entities.Favourite
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.entities.ProductOption
import uk.ac.dmu.koffeecraft.data.model.MenuProductConfiguration

class MenuRepository(
    private val productDao: ProductDao,
    private val favouriteDao: FavouriteDao,
    private val productOptionDao: ProductOptionDao,
    private val addOnDao: AddOnDao,
    private val allergenDao: AllergenDao,
    private val customerFavouritePresetDao: CustomerFavouritePresetDao,
    private val cartRepository: CartRepository
) {

    fun observeProductsByCategory(category: String): Flow<List<Product>> {
        return productDao.observeByCategory(category)
    }

    fun observeFavouriteProductIds(customerId: Long): Flow<List<Long>> {
        return favouriteDao.observeFavouriteProductIdsForCustomer(customerId)
    }

    suspend fun addFavourite(customerId: Long, productId: Long) {
        favouriteDao.insert(
            Favourite(
                customerId = customerId,
                productId = productId
            )
        )
    }

    suspend fun removeFavourite(customerId: Long, productId: Long) {
        favouriteDao.delete(customerId, productId)
    }

    suspend fun loadProductConfiguration(productId: Long): MenuProductConfiguration {
        val options = productOptionDao.getForProduct(productId)
        val addOns = addOnDao.getActiveForProduct(productId)
        val baseAllergens = allergenDao.getForProduct(productId)
        val addOnAllergens = addOns.associate { addOn ->
            addOn.addOnId to allergenDao.getForAddOn(addOn.addOnId)
        }

        val defaultOptionId =
            options.firstOrNull { it.isDefault }?.optionId ?: options.firstOrNull()?.optionId

        return MenuProductConfiguration(
            options = options,
            addOns = addOns,
            baseAllergens = baseAllergens,
            addOnAllergens = addOnAllergens,
            defaultOptionId = defaultOptionId
        )
    }

    suspend fun saveFavouritePreset(
        customerId: Long,
        product: Product,
        option: ProductOption,
        selectedAddOns: List<AddOn>
    ) {
        val totalPrice = product.price + option.extraPrice + selectedAddOns.sumOf { it.price }
        val totalCalories = option.estimatedCalories + selectedAddOns.sumOf { it.estimatedCalories }

        val presetId = customerFavouritePresetDao.insertPreset(
            CustomerFavouritePreset(
                customerId = customerId,
                productId = product.productId,
                optionId = option.optionId,
                totalPriceSnapshot = totalPrice,
                totalCaloriesSnapshot = totalCalories
            )
        )

        if (selectedAddOns.isNotEmpty()) {
            customerFavouritePresetDao.insertAddOnRefs(
                selectedAddOns.map { addOn ->
                    CustomerFavouritePresetAddOnCrossRef(
                        presetId = presetId,
                        addOnId = addOn.addOnId
                    )
                }
            )
        }
    }

    fun addConfiguredProductToCart(
        product: Product,
        option: ProductOption,
        selectedAddOns: List<AddOn>
    ) {
        cartRepository.addCustomisedProduct(
            product = product,
            option = option,
            addOns = selectedAddOns
        )
    }
}