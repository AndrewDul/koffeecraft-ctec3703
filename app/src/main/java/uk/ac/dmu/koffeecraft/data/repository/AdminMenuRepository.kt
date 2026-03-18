package uk.ac.dmu.koffeecraft.data.repository

import kotlinx.coroutines.flow.Flow
import uk.ac.dmu.koffeecraft.data.dao.ProductDao
import uk.ac.dmu.koffeecraft.data.entities.Product

class AdminMenuRepository(
    private val productDao: ProductDao
) {
    fun observeProducts(): Flow<List<Product>> = productDao.observeAll()

    suspend fun setProductActive(productId: Long, isActive: Boolean) {
        productDao.setActive(productId, isActive)
    }

    suspend fun archiveProduct(productId: Long) {
        productDao.archiveById(productId)
    }

    suspend fun restoreProduct(productId: Long) {
        productDao.restoreById(productId)
    }

    suspend fun createProduct(
        name: String,
        productFamily: String,
        description: String,
        price: Double,
        rewardEnabled: Boolean,
        isNew: Boolean
    ): Product? {
        val insertedId = productDao.insert(
            Product(
                name = name,
                productFamily = productFamily,
                description = description,
                price = price,
                isActive = true,
                isNew = isNew,
                imageKey = null,
                rewardEnabled = rewardEnabled
            )
        )

        return productDao.getById(insertedId)
    }

    suspend fun updateProduct(
        existing: Product,
        name: String,
        productFamily: String,
        description: String,
        price: Double,
        rewardEnabled: Boolean,
        isNew: Boolean
    ): Product? {
        val updated = existing.copy(
            name = name,
            productFamily = productFamily,
            description = description,
            price = price,
            rewardEnabled = rewardEnabled,
            isNew = isNew
        )

        productDao.update(updated)
        return productDao.getById(existing.productId)
    }
}