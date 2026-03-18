package uk.ac.dmu.koffeecraft.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "feedback",
    foreignKeys = [
        ForeignKey(
            entity = OrderItem::class,
            parentColumns = ["orderItemId"],
            childColumns = ["orderItemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["customerId"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["orderItemId"], unique = true),
        Index(value = ["customerId"])
    ]
)
data class Feedback(
    @PrimaryKey(autoGenerate = true) val feedbackId: Long = 0,
    val orderItemId: Long,
    val customerId: Long,
    val rating: Int,
    val comment: String,
    val isHidden: Boolean = false,
    val isModerated: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)