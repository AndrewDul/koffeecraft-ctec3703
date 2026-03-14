package uk.ac.dmu.koffeecraft.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import uk.ac.dmu.koffeecraft.data.entities.AddOn
import uk.ac.dmu.koffeecraft.data.entities.ProductAddOnCrossRef

@Dao
interface AddOnDao {

    @Query("""
        SELECT * FROM add_ons
        WHERE category = :category
        ORDER BY name ASC
    """)
    fun observeByCategory(category: String): Flow<List<AddOn>>

    @Query("""
        SELECT * FROM add_ons
        WHERE category = :category
        ORDER BY name ASC
    """)
    suspend fun getAllByCategory(category: String): List<AddOn>

    @Query("""
        SELECT * FROM add_ons
        WHERE category = :category
          AND isActive = 1
        ORDER BY name ASC
    """)
    suspend fun getActiveByCategory(category: String): List<AddOn>

    @Query("""
        SELECT a.*
        FROM add_ons a
        INNER JOIN product_add_on_cross_ref pa ON pa.addOnId = a.addOnId
        WHERE pa.productId = :productId
        ORDER BY a.name ASC
    """)
    suspend fun getAssignedForProduct(productId: Long): List<AddOn>

    @Query("""
        SELECT addOnId
        FROM product_add_on_cross_ref
        WHERE productId = :productId
    """)
    suspend fun getAssignedIdsForProduct(productId: Long): List<Long>

    @Query("""
        SELECT a.*
        FROM add_ons a
        INNER JOIN product_add_on_cross_ref pa ON pa.addOnId = a.addOnId
        WHERE pa.productId = :productId
          AND a.isActive = 1
        ORDER BY a.name ASC
    """)
    suspend fun getActiveForProduct(productId: Long): List<AddOn>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(addOn: AddOn): Long

    @Update
    suspend fun update(addOn: AddOn)

    @Query("UPDATE add_ons SET isActive = :isActive WHERE addOnId = :addOnId")
    suspend fun setActive(addOnId: Long, isActive: Boolean)

    @Query("DELETE FROM add_ons WHERE addOnId = :addOnId")
    suspend fun deleteById(addOnId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductRefs(refs: List<ProductAddOnCrossRef>)
    @Query("DELETE FROM product_add_on_cross_ref WHERE productId = :productId AND addOnId = :addOnId")
    suspend fun deleteProductRef(productId: Long, addOnId: Long)
    @Query("DELETE FROM product_add_on_cross_ref WHERE productId = :productId")
    suspend fun deleteRefsForProduct(productId: Long)

    @Query("DELETE FROM product_add_on_cross_ref WHERE addOnId = :addOnId")
    suspend fun deleteRefsForAddOn(addOnId: Long)
}