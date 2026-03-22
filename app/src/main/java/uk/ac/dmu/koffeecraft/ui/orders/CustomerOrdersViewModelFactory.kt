package uk.ac.dmu.koffeecraft.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import uk.ac.dmu.koffeecraft.data.repository.CustomerOrdersRepository

class CustomerOrdersViewModelFactory(
    private val customerOrdersRepository: CustomerOrdersRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CustomerOrdersViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CustomerOrdersViewModel(customerOrdersRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}