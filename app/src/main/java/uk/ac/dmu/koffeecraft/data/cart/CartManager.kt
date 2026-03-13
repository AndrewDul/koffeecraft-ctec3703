package uk.ac.dmu.koffeecraft.data.cart

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import uk.ac.dmu.koffeecraft.data.entities.Product

data class CartItem(
    val lineKey: String,
    val product: Product,
    var quantity: Int,
    val isReward: Boolean = false,
    val rewardType: String? = null,
    val beansCostPerUnit: Int = 0
)

object CartManager {

    private val items = linkedMapOf<String, CartItem>()

    private val _itemCount = MutableStateFlow(0)
    val itemCount: StateFlow<Int> = _itemCount

    private fun refreshItemCount() {
        _itemCount.value = items.values.sumOf { it.quantity }
    }

    fun add(product: Product) {
        val key = "product_${product.productId}"
        val existing = items[key]

        if (existing == null) {
            items[key] = CartItem(
                lineKey = key,
                product = product,
                quantity = 1,
                isReward = false,
                rewardType = null,
                beansCostPerUnit = 0
            )
        } else {
            existing.quantity += 1
        }

        refreshItemCount()
    }

    fun add(product: Product, quantity: Int) {
        repeat(quantity) { add(product) }
    }

    fun addReward(sourceProduct: Product, rewardType: String, beansCostPerUnit: Int) {
        val key = "reward_${rewardType}_${sourceProduct.productId}"
        val existing = items[key]

        val rewardDisplayName = when (rewardType) {
            "FREE_COFFEE", "FREE_CAKE" -> "Reward: ${sourceProduct.name}"
            else -> sourceProduct.name
        }

        val rewardProduct = sourceProduct.copy(
            name = rewardDisplayName,
            price = 0.0
        )

        if (existing == null) {
            items[key] = CartItem(
                lineKey = key,
                product = rewardProduct,
                quantity = 1,
                isReward = true,
                rewardType = rewardType,
                beansCostPerUnit = beansCostPerUnit
            )
        } else {
            existing.quantity += 1
        }

        refreshItemCount()
    }

    fun removeOne(lineKey: String) {
        val existing = items[lineKey] ?: return

        if (existing.quantity <= 1) {
            items.remove(lineKey)
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

    fun beansToSpend(): Int = items.values.sumOf { it.beansCostPerUnit * it.quantity }

    fun purchasedProductCountForBeans(): Int =
        items.values
            .filter { !it.isReward }
            .sumOf { it.quantity }
}