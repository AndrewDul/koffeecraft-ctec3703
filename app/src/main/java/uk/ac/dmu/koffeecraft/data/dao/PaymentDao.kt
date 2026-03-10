package uk.ac.dmu.koffeecraft.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import uk.ac.dmu.koffeecraft.data.entities.Payment

@Dao
interface PaymentDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(payment: Payment): Long
}