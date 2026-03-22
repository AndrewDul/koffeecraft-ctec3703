package uk.ac.dmu.koffeecraft.data.repository

import android.content.Context
import uk.ac.dmu.koffeecraft.data.dao.AdminAccountTarget
import uk.ac.dmu.koffeecraft.data.dao.CustomerAccountTarget
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.util.security.PasswordHasher
import uk.ac.dmu.koffeecraft.util.validation.PasswordRulesValidator
import java.util.Locale

class AdminAccountsRepository(
    context: Context,
    private val db: KoffeeCraftDatabase
) {

    private val appContext = context.applicationContext
    private val cleanupRepository = CustomerAccountCleanupRepository(appContext, db)

    suspend fun findCustomerByOrderId(orderId: Long): CustomerAccountTarget? {
        return db.customerDao().getAccountTargetByOrderId(orderId)
    }

    suspend fun findCustomerByCustomerId(customerId: Long): CustomerAccountTarget? {
        return db.customerDao().getAccountTargetByCustomerId(customerId)
    }

    suspend fun findAdminByAdminId(adminId: Long): AdminAccountTarget? {
        return db.adminDao().getAccountTargetByAdminId(adminId)
    }

    suspend fun findAdminByEmail(email: String): AdminAccountTarget? {
        return db.adminDao().getAccountTargetByEmail(email.trim().lowercase(Locale.UK))
    }

    suspend fun findAdminByUsername(username: String): AdminAccountTarget? {
        return db.adminDao().getAccountTargetByUsername(username.trim().lowercase(Locale.UK))
    }

    suspend fun updateCustomerStatus(
        customerId: Long,
        isActive: Boolean
    ): SettingsActionResult {
        db.customerDao().updateActiveStatus(customerId, isActive)
        return SettingsActionResult.Success(
            if (isActive) {
                "Customer account activated successfully."
            } else {
                "Customer account deactivated successfully."
            }
        )
    }

    suspend fun updateAdminStatus(
        adminId: Long,
        isActive: Boolean,
        currentAdminId: Long?
    ): SettingsActionResult {
        if (!isActive) {
            if (currentAdminId == adminId) {
                return SettingsActionResult.Error(
                    "You cannot deactivate the currently signed-in admin account."
                )
            }

            val activeCount = db.adminDao().countActiveAdmins()
            if (activeCount <= 1) {
                return SettingsActionResult.Error(
                    "At least one active admin account must remain."
                )
            }
        }

        db.adminDao().updateActiveStatus(adminId, isActive)

        return SettingsActionResult.Success(
            if (isActive) {
                "Admin account activated successfully."
            } else {
                "Admin account deactivated successfully."
            }
        )
    }

    suspend fun deleteCustomerPermanently(customerId: Long): SettingsActionResult {
        cleanupRepository.deleteCustomerCompletely(customerId)
        return SettingsActionResult.Success("Customer account deleted permanently.")
    }

    suspend fun resetAdminPassword(
        adminId: Long,
        newPassword: String,
        confirmPassword: String
    ): SettingsActionResult {
        if (!PasswordRulesValidator.isValid(newPassword)) {
            return SettingsActionResult.Error(
                "The new password does not meet all password rules."
            )
        }

        if (confirmPassword != newPassword) {
            return SettingsActionResult.Error(
                "The password confirmation does not match."
            )
        }

        val admin = db.adminDao().getById(adminId)
            ?: return SettingsActionResult.Error("Admin account could not be found.")

        val newSalt = PasswordHasher.generateSaltBase64()
        val passwordChars = newPassword.toCharArray()
        val newHash = PasswordHasher.hashPasswordBase64(passwordChars, newSalt)
        passwordChars.fill('\u0000')

        db.adminDao().updatePassword(admin.adminId, newHash, newSalt)

        return SettingsActionResult.Success("Admin password updated successfully.")
    }
}