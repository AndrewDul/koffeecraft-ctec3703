package uk.ac.dmu.koffeecraft.data.entities

import androidx.room.Entity

@Entity(
    tableName = "product_add_on_cross_ref",
    primaryKeys = ["productId", "addOnId"]
)
data class ProductAddOnCrossRef(
    val productId: Long,
    val addOnId: Long
)

@Entity(
    tableName = "product_allergen_cross_ref",
    primaryKeys = ["productId", "allergenId"]
)
data class ProductAllergenCrossRef(
    val productId: Long,
    val allergenId: Long
)

@Entity(
    tableName = "add_on_allergen_cross_ref",
    primaryKeys = ["addOnId", "allergenId"]
)
data class AddOnAllergenCrossRef(
    val addOnId: Long,
    val allergenId: Long
)