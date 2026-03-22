package uk.ac.dmu.koffeecraft.data.repository

import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.Product

class RewardProductPickerRepository(
    private val db: KoffeeCraftDatabase
) {

    suspend fun loadProducts(category: String): List<Product> {
        return db.productDao().getAvailableByCategory(category)
    }
}