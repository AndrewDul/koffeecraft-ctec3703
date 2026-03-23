package uk.ac.dmu.koffeecraft.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.repository.CustomerHomeRepository
import uk.ac.dmu.koffeecraft.data.repository.CustomerHomeScreenData
import uk.ac.dmu.koffeecraft.data.session.SessionRepository
import uk.ac.dmu.koffeecraft.util.rewards.BeansBoosterManager
import java.util.Locale

data class CustomerHomeUiState(
    val beansValue: String = "0 beans",
    val beansMeta: String = "Bean booster progress",
    val beansProgressValue: String = "0/10",
    val beansProgress: Int = 0,
    val rewardItems: List<CustomerHomeCarouselItem> = emptyList(),
    val newArrivalItems: List<CustomerHomeCarouselItem> = emptyList(),
    val topCoffeeItems: List<CustomerHomeCarouselItem> = emptyList(),
    val topCakeItems: List<CustomerHomeCarouselItem> = emptyList(),
    val mostLovedItems: List<CustomerHomeCarouselItem> = emptyList()
)

class CustomerHomeViewModel(
    private val repository: CustomerHomeRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CustomerHomeUiState())
    val state: StateFlow<CustomerHomeUiState> = _state

    fun refresh() {
        val customerId = sessionRepository.currentCustomerId ?: return

        viewModelScope.launch {
            val data = repository.loadHomeContent(customerId) ?: return@launch
            _state.value = mapToUiState(data)
        }
    }

    private fun mapToUiState(data: CustomerHomeScreenData): CustomerHomeUiState {
        val boosterProgress = data.beansBoosterProgress.coerceIn(0, 9)

        return CustomerHomeUiState(
            beansValue = "${data.beansBalance} beans",
            beansMeta = "Bean booster progress",
            beansProgressValue = if (data.pendingBeansBoosters > 0) {
                "${data.pendingBeansBoosters} ready • ${boosterProgress}/10"
            } else {
                "${boosterProgress}/10"
            },
            beansProgress = boosterProgress,
            rewardItems = buildRewardPreviewItems(
                beansBalance = data.beansBalance,
                beansBoosterProgress = data.beansBoosterProgress,
                pendingBeansBoosters = data.pendingBeansBoosters,
                rewardProducts = data.rewardProducts
            ),
            newArrivalItems = data.newProducts.map { product ->
                CustomerHomeCarouselItem(
                    title = product.name,
                    subtitle = product.description.ifBlank {
                        "Freshly added to the KoffeeCraft collection."
                    },
                    metaLine = if (product.isMerch) {
                        if (product.rewardEnabled) "Reward item" else "Merch item"
                    } else {
                        String.format(Locale.UK, "From £%.2f", product.price)
                    },
                    badgeLabel = "NEW"
                )
            },
            topCoffeeItems = data.topCoffees.map { item ->
                CustomerHomeCarouselItem(
                    title = item.productName,
                    subtitle = item.productDescription.ifBlank { "Top-rated crafted coffee." },
                    metaLine = String.format(
                        Locale.UK,
                        "★ %.1f • %d ratings • From £%.2f",
                        item.averageRating,
                        item.ratingCount,
                        item.price
                    ),
                    badgeLabel = "COFFEE"
                )
            },
            topCakeItems = data.topCakes.map { item ->
                CustomerHomeCarouselItem(
                    title = item.productName,
                    subtitle = item.productDescription.ifBlank { "Top-rated crafted cake." },
                    metaLine = String.format(
                        Locale.UK,
                        "★ %.1f • %d ratings • From £%.2f",
                        item.averageRating,
                        item.ratingCount,
                        item.price
                    ),
                    badgeLabel = "CAKE"
                )
            },
            mostLovedItems = data.mostLovedProducts.map { item ->
                CustomerHomeCarouselItem(
                    title = item.productName,
                    subtitle = item.productDescription.ifBlank {
                        "Customer favourite across the KoffeeCraft menu."
                    },
                    metaLine = String.format(
                        Locale.UK,
                        "♥ %d favourites • From £%.2f",
                        item.favouriteCount,
                        item.price
                    ),
                    badgeLabel = when {
                        item.productFamily.equals("COFFEE", ignoreCase = true) -> "COFFEE"
                        item.productFamily.equals("CAKE", ignoreCase = true) -> "CAKE"
                        else -> "LOVED"
                    }
                )
            }
        )
    }

    private fun buildRewardPreviewItems(
        beansBalance: Int,
        beansBoosterProgress: Int,
        pendingBeansBoosters: Int,
        rewardProducts: List<uk.ac.dmu.koffeecraft.data.entities.Product>
    ): List<CustomerHomeCarouselItem> {
        val items = mutableListOf<CustomerHomeCarouselItem>()

        items += CustomerHomeCarouselItem(
            title = "5 Bean Booster",
            subtitle = "Every 10 earned beans unlock a +5 bean booster.",
            metaLine = BeansBoosterManager.rewardMetaLine(
                beansBoosterProgress,
                pendingBeansBoosters
            ),
            badgeLabel = if (pendingBeansBoosters > 0) "READY" else "REWARD"
        )

        items += CustomerHomeCarouselItem(
            title = "Free Coffee",
            subtitle = "Redeem a crafted coffee from the rewards screen.",
            metaLine = "15 beans",
            badgeLabel = if (beansBalance >= 15) "AVAILABLE" else "REWARD"
        )

        items += CustomerHomeCarouselItem(
            title = "Free Cake",
            subtitle = "Redeem a crafted cake from the rewards screen.",
            metaLine = "18 beans",
            badgeLabel = if (beansBalance >= 18) "AVAILABLE" else "REWARD"
        )

        rewardProducts.forEach { product ->
            val beansCost = when (product.name) {
                "KoffeeCraft Mug" -> 125
                "KoffeeCraft Teddy Bear" -> 250
                "1kg Crafted Coffee Beans" -> 370
                else -> 0
            }

            items += CustomerHomeCarouselItem(
                title = product.name,
                subtitle = product.description.ifBlank {
                    "Special reward item available in the rewards screen."
                },
                metaLine = if (beansCost > 0) "$beansCost beans" else "Reward item",
                badgeLabel = if (beansCost > 0 && beansBalance >= beansCost) {
                    "AVAILABLE"
                } else {
                    "REWARD"
                }
            )
        }

        return items
    }

    class Factory(
        private val repository: CustomerHomeRepository,
        private val sessionRepository: SessionRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CustomerHomeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CustomerHomeViewModel(repository, sessionRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}