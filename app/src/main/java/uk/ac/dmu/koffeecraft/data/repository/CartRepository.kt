package uk.ac.dmu.koffeecraft.data.repository

import android.content.Context
import kotlinx.coroutines.flow.StateFlow
import uk.ac.dmu.koffeecraft.data.cart.CartItem
import uk.ac.dmu.koffeecraft.data.cart.CartManager
import uk.ac.dmu.koffeecraft.data.cart.CartSnapshot
import uk.ac.dmu.koffeecraft.data.cart.RememberedCartStore
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.AddOn
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.entities.ProductOption
import uk.ac.dmu.koffeecraft.data.session.SessionRepository

class CartRepository(
    context: Context,
    private val sessionRepository: SessionRepository,
    private val db: KoffeeCraftDatabase
) {

    private val appContext = context.applicationContext

    fun observeCart(): StateFlow<CartSnapshot> = CartManager.snapshot

    fun observeItemCount(): StateFlow<Int> = CartManager.itemCount

    fun getCurrentCart(): CartSnapshot = CartManager.currentSnapshot()

    fun replaceAll(
        newItems: List<CartItem>,
        persist: Boolean = true
    ) {
        CartManager.replaceAll(newItems)
        if (persist) {
            persistCurrentCartForActiveCustomer()
        }
    }

    fun addProduct(product: Product) {
        CartManager.add(product)
        persistCurrentCartForActiveCustomer()
    }

    fun addProduct(
        product: Product,
        quantity: Int
    ) {
        CartManager.add(product, quantity)
        persistCurrentCartForActiveCustomer()
    }

    fun addExisting(item: CartItem) {
        CartManager.addExisting(item)
        persistCurrentCartForActiveCustomer()
    }

    fun addCustomisedProduct(
        product: Product,
        option: ProductOption,
        addOns: List<AddOn>
    ) {
        CartManager.addCustomisedProduct(
            product = product,
            option = option,
            addOns = addOns
        )
        persistCurrentCartForActiveCustomer()
    }

    fun addReward(
        sourceProduct: Product,
        rewardType: String,
        beansCostPerUnit: Int
    ): Boolean {
        val added = CartManager.addReward(
            sourceProduct = sourceProduct,
            rewardType = rewardType,
            beansCostPerUnit = beansCostPerUnit
        )
        persistCurrentCartForActiveCustomer()
        return added
    }

    fun addRewardCustomisedProduct(
        sourceProduct: Product,
        rewardType: String,
        beansCostPerUnit: Int,
        option: ProductOption,
        addOns: List<AddOn>
    ): Boolean {
        val added = CartManager.addRewardCustomisedProduct(
            sourceProduct = sourceProduct,
            rewardType = rewardType,
            beansCostPerUnit = beansCostPerUnit,
            option = option,
            addOns = addOns
        )
        persistCurrentCartForActiveCustomer()
        return added
    }

    fun removeOne(lineKey: String) {
        CartManager.removeOne(lineKey)
        persistCurrentCartForActiveCustomer()
    }

    fun clear() {
        CartManager.clear()
        persistCurrentCartForActiveCustomer()
    }

    fun clearInMemoryOnly() {
        CartManager.clear()
    }

    fun clearPersistedCartForCustomer(customerId: Long) {
        RememberedCartStore.clearCartForCustomer(appContext, customerId)
    }

    fun beansToSpend(): Int = CartManager.beansToSpend()

    fun purchasedProductCountForBeans(): Int = CartManager.purchasedProductCountForBeans()

    suspend fun removeUnavailableItems(): Int {
        val currentItems = CartManager.getItems()
        if (currentItems.isEmpty()) return 0

        val validItems = buildList {
            currentItems.forEach { item ->
                if (isCartItemStillValid(item)) {
                    add(item)
                }
            }
        }

        val removedCount = currentItems.size - validItems.size
        if (removedCount > 0) {
            CartManager.replaceAll(validItems)
            persistCurrentCartForActiveCustomer()
        }

        return removedCount
    }

    private suspend fun isCartItemStillValid(item: CartItem): Boolean {
        val product = db.productDao().getById(item.product.productId) ?: return false
        if (!product.isActive) return false

        if (item.isReward && !product.rewardEnabled) {
            return false
        }

        val optionId = item.selectedOptionId
        if (optionId != null) {
            val option = db.productOptionDao().getById(optionId) ?: return false
            if (option.productId != product.productId) return false
        }

        if (item.selectedAddOnIds.isNotEmpty()) {
            val activeAssignedIds = db.addOnDao()
                .getActiveForProduct(product.productId)
                .map { it.addOnId }
                .toSet()

            val allStillValid = item.selectedAddOnIds.all { it in activeAssignedIds }
            if (!allStillValid) return false
        }

        return true
    }

    private fun persistCurrentCartForActiveCustomer() {
        val customerId = sessionRepository.currentCustomerId ?: return
        if (sessionRepository.isAdmin) return

        RememberedCartStore.saveCartForCustomer(
            context = appContext,
            customerId = customerId,
            items = CartManager.getItems()
        )
    }
}