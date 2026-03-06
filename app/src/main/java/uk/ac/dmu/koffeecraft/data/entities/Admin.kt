package uk.ac.dmu.koffeecraft.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "admins",
    indices = [Index(value = ["email"], unique = true)]
)
data class Admin(
    @PrimaryKey(autoGenerate = true) val adminId: Long = 0,
    val email: String,
    val passwordHash: String,
    val passwordSalt: String,
    val createdAt: Long = System.currentTimeMillis()
)