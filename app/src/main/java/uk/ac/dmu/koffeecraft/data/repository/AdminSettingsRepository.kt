package uk.ac.dmu.koffeecraft.data.repository

import android.content.Context
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.session.RememberedSessionStore
import uk.ac.dmu.koffeecraft.data.session.SessionRepository
import uk.ac.dmu.koffeecraft.data.settings.SimulationSettings
import uk.ac.dmu.koffeecraft.data.settings.ThemeSettings

data class AdminSettingsData(
    val adminName: String?,
    val adminEmail: String?,
    val simulationEnabled: Boolean,
    val darkModeEnabled: Boolean
)

class AdminSettingsRepository(
    context: Context,
    private val db: KoffeeCraftDatabase,
    private val sessionRepository: SessionRepository,
    private val cartRepository: CartRepository
) {

    private val appContext = context.applicationContext

    suspend fun loadScreenData(adminId: Long?): AdminSettingsData {
        val simulationEnabled = SimulationSettings.isEnabled(appContext)
        val darkModeEnabled = ThemeSettings.isDarkModeEnabled(appContext)

        if (adminId == null) {
            return AdminSettingsData(
                adminName = null,
                adminEmail = null,
                simulationEnabled = simulationEnabled,
                darkModeEnabled = darkModeEnabled
            )
        }

        val admin = db.adminDao().getById(adminId)

        return AdminSettingsData(
            adminName = admin?.fullName,
            adminEmail = admin?.email,
            simulationEnabled = simulationEnabled,
            darkModeEnabled = darkModeEnabled
        )
    }

    fun setSimulationEnabled(enabled: Boolean) {
        SimulationSettings.setEnabled(appContext, enabled)
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        ThemeSettings.setDarkModeEnabled(appContext, enabled)
    }

    fun signOut() {
        cartRepository.clearInMemoryOnly()
        sessionRepository.clear()
        RememberedSessionStore.clear(appContext)
    }
}