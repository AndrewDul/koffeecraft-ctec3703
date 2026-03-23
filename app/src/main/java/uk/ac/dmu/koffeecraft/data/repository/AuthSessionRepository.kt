package uk.ac.dmu.koffeecraft.data.repository

import android.content.Context
import uk.ac.dmu.koffeecraft.data.cart.CartManager
import uk.ac.dmu.koffeecraft.data.session.RememberedSessionStore
import uk.ac.dmu.koffeecraft.data.session.SessionRepository

sealed interface AuthSessionLoginResult {
    data class Customer(val customerId: Long) : AuthSessionLoginResult
    data class Admin(val adminId: Long) : AuthSessionLoginResult
    data class Error(val message: String) : AuthSessionLoginResult
}

sealed interface AuthSessionRegisterResult {
    data class Success(val customerId: Long) : AuthSessionRegisterResult
    data class Error(val message: String) : AuthSessionRegisterResult
}

class AuthSessionRepository(
    context: Context,
    private val authRepository: AuthRepository,
    private val cartPersistenceRepository: CartPersistenceRepository,
    private val sessionRepository: SessionRepository
) {

    private val appContext = context.applicationContext

    init {
        CartManager.attachContext(appContext)
    }

    suspend fun login(
        email: String,
        password: CharArray
    ): AuthSessionLoginResult {
        return when (val result = authRepository.login(email, password)) {
            is AuthRepository.LoginResult.Error -> {
                AuthSessionLoginResult.Error(result.message)
            }

            is AuthRepository.LoginResult.Success -> {
                when (result.role) {
                    AuthRepository.UserRole.ADMIN -> {
                        sessionRepository.setAdmin(result.userId)
                        RememberedSessionStore.saveAdminSession(appContext, result.userId)
                        CartManager.clearInMemoryOnly()

                        AuthSessionLoginResult.Admin(result.userId)
                    }

                    AuthRepository.UserRole.CUSTOMER -> {
                        sessionRepository.setCustomer(result.userId)
                        RememberedSessionStore.saveCustomerSession(
                            context = appContext,
                            customerId = result.userId,
                            onboardingPending = false
                        )

                        val restoredItems =
                            cartPersistenceRepository.restoreCartForCustomer(result.userId)

                        CartManager.replaceAll(restoredItems, persist = false)

                        AuthSessionLoginResult.Customer(result.userId)
                    }
                }
            }
        }
    }

    suspend fun registerCustomer(
        firstName: String,
        lastName: String,
        country: String,
        dateOfBirth: String,
        email: String,
        password: CharArray,
        marketingInboxConsent: Boolean,
        termsAccepted: Boolean,
        privacyAccepted: Boolean
    ): AuthSessionRegisterResult {
        return when (
            val result = authRepository.registerCustomer(
                firstName = firstName,
                lastName = lastName,
                country = country,
                dateOfBirth = dateOfBirth,
                email = email,
                password = password,
                marketingInboxConsent = marketingInboxConsent,
                termsAccepted = termsAccepted,
                privacyAccepted = privacyAccepted
            )
        ) {
            is AuthRepository.RegisterResult.Error -> {
                AuthSessionRegisterResult.Error(result.message)
            }

            is AuthRepository.RegisterResult.Success -> {
                sessionRepository.setCustomer(result.customerId)
                RememberedSessionStore.saveCustomerSession(
                    context = appContext,
                    customerId = result.customerId,
                    onboardingPending = true
                )
                CartManager.clearInMemoryOnly()

                AuthSessionRegisterResult.Success(result.customerId)
            }
        }
    }
}