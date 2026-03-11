package uk.ac.dmu.koffeecraft.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "feedback",
    indices = [
        Index(value = ["orderId"], unique = true),
        Index(value = ["customerId"])
    ]
)
data class Feedback(
    @PrimaryKey(autoGenerate = true) val feedbackId: Long = 0,
    val orderId: Long,
    val customerId: Long,
    val rating: Int, // 1..5
    val comment: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)