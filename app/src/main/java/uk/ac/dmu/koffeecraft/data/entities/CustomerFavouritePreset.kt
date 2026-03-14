package uk.ac.dmu.koffeecraft.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customer_favourite_presets",
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