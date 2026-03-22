package uk.ac.dmu.koffeecraft.data.repository

import android.content.Context
import uk.ac.dmu.koffeecraft.data.cart.CartManager
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.Customer
import uk.ac.dmu.koffeecraft.data.session.RememberedSessionStore
import uk.ac.dmu.koffeecraft.data.session.SessionManager
import uk.ac.dmu.koffeecraft.data.settings.ThemeSettings
import uk.ac.dmu.koffeecraft.util.security.PasswordHasher
import uk.ac.dmu.koffeecraft.util.validation.EmailValidator
import uk.ac.dmu.koffeecraft.util.validation.PasswordRulesValidator

data class CustomerSettingsScreenData(
    val customerName: String?,
    val customerEmail: String?,
    val darkModeEnabled: Boolean
)

class CustomerSettingsRepository(
    context: Context,
    private val db: KoffeeCraftDatabase
) {

    private val appContext = context.applicationContext
    private val cleanupRepository = CustomerAccountCleanupRepository(appContext, db)

    suspend fun getCustomer(customerId: Long): Customer? {
        return db.customerDao().getById(customerId)
    }

    suspend fun loadScreenData(customerId: Long?): CustomerSettingsScreenData {
        val darkModeEnabled = ThemeSettings.isDarkModeEnabled(appContext)

        if (customerId == null) {
            return CustomerSettingsScreenData(
                customerName = null,
                customerEmail = null,
                darkModeEnabled = darkModeEnabled
            )
        }

        val customer = db.customerDao().getById(customerId)

        return CustomerSettingsScreenData(
            customerName = customer?.let { "${it.firstName} ${it.lastName}".trim() },
            customerEmail = customer?.email,
            darkModeEnabled = darkModeEnabled
        )
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        ThemeSettings.setDarkModeEnabled(appContext, enabled)
    }

    fun signOut() {
        CartManager.clearInMemoryOnly()
        SessionManager.clear()
        RememberedSessionStore.clear(appContext)
    }

    suspend fun updatePersonalInfo(
        customerId: Long,
        firstName: String,
        lastName: String,
        email: String
    ): SettingsActionResult {
        val customer = db.customerDao().getById(customerId)
            ?: return SettingsActionResult.Error("Customer account could not be found.")

        val cleanFirstName = firstName.trim()
        val cleanLastName = lastName.trim()
        val cleanEmail = email.trim().lowercase()

        if (cleanFirstName.isBlank() || cleanLastName.isBlank() || cleanEmail.isBlank()) {
            return SettingsActionResult.Error("All required fields must be completed.")
        }

        if (!EmailValidator.isValid(cleanEmail)) {
            return SettingsActionResult.Error("Please enter a valid email address.")
        }

        val existing = db.customerDao().findByEmail(cleanEmail)
        if (existing != null && existing.customerId != customerId) {
            return SettingsActionResult.Error("This email is already registered.")
        }

        db.customerDao().update(
            customer.copy(
                firstName = cleanFirstName,
                lastName = cleanLastName,
                email = cleanEmail
            )
        )

        return SettingsActionResult.Success("Personal info updated.")
    }

    suspend fun updateMarketingInboxConsent(
        customerId: Long,
        enabled: Boolean
    ): SettingsActionResult {
        val customer = db.customerDao().getById(customerId)
            ?: return SettingsActionResult.Error("Customer account could not be found.")

        db.customerDao().update(
            customer.copy(marketingInboxConsent = enabled)
        )

        return SettingsActionResult.Success("Inbox preferences updated.")
    }

    suspend fun changePassword(
        customerId: Long,
        currentPassword: String,
        newPassword: String
    ): SettingsActionResult {
        val customer = db.customerDao().getById(customerId)
            ?: return SettingsActionResult.Error("Customer account could not be found.")

        if (currentPassword.isBlank()) {
            return SettingsActionResult.Error("Enter current password.")
        }

        if (!PasswordRulesValidator.isValid(newPassword)) {
            return SettingsActionResult.Error("New password does not meet all rules.")
        }

        val currentPasswordChars = currentPassword.toCharArray()
        val currentValid = PasswordHasher.verify(
            currentPasswordChars,
            customer.passwordSalt,
            customer.passwordHash
        )
        currentPasswordChars.fill('\u0000')

        if (!currentValid) {
            return SettingsActionResult.Error("Current password is incorrect.")
        }

        val newSalt = PasswordHasher.generateSaltBase64()
        val newPasswordChars = newPassword.toCharArray()
        val newHash = PasswordHasher.hashPasswordBase64(newPasswordChars, newSalt)
        newPasswordChars.fill('\u0000')

        db.customerDao().update(
            customer.copy(
                passwordHash = newHash,
                passwordSalt = newSalt
            )
        )

        return SettingsActionResult.Success("Password changed successfully.")
    }

    suspend fun deleteAccount(
        customerId: Long,
        currentPassword: String
    ): SettingsActionResult {
        val customer = db.customerDao().getById(customerId)
            ?: return SettingsActionResult.Error("Customer account could not be found.")

        if (currentPassword.isBlank()) {
            return SettingsActionResult.Error("Enter current password.")
        }

        val passwordChars = currentPassword.toCharArray()
        val valid = PasswordHasher.verify(
            passwordChars,
            customer.passwordSalt,
            customer.passwordHash
        )
        passwordChars.fill('\u0000')

        if (!valid) {
            return SettingsActionResult.Error("Current password is incorrect.")
        }

        cleanupRepository.deleteCustomerCompletely(customerId)
        return SettingsActionResult.Success("Account deleted permanently.")
    }
}