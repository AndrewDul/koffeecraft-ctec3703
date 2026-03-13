package uk.ac.dmu.koffeecraft.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import uk.ac.dmu.koffeecraft.data.entities.AddOnAllergenCrossRef
import uk.ac.dmu.koffeecraft.data.entities.Allergen
import uk.ac.dmu.koffeecraft.data.entities.ProductAllergenCrossRef

@Dao
interface AllergenDao {

    @Query("SELECT * FROM allergens ORDER BY name ASC")
    suspend fun getAll(): List<Allergen>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(allergen: Allergen): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductRefs(refs: List<ProductAllergenCrossRef>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddOnRefs(refs: List<AddOnAllergenCrossRef>)

    @Query("DELETE FROM product_allergen_cross_ref WHERE productId = :productId")
    suspend fun deleteProductRefs(productId: Long)

    @Query("DELETE FROM add_on_allergen_cross_ref WHERE addOnId = :addOnId")
    suspend fun deleteAddOnRefs(addOnId: Long)

    @Query("""
        SELECT allergenId
        FROM product_allergen_cross_ref
        WHERE productId = :productId
    """)
    suspend fun getProductAllergenIds(productId: Long): List<Long>

    @Query("""
        SELECT allergenId
        FROM add_on_allergen_cross_ref
        WHERE addOnId = :addOnId
    """)
    suspend fun getAddOnAllergenIds(addOnId: Long): List<Long>

    @Query("""
        SELECT al.*
        FROM allergens al
        INNER JOIN product_allergen_cross_ref pa ON pa.allergenId = al.allergenId
        WHERE pa.productId = :productId
        ORDER BY al.name ASC
    """)
    suspend fun getForProduct(productId: Long): List<Allergen>

    @Query("""
        SELECT al.*
        FROM allergens al
        INNER JOIN add_on_allergen_cross_ref aa ON aa.allergenId = al.allergenId
        WHERE aa.addOnId = :addOnId
        ORDER BY al.name ASC
    """)
    suspend fun getForAddOn(addOnId: Long): List<Allergen>
}