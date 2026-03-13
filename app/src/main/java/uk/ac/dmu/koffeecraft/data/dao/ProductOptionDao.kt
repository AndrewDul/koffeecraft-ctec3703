package uk.ac.dmu.koffeecraft.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import uk.ac.dmu.koffeecraft.data.entities.ProductOption

@Dao
interface ProductOptionDao {

    @Query("""
        SELECT * FROM product_options
        WHERE productId = :productId
        ORDER BY isDefault DESC, extraPrice ASC, optionId ASC
    """)
    fun observeForProduct(productId: Long): Flow<List<ProductOption>>

    @Query("""
        SELECT * FROM product_options
        WHERE productId = :productId
        ORDER BY isDefault DESC, extraPrice ASC, optionId ASC
    """)
    suspend fun getForProduct(productId: Long): List<ProductOption>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(option: ProductOption): Long

    @Update
    suspend fun update(option: ProductOption)

    @Query("UPDATE product_options SET isDefault = 0 WHERE productId = :productId")
    suspend fun clearDefaultForProduct(productId: Long)

    @Query("DELETE FROM product_options WHERE optionId = :optionId")
    suspend fun deleteById(optionId: Long)

    @Query("DELETE FROM product_options WHERE productId = :productId")
    suspend fun deleteByProduct(productId: Long)
}