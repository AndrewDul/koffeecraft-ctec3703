package uk.ac.dmu.koffeecraft.data.dto

data class AdminOrderRow(
    val orderId: Long,
    val customerId: Long,
    val customerEmail: String,
    val status: String,
    val totalAmount: Double,
    val createdAt: Long
)