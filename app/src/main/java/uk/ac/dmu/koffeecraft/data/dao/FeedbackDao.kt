package uk.ac.dmu.koffeecraft.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import uk.ac.dmu.koffeecraft.data.entities.Feedback

@Dao
interface FeedbackDao {

    @Query("SELECT * FROM feedback WHERE orderId = :orderId LIMIT 1")
    suspend fun getByOrderId(orderId: Long): Feedback?

    @Query("SELECT * FROM feedback WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun observeByCustomer(customerId: Long): Flow<List<Feedback>>

    // Unique index on orderId means REPLACE will overwrite existing feedback for that order
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(feedback: Feedback)
}