package uk.ac.dmu.koffeecraft.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import uk.ac.dmu.koffeecraft.data.entities.Feedback

@Dao
interface FeedbackDao {

    @Query("SELECT * FROM feedback WHERE orderItemId = :orderItemId LIMIT 1")
    suspend fun getByOrderItemId(orderItemId: Long): Feedback?

    @Query("SELECT * FROM feedback WHERE customerId = :customerId ORDER BY updatedAt DESC")
    fun observeByCustomer(customerId: Long): Flow<List<Feedback>>

    @Query("""
        SELECT
            f.feedbackId AS feedbackId,
            f.orderItemId AS orderItemId,
            oi.orderId AS orderId,
            oi.productId AS productId,
            p.name AS productName,
            p.category AS productCategory,
            f.customerId AS customerId,
            f.rating AS rating,
            f.comment AS comment,
            f.isHidden AS isHidden,
            f.isModerated AS isModerated,
            f.createdAt AS createdAt,
            f.updatedAt AS updatedAt
        FROM feedback f
        INNER JOIN order_items oi ON oi.orderItemId = f.orderItemId
        INNER JOIN products p ON p.productId = oi.productId
        ORDER BY f.updatedAt DESC
    """)
    fun observeAllForAdmin(): Flow<List<AdminFeedbackItem>>

    @Query("""
        SELECT
            p.productId AS productId,
            p.name AS productName,
            AVG(CAST(f.rating AS REAL)) AS averageRating,
            COUNT(f.feedbackId) AS ratingCount
        FROM feedback f
        INNER JOIN order_items oi ON oi.orderItemId = f.orderItemId
        INNER JOIN products p ON p.productId = oi.productId
        GROUP BY p.productId, p.name
        ORDER BY averageRating DESC, ratingCount DESC, p.name ASC
        LIMIT 3
    """)
    suspend fun getTopRatedProducts(): List<ProductRatingInsight>

    @Query("""
        SELECT
            p.productId AS productId,
            p.name AS productName,
            AVG(CAST(f.rating AS REAL)) AS averageRating,
            COUNT(f.feedbackId) AS ratingCount
        FROM feedback f
        INNER JOIN order_items oi ON oi.orderItemId = f.orderItemId
        INNER JOIN products p ON p.productId = oi.productId
        GROUP BY p.productId, p.name
        ORDER BY averageRating ASC, ratingCount DESC, p.name ASC
        LIMIT 3
    """)
    suspend fun getLowestRatedProducts(): List<ProductRatingInsight>

    @Query("""
        SELECT
            p.productId AS productId,
            p.name AS productName,
            SUM(CASE WHEN TRIM(f.comment) != '' THEN 1 ELSE 0 END) AS commentCount,
            AVG(CAST(f.rating AS REAL)) AS averageRating,
            COUNT(f.feedbackId) AS ratingCount
        FROM feedback f
        INNER JOIN order_items oi ON oi.orderItemId = f.orderItemId
        INNER JOIN products p ON p.productId = oi.productId
        GROUP BY p.productId, p.name
        HAVING SUM(CASE WHEN TRIM(f.comment) != '' THEN 1 ELSE 0 END) > 0
        ORDER BY commentCount DESC, averageRating DESC, p.name ASC
        LIMIT 3
    """)
    suspend fun getMostCommentedProducts(): List<ProductCommentInsight>

    @Query("""
        SELECT
            p.productId AS productId,
            p.name AS productName,
            SUM(CASE WHEN TRIM(f.comment) != '' THEN 1 ELSE 0 END) AS commentCount,
            AVG(CAST(f.rating AS REAL)) AS averageRating,
            COUNT(f.feedbackId) AS ratingCount
        FROM feedback f
        INNER JOIN order_items oi ON oi.orderItemId = f.orderItemId
        INNER JOIN products p ON p.productId = oi.productId
        GROUP BY p.productId, p.name
        HAVING SUM(CASE WHEN TRIM(f.comment) != '' THEN 1 ELSE 0 END) > 0
        ORDER BY commentCount ASC, averageRating DESC, p.name ASC
        LIMIT 3
    """)
    suspend fun getLeastCommentedProducts(): List<ProductCommentInsight>

    @Query("""
        SELECT
            p.productId AS productId,
            p.name AS productName,
            p.description AS productDescription,
            p.price AS price,
            p.imageKey AS imageKey,
            AVG(CAST(f.rating AS REAL)) AS averageRating,
            COUNT(f.feedbackId) AS ratingCount
        FROM feedback f
        INNER JOIN order_items oi ON oi.orderItemId = f.orderItemId
        INNER JOIN products p ON p.productId = oi.productId
        WHERE p.category = :productFamily
          AND p.isAvailable = 1
        GROUP BY p.productId, p.name, p.description, p.price, p.imageKey
        HAVING COUNT(f.feedbackId) >= :minimumRatings
        ORDER BY averageRating DESC, ratingCount DESC, p.name ASC
        LIMIT :limit
    """)
    suspend fun getTopRatedProductsByFamily(
        productFamily: String,
        minimumRatings: Int,
        limit: Int
    ): List<HomeRatedProductInsight>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(feedback: Feedback)

    @Query("DELETE FROM feedback WHERE feedbackId = :feedbackId")
    suspend fun deleteById(feedbackId: Long)

    @Query("""
        UPDATE feedback
        SET isHidden = 1,
            isModerated = 1,
            updatedAt = :updatedAt
        WHERE feedbackId = :feedbackId
    """)
    suspend fun hideComment(feedbackId: Long, updatedAt: Long)

    @Query("""
        UPDATE feedback
        SET isHidden = 0,
            updatedAt = :updatedAt
        WHERE feedbackId = :feedbackId
    """)
    suspend fun unhideComment(feedbackId: Long, updatedAt: Long)
}

data class AdminFeedbackItem(
    val feedbackId: Long,
    val orderItemId: Long,
    val orderId: Long,
    val productId: Long,
    val productName: String,
    val productCategory: String,
    val customerId: Long,
    val rating: Int,
    val comment: String,
    val isHidden: Boolean,
    val isModerated: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

data class ProductRatingInsight(
    val productId: Long,
    val productName: String,
    val averageRating: Double,
    val ratingCount: Int
)

data class ProductCommentInsight(
    val productId: Long,
    val productName: String,
    val commentCount: Int,
    val averageRating: Double,
    val ratingCount: Int
)

data class HomeRatedProductInsight(
    val productId: Long,
    val productName: String,
    val productDescription: String,
    val price: Double,
    val imageKey: String?,
    val averageRating: Double,
    val ratingCount: Int
)