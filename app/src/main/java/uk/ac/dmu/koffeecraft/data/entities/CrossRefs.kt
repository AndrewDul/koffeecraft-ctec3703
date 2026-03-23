package uk.ac.dmu.koffeecraft.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "product_add_on_cross_ref",
    primaryKeys = ["productId", "addOnId"],
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["productId"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AddOn::class,
            parentColumns = ["addOnId"],
            childColumns = ["addOnId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["addOnId"])
    ]
)
data class ProductAddOnCrossRef(
    val productId: Long,
    val addOnId: Long
)

@Entity(
    tableName = "product_allergen_cross_ref",
    primaryKeys = ["productId", "allergenId"],
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["productId"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Allergen::class,
            parentColumns = ["allergenId"],
            childColumns = ["allergenId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["allergenId"])
    ]
)
data class ProductAllergenCrossRef(
    val productId: Long,
    val allergenId: Long
)

@Entity(
    tableName = "add_on_allergen_cross_ref",
    primaryKeys = ["addOnId", "allergenId"],
    foreignKeys = [
        ForeignKey(
            entity = AddOn::class,
            parentColumns = ["addOnId"],
            childColumns = ["addOnId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Allergen::class,
            parentColumns = ["allergenId"],
            childColumns = ["allergenId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["allergenId"])
    ]
)
data class AddOnAllergenCrossRef(
    val addOnId: Long,
    val allergenId: Long
)