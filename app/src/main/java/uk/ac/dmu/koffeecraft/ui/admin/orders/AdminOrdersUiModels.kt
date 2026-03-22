package uk.ac.dmu.koffeecraft.ui.admin.orders

data class AdminOrderDetailsUi(
    val orderId: Long,
    val customerId: Long,
    val customerName: String,
    val customerEmail: String,
    val promoEligible: Boolean,
    val paymentType: String,
    val totalAmount: Double,
    val createdAt: Long,
    val status: String,
    val feedbackWritten: Boolean,
    val hasCraftedItems: Boolean,
    val hasRewardItems: Boolean,
    val items: List<AdminOrderLineUi>
)

data class AdminOrderLineUi(
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

    val isReward: Boolean
        get() = unitPrice <= 0.0
}