package uk.ac.dmu.koffeecraft.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import uk.ac.dmu.koffeecraft.data.cart.CartManager
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.InboxMessage
import uk.ac.dmu.koffeecraft.data.session.RememberedSessionStore
import uk.ac.dmu.koffeecraft.data.session.SessionRepository
import uk.ac.dmu.koffeecraft.util.notifications.NotificationHelper

data class MainActivityBadgeData(
    val cartCount: Int,
    val inboxUnreadCount: Int,
    val notificationUnreadCount: Int,
    val unreadPromoMessages: List<InboxMessage>
)

sealed interface MainBootstrapResult {
    data object None : MainBootstrapResult
    data object LaunchAdmin : MainBootstrapResult
    data class LaunchCustomer(
        val customerId: Long,
        val onboardingPending: Boolean
    ) : MainBootstrapResult
}

class MainActivityRepository(
    context: Context,
    private val db: KoffeeCraftDatabase,
    private val cartPersistenceRepository: CartPersistenceRepository,
    private val sessionRepository: SessionRepository
) {

    private val appContext = context.applicationContext

    suspend fun bootstrapRememberedSession(): MainBootstrapResult {
        val rememberedSession = RememberedSessionStore.restoreIntoMemory(appContext)
            ?: return MainBootstrapResult.None

        return when (rememberedSession.role) {
            RememberedSessionStore.Role.ADMIN -> {
                val admin = db.adminDao().getById(rememberedSession.userId)

                if (admin == null || !admin.isActive) {
                    sessionRepository.clear()
                    RememberedSessionStore.clear(appContext)
                    CartManager.clearInMemoryOnly()
                    MainBootstrapResult.None
                } else {
                    sessionRepository.setAdmin(admin.adminId)
                    CartManager.clearInMemoryOnly()
                    MainBootstrapResult.LaunchAdmin
                }
            }

            RememberedSessionStore.Role.CUSTOMER -> {
                val customer = db.customerDao().getById(rememberedSession.userId)

                if (customer == null || !customer.isActive) {
                    sessionRepository.clear()
                    RememberedSessionStore.clear(appContext)
                    CartManager.clearInMemoryOnly()
                    MainBootstrapResult.None
                } else {
                    sessionRepository.setCustomer(customer.customerId)

                    val restoredItems =
                        cartPersistenceRepository.restoreCartForCustomer(customer.customerId)
                    CartManager.replaceAll(restoredItems, persist = false)

                    MainBootstrapResult.LaunchCustomer(
                        customerId = customer.customerId,
                        onboardingPending = rememberedSession.onboardingPending
                    )
                }
            }
        }
    }

    fun currentCustomerId(): Long? = sessionRepository.currentCustomerId

    fun isAdminSession(): Boolean = sessionRepository.isAdmin

    fun observeCustomerBadges(customerId: Long): Flow<MainActivityBadgeData> {
        return combine(
            CartManager.itemCount,
            db.inboxMessageDao().observeUnreadCountForCustomer(customerId),
            db.notificationDao().observeUnreadCustomerCount(customerId),
            db.inboxMessageDao().observeInboxForCustomer(customerId)
        ) { cartCount, inboxUnreadCount, notificationUnreadCount, inboxItems ->
            MainActivityBadgeData(
                cartCount = cartCount,
                inboxUnreadCount = inboxUnreadCount,
                notificationUnreadCount = notificationUnreadCount,
                unreadPromoMessages = inboxItems.filter {
                    !it.isRead && it.deliveryType.startsWith("PROMO")
                }
            )
        }
    }

    suspend fun markCustomerOrderNotificationsAsRead(
        customerId: Long,
        orderId: Long
    ) {
        db.notificationDao().markCustomerOrderNotificationsAsRead(customerId, orderId)
    }

    fun deliverPromoNotifications(messages: List<InboxMessage>) {
        messages.forEach { message ->
            if (!NotificationHelper.wasPromoMessageDelivered(appContext, message.inboxMessageId)) {
                NotificationHelper.showPromoNotification(
                    context = appContext,
                    title = message.title,
                    message = buildPromoPreview(message.body),
                    notificationId = 600000 + (message.inboxMessageId % 50000).toInt(),
                    inboxMessageId = message.inboxMessageId
                )

                NotificationHelper.markPromoMessageDelivered(
                    context = appContext,
                    inboxMessageId = message.inboxMessageId
                )
            }
        }
    }

    private fun buildPromoPreview(body: String): String {
        val singleLine = body.replace("\n", " ").trim()
        return if (singleLine.length <= 100) {
            singleLine
        } else {
            singleLine.take(97).trimEnd() + "..."
        }
    }
}