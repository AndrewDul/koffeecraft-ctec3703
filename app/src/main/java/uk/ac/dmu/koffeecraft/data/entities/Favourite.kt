package uk.ac.dmu.koffeecraft.data.entities

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "favourites",
    primaryKeys = ["customerId", "productId"],
    indices = [Index(value = ["customerId"]), Index(value = ["productId"])]
)
data class Favourite(
    val customerId: Long,
    val productId: Long,
    val createdAt: Long = System.currentTimeMillis()
)