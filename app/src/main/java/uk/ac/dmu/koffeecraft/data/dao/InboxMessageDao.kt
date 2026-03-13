package uk.ac.dmu.koffeecraft.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import uk.ac.dmu.koffeecraft.data.entities.InboxMessage

@Dao
interface InboxMessageDao {

    @Query("""
        SELECT * FROM inbox_messages
        WHERE recipientCustomerId = :customerId
        ORDER BY isRead ASC, createdAt DESC
    """)
    fun observeInboxForCustomer(customerId: Long): Flow<List<InboxMessage>>

    @Query("""
        SELECT COUNT(*) FROM inbox_messages
        WHERE recipientCustomerId = :customerId
          AND isRead = 0
    """)
    fun observeUnreadCountForCustomer(customerId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: InboxMessage): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<InboxMessage>)

    @Query("""
        UPDATE inbox_messages
        SET isRead = 1
        WHERE inboxMessageId = :inboxMessageId
    """)
    suspend fun markAsRead(inboxMessageId: Long)

    @Query("""
        UPDATE inbox_messages
        SET isRead = 1
        WHERE recipientCustomerId = :customerId
    """)
    suspend fun markAllAsRead(customerId: Long)

    @Query("""
        DELETE FROM inbox_messages
        WHERE inboxMessageId = :inboxMessageId
    """)
    suspend fun deleteById(inboxMessageId: Long)
}