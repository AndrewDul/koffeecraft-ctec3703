package uk.ac.dmu.koffeecraft.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "customer_favourite_preset_add_on_cross_ref",
    primaryKeys = ["presetId", "addOnId"],
    foreignKeys = [
        ForeignKey(
            entity = CustomerFavouritePreset::class,
            parentColumns = ["presetId"],
            childColumns = ["presetId"],
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
        Index(value = ["presetId"]),
        Index(value = ["addOnId"])
    ]
)
data class CustomerFavouritePresetAddOnCrossRef(
    val presetId: Long,
    val addOnId: Long
)