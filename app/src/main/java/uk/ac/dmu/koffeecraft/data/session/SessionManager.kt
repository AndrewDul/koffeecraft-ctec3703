package uk.ac.dmu.koffeecraft.data.session

object SessionManager {
    var currentCustomerId: Long? = null
        private set

    var isAdmin: Boolean = false
        private set

    fun setCustomer(customerId: Long) {
        currentCustomerId = customerId
        isAdmin = false
    }

    fun setAdmin() {
        currentCustomerId = null
        isAdmin = true
    }

    fun clear() {
        currentCustomerId = null
        isAdmin = false
    }
}