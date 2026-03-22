package uk.ac.dmu.koffeecraft.ui.admin.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import uk.ac.dmu.koffeecraft.data.repository.AdminMenuRepository

class AdminMenuViewModelFactory(
    private val repository: AdminMenuRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminMenuViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminMenuViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}