package uk.ac.dmu.koffeecraft.data.repository

import kotlinx.coroutines.flow.Flow
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.AddOn
import uk.ac.dmu.koffeecraft.data.entities.AddOnAllergenCrossRef
import uk.ac.dmu.koffeecraft.data.entities.Allergen
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.entities.ProductAddOnCrossRef
import uk.ac.dmu.koffeecraft.data.entities.ProductAllergenCrossRef
import uk.ac.dmu.koffeecraft.data.entities.ProductOption

class AdminMenuRepository(
    private val db: KoffeeCraftDatabase
) {
    fun observeProducts(): Flow<List<Product>> = db.productDao().observeAll()

    suspend fun setProductActive(productId: Long, isActive: Boolean) {
        db.productDao().setActive(productId, isActive)
    }

    suspend fun archiveProduct(productId: Long) {
        db.productDao().archiveById(productId)
    }

    suspend fun restoreProduct(productId: Long) {
        db.productDao().restoreById(productId)
    }

    suspend fun createProduct(
        name: String,
        productFamily: String,
        description: String,
        price: Double,
        rewardEnabled: Boolean,
        isNew: Boolean
    ): Product? {
        val insertedId = db.productDao().insert(
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

        return db.productDao().getById(insertedId)
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

        db.productDao().update(updated)
        return db.productDao().getById(existing.productId)
    }

    fun productOptionDao(): ProductOptionGateway = ProductOptionGateway(db)
    fun addOnDao(): AddOnGateway = AddOnGateway(db)
    fun allergenDao(): AllergenGateway = AllergenGateway(db)
}

class ProductOptionGateway(
    private val db: KoffeeCraftDatabase
) {
    suspend fun getForProduct(productId: Long): List<ProductOption> {
        return db.productOptionDao().getForProduct(productId)
    }

    suspend fun deleteById(optionId: Long) {
        db.productOptionDao().deleteById(optionId)
    }

    suspend fun clearDefaultForProduct(productId: Long) {
        db.productOptionDao().clearDefaultForProduct(productId)
    }

    suspend fun insert(option: ProductOption): Long {
        return db.productOptionDao().insert(option)
    }

    suspend fun update(option: ProductOption) {
        db.productOptionDao().update(option)
    }
}

class AddOnGateway(
    private val db: KoffeeCraftDatabase
) {
    suspend fun getAllByCategory(category: String): List<AddOn> {
        return db.addOnDao().getAllByCategory(category)
    }

    suspend fun getAssignedForProduct(productId: Long): List<AddOn> {
        return db.addOnDao().getAssignedForProduct(productId)
    }

    suspend fun deleteProductRef(productId: Long, addOnId: Long) {
        db.addOnDao().deleteProductRef(productId, addOnId)
    }

    suspend fun setActive(addOnId: Long, isActive: Boolean) {
        db.addOnDao().setActive(addOnId, isActive)
    }

    suspend fun insertProductRefs(refs: List<ProductAddOnCrossRef>) {
        db.addOnDao().insertProductRefs(refs)
    }

    suspend fun deleteRefsForAddOn(addOnId: Long) {
        db.addOnDao().deleteRefsForAddOn(addOnId)
    }

    suspend fun deleteById(addOnId: Long) {
        db.addOnDao().deleteById(addOnId)
    }

    suspend fun insert(addOn: AddOn): Long {
        return db.addOnDao().insert(addOn)
    }

    suspend fun update(addOn: AddOn) {
        db.addOnDao().update(addOn)
    }
}

class AllergenGateway(
    private val db: KoffeeCraftDatabase
) {
    suspend fun getForProduct(productId: Long): List<Allergen> {
        return db.allergenDao().getForProduct(productId)
    }

    suspend fun getAll(): List<Allergen> {
        return db.allergenDao().getAll()
    }

    suspend fun insert(allergen: Allergen): Long {
        return db.allergenDao().insert(allergen)
    }

    suspend fun getProductAllergenIds(productId: Long): List<Long> {
        return db.allergenDao().getProductAllergenIds(productId)
    }

    suspend fun deleteProductRefs(productId: Long) {
        db.allergenDao().deleteProductRefs(productId)
    }

    suspend fun insertProductRefs(refs: List<ProductAllergenCrossRef>) {
        db.allergenDao().insertProductRefs(refs)
    }

    suspend fun getForAddOn(addOnId: Long): List<Allergen> {
        return db.allergenDao().getForAddOn(addOnId)
    }

    suspend fun getAddOnAllergenIds(addOnId: Long): List<Long> {
        return db.allergenDao().getAddOnAllergenIds(addOnId)
    }

    suspend fun deleteAddOnRefs(addOnId: Long) {
        db.allergenDao().deleteAddOnRefs(addOnId)
    }

    suspend fun insertAddOnRefs(refs: List<AddOnAllergenCrossRef>) {
        db.allergenDao().insertAddOnRefs(refs)
    }
}