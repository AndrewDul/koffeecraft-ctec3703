package uk.ac.dmu.koffeecraft.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.querymodel.CustomerFavouritePresetCard
import uk.ac.dmu.koffeecraft.data.querymodel.StandardFavouriteCard
data class CustomerFavouritesData(
    val presets: List<CustomerFavouritePresetCard>,
    val standardProducts: List<StandardFavouriteCard>
)

sealed interface CustomerFavouritesActionResult {
    data class Success(val message: String) : CustomerFavouritesActionResult
    data class Error(val message: String) : CustomerFavouritesActionResult
}

class CustomerFavouritesRepository(
    private val db: KoffeeCraftDatabase,
    private val cartRepository: CartRepository
) {

    fun observeFavourites(customerId: Long): Flow<CustomerFavouritesData> {
        return combine(
            db.customerFavouritePresetDao().observePresetCardsForCustomer(customerId),
            db.favouriteDao().observeStandardFavouriteCardsForCustomer(customerId)
        ) { presets, products ->
            CustomerFavouritesData(
                presets = presets,
                standardProducts = products
            )
        }
    }

    suspend fun removePreset(presetId: Long): CustomerFavouritesActionResult {
        db.customerFavouritePresetDao().deleteAddOnRefsForPreset(presetId)
        db.customerFavouritePresetDao().deletePresetById(presetId)

        return CustomerFavouritesActionResult.Success("Saved favourite removed.")
    }

    suspend fun removeStandardFavourite(
        customerId: Long,
        productId: Long
    ): CustomerFavouritesActionResult {
        db.favouriteDao().delete(customerId, productId)
        return CustomerFavouritesActionResult.Success("Favourite removed.")
    }

    suspend fun buyStandardFavouriteAgain(productId: Long): CustomerFavouritesActionResult {
        val product: Product? = db.productDao().getById(productId)

        if (product == null || !product.isActive) {
            return CustomerFavouritesActionResult.Error("This product is currently unavailable.")
        }

        cartRepository.addProduct(product)
        return CustomerFavouritesActionResult.Success("Favourite added to cart.")
    }

    suspend fun buyPresetAgain(presetId: Long): CustomerFavouritesActionResult {
        val preset = db.customerFavouritePresetDao().getPresetById(presetId)
            ?: return CustomerFavouritesActionResult.Error("This saved configuration is no longer available.")

        val product = db.productDao().getById(preset.productId)
        val option = db.productOptionDao().getById(preset.optionId)
        val addOns = db.customerFavouritePresetDao().getAddOnsForPreset(presetId)

        if (product == null || option == null) {
            return CustomerFavouritesActionResult.Error("This saved configuration is no longer available.")
        }

        cartRepository.addCustomisedProduct(
            product = product,
            option = option,
            addOns = addOns
        )

        return CustomerFavouritesActionResult.Success("Saved configuration added to cart.")
    }
}