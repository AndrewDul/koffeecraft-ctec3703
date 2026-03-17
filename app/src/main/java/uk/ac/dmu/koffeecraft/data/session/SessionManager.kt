package uk.ac.dmu.koffeecraft.data.session

object SessionManager {
    var currentCustomerId: Long? = null
        private set

    var currentAdminId: Long? = null
        private set

    var isAdmin: Boolean = false
        private set

    fun setCustomer(customerId: Long) {
        currentCustomerId = customerId
        currentAdminId = null
        isAdmin = false
    }

    fun setAdmin(adminId: Long? = null) {
        currentCustomerId = null
        currentAdminId = adminId
        isAdmin = true
    }

    fun clear() {
        currentCustomerId = null
        currentAdminId = null
        isAdmin = false
    }
}