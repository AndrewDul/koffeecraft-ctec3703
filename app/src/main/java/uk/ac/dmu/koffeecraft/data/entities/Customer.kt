package uk.ac.dmu.koffeecraft.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customers",
    indices = [Index(value = ["email"], unique = true)]
)
data class Customer(
    @PrimaryKey(autoGenerate = true) val customerId: Long = 0,
    val firstName: String,
    val lastName: String,
    val country: String = "",
    val email: String,
    val passwordHash: String,
    val passwordSalt: String,
    val dateOfBirth: String? = null,
    val marketingInboxConsent: Boolean = false,
    val termsAccepted: Boolean = false,
    val privacyAccepted: Boolean = false,
    val beansBalance: Int = 0,
    val nextBeansBonusThreshold: Int = 10, // legacy field kept for older installs
    val beansBoosterProgress: Int = 0,
    val pendingBeansBoosters: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)