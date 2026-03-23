package uk.ac.dmu.koffeecraft.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "product_options",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["productId"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["productId"]),
        Index(value = ["productId", "optionName"], unique = true)
    ]
)
data class ProductOption(
    @PrimaryKey(autoGenerate = true) val optionId: Long = 0,
    val productId: Long,
    val optionName: String,
    val displayLabel: String,
    val sizeValue: Int,
    val sizeUnit: String,
    val extraPrice: Double,
    val estimatedCalories: Int,
    val isDefault: Boolean = false
)