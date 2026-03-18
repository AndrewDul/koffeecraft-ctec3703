package uk.ac.dmu.koffeecraft

import android.app.Application
import uk.ac.dmu.koffeecraft.data.settings.ThemeSettings

class KoffeeCraftApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ThemeSettings.applySavedTheme(this)
    }
}