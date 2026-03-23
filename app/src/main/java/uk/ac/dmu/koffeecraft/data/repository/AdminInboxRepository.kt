package uk.ac.dmu.koffeecraft.data.repository


import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.InboxMessage
import uk.ac.dmu.koffeecraft.data.querymodel.CustomerInboxTarget
sealed interface AdminInboxSendResult {
    data class Success(val message: String) : AdminInboxSendResult
    data class Error(val message: String) : AdminInboxSendResult
}

class AdminInboxRepository(
    private val db: KoffeeCraftDatabase
) {

    suspend fun findTarget(
        mode: AdminInboxTargetMode,
        rawQuery: String
    ): CustomerInboxTarget? {
        return when (mode) {
            AdminInboxTargetMode.ORDER_NUMBER -> {
                val orderId = rawQuery.toLongOrNull() ?: return null
                db.customerDao().getInboxTargetByOrderId(orderId)
            }

            AdminInboxTargetMode.CUSTOMER_ID -> {
                val customerId = rawQuery.toLongOrNull() ?: return null
                db.customerDao().getInboxTargetByCustomerId(customerId)
            }
        }
    }

    suspend fun sendDirectMessage(
        target: CustomerInboxTarget,
        title: String,
        body: String,
        messageType: AdminInboxMessageType
    ): AdminInboxSendResult {
        if (title.isBlank()) {
            return AdminInboxSendResult.Error("Enter a message title.")
        }

        if (body.isBlank()) {
            return AdminInboxSendResult.Error("Write a message first.")
        }

        val resolvedBody = body
            .replace("[FIRST_NAME]", target.firstName.ifBlank { "Customer" })
            .replace("[LAST_NAME]", target.lastName.ifBlank { "" })

        val deliveryType = when (messageType) {
            AdminInboxMessageType.IMPORTANT -> "IMPORTANT_DIRECT"
            AdminInboxMessageType.SERVICE -> "SERVICE_DIRECT"
            AdminInboxMessageType.CUSTOM -> "CUSTOM_DIRECT"
        }

        db.inboxMessageDao().insertAll(
            listOf(
                InboxMessage(
                    recipientCustomerId = target.customerId,
                    title = title,
                    body = resolvedBody,
                    deliveryType = deliveryType
                )
            )
        )

        return AdminInboxSendResult.Success("Direct message sent successfully.")
    }
}