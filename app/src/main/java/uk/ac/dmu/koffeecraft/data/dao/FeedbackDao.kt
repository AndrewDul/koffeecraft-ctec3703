package uk.ac.dmu.koffeecraft.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import uk.ac.dmu.koffeecraft.data.entities.Feedback
import uk.ac.dmu.koffeecraft.data.querymodel.AdminFeedbackItem
import uk.ac.dmu.koffeecraft.data.querymodel.AdminFeedbackOverview
import uk.ac.dmu.koffeecraft.data.querymodel.AdminFeedbackRatingBreakdown
import uk.ac.dmu.koffeecraft.data.querymodel.HomeRatedProductInsight
import uk.ac.dmu.koffeecraft.data.querymodel.ProductCommentInsight
import uk.ac.dmu.koffeecraft.data.querymodel.ProductRatingInsight
@Dao
interface FeedbackDao {

    @Query("SELECT * FROM feedback WHERE orderItemId = :orderItemId LIMIT 1")
    suspend fun getByOrderItemId(orderItemId: Long): Feedback?

    @Query("SELECT * FROM feedback WHERE customerId = :customerId ORDER BY updatedAt DESC")
    fun observeByCustomer(customerId: Long): Flow<List<Feedback>>

    @Query(
        """
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
        """
    )
    fun observeAllForAdmin(): Flow<List<AdminFeedbackItem>>

    @Query(
        """
        SELECT
            ROUND(COALESCE(AVG(CAST(f.rating AS REAL)), 0), 1) AS overallAverage,
            ROUND(COALESCE((
                SELECT AVG(CAST(f2.rating AS REAL))
                FROM feedback f2
                INNER JOIN order_items oi2 ON oi2.orderItemId = f2.orderItemId
                INNER JOIN products p2 ON p2.productId = oi2.productId
                WHERE UPPER(p2.category) = 'COFFEE'
            ), 0), 1) AS coffeeAverage,
            ROUND(COALESCE((
                SELECT AVG(CAST(f3.rating AS REAL))
                FROM feedback f3
                INNER JOIN order_items oi3 ON oi3.orderItemId = f3.orderItemId
                INNER JOIN products p3 ON p3.productId = oi3.productId
                WHERE UPPER(p3.category) = 'CAKE'
            ), 0), 1) AS cakeAverage,
            COUNT(*) AS totalReviews,
            COALESCE(SUM(CASE WHEN TRIM(f.comment) != '' THEN 1 ELSE 0 END), 0) AS reviewsWithComments,
            COALESCE(SUM(CASE WHEN f.isHidden = 1 AND TRIM(f.comment) != '' THEN 1 ELSE 0 END), 0) AS hiddenComments
        FROM feedback f
        """
    )
    suspend fun getAdminFeedbackOverview(): AdminFeedbackOverview

    @Query(
        """
        SELECT
            rating AS rating,
            COUNT(*) AS reviewCount
        FROM feedback
        GROUP BY rating
        ORDER BY rating DESC
        """
    )
    suspend fun getAdminFeedbackBreakdown(): List<AdminFeedbackRatingBreakdown>

    @Query(
        """
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
        """
    )
    suspend fun getTopRatedProducts(): List<ProductRatingInsight>

    @Query(
        """
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
        """
    )
    suspend fun getLowestRatedProducts(): List<ProductRatingInsight>

    @Query(
        """
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
        """
    )
    suspend fun getMostCommentedProducts(): List<ProductCommentInsight>

    @Query(
        """
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
        """
    )
    suspend fun getLeastCommentedProducts(): List<ProductCommentInsight>

    @Query(
        """
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
        """
    )
    suspend fun getTopRatedProductsByFamily(
        productFamily: String,
        minimumRatings: Int,
        limit: Int
    ): List<HomeRatedProductInsight>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(feedback: Feedback)

    @Query("DELETE FROM feedback WHERE feedbackId = :feedbackId")
    suspend fun deleteById(feedbackId: Long)

    @Query(
        """
        UPDATE feedback
        SET isHidden = 1,
            isModerated = 1,
            updatedAt = :updatedAt
        WHERE feedbackId = :feedbackId
        """
    )
    suspend fun hideComment(feedbackId: Long, updatedAt: Long)

    @Query(
        """
        UPDATE feedback
        SET isHidden = 0,
            updatedAt = :updatedAt
        WHERE feedbackId = :feedbackId
        """
    )
    suspend fun unhideComment(feedbackId: Long, updatedAt: Long)
}

