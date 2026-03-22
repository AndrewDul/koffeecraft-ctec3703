package uk.ac.dmu.koffeecraft.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase

data class AdminActivityBadgeData(
    val unreadNotificationsCount: Int,
    val showBadge: Boolean,
    val badgeText: String
)

class AdminActivityRepository(
    private val db: KoffeeCraftDatabase
) {

    fun observeAdminBadge(): Flow<AdminActivityBadgeData> {
        return db.notificationDao().observeUnreadAdminCount().map { count ->
            AdminActivityBadgeData(
                unreadNotificationsCount = count,
                showBadge = count > 0,
                badgeText = if (count > 99) "99+" else count.toString()
            )
        }
    }
}