package uk.ac.dmu.koffeecraft.data.querymodel

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

data class AdminFeedbackOverview(
    val overallAverage: Double,
    val coffeeAverage: Double,
    val cakeAverage: Double,
    val totalReviews: Int,
    val reviewsWithComments: Int,
    val hiddenComments: Int
)

data class AdminFeedbackRatingBreakdown(
    val rating: Int,
    val reviewCount: Int
)

data class ProductRatingInsight(
    val productId: Long,
    val productName: String,
    val productFamily: String,
    val imageKey: String?,
    val customImagePath: String?,
    val averageRating: Double,
    val ratingCount: Int
)

data class ProductCommentInsight(
    val productId: Long,
    val productName: String,
    val productFamily: String,
    val imageKey: String?,
    val customImagePath: String?,
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
    val customImagePath: String?,
    val averageRating: Double,
    val ratingCount: Int
)