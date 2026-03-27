package uk.ac.dmu.koffeecraft.data.repository

import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.util.rewards.BeansBoosterManager

data class CustomerRewardsScreenData(
    val beansBalance: Int,
    val reservedBeans: Int,
    val availableBeansForNewRewards: Int,
    val boosterProgress: Int,
    val pendingBoosters: Int,
    val rewardProducts: List<Product>
)

sealed interface CustomerRewardsActionResult {
    data class Success(val message: String) : CustomerRewardsActionResult
    data class Error(val message: String) : CustomerRewardsActionResult
}

sealed interface CustomerRewardChoiceResult {
    data object Allowed : CustomerRewardChoiceResult
    data class Error(val message: String) : CustomerRewardChoiceResult
}

class CustomerRewardsRepository(
    private val db: KoffeeCraftDatabase,
    private val cartRepository: CartRepository
) {

    suspend fun loadRewardsScreen(customerId: Long): CustomerRewardsScreenData? {
        val customer = db.customerDao().getById(customerId) ?: return null
        val rewardProducts = db.productDao().getRewardProducts()

        val reservedBeans = cartRepository.beansToSpend()
        val availableBeansForNewRewards = (customer.beansBalance - reservedBeans).coerceAtLeast(0)
        val progress = customer.beansBoosterProgress.coerceIn(
            0,
            BeansBoosterManager.BOOSTER_STEP - 1
        )
        val pendingBoosters = customer.pendingBeansBoosters.coerceAtLeast(0)

        return CustomerRewardsScreenData(
            beansBalance = customer.beansBalance,
            reservedBeans = reservedBeans,
            availableBeansForNewRewards = availableBeansForNewRewards,
            boosterProgress = progress,
            pendingBoosters = pendingBoosters,
            rewardProducts = rewardProducts
        )
    }

    suspend fun claimBeanBooster(customerId: Long): CustomerRewardsActionResult {
        val customer = db.customerDao().getById(customerId)
            ?: return CustomerRewardsActionResult.Error("Customer account could not be found.")

        if (customer.pendingBeansBoosters <= 0) {
            return CustomerRewardsActionResult.Error("You do not have a bean booster ready yet.")
        }

        db.customerDao().update(
            customer.copy(
                beansBalance = customer.beansBalance + BeansBoosterManager.BOOSTER_REWARD,
                pendingBeansBoosters = (customer.pendingBeansBoosters - 1).coerceAtLeast(0)
            )
        )

        return CustomerRewardsActionResult.Success("You claimed +5 beans.")
    }

    suspend fun validateRewardChoice(
        customerId: Long,
        category: String,
        beansCost: Int
    ): CustomerRewardChoiceResult {
        val customer = db.customerDao().getById(customerId)
            ?: return CustomerRewardChoiceResult.Error("Customer account could not be found.")

        val availableBeans = (customer.beansBalance - cartRepository.beansToSpend()).coerceAtLeast(0)
        if (availableBeans < beansCost) {
            return CustomerRewardChoiceResult.Error("You do not have enough beans for this reward.")
        }

        val options = db.productDao().getAvailableByCategory(category)
        if (options.isEmpty()) {
            return CustomerRewardChoiceResult.Error("No products are available in this category right now.")
        }

        return CustomerRewardChoiceResult.Allowed
    }

    suspend fun addPhysicalReward(
        customerId: Long,
        productName: String,
        beansCost: Int
    ): CustomerRewardsActionResult {
        val customer = db.customerDao().getById(customerId)
            ?: return CustomerRewardsActionResult.Error("Customer account could not be found.")

        val availableBeans = (customer.beansBalance - cartRepository.beansToSpend()).coerceAtLeast(0)
        if (availableBeans < beansCost) {
            return CustomerRewardsActionResult.Error("You do not have enough beans for this reward.")
        }

        val rewards = db.productDao().getRewardProducts()
        val selected = rewards.firstOrNull { it.name == productName }
            ?: return CustomerRewardsActionResult.Error("Reward product not found.")

        val added = cartRepository.addReward(
            sourceProduct = selected,
            rewardType = "PHYSICAL_REWARD",
            beansCostPerUnit = beansCost
        )

        return if (added) {
            CustomerRewardsActionResult.Success("Reward added to cart.")
        } else {
            CustomerRewardsActionResult.Success("That reward is already in your cart.")
        }
    }
}