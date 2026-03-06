package uk.ac.dmu.koffeecraft.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payments",
    indices = [Index(value = ["orderId"])]
)
data class Payment(
    @PrimaryKey(autoGenerate = true) val paymentId: Long = 0,
    val orderId: Long,
    val paymentType: String, // "CARD", "CASH"
    val amount: Double,
    val paymentDate: Long = System.currentTimeMillis()
)