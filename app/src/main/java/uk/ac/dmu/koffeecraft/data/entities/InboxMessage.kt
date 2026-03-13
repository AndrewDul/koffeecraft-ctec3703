package uk.ac.dmu.koffeecraft.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inbox_messages",
    indices = [
        Index(value = ["recipientCustomerId"]),
        Index(value = ["isRead"]),
        Index(value = ["createdAt"])
    ]
)
data class InboxMessage(
    @PrimaryKey(autoGenerate = true) val inboxMessageId: Long = 0,
    val recipientCustomerId: Long,
    val title: String,
    val body: String,
    val deliveryType: String, // "BROADCAST", "DIRECT", "BIRTHDAY"
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)