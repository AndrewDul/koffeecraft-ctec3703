package uk.ac.dmu.koffeecraft.data.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class SessionSnapshot(
    val currentCustomerId: Long? = null,
    val currentAdminId: Long? = null,
    val isAdmin: Boolean = false
)

class SessionRepository {

    private val _session = MutableStateFlow(readSnapshot())
    val session: StateFlow<SessionSnapshot> = _session

    val currentCustomerId: Long?
        get() = _session.value.currentCustomerId

    val currentAdminId: Long?
        get() = _session.value.currentAdminId

    val isAdmin: Boolean
        get() = _session.value.isAdmin

    fun getSnapshot(): SessionSnapshot = _session.value

    fun setCustomer(customerId: Long) {
        SessionManager.setCustomer(customerId)
        syncFromManager()
    }

    fun setAdmin(adminId: Long? = null) {
        SessionManager.setAdmin(adminId)
        syncFromManager()
    }

    fun clear() {
        SessionManager.clear()
        syncFromManager()
    }

    fun isCurrentCustomer(customerId: Long): Boolean {
        return currentCustomerId == customerId && !isAdmin
    }

    fun syncFromManager() {
        _session.value = readSnapshot()
    }

    private fun readSnapshot(): SessionSnapshot {
        return SessionSnapshot(
            currentCustomerId = SessionManager.currentCustomerId,
            currentAdminId = SessionManager.currentAdminId,
            isAdmin = SessionManager.isAdmin
        )
    }
}