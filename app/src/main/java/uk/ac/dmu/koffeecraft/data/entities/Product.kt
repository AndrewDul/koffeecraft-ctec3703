package uk.ac.dmu.koffeecraft.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val productId: Long = 0,
    val name: String,

    @ColumnInfo(name = "category")
    val productFamily: String, // "COFFEE", "CAKE", "MERCH"

    val description: String,
    val price: Double,

    @ColumnInfo(name = "isAvailable")
    val isActive: Boolean = true,

    val isNew: Boolean = false,
    val imageKey: String? = null,
    val customImagePath: String? = null,

    @ColumnInfo(defaultValue = "0")
    val rewardEnabled: Boolean = false
) {
    val category: String
        get() = productFamily

    val isCoffee: Boolean
        get() = productFamily.equals("COFFEE", ignoreCase = true)

    val isCake: Boolean
        get() = productFamily.equals("CAKE", ignoreCase = true)

    val isMerch: Boolean
        get() = productFamily.equals("MERCH", ignoreCase = true)

    val familyLabel: String
        get() = when {
            isCoffee -> "Coffee"
            isCake -> "Cake"
            isMerch -> "Merch"
            else -> productFamily.replaceFirstChar { it.uppercase() }
        }

    val listingLabel: String
        get() = when {
            isMerch && rewardEnabled -> "Reward item"
            rewardEnabled -> "Menu + rewards"
            else -> "Menu only"
        }
}