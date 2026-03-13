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

    // Seed helpers
    @Query("SELECT COUNT(*) FROM products")
    suspend fun countProducts(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(products: List<Product>)

    // Customer: I return all products in a category (including disabled).
    @Query("SELECT * FROM products WHERE category = :category ORDER BY name ASC")
    fun observeByCategory(category: String): Flow<List<Product>>

    // Admin: I return all products for management.
    @Query("SELECT * FROM products ORDER BY category ASC, name ASC")
    fun observeAll(): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(product: Product): Long

    @Update
    suspend fun update(product: Product)

    @Query("DELETE FROM products WHERE productId = :productId")
    suspend fun deleteById(productId: Long)

    // Admin toggles (DB column is isAvailable)
    @Query("UPDATE products SET isAvailable = :isActive WHERE productId = :productId")
    suspend fun setActive(productId: Long, isActive: Boolean)

    @Query("UPDATE products SET isNew = :isNew WHERE productId = :productId")
    suspend fun setNew(productId: Long, isNew: Boolean)

    @Query("UPDATE products SET imageKey = :imageKey WHERE productId = :productId")
    suspend fun setImageKey(productId: Long, imageKey: String?)

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