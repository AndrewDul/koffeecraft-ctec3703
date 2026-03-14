package uk.ac.dmu.koffeecraft.data.entities

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "customer_favourite_preset_add_on_cross_ref",
    primaryKeys = ["presetId", "addOnId"],
    indices = [
        Index(value = ["presetId"]),
        Index(value = ["addOnId"])
    ]
)
data class CustomerFavouritePresetAddOnCrossRef(
    val presetId: Long,
    val addOnId: Long
)