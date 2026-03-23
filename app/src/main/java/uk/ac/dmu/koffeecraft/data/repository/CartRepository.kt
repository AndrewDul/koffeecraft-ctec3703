package uk.ac.dmu.koffeecraft.data.repository

import kotlinx.coroutines.flow.StateFlow
import uk.ac.dmu.koffeecraft.data.cart.CartItem
import uk.ac.dmu.koffeecraft.data.cart.CartManager
import uk.ac.dmu.koffeecraft.data.cart.CartSnapshot

class CartRepository {

    fun observeCart(): StateFlow<CartSnapshot> = CartManager.snapshot

    fun getCurrentCart(): CartSnapshot = CartManager.currentSnapshot()

    fun addExisting(item: CartItem) {
        CartManager.addExisting(item)
    }

    fun removeOne(lineKey: String) {
        CartManager.removeOne(lineKey)
    }

    fun clear() {
        CartManager.clear()
    }
}