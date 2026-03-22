package uk.ac.dmu.koffeecraft.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import uk.ac.dmu.koffeecraft.data.repository.CheckoutRepository

class CheckoutViewModelFactory(
    private val checkoutRepository: CheckoutRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CheckoutViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CheckoutViewModel(checkoutRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}