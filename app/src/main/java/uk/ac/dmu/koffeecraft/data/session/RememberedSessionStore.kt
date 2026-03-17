package uk.ac.dmu.koffeecraft.data.session

import android.content.Context

object RememberedSessionStore {

    private const val PREFS_NAME = "koffeecraft_remembered_session"

    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_ROLE = "role"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_ONBOARDING_PENDING = "onboarding_pending"

    private const val ROLE_CUSTOMER = "CUSTOMER"
    private const val ROLE_ADMIN = "ADMIN"

    data class RememberedSession(
        val userId: Long,
        val role: Role,
        val onboardingPending: Boolean
    )

    enum class Role {
        CUSTOMER,
        ADMIN
    }

    fun saveCustomerSession(
        context: Context,
        customerId: Long,
        onboardingPending: Boolean = false
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_ROLE, ROLE_CUSTOMER)
            .putLong(KEY_USER_ID, customerId)
            .putBoolean(KEY_ONBOARDING_PENDING, onboardingPending)
            .apply()
    }

    fun saveAdminSession(
        context: Context,
        adminId: Long
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_ROLE, ROLE_ADMIN)
            .putLong(KEY_USER_ID, adminId)
            .putBoolean(KEY_ONBOARDING_PENDING, false)
            .apply()
    }

    fun markCustomerOnboardingComplete(context: Context) {
        val session = getSession(context) ?: return
        if (session.role != Role.CUSTOMER) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_ONBOARDING_PENDING, false)
            .apply()
    }

    fun restoreIntoMemory(context: Context): RememberedSession? {
        val session = getSession(context) ?: run {
            SessionManager.clear()
            return null
        }

        when (session.role) {
            Role.CUSTOMER -> SessionManager.setCustomer(session.userId)
            Role.ADMIN -> SessionManager.setAdmin(session.userId)
        }

        return session
    }

    fun getSession(context: Context): RememberedSession? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (!isLoggedIn) return null

        val roleRaw = prefs.getString(KEY_ROLE, null) ?: return null
        val userId = prefs.getLong(KEY_USER_ID, -1L)
        if (userId <= 0L) return null

        val onboardingPending = prefs.getBoolean(KEY_ONBOARDING_PENDING, false)

        val role = when (roleRaw) {
            ROLE_CUSTOMER -> Role.CUSTOMER
            ROLE_ADMIN -> Role.ADMIN
            else -> return null
        }

        return RememberedSession(
            userId = userId,
            role = role,
            onboardingPending = onboardingPending
        )
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}