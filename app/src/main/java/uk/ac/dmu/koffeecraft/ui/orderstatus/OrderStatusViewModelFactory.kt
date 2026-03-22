package uk.ac.dmu.koffeecraft.ui.orderstatus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import uk.ac.dmu.koffeecraft.data.repository.CustomerOrdersRepository

class OrderStatusViewModelFactory(
    private val customerOrdersRepository: CustomerOrdersRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OrderStatusViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OrderStatusViewModel(customerOrdersRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}