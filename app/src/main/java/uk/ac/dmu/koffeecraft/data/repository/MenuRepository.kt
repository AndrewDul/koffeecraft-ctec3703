package uk.ac.dmu.koffeecraft.data.repository

import kotlinx.coroutines.flow.Flow
import uk.ac.dmu.koffeecraft.data.dao.FavouriteDao
import uk.ac.dmu.koffeecraft.data.dao.ProductDao
import uk.ac.dmu.koffeecraft.data.entities.Favourite
import uk.ac.dmu.koffeecraft.data.entities.Product

class MenuRepository(
    private val productDao: ProductDao,
    private val favouriteDao: FavouriteDao
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
}