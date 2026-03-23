package uk.ac.dmu.koffeecraft.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import uk.ac.dmu.koffeecraft.data.entities.ProductOption

@Dao
interface ProductOptionDao {

    @Query(
        """
        SELECT *
        FROM product_options
        WHERE productId = :productId
        ORDER BY isDefault DESC, sizeValue ASC, optionId ASC
        """
    )
    suspend fun getForProduct(productId: Long): List<ProductOption>

    @Query(
        """
        SELECT *
        FROM product_options
        WHERE optionId = :optionId
        LIMIT 1
        """
    )
    suspend fun getById(optionId: Long): ProductOption?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(option: ProductOption): Long

    @Update
    suspend fun update(option: ProductOption)

    @Query(
        """
        DELETE FROM product_options
        WHERE optionId = :optionId
        """
    )
    suspend fun deleteById(optionId: Long)

    @Query(
        """
        UPDATE product_options
        SET isDefault = 0
        WHERE productId = :productId
        """
    )
    suspend fun clearDefaultForProduct(productId: Long)
}