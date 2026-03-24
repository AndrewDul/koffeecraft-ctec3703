package uk.ac.dmu.koffeecraft.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import uk.ac.dmu.koffeecraft.data.entities.Favourite
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.querymodel.HomeLovedProductInsight
import uk.ac.dmu.koffeecraft.data.querymodel.ProductFavouriteInsight
import uk.ac.dmu.koffeecraft.data.querymodel.StandardFavouriteCard

@Dao
interface FavouriteDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(favourite: Favourite)

    @Query(
        """
        DELETE FROM favourites
        WHERE customerId = :customerId
          AND productId = :productId
        """
    )
    suspend fun delete(customerId: Long, productId: Long)

    @Query(
        """
        SELECT productId
        FROM favourites
        WHERE customerId = :customerId
        """
    )
    fun observeFavouriteProductIdsForCustomer(customerId: Long): Flow<List<Long>>

    @Query(
        """
        SELECT p.*
        FROM products p
        INNER JOIN favourites f ON f.productId = p.productId
        WHERE f.customerId = :customerId
          AND p.isAvailable = 1
        ORDER BY f.createdAt DESC
        """
    )
    fun observeFavouriteProductsForCustomer(customerId: Long): Flow<List<Product>>

    @Query(
        """
        SELECT
            p.productId AS productId,
            p.name AS name,
            p.description AS description,
            p.category AS productFamily,
            p.price AS price,
            p.isAvailable AS isActive,
            po.displayLabel AS standardOptionLabel,
            po.sizeValue AS standardSizeValue,
            po.sizeUnit AS standardSizeUnit,
            po.estimatedCalories AS standardCalories
        FROM products p
        INNER JOIN favourites f ON f.productId = p.productId
        LEFT JOIN product_options po
            ON po.productId = p.productId
           AND po.isDefault = 1
        WHERE f.customerId = :customerId
          AND p.isAvailable = 1
        ORDER BY f.createdAt DESC
        """
    )
    fun observeStandardFavouriteCardsForCustomer(customerId: Long): Flow<List<StandardFavouriteCard>>

    @Query(
        """
        SELECT
            p.productId AS productId,
            p.name AS productName,
            COUNT(f.productId) AS favouriteCount
        FROM products p
        INNER JOIN favourites f ON f.productId = p.productId
        GROUP BY p.productId, p.name
        ORDER BY favouriteCount DESC, p.name ASC
        LIMIT 3
        """
    )
    suspend fun getTopFavouriteProducts(): List<ProductFavouriteInsight>

    @Query(
        """
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
        """
    )
    suspend fun getLowestNonZeroFavouriteProducts(): List<ProductFavouriteInsight>

    @Query(
        """
        SELECT
            p.productId AS productId,
            p.name AS productName,
            p.description AS productDescription,
            p.category AS productFamily,
            p.price AS price,
            COUNT(f.productId) AS favouriteCount
        FROM products p
        INNER JOIN favourites f ON f.productId = p.productId
        WHERE p.isAvailable = 1
          AND p.category IN ('COFFEE', 'CAKE')
        GROUP BY p.productId, p.name, p.description, p.category, p.price
        HAVING COUNT(f.productId) > 0
        ORDER BY favouriteCount DESC, p.name ASC
        LIMIT :limit
        """
    )
    suspend fun getMostLovedProducts(limit: Int): List<HomeLovedProductInsight>
}