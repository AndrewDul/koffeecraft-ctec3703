package uk.ac.dmu.koffeecraft.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import uk.ac.dmu.koffeecraft.data.entities.Product

@Dao
interface ProductDao {

    @Query(
        """
        SELECT * FROM products
        ORDER BY
            CASE category
                WHEN 'COFFEE' THEN 0
                WHEN 'CAKE' THEN 1
                WHEN 'MERCH' THEN 2
                ELSE 3
            END,
            rewardEnabled DESC,
            name ASC
        """
    )
    fun observeAll(): Flow<List<Product>>

    @Query(
        """
        SELECT * FROM products
        ORDER BY
            CASE category
                WHEN 'COFFEE' THEN 0
                WHEN 'CAKE' THEN 1
                WHEN 'MERCH' THEN 2
                ELSE 3
            END,
            rewardEnabled DESC,
            name ASC
        """
    )
    suspend fun getAllOnce(): List<Product>

    @Query(
        """
        SELECT * FROM products
        WHERE category = :category
          AND isAvailable = 1
        ORDER BY name ASC
        """
    )
    fun observeByCategory(category: String): Flow<List<Product>>

    @Query("SELECT COUNT(*) FROM products")
    suspend fun countProducts(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(products: List<Product>)

    @Update
    suspend fun update(product: Product)

    @Query(
        """
        UPDATE products
        SET isAvailable = :isActive
        WHERE productId = :productId
        """
    )
    suspend fun setActive(productId: Long, isActive: Boolean)

    @Query(
        """
        UPDATE products
        SET isAvailable = 0
        WHERE productId = :productId
        """
    )
    suspend fun archiveById(productId: Long)

    @Query(
        """
        UPDATE products
        SET isAvailable = 1
        WHERE productId = :productId
        """
    )
    suspend fun restoreById(productId: Long)

    @Query(
        """
        SELECT * FROM products
        WHERE productId = :productId
        LIMIT 1
        """
    )
    suspend fun getById(productId: Long): Product?

    @Query(
        """
        SELECT * FROM products
        WHERE category = :category
          AND isAvailable = 1
          AND rewardEnabled = 1
        ORDER BY name ASC
        """
    )
    suspend fun getAvailableByCategory(category: String): List<Product>

    @Query(
        """
        SELECT * FROM products
        WHERE category = 'MERCH'
          AND isAvailable = 1
          AND rewardEnabled = 1
        ORDER BY name ASC
        """
    )
    suspend fun getRewardProducts(): List<Product>

    @Query(
        """
        SELECT * FROM products
        WHERE isAvailable = 1
          AND isNew = 1
        ORDER BY
            CASE category
                WHEN 'COFFEE' THEN 0
                WHEN 'CAKE' THEN 1
                WHEN 'MERCH' THEN 2
                ELSE 3
            END,
            name ASC
        """
    )
    suspend fun getActiveNewProducts(): List<Product>

    @Query("SELECT COUNT(*) FROM products WHERE isAvailable = 1")
    suspend fun countActiveProducts(): Int

    @Query("SELECT COUNT(*) FROM products WHERE isAvailable = 0")
    suspend fun countDisabledProducts(): Int

    @Query("SELECT COUNT(*) FROM products WHERE isAvailable = 1 AND rewardEnabled = 1")
    suspend fun countActiveRewardEnabledProducts(): Int

    @Query("SELECT COUNT(*) FROM products WHERE isAvailable = 1 AND isNew = 1")
    suspend fun countActiveNewProducts(): Int
}