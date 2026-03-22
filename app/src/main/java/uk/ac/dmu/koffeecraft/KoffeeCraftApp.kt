package uk.ac.dmu.koffeecraft

import android.app.Application
import uk.ac.dmu.koffeecraft.core.di.AppContainer
import uk.ac.dmu.koffeecraft.data.settings.ThemeSettings

class KoffeeCraftApp : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        ThemeSettings.applySavedTheme(this)
        appContainer = AppContainer(this)
    }
}