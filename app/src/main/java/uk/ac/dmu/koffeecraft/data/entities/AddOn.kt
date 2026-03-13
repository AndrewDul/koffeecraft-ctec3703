package uk.ac.dmu.koffeecraft.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "add_ons",
    indices = [Index(value = ["category"]), Index(value = ["name", "category"], unique = true)]
)
data class AddOn(
    @PrimaryKey(autoGenerate = true) val addOnId: Long = 0,
    val name: String,
    val category: String, // "COFFEE" or "CAKE"
    val price: Double = 0.30,
    val estimatedCalories: Int = 0,
    val isActive: Boolean = true
)