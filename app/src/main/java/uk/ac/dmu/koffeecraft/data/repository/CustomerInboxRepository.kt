package uk.ac.dmu.koffeecraft.data.repository

import kotlinx.coroutines.flow.Flow
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.InboxMessage

class CustomerInboxRepository(
    private val db: KoffeeCraftDatabase
) {

    fun observeInbox(customerId: Long): Flow<List<InboxMessage>> {
        return db.inboxMessageDao().observeInboxForCustomer(customerId)
    }

    suspend fun deleteMessage(inboxMessageId: Long) {
        db.inboxMessageDao().deleteById(inboxMessageId)
    }

    suspend fun markAsRead(inboxMessageId: Long) {
        db.inboxMessageDao().markAsRead(inboxMessageId)
    }
}