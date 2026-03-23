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
    val selectedOptionId: Long? = null,
    val selectedOptionLabel: String? = null,
    val selectedOptionSizeValue: Int? = null,
    val selectedOptionSizeUnit: String? = null,
    val selectedAddOnIds: List<Long> = emptyList(),
    val selectedAddOnsSummary: String? = null,
    val estimatedCalories: Int? = null
)

data class CartSnapshot(
    val items: List<CartItem> = emptyList(),
    val itemCount: Int = 0,
    val total: Double = 0.0,
    val beansToSpend: Int = 0,
    val purchasedProductCountForBeans: Int = 0
)

object CartManager {

    private val items = linkedMapOf<String, CartItem>()

    private val _snapshot = MutableStateFlow(CartSnapshot())
    val snapshot: StateFlow<CartSnapshot> = _snapshot

    private val _itemCount = MutableStateFlow(0)
    val itemCount: StateFlow<Int> = _itemCount

    private fun isMeaningfullyCustomised(option: ProductOption, addOns: List<AddOn>): Boolean {
        return !option.isDefault || addOns.isNotEmpty()
    }

    private fun normalizeRewardQuantities() {
        items.values
            .filter { it.isReward && it.quantity > 1 }
            .forEach { it.quantity = 1 }
    }

    private fun refreshCartState() {
        normalizeRewardQuantities()

        val currentItems = items.values.toList()
        val nextSnapshot = CartSnapshot(
            items = currentItems,
            itemCount = currentItems.sumOf { it.quantity },
            total = currentItems.sumOf { it.unitPrice * it.quantity },
            beansToSpend = currentItems.sumOf { it.beansCostPerUnit * it.quantity },
            purchasedProductCountForBeans = currentItems
                .filter { !it.isReward }
                .sumOf { it.quantity }
        )

        _snapshot.value = nextSnapshot
        _itemCount.value = nextSnapshot.itemCount
    }

    fun replaceAll(newItems: List<CartItem>) {
        items.clear()
        newItems.forEach { item ->
            items[item.lineKey] = item
        }
        refreshCartState()
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

        refreshCartState()
    }

    fun add(product: Product, quantity: Int) {
        repeat(quantity.coerceAtLeast(0)) { add(product) }
    }

    fun addExisting(item: CartItem) {
        if (item.isReward) {
            refreshCartState()
            return
        }

        val existing = items[item.lineKey]
        if (existing == null) {
            items[item.lineKey] = item.copy(quantity = 1)
        } else {
            existing.quantity += 1
        }

        refreshCartState()
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
            refreshCartState()
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

        refreshCartState()
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

        if (!isMeaningfullyCustomised(option, sortedAddOns)) {
            return addReward(
                sourceProduct = sourceProduct,
                rewardType = rewardType,
                beansCostPerUnit = beansCostPerUnit
            )
        }

        val addOnKey = sortedAddOns.joinToString("_") { it.addOnId.toString() }
        val key = "reward_custom_${rewardType}_${sourceProduct.productId}_${option.optionId}_$addOnKey"

        val existing = items[key]
        if (existing != null) {
            refreshCartState()
            return false
        }

        val rewardProduct = sourceProduct.copy(
            name = "Reward: ${sourceProduct.name}",
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
            selectedOptionId = option.optionId,
            selectedOptionLabel = option.displayLabel,
            selectedOptionSizeValue = option.sizeValue,
            selectedOptionSizeUnit = option.sizeUnit,
            selectedAddOnIds = sortedAddOns.map { it.addOnId },
            selectedAddOnsSummary = addOnSummary,
            estimatedCalories = finalCalories
        )

        refreshCartState()
        return true
    }

    fun addCustomisedProduct(
        product: Product,
        option: ProductOption,
        addOns: List<AddOn>
    ) {
        val sortedAddOns = addOns.sortedBy { it.addOnId }

        if (!isMeaningfullyCustomised(option, sortedAddOns)) {
            add(product)
            return
        }

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
                selectedOptionId = option.optionId,
                selectedOptionLabel = option.displayLabel,
                selectedOptionSizeValue = option.sizeValue,
                selectedOptionSizeUnit = option.sizeUnit,
                selectedAddOnIds = sortedAddOns.map { it.addOnId },
                selectedAddOnsSummary = addOnSummary,
                estimatedCalories = finalCalories
            )
        } else {
            existing.quantity += 1
        }

        refreshCartState()
    }

    fun removeOne(lineKey: String) {
        val existing = items[lineKey] ?: return

        if (existing.quantity <= 1) {
            items.remove(lineKey)
        } else {
            existing.quantity -= 1
        }

        refreshCartState()
    }

    fun clear() {
        items.clear()
        refreshCartState()
    }

    fun currentSnapshot(): CartSnapshot {
        refreshCartState()
        return _snapshot.value
    }

    fun getItems(): List<CartItem> = currentSnapshot().items

    fun total(): Double = currentSnapshot().total

    fun beansToSpend(): Int = currentSnapshot().beansToSpend

    fun purchasedProductCountForBeans(): Int = currentSnapshot().purchasedProductCountForBeans
}