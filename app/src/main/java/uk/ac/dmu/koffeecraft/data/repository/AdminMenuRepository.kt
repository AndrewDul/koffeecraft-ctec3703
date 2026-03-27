package uk.ac.dmu.koffeecraft.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.AddOn
import uk.ac.dmu.koffeecraft.data.entities.AddOnAllergenCrossRef
import uk.ac.dmu.koffeecraft.data.entities.Allergen
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.entities.ProductAddOnCrossRef
import uk.ac.dmu.koffeecraft.data.entities.ProductAllergenCrossRef
import uk.ac.dmu.koffeecraft.data.entities.ProductOption
import uk.ac.dmu.koffeecraft.data.db.CatalogDefaults
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
        isNew: Boolean,
        imageKey: String?,
        customImagePath: String?
    ): Product? = db.withTransaction {
        val insertedId = db.productDao().insert(
            Product(
                name = name,
                productFamily = productFamily,
                description = description,
                price = price,
                isActive = true,
                isNew = isNew,
                imageKey = imageKey,
                customImagePath = customImagePath,
                rewardEnabled = rewardEnabled
            )
        )

        val createdProduct = db.productDao().getById(insertedId)
        if (createdProduct != null) {
            CatalogDefaults.seedCatalogLibrary(db)
            CatalogDefaults.seedProductDefaults(db, createdProduct)
        }

        createdProduct
    }

    suspend fun updateProduct(
        existing: Product,
        name: String,
        productFamily: String,
        description: String,
        price: Double,
        rewardEnabled: Boolean,
        isNew: Boolean,
        imageKey: String?,
        customImagePath: String?
    ): Product? = db.withTransaction {
        val updated = existing.copy(
            name = name,
            productFamily = productFamily,
            description = description,
            price = price,
            rewardEnabled = rewardEnabled,
            isNew = isNew,
            imageKey = imageKey,
            customImagePath = customImagePath
        )

        db.productDao().update(updated)
        db.productDao().getById(existing.productId)
    }

    suspend fun getOptionsForProduct(productId: Long): List<ProductOption> {
        return db.productOptionDao().getForProduct(productId)
    }

    suspend fun deleteOptionById(optionId: Long) {
        db.productOptionDao().deleteById(optionId)
    }

    suspend fun saveProductOption(
        productId: Long,
        existing: ProductOption?,
        optionName: String,
        displayLabel: String,
        sizeValue: Int,
        sizeUnit: String,
        extraPrice: Double,
        estimatedCalories: Int,
        isDefault: Boolean
    ) {
        db.withTransaction {
            if (isDefault) {
                db.productOptionDao().clearDefaultForProduct(productId)
            }

            if (existing == null) {
                db.productOptionDao().insert(
                    ProductOption(
                        productId = productId,
                        optionName = optionName,
                        displayLabel = displayLabel,
                        sizeValue = sizeValue,
                        sizeUnit = sizeUnit,
                        extraPrice = extraPrice,
                        estimatedCalories = estimatedCalories,
                        isDefault = isDefault
                    )
                )
            } else {
                db.productOptionDao().update(
                    existing.copy(
                        displayLabel = displayLabel,
                        sizeValue = sizeValue,
                        sizeUnit = sizeUnit,
                        extraPrice = extraPrice,
                        estimatedCalories = estimatedCalories,
                        isDefault = isDefault
                    )
                )
            }
        }
    }

    suspend fun getAllAddOnsByCategory(category: String): List<AddOn> {
        return db.addOnDao().getAllByCategory(category)
    }

    suspend fun getAssignedAddOnsForProduct(productId: Long): List<AddOn> {
        return db.addOnDao().getAssignedForProduct(productId)
    }

    suspend fun removeAddOnFromProduct(productId: Long, addOnId: Long) {
        db.addOnDao().deleteProductRef(productId, addOnId)
    }

    suspend fun assignAddOnToProduct(productId: Long, addOnId: Long) {
        db.addOnDao().insertProductRefs(
            listOf(
                ProductAddOnCrossRef(
                    productId = productId,
                    addOnId = addOnId
                )
            )
        )
    }

    suspend fun setAddOnActive(addOnId: Long, isActive: Boolean) {
        db.addOnDao().setActive(addOnId, isActive)
    }

    suspend fun saveAddOn(
        existing: AddOn?,
        name: String,
        category: String,
        price: Double,
        estimatedCalories: Int,
        isActive: Boolean
    ) {
        if (existing == null) {
            db.addOnDao().insert(
                AddOn(
                    name = name,
                    category = category,
                    price = price,
                    estimatedCalories = estimatedCalories,
                    isActive = isActive
                )
            )
        } else {
            db.addOnDao().update(
                existing.copy(
                    name = name,
                    price = price,
                    estimatedCalories = estimatedCalories,
                    isActive = isActive
                )
            )
        }
    }

    suspend fun deleteAddOnPermanently(addOnId: Long) {
        db.withTransaction {
            db.addOnDao().deleteRefsForAddOn(addOnId)
            db.allergenDao().deleteAddOnRefs(addOnId)
            db.addOnDao().deleteById(addOnId)
        }
    }

    suspend fun getAllAllergens(): List<Allergen> {
        return db.allergenDao().getAll()
    }

    suspend fun createAllergen(name: String): Long {
        return db.allergenDao().insert(Allergen(name = name))
    }

    suspend fun getAllergensForProduct(productId: Long): List<Allergen> {
        return db.allergenDao().getForProduct(productId)
    }

    suspend fun getSelectedProductAllergenIds(productId: Long): Set<Long> {
        return db.allergenDao().getProductAllergenIds(productId).toSet()
    }

    suspend fun replaceProductAllergenSelection(
        productId: Long,
        allergenIds: Set<Long>
    ) {
        val refs = allergenIds.map { allergenId ->
            ProductAllergenCrossRef(
                productId = productId,
                allergenId = allergenId
            )
        }

        db.withTransaction {
            db.allergenDao().deleteProductRefs(productId)
            if (refs.isNotEmpty()) {
                db.allergenDao().insertProductRefs(refs)
            }
        }
    }

    suspend fun getAllergensForAddOn(addOnId: Long): List<Allergen> {
        return db.allergenDao().getForAddOn(addOnId)
    }

    suspend fun getSelectedAddOnAllergenIds(addOnId: Long): Set<Long> {
        return db.allergenDao().getAddOnAllergenIds(addOnId).toSet()
    }

    suspend fun replaceAddOnAllergenSelection(
        addOnId: Long,
        allergenIds: Set<Long>
    ) {
        val refs = allergenIds.map { allergenId ->
            AddOnAllergenCrossRef(
                addOnId = addOnId,
                allergenId = allergenId
            )
        }

        db.withTransaction {
            db.allergenDao().deleteAddOnRefs(addOnId)
            if (refs.isNotEmpty()) {
                db.allergenDao().insertAddOnRefs(refs)
            }
        }
    }
}