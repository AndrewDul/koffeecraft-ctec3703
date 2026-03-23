package uk.ac.dmu.koffeecraft.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import uk.ac.dmu.koffeecraft.data.entities.Admin
import uk.ac.dmu.koffeecraft.data.querymodel.AdminAccountTarget
@Dao
interface AdminDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(admin: Admin): Long

    @Query("SELECT * FROM admins WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): Admin?

    @Query("SELECT * FROM admins WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): Admin?

    @Query("SELECT * FROM admins WHERE adminId = :adminId LIMIT 1")
    suspend fun getById(adminId: Long): Admin?

    @Query(
        """
        SELECT
            adminId AS adminId,
            fullName AS fullName,
            email AS email,
            phone AS phone,
            username AS username,
            isActive AS isActive,
            createdAt AS createdAt
        FROM admins
        WHERE adminId = :adminId
        LIMIT 1
        """
    )
    suspend fun getAccountTargetByAdminId(adminId: Long): AdminAccountTarget?

    @Query(
        """
        SELECT
            adminId AS adminId,
            fullName AS fullName,
            email AS email,
            phone AS phone,
            username AS username,
            isActive AS isActive,
            createdAt AS createdAt
        FROM admins
        WHERE email = :email
        LIMIT 1
        """
    )
    suspend fun getAccountTargetByEmail(email: String): AdminAccountTarget?

    @Query(
        """
        SELECT
            adminId AS adminId,
            fullName AS fullName,
            email AS email,
            phone AS phone,
            username AS username,
            isActive AS isActive,
            createdAt AS createdAt
        FROM admins
        WHERE username = :username
        LIMIT 1
        """
    )
    suspend fun getAccountTargetByUsername(username: String): AdminAccountTarget?

    @Query(
        """
        UPDATE admins
        SET isActive = :isActive
        WHERE adminId = :adminId
        """
    )
    suspend fun updateActiveStatus(adminId: Long, isActive: Boolean)

    @Query(
        """
        UPDATE admins
        SET passwordHash = :passwordHash,
            passwordSalt = :passwordSalt
        WHERE adminId = :adminId
        """
    )
    suspend fun updatePassword(adminId: Long, passwordHash: String, passwordSalt: String)

    @Query("SELECT COUNT(*) FROM admins")
    suspend fun countAdmins(): Int

    @Query("SELECT COUNT(*) FROM admins WHERE isActive = 1")
    suspend fun countActiveAdmins(): Int
}

