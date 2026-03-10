package uk.ac.dmu.koffeecraft.data.cart


import uk.ac.dmu.koffeecraft.data.entities.Product

data class CartItem(val product: Product, var quantity: Int)

object CartManager {

    private val items = linkedMapOf<Long, CartItem>()

    fun add(product: Product) {
        val existing = items[product.productId]
        if (existing == null) items[product.productId] = CartItem(product, 1)
        else existing.quantity += 1
    }

    fun removeOne(productId: Long) {
        val existing = items[productId] ?: return
        if (existing.quantity <= 1) items.remove(productId)
        else existing.quantity -= 1
    }

    fun clear() = items.clear()

    fun getItems(): List<CartItem> = items.values.toList()

    fun total(): Double = items.values.sumOf { it.product.price * it.quantity }
}