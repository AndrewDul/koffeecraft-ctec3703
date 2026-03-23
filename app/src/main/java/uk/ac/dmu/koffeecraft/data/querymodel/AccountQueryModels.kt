package uk.ac.dmu.koffeecraft.data.querymodel

data class AdminAccountTarget(
    val adminId: Long,
    val fullName: String,
    val email: String,
    val phone: String,
    val username: String,
    val isActive: Boolean,
    val createdAt: Long
)

data class CustomerInboxTarget(
    val customerId: Long,
    val firstName: String,
    val lastName: String,
    val email: String,
    val dateOfBirth: String?,
    val marketingInboxConsent: Boolean
)

data class CustomerAccountTarget(
    val customerId: Long,
    val firstName: String,
    val lastName: String,
    val email: String,
    val isActive: Boolean,
    val createdAt: Long
)

data class CustomerCampaignTarget(
    val customerId: Long,
    val firstName: String,
    val lastName: String,
    val email: String,
    val dateOfBirth: String?,
    val marketingInboxConsent: Boolean,
    val beansBalance: Int,
    val orderCount: Int,
    val lastOrderAt: Long?,
    val lifetimeSpend: Double
) {
    val displayName: String
        get() = "$firstName $lastName".trim()
}