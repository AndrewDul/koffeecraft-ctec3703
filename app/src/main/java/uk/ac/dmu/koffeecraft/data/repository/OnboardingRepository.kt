package uk.ac.dmu.koffeecraft.data.repository

import android.content.Context
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.AppNotification
import uk.ac.dmu.koffeecraft.data.entities.InboxMessage
import uk.ac.dmu.koffeecraft.data.session.RememberedSessionStore
import uk.ac.dmu.koffeecraft.data.session.SessionRepository

data class OnboardingInitialData(
    val marketingInboxConsent: Boolean
)

sealed interface OnboardingFinishResult {
    data object Success : OnboardingFinishResult
    data class Error(val message: String) : OnboardingFinishResult
}

class OnboardingRepository(
    private val context: Context,
    private val db: KoffeeCraftDatabase,
    private val sessionRepository: SessionRepository
) {

    private val appContext = context.applicationContext

    suspend fun loadInitialData(customerId: Long): OnboardingInitialData? {
        val customer = db.customerDao().getById(customerId) ?: return null
        return OnboardingInitialData(
            marketingInboxConsent = customer.marketingInboxConsent
        )
    }

    suspend fun finishOnboarding(
        customerId: Long,
        promoConsentChoice: Boolean
    ): OnboardingFinishResult {
        val customer = db.customerDao().getById(customerId)
            ?: return OnboardingFinishResult.Error("Customer account could not be found.")

        db.inboxMessageDao().insertAll(
            listOf(
                InboxMessage(
                    recipientCustomerId = customerId,
                    title = "Welcome to KoffeeCraft",
                    body = "Welcome to KoffeeCraft. Your account is ready, your rewards journey has started, and your Inbox will keep your coffee moments close.",
                    deliveryType = "WELCOME"
                )
            )
        )

        val finalConsent = customer.marketingInboxConsent || promoConsentChoice

        if (finalConsent) {
            db.customerDao().update(
                customer.copy(
                    marketingInboxConsent = true,
                    beansBalance = customer.beansBalance + 5
                )
            )

            db.notificationDao().insert(
                AppNotification(
                    recipientRole = "CUSTOMER",
                    recipientCustomerId = customerId,
                    title = "You received 5 beans",
                    message = "Thanks for enabling promotional messages. Your first 5 beans have been added to your account.",
                    notificationType = "WELCOME_BEANS",
                    orderId = null,
                    orderCreatedAt = null,
                    orderStatus = null,
                    isRead = false
                )
            )
        }

        sessionRepository.setCustomer(customerId)
        RememberedSessionStore.saveCustomerSession(
            context = appContext,
            customerId = customerId,
            onboardingPending = false
        )

        return OnboardingFinishResult.Success
    }
}