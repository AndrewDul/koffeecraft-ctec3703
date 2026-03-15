package uk.ac.dmu.koffeecraft.data.cart

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import uk.ac.dmu.koffeecraft.data.entities.AddOn
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.entities.ProductOption

data class CartItem(
    val lineKey: String,
    val product: Product,
    var quantity: Int,
    val unitPrice: Double,
    val isReward: Boolean = false,
    val rewardType: String? = null,
    val beansCostPerUnit: Int = 0,
    val selectedOptionLabel: String? = null,
    val selectedOptionSizeValue: Int? = null,
    val selectedOptionSizeUnit: String? = null,
    val selectedAddOnsSummary: String? = null,
    val estimatedCalories: Int? = null
)

object CartManager {

    private val items = linkedMapOf<String, CartItem>()

    private val _itemCount = MutableStateFlow(0)
    val itemCount: StateFlow<Int> = _itemCount

    private fun normalizeRewardQuantities() {
        items.values
            .filter { it.isReward && it.quantity > 1 }
            .forEach { it.quantity = 1 }
    }

    private fun refreshItemCount() {
        normalizeRewardQuantities()
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
                unitPrice = product.price
            )
        } else {
            existing.quantity += 1
        }

        refreshItemCount()
    }

    fun add(product: Product, quantity: Int) {
        repeat(quantity) { add(product) }
    }

    fun addExisting(item: CartItem) {
        if (item.isReward) {
            refreshItemCount()
            return
        }

        val existing = items[item.lineKey]
        if (existing == null) {
            items[item.lineKey] = item.copy(quantity = 1)
        } else {
            existing.quantity += 1
        }

        refreshItemCount()
    }

    fun addReward(
        sourceProduct: Product,
        rewardType: String,
        beansCostPerUnit: Int
    ): Boolean {
        normalizeRewardQuantities()

        val key = "reward_${rewardType}_${sourceProduct.productId}"
        val existing = items[key]

        if (existing != null) {
            refreshItemCount()
            return false
        }

        val rewardDisplayName = when (rewardType) {
            "FREE_COFFEE", "FREE_CAKE" -> "Reward: ${sourceProduct.name}"
            else -> sourceProduct.name
        }

        val rewardProduct = sourceProduct.copy(
            name = rewardDisplayName,
            price = 0.0
        )

        items[key] = CartItem(
            lineKey = key,
            product = rewardProduct,
            quantity = 1,
            unitPrice = 0.0,
            isReward = true,
            rewardType = rewardType,
            beansCostPerUnit = beansCostPerUnit
        )

        refreshItemCount()
        return true
    }

    fun addRewardCustomisedProduct(
        sourceProduct: Product,
        rewardType: String,
        beansCostPerUnit: Int,
        option: ProductOption,
        addOns: List<AddOn>
    ): Boolean {
        normalizeRewardQuantities()

        val sortedAddOns = addOns.sortedBy { it.addOnId }
        val addOnKey = sortedAddOns.joinToString("_") { it.addOnId.toString() }

        val key = "reward_custom_${rewardType}_${sourceProduct.productId}_${option.optionId}_$addOnKey"
        val existing = items[key]

        if (existing != null) {
            refreshItemCount()
            return false
        }

        val rewardDisplayName = "Reward: ${sourceProduct.name}"
        val rewardProduct = sourceProduct.copy(
            name = rewardDisplayName,
            price = 0.0
        )

        val finalPrice = option.extraPrice + sortedAddOns.sumOf { it.price }
        val finalCalories = option.estimatedCalories + sortedAddOns.sumOf { it.estimatedCalories }
        val addOnSummary = if (sortedAddOns.isEmpty()) {
            null
        } else {
            sortedAddOns.joinToString(", ") { it.name }
        }

        items[key] = CartItem(
            lineKey = key,
            product = rewardProduct,
            quantity = 1,
            unitPrice = finalPrice,
            isReward = true,
            rewardType = rewardType,
            beansCostPerUnit = beansCostPerUnit,
            selectedOptionLabel = option.displayLabel,
            selectedOptionSizeValue = option.sizeValue,
            selectedOptionSizeUnit = option.sizeUnit,
            selectedAddOnsSummary = addOnSummary,
            estimatedCalories = finalCalories
        )

        refreshItemCount()
        return true
    }

    fun addCustomisedProduct(
        product: Product,
        option: ProductOption,
        addOns: List<AddOn>
    ) {
        val sortedAddOns = addOns.sortedBy { it.addOnId }
        val addOnKey = sortedAddOns.joinToString("_") { it.addOnId.toString() }

        val key = "custom_${product.productId}_${option.optionId}_$addOnKey"
        val existing = items[key]

        val finalPrice = product.price + option.extraPrice + sortedAddOns.sumOf { it.price }
        val finalCalories = option.estimatedCalories + sortedAddOns.sumOf { it.estimatedCalories }
        val addOnSummary = if (sortedAddOns.isEmpty()) {
            null
        } else {
            sortedAddOns.joinToString(", ") { it.name }
        }

        if (existing == null) {
            items[key] = CartItem(
                lineKey = key,
                product = product,
                quantity = 1,
                unitPrice = finalPrice,
                selectedOptionLabel = option.displayLabel,
                selectedOptionSizeValue = option.sizeValue,
                selectedOptionSizeUnit = option.sizeUnit,
                selectedAddOnsSummary = addOnSummary,
                estimatedCalories = finalCalories
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

    fun getItems(): List<CartItem> {
        normalizeRewardQuantities()
        refreshItemCount()
        return items.values.toList()
    }

    fun total(): Double {
        normalizeRewardQuantities()
        return items.values.sumOf { it.unitPrice * it.quantity }
    }

    fun beansToSpend(): Int {
        normalizeRewardQuantities()
        return items.values.sumOf { it.beansCostPerUnit * it.quantity }
    }

    fun purchasedProductCountForBeans(): Int {
        normalizeRewardQuantities()
        return items.values
            .filter { !it.isReward }
            .sumOf { it.quantity }
    }
}