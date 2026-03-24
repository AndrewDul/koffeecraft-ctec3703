package uk.ac.dmu.koffeecraft.core.di

import android.content.Context
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.repository.AdminAccountsRepository
import uk.ac.dmu.koffeecraft.data.repository.AdminActivityRepository
import uk.ac.dmu.koffeecraft.data.repository.AdminCampaignRepository
import uk.ac.dmu.koffeecraft.data.repository.AdminFeedbackRepository
import uk.ac.dmu.koffeecraft.data.repository.AdminHomeRepository
import uk.ac.dmu.koffeecraft.data.repository.AdminInboxRepository
import uk.ac.dmu.koffeecraft.data.repository.AdminMenuRepository
import uk.ac.dmu.koffeecraft.data.repository.AdminNotificationsRepository
import uk.ac.dmu.koffeecraft.data.repository.AdminOrdersRepository
import uk.ac.dmu.koffeecraft.data.repository.AdminSettingsRepository
import uk.ac.dmu.koffeecraft.data.repository.AuthRepository
import uk.ac.dmu.koffeecraft.data.repository.AuthSessionRepository
import uk.ac.dmu.koffeecraft.data.repository.CartPersistenceRepository
import uk.ac.dmu.koffeecraft.data.repository.CartRepository
import uk.ac.dmu.koffeecraft.data.repository.CheckoutRepository
import uk.ac.dmu.koffeecraft.data.repository.CustomerFavouritesRepository
import uk.ac.dmu.koffeecraft.data.repository.CustomerHomeRepository
import uk.ac.dmu.koffeecraft.data.repository.CustomerInboxRepository
import uk.ac.dmu.koffeecraft.data.repository.CustomerNotificationsRepository
import uk.ac.dmu.koffeecraft.data.repository.CustomerOrdersRepository
import uk.ac.dmu.koffeecraft.data.repository.CustomerPaymentMethodsRepository
import uk.ac.dmu.koffeecraft.data.repository.CustomerRewardsRepository
import uk.ac.dmu.koffeecraft.data.repository.CustomerSettingsRepository
import uk.ac.dmu.koffeecraft.data.repository.FeedbackRepository
import uk.ac.dmu.koffeecraft.data.repository.MainActivityRepository
import uk.ac.dmu.koffeecraft.data.repository.MenuRepository
import uk.ac.dmu.koffeecraft.data.repository.OnboardingRepository
import uk.ac.dmu.koffeecraft.data.repository.OrderRepository
import uk.ac.dmu.koffeecraft.data.repository.ProductCustomizationRepository
import uk.ac.dmu.koffeecraft.data.repository.RewardProductPickerRepository
import uk.ac.dmu.koffeecraft.data.session.SessionRepository

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: KoffeeCraftDatabase by lazy {
        KoffeeCraftDatabase.getInstance(appContext)
    }

    val sessionRepository: SessionRepository by lazy {
        SessionRepository()
    }

    val cartRepository: CartRepository by lazy {
        CartRepository(
            context = appContext,
            sessionRepository = sessionRepository,
            db = database
        )
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(database)
    }

    val orderRepository: OrderRepository by lazy {
        OrderRepository(database)
    }

    val checkoutRepository: CheckoutRepository by lazy {
        CheckoutRepository(database)
    }

    val customerOrdersRepository: CustomerOrdersRepository by lazy {
        CustomerOrdersRepository(
            context = appContext,
            db = database,
            cartRepository = cartRepository
        )
    }

    val customerSettingsRepository: CustomerSettingsRepository by lazy {
        CustomerSettingsRepository(
            context = appContext,
            db = database,
            sessionRepository = sessionRepository,
            cartRepository = cartRepository
        )
    }

    val customerPaymentMethodsRepository: CustomerPaymentMethodsRepository by lazy {
        CustomerPaymentMethodsRepository(database)
    }

    val customerRewardsRepository: CustomerRewardsRepository by lazy {
        CustomerRewardsRepository(
            db = database,
            cartRepository = cartRepository
        )
    }

    val customerNotificationsRepository: CustomerNotificationsRepository by lazy {
        CustomerNotificationsRepository(database)
    }

    val customerInboxRepository: CustomerInboxRepository by lazy {
        CustomerInboxRepository(database)
    }

    val onboardingRepository: OnboardingRepository by lazy {
        OnboardingRepository(
            context = appContext,
            db = database,
            sessionRepository = sessionRepository
        )
    }

    val adminAccountsRepository: AdminAccountsRepository by lazy {
        AdminAccountsRepository(
            context = appContext,
            db = database,
            sessionRepository = sessionRepository,
            cartRepository = cartRepository
        )
    }

    val adminCampaignRepository: AdminCampaignRepository by lazy {
        AdminCampaignRepository(database)
    }

    val adminOrdersRepository: AdminOrdersRepository by lazy {
        AdminOrdersRepository(
            context = appContext,
            db = database
        )
    }

    val adminMenuRepository: AdminMenuRepository by lazy {
        AdminMenuRepository(database)
    }

    val adminHomeRepository: AdminHomeRepository by lazy {
        AdminHomeRepository(database)
    }

    val adminInboxRepository: AdminInboxRepository by lazy {
        AdminInboxRepository(database)
    }

    val adminFeedbackRepository: AdminFeedbackRepository by lazy {
        AdminFeedbackRepository(database)
    }

    val adminNotificationsRepository: AdminNotificationsRepository by lazy {
        AdminNotificationsRepository(
            context = appContext,
            db = database
        )
    }

    val adminSettingsRepository: AdminSettingsRepository by lazy {
        AdminSettingsRepository(
            context = appContext,
            db = database,
            sessionRepository = sessionRepository,
            cartRepository = cartRepository
        )
    }

    val customerHomeRepository: CustomerHomeRepository by lazy {
        CustomerHomeRepository(database)
    }

    val customerFavouritesRepository: CustomerFavouritesRepository by lazy {
        CustomerFavouritesRepository(
            db = database,
            cartRepository = cartRepository
        )
    }

    val rewardProductPickerRepository: RewardProductPickerRepository by lazy {
        RewardProductPickerRepository(database)
    }

    val productCustomizationRepository: ProductCustomizationRepository by lazy {
        ProductCustomizationRepository(
            db = database,
            cartRepository = cartRepository
        )
    }

    val feedbackRepository: FeedbackRepository by lazy {
        FeedbackRepository(database)
    }

    val menuRepository: MenuRepository by lazy {
        MenuRepository(
            productDao = database.productDao(),
            favouriteDao = database.favouriteDao(),
            productOptionDao = database.productOptionDao(),
            addOnDao = database.addOnDao(),
            allergenDao = database.allergenDao(),
            customerFavouritePresetDao = database.customerFavouritePresetDao(),
            cartRepository = cartRepository
        )
    }

    val cartPersistenceRepository: CartPersistenceRepository by lazy {
        CartPersistenceRepository(
            context = appContext,
            db = database
        )
    }

    val mainActivityRepository: MainActivityRepository by lazy {
        MainActivityRepository(
            context = appContext,
            db = database,
            cartPersistenceRepository = cartPersistenceRepository,
            sessionRepository = sessionRepository,
            cartRepository = cartRepository
        )
    }

    val adminActivityRepository: AdminActivityRepository by lazy {
        AdminActivityRepository(database)
    }

    val authSessionRepository: AuthSessionRepository by lazy {
        AuthSessionRepository(
            context = appContext,
            authRepository = authRepository,
            cartPersistenceRepository = cartPersistenceRepository,
            sessionRepository = sessionRepository,
            cartRepository = cartRepository
        )
    }
}