package uk.ac.dmu.koffeecraft.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import uk.ac.dmu.koffeecraft.data.entities.Favourite
import uk.ac.dmu.koffeecraft.data.entities.Product

@Dao
interface FavouriteDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(favourite: Favourite)

    @Query("""
        DELETE FROM favourites
        WHERE customerId = :customerId
          AND productId = :productId
    """)
    suspend fun delete(customerId: Long, productId: Long)

    @Query("""
        SELECT productId
        FROM favourites
        WHERE customerId = :customerId
    """)
    fun observeFavouriteProductIdsForCustomer(customerId: Long): Flow<List<Long>>

    @Query("""
        SELECT p.*
        FROM products p
        INNER JOIN favourites f ON f.productId = p.productId
        WHERE f.customerId = :customerId
        ORDER BY f.createdAt DESC
    """)
    fun observeFavouriteProductsForCustomer(customerId: Long): Flow<List<Product>>

    @Query("""
        SELECT
            p.productId AS productId,
            p.name AS productName,
            COUNT(f.productId) AS favouriteCount
        FROM products p
        INNER JOIN favourites f ON f.productId = p.productId
        GROUP BY p.productId, p.name
        ORDER BY favouriteCount DESC, p.name ASC
        LIMIT 3
    """)
    suspend fun getTopFavouriteProducts(): List<ProductFavouriteInsight>

    @Query("""
        SELECT
            p.productId AS productId,
            p.name AS productName,
            COUNT(f.productId) AS favouriteCount
        FROM products p
        INNER JOIN favourites f ON f.productId = p.productId
        GROUP BY p.productId, p.name
        HAVING COUNT(f.productId) > 0
        ORDER BY favouriteCount ASC, p.name ASC
        LIMIT 3
    """)
    suspend fun getLowestNonZeroFavouriteProducts(): List<ProductFavouriteInsight>
}

data class ProductFavouriteInsight(
    val productId: Long,
    val productName: String,
    val favouriteCount: Int
)