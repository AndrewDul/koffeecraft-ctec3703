package uk.ac.dmu.koffeecraft.data.dto

data class AdminOrderRow(
    val orderId: Long,
    val customerId: Long,
    val customerFirstName: String,
    val customerLastName: String,
    val customerEmail: String,
    val status: String,
    val totalAmount: Double,
    val createdAt: Long,
    val itemCount: Int,
    val craftedLineCount: Int,
    val rewardLineCount: Int
) {
    val customerDisplayName: String
        get() = "$customerFirstName $customerLastName".trim()
}