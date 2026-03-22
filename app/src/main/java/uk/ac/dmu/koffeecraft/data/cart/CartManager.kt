package uk.ac.dmu.koffeecraft.data.cart

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import uk.ac.dmu.koffeecraft.data.entities.AddOn
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.entities.ProductOption
import uk.ac.dmu.koffeecraft.data.session.SessionManager

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

object CartManager {

    private val items = linkedMapOf<String, CartItem>()
    private lateinit var appContext: Context

    private val _itemCount = MutableStateFlow(0)
    val itemCount: StateFlow<Int> = _itemCount

    fun attachContext(context: Context) {
        appContext = context.applicationContext
    }

    private fun isMeaningfullyCustomised(option: ProductOption, addOns: List<AddOn>): Boolean {
        return !option.isDefault || addOns.isNotEmpty()
    }

    private fun normalizeRewardQuantities() {
        items.values
            .filter { it.isReward && it.quantity > 1 }
            .forEach { it.quantity = 1 }
    }

    private fun refreshItemCount(persist: Boolean = true) {
        normalizeRewardQuantities()
        _itemCount.value = items.values.sumOf { it.quantity }

        if (persist) {
            persistCurrentCartForSession()
        }
    }

    private fun persistCurrentCartForSession() {
        if (!::appContext.isInitialized) return
        if (SessionManager.isAdmin) return

        val customerId = SessionManager.currentCustomerId ?: return
        RememberedCartStore.saveCartForCustomer(
            context = appContext,
            customerId = customerId,
            items = items.values.toList()
        )
    }

    fun replaceAll(
        newItems: List<CartItem>,
        persist: Boolean = true
    ) {
        items.clear()
        newItems.forEach { item ->
            items[item.lineKey] = item
        }
        refreshItemCount(persist = persist)
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
        repeat(quantity.coerceAtLeast(0)) { add(product) }
    }

    fun addExisting(item: CartItem) {
        if (item.isReward) {
            refreshItemCount(persist = false)
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
            refreshItemCount(persist = false)
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
            refreshItemCount(persist = false)
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

        refreshItemCount()
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

    fun clearInMemoryOnly() {
        items.clear()
        refreshItemCount(persist = false)
    }

    fun clearPersistedCartForCustomer(
        context: Context,
        customerId: Long
    ) {
        RememberedCartStore.clearCartForCustomer(context.applicationContext, customerId)
    }

    fun getItems(): List<CartItem> {
        normalizeRewardQuantities()
        refreshItemCount(persist = false)
        return items.values.toList()
    }

    fun total(): Double {
        normalizeRewardQuantities()
        refreshItemCount(persist = false)
        return items.values.sumOf { it.unitPrice * it.quantity }
    }

    fun beansToSpend(): Int {
        normalizeRewardQuantities()
        refreshItemCount(persist = false)
        return items.values.sumOf { it.beansCostPerUnit * it.quantity }
    }

    fun purchasedProductCountForBeans(): Int {
        normalizeRewardQuantities()
        refreshItemCount(persist = false)
        return items.values
            .filter { !it.isReward }
            .sumOf { it.quantity }
    }
}