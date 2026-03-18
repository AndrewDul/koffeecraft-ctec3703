package uk.ac.dmu.koffeecraft.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = Order::class,
            parentColumns = ["orderId"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["orderId"]),
        Index(value = ["productId"])
    ]
)
data class OrderItem(
    @PrimaryKey(autoGenerate = true) val orderItemId: Long = 0,
    val orderId: Long,
    val productId: Long,
    val quantity: Int,
    val unitPrice: Double,
    val selectedOptionLabel: String? = null,
    val selectedOptionSizeValue: Int? = null,
    val selectedOptionSizeUnit: String? = null,
    val selectedAddOnsSummary: String? = null,
    val estimatedCalories: Int? = null,
    val productNameSnapshot: String? = null,
    val productDescriptionSnapshot: String? = null
)