package uk.ac.dmu.koffeecraft.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import uk.ac.dmu.koffeecraft.data.entities.Admin

@Dao
interface AdminDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(admin: Admin): Long

    @Query("SELECT * FROM admins WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): Admin?

    @Query("SELECT COUNT(*) FROM admins")
    suspend fun countAdmins(): Int
}