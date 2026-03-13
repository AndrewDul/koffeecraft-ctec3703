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
    val email: String,
    val passwordHash: String,
    val passwordSalt: String,
    val dateOfBirth: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)