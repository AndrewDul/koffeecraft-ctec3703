package uk.ac.dmu.koffeecraft.data.repository

import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.querymodel.HomeLovedProductInsight
import uk.ac.dmu.koffeecraft.data.querymodel.HomeRatedProductInsight
data class CustomerHomeScreenData(
    val beansBalance: Int,
    val beansBoosterProgress: Int,
    val pendingBeansBoosters: Int,
    val rewardProducts: List<Product>,
    val newProducts: List<Product>,
    val topCoffees: List<HomeRatedProductInsight>,
    val topCakes: List<HomeRatedProductInsight>,
    val mostLovedProducts: List<HomeLovedProductInsight>
)

class CustomerHomeRepository(
    private val db: KoffeeCraftDatabase
) {

    suspend fun loadHomeContent(customerId: Long): CustomerHomeScreenData? {
        val customer = db.customerDao().getById(customerId) ?: return null

        val rewardProducts = db.productDao().getRewardProducts()
        val newProducts = db.productDao().getActiveNewProducts()

        val topCoffees = db.feedbackDao().getTopRatedProductsByFamily(
            productFamily = "COFFEE",
            minimumRatings = 1,
            limit = 3
        )

        val topCakes = db.feedbackDao().getTopRatedProductsByFamily(
            productFamily = "CAKE",
            minimumRatings = 1,
            limit = 3
        )

        val mostLovedProducts = db.favouriteDao().getMostLovedProducts(limit = 5)

        return CustomerHomeScreenData(
            beansBalance = customer.beansBalance,
            beansBoosterProgress = customer.beansBoosterProgress,
            pendingBeansBoosters = customer.pendingBeansBoosters,
            rewardProducts = rewardProducts,
            newProducts = newProducts,
            topCoffees = topCoffees,
            topCakes = topCakes,
            mostLovedProducts = mostLovedProducts
        )
    }
}