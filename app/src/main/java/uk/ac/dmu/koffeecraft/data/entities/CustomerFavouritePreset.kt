package uk.ac.dmu.koffeecraft.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customer_favourite_presets",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["customerId"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["productId"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductOption::class,
            parentColumns = ["optionId"],
            childColumns = ["optionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["productId"]),
        Index(value = ["optionId"]),
        Index(value = ["createdAt"])
    ]
)
data class CustomerFavouritePreset(
    @PrimaryKey(autoGenerate = true) val presetId: Long = 0,
    val customerId: Long,
    val productId: Long,
    val optionId: Long,
    val totalPriceSnapshot: Double,
    val totalCaloriesSnapshot: Int,
    val createdAt: Long = System.currentTimeMillis()
)