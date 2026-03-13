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

    @Query("""
        SELECT * FROM products
        ORDER BY category ASC, name ASC
    """)
    fun observeAll(): Flow<List<Product>>

    @Query("""
        SELECT * FROM products
        WHERE category = :category
        ORDER BY name ASC
    """)
    fun observeByCategory(category: String): Flow<List<Product>>

    @Query("SELECT COUNT(*) FROM products")
    suspend fun countProducts(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(products: List<Product>)

    @Update
    suspend fun update(product: Product)

    @Query("""
        UPDATE products
        SET isAvailable = :isActive
        WHERE productId = :productId
    """)
    suspend fun setActive(productId: Long, isActive: Boolean)

    @Query("""
        DELETE FROM products
        WHERE productId = :productId
    """)
    suspend fun deleteById(productId: Long)

    @Query("""
        SELECT * FROM products
        WHERE productId = :productId
        LIMIT 1
    """)
    suspend fun getById(productId: Long): Product?

    @Query("""
        SELECT * FROM products
        WHERE category = :category
          AND isAvailable = 1
        ORDER BY name ASC
    """)
    suspend fun getAvailableByCategory(category: String): List<Product>

    @Query("""
        SELECT * FROM products
        WHERE category = 'REWARD'
        ORDER BY name ASC
    """)
    suspend fun getRewardProducts(): List<Product>
}