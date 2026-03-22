package uk.ac.dmu.koffeecraft.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import uk.ac.dmu.koffeecraft.data.repository.MenuRepository

class MenuViewModelFactory(
    private val menuRepository: MenuRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MenuViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MenuViewModel(menuRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}