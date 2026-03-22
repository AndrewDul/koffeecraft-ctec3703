package uk.ac.dmu.koffeecraft.core.di

import android.content.Context
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.repository.AdminAccountsRepository
import uk.ac.dmu.koffeecraft.data.repository.AdminCampaignRepository
import uk.ac.dmu.koffeecraft.data.repository.AdminHomeRepository
import uk.ac.dmu.koffeecraft.data.repository.AdminMenuRepository
import uk.ac.dmu.koffeecraft.data.repository.AdminOrdersRepository
import uk.ac.dmu.koffeecraft.data.repository.AuthRepository
import uk.ac.dmu.koffeecraft.data.repository.CheckoutRepository
import uk.ac.dmu.koffeecraft.data.repository.CustomerOrdersRepository
import uk.ac.dmu.koffeecraft.data.repository.CustomerPaymentMethodsRepository
import uk.ac.dmu.koffeecraft.data.repository.CustomerSettingsRepository
import uk.ac.dmu.koffeecraft.data.repository.MenuRepository
import uk.ac.dmu.koffeecraft.data.repository.OrderRepository
import uk.ac.dmu.koffeecraft.data.repository.CustomerRewardsRepository
import uk.ac.dmu.koffeecraft.data.repository.OnboardingRepository
import uk.ac.dmu.koffeecraft.data.repository.CustomerNotificationsRepository
import uk.ac.dmu.koffeecraft.data.repository.CustomerInboxRepository


class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: KoffeeCraftDatabase by lazy {
        KoffeeCraftDatabase.getInstance(appContext)
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
            db = database
        )
    }

    val customerSettingsRepository: CustomerSettingsRepository by lazy {
        CustomerSettingsRepository(
            context = appContext,
            db = database
        )
    }

    val customerPaymentMethodsRepository: CustomerPaymentMethodsRepository by lazy {
        CustomerPaymentMethodsRepository(database)
    }

    val customerRewardsRepository: CustomerRewardsRepository by lazy {
        CustomerRewardsRepository(database)
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
            db = database
        )
    }

    val adminAccountsRepository: AdminAccountsRepository by lazy {
        AdminAccountsRepository(
            context = appContext,
            db = database
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

    val menuRepository: MenuRepository by lazy {
        MenuRepository(
            productDao = database.productDao(),
            favouriteDao = database.favouriteDao()
        )
    }
}