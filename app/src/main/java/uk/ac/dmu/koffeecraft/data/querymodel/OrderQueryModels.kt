package uk.ac.dmu.koffeecraft.data.querymodel

import androidx.room.Embedded
import uk.ac.dmu.koffeecraft.data.entities.Product

data class ReorderItem(
    @Embedded val product: Product,
    val quantity: Int
)

data class OrderFeedbackItem(
    val orderItemId: Long,
    val orderId: Long,
    val productId: Long,
    val productName: String,
    val productDescription: String,
    val quantity: Int,
    val unitPrice: Double,
    val selectedOptionLabel: String?,
    val selectedOptionSizeValue: Int?,
    val selectedOptionSizeUnit: String?,
    val selectedAddOnsSummary: String?,
    val estimatedCalories: Int?,
    val feedbackId: Long?,
    val rating: Int?,
    val comment: String?
) {
    val isCrafted: Boolean
        get() = !selectedAddOnsSummary.isNullOrBlank() ||
                (
                        !selectedOptionLabel.isNullOrBlank() &&
                                selectedOptionSizeValue != null &&
                                !selectedOptionSizeUnit.isNullOrBlank()
                        )
}

data class OrderDisplayItem(
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val selectedOptionLabel: String?,
    val selectedOptionSizeValue: Int?,
    val selectedOptionSizeUnit: String?,
    val selectedAddOnsSummary: String?,
    val estimatedCalories: Int?
) {
    val isCrafted: Boolean
        get() = !selectedAddOnsSummary.isNullOrBlank() ||
                (
                        !selectedOptionLabel.isNullOrBlank() &&
                                selectedOptionSizeValue != null &&
                                !selectedOptionSizeUnit.isNullOrBlank()
                        )
}