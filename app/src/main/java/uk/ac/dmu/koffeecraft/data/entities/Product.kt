package uk.ac.dmu.koffeecraft.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val productId: Long = 0,
    val name: String,
    val category: String, // "COFFEE" or "CAKE"
    val description: String,
    val price: Double,

    // I map this Kotlin property to the existing DB column name "isAvailable".
    @ColumnInfo(name = "isAvailable")
    val isActive: Boolean = true,

    // I use this flag to drive the customer "NEW" carousel and admin NEW badge.
    val isNew: Boolean = false,

    // I store a drawable resource key here (e.g., "p_latte"). Null means "use placeholder".
    val imageKey: String? = null
)