package uk.ac.dmu.koffeecraft.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(
    tableName = "customer_payment_cards",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["customerId"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["customerId", "isDefault"]),
        Index(value = ["customerId", "createdAt"])
    ]
)
data class CustomerPaymentCard(
    @PrimaryKey(autoGenerate = true) val cardId: Long = 0,
    val customerId: Long,
    val nickname: String,
    val cardholderName: String,
    val brand: String,
    val maskedCardNumber: String,
    val last4: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    @ColumnInfo(defaultValue = "0")
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val expiryLabel: String
        get() = String.format(Locale.UK, "%02d/%02d", expiryMonth, expiryYear % 100)
}