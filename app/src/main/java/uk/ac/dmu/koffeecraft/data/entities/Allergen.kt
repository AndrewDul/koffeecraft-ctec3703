package uk.ac.dmu.koffeecraft.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "allergens",
    indices = [Index(value = ["name"], unique = true)]
)
data class Allergen(
    @PrimaryKey(autoGenerate = true) val allergenId: Long = 0,
    val name: String
)