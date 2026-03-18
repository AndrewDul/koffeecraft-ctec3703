package uk.ac.dmu.koffeecraft.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "favourites",
    primaryKeys = ["customerId", "productId"],
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["customerId"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["productId"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["productId"])
    ]
)
data class Favourite(
    val customerId: Long,
    val productId: Long,
    val createdAt: Long = System.currentTimeMillis()
)