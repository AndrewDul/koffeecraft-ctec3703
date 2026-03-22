package uk.ac.dmu.koffeecraft.core.di

import android.content.Context
import androidx.fragment.app.Fragment
import uk.ac.dmu.koffeecraft.KoffeeCraftApp

val Context.koffeeCraftApp: KoffeeCraftApp
    get() = applicationContext as KoffeeCraftApp

val Context.appContainer: AppContainer
    get() = koffeeCraftApp.appContainer

val Fragment.appContainer: AppContainer
    get() = requireContext().appContainer