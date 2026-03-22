package uk.ac.dmu.koffeecraft.data.repository

import android.content.Context
import uk.ac.dmu.koffeecraft.data.cart.CartItem
import uk.ac.dmu.koffeecraft.data.cart.RememberedCartStore
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.Product

class CartPersistenceRepository(
    context: Context,
    private val db: KoffeeCraftDatabase
) {

    private val appContext = context.applicationContext

    suspend fun restoreCartForCustomer(customerId: Long): List<CartItem> {
        val snapshots = RememberedCartStore.loadCartForCustomer(appContext, customerId)
        val restoredItems = linkedMapOf<String, CartItem>()

        snapshots.forEach { snapshot ->
            val restored = buildRestoredCartItem(snapshot)
            if (restored != null) {
                restoredItems[restored.lineKey] = restored
            }
        }

        val finalItems = restoredItems.values.toList()

        RememberedCartStore.saveCartForCustomer(
            context = appContext,
            customerId = customerId,
            items = finalItems
        )

        return finalItems
    }

    private suspend fun buildRestoredCartItem(
        snapshot: RememberedCartStore.CartItemSnapshot
    ): CartItem? {
        val product = db.productDao().getById(snapshot.productId) ?: return null
        if (!product.isActive) return null

        val optionId = snapshot.selectedOptionId
        if (optionId != null) {
            val option = db.productOptionDao().getById(optionId) ?: return null
            if (option.productId != product.productId) return null
        }

        if (snapshot.selectedAddOnIds.isNotEmpty()) {
            val activeAssignedAddOns = db.addOnDao().getActiveForProduct(product.productId)
            val activeAssignedIds = activeAssignedAddOns.map { it.addOnId }.toSet()

            val allAddOnsStillValid = snapshot.selectedAddOnIds.all { it in activeAssignedIds }
            if (!allAddOnsStillValid) return null
        }

        if (snapshot.isReward && !product.rewardEnabled) {
            return null
        }

        val displayProduct = buildDisplayProductForRestore(
            sourceProduct = product,
            isReward = snapshot.isReward,
            rewardType = snapshot.rewardType,
            hasCustomisation = optionId != null || snapshot.selectedAddOnIds.isNotEmpty()
        )

        return CartItem(
            lineKey = snapshot.lineKey,
            product = displayProduct,
            quantity = snapshot.quantity.coerceAtLeast(1),
            unitPrice = snapshot.unitPrice,
            isReward = snapshot.isReward,
            rewardType = snapshot.rewardType,
            beansCostPerUnit = snapshot.beansCostPerUnit,
            selectedOptionId = snapshot.selectedOptionId,
            selectedOptionLabel = snapshot.selectedOptionLabel,
            selectedOptionSizeValue = snapshot.selectedOptionSizeValue,
            selectedOptionSizeUnit = snapshot.selectedOptionSizeUnit,
            selectedAddOnIds = snapshot.selectedAddOnIds,
            selectedAddOnsSummary = snapshot.selectedAddOnsSummary,
            estimatedCalories = snapshot.estimatedCalories
        )
    }

    private fun buildDisplayProductForRestore(
        sourceProduct: Product,
        isReward: Boolean,
        rewardType: String?,
        hasCustomisation: Boolean
    ): Product {
        if (!isReward) return sourceProduct

        val shouldPrefixRewardName = hasCustomisation ||
                rewardType == "FREE_COFFEE" ||
                rewardType == "FREE_CAKE" ||
                rewardType == "CUSTOM_REWARD"

        return sourceProduct.copy(
            name = if (shouldPrefixRewardName) {
                "Reward: ${sourceProduct.name}"
            } else {
                sourceProduct.name
            },
            price = 0.0
        )
    }
}