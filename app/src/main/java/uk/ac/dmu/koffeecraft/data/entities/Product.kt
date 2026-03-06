package uk.ac.dmu.koffeecraft.data.entities


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val productId: Long = 0,
    val name: String,
    val category: String, // "COFFEE" or "CAKE"
    val description: String,
    val price: Double,
    val isAvailable: Boolean = true
)