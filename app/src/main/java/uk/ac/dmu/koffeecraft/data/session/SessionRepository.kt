package uk.ac.dmu.koffeecraft.data.session

data class SessionSnapshot(
    val currentCustomerId: Long? = null,
    val currentAdminId: Long? = null,
    val isAdmin: Boolean = false
)

class SessionRepository {

    val currentCustomerId: Long?
        get() = SessionManager.currentCustomerId

    val currentAdminId: Long?
        get() = SessionManager.currentAdminId

    val isAdmin: Boolean
        get() = SessionManager.isAdmin

    fun getSnapshot(): SessionSnapshot {
        return SessionSnapshot(
            currentCustomerId = SessionManager.currentCustomerId,
            currentAdminId = SessionManager.currentAdminId,
            isAdmin = SessionManager.isAdmin
        )
    }

    fun setCustomer(customerId: Long) {
        SessionManager.setCustomer(customerId)
    }

    fun setAdmin(adminId: Long? = null) {
        SessionManager.setAdmin(adminId)
    }

    fun clear() {
        SessionManager.clear()
    }
}