package uk.ac.dmu.koffeecraft.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import uk.ac.dmu.koffeecraft.data.entities.Product

@Dao
interface ProductDao {

    @Query("SELECT * FROM products WHERE isAvailable = 1 ORDER BY name ASC")
    fun observeAllAvailable(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE category = :category AND isAvailable = 1 ORDER BY name ASC")
    fun observeByCategory(category: String): Flow<List<Product>>

    @Query("SELECT COUNT(*) FROM products")
    suspend fun countProducts(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)
}