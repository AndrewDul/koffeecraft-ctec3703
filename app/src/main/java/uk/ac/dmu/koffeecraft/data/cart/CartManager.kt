package uk.ac.dmu.koffeecraft.data.cart

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import uk.ac.dmu.koffeecraft.data.entities.Product

data class CartItem(val product: Product, var quantity: Int)

object CartManager {

    private val items = linkedMapOf<Long, CartItem>()

    private val _itemCount = MutableStateFlow(0)
    val itemCount: StateFlow<Int> = _itemCount

    private fun refreshItemCount() {
        _itemCount.value = items.values.sumOf { it.quantity }
    }

    fun add(product: Product) {
        val existing = items[product.productId]
        if (existing == null) {
            items[product.productId] = CartItem(product, 1)
        } else {
            existing.quantity += 1
        }
        refreshItemCount()
    }

    fun add(product: Product, quantity: Int) {
        repeat(quantity) { add(product) }
    }

    fun removeOne(productId: Long) {
        val existing = items[productId] ?: return

        if (existing.quantity <= 1) {
            items.remove(productId)
        } else {
            existing.quantity -= 1
        }

        refreshItemCount()
    }

    fun clear() {
        items.clear()
        refreshItemCount()
    }

    fun getItems(): List<CartItem> = items.values.toList()

    fun total(): Double = items.values.sumOf { it.product.price * it.quantity }
}