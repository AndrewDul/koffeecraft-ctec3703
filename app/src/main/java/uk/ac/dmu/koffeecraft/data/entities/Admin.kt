package uk.ac.dmu.koffeecraft.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "admins",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["username"], unique = true)
    ]
)
data class Admin(
    @PrimaryKey(autoGenerate = true) val adminId: Long = 0,
    val fullName: String,
    val email: String,
    val phone: String,
    val username: String,
    val passwordHash: String,
    val passwordSalt: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)