package uk.ac.dmu.koffeecraft.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import uk.ac.dmu.koffeecraft.data.entities.OrderItem

@Dao
interface OrderItemDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<OrderItem>)
}