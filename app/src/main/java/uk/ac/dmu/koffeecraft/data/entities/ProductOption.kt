package uk.ac.dmu.koffeecraft.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "product_options",
    indices = [Index(value = ["productId"]), Index(value = ["productId", "optionName"], unique = true)]
)
data class ProductOption(
    @PrimaryKey(autoGenerate = true) val optionId: Long = 0,
    val productId: Long,
    val optionName: String,      // "SMALL", "MEDIUM", "LARGE", "STANDARD"
    val displayLabel: String,    // "Small", "Medium", "Large", "Standard slice"
    val sizeValue: Int,          // ml or g
    val sizeUnit: String,        // "ML" or "G"
    val extraPrice: Double,
    val estimatedCalories: Int,
    val isDefault: Boolean = false
)