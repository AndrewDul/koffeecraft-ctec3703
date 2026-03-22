package uk.ac.dmu.koffeecraft.ui.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.repository.CustomerRewardChoiceResult
import uk.ac.dmu.koffeecraft.data.repository.CustomerRewardsActionResult
import uk.ac.dmu.koffeecraft.data.repository.CustomerRewardsRepository
import uk.ac.dmu.koffeecraft.data.repository.CustomerRewardsScreenData
import uk.ac.dmu.koffeecraft.util.rewards.BeansBoosterManager

data class CustomerRewardsUiState(
    val isLoading: Boolean = false,
    val beansCountValue: String = "0",
    val beansSubtitle: String = "Available now: 0 • Reserved in cart: 0",
    val boosterProgress: Int = 0,
    val boosterProgressText: String = "",
    val rewards: List<RewardUiModel> = emptyList()
)

class CustomerRewardsViewModel(
    private val repository: CustomerRewardsRepository
) : ViewModel() {

    sealed interface UiEffect {
        data class ShowMessage(val message: String) : UiEffect
        data class OpenRewardPicker(
            val category: String,
            val rewardType: String,
            val beansCost: Int
        ) : UiEffect
    }

    private val _state = MutableStateFlow(CustomerRewardsUiState())
    val state: StateFlow<CustomerRewardsUiState> = _state

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var customerId: Long? = null

    fun start(customerId: Long) {
        this.customerId = customerId
        refreshRewards()
    }

    fun refreshRewards() {
        val safeCustomerId = customerId ?: return

        _state.value = _state.value.copy(isLoading = true)

        viewModelScope.launch {
            val data = repository.loadRewardsScreen(safeCustomerId)
            if (data == null) {
                _state.value = _state.value.copy(isLoading = false)
                _effects.send(UiEffect.ShowMessage("Customer account could not be found."))
                return@launch
            }

            _state.value = mapToUiState(data)
        }
    }

    fun handleRewardAction(reward: RewardUiModel) {
        val safeCustomerId = customerId ?: return

        when (reward.id) {
            "BEAN_BOOSTER" -> {
                viewModelScope.launch {
                    when (val result = repository.claimBeanBooster(safeCustomerId)) {
                        is CustomerRewardsActionResult.Success -> {
                            _effects.send(UiEffect.ShowMessage(result.message))
                            refreshRewards()
                        }

                        is CustomerRewardsActionResult.Error -> {
                            _effects.send(UiEffect.ShowMessage(result.message))
                        }
                    }
                }
            }

            "FREE_COFFEE" -> openRewardChoice(
                customerId = safeCustomerId,
                category = "COFFEE",
                rewardType = "FREE_COFFEE",
                beansCost = 15
            )

            "FREE_CAKE" -> openRewardChoice(
                customerId = safeCustomerId,
                category = "CAKE",
                rewardType = "FREE_CAKE",
                beansCost = 18
            )

            "MUG" -> addPhysicalReward(
                customerId = safeCustomerId,
                productName = "KoffeeCraft Mug",
                beansCost = 125
            )

            "TEDDY" -> addPhysicalReward(
                customerId = safeCustomerId,
                productName = "KoffeeCraft Teddy Bear",
                beansCost = 250
            )

            "BEANS_1KG" -> addPhysicalReward(
                customerId = safeCustomerId,
                productName = "1kg Crafted Coffee Beans",
                beansCost = 370
            )
        }
    }

    private fun openRewardChoice(
        customerId: Long,
        category: String,
        rewardType: String,
        beansCost: Int
    ) {
        viewModelScope.launch {
            when (val result = repository.validateRewardChoice(customerId, category, beansCost)) {
                CustomerRewardChoiceResult.Allowed -> {
                    _effects.send(
                        UiEffect.OpenRewardPicker(
                            category = category,
                            rewardType = rewardType,
                            beansCost = beansCost
                        )
                    )
                }

                is CustomerRewardChoiceResult.Error -> {
                    _effects.send(UiEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun addPhysicalReward(
        customerId: Long,
        productName: String,
        beansCost: Int
    ) {
        viewModelScope.launch {
            when (val result = repository.addPhysicalReward(customerId, productName, beansCost)) {
                is CustomerRewardsActionResult.Success -> {
                    _effects.send(UiEffect.ShowMessage(result.message))
                    refreshRewards()
                }

                is CustomerRewardsActionResult.Error -> {
                    _effects.send(UiEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun mapToUiState(data: CustomerRewardsScreenData): CustomerRewardsUiState {
        return CustomerRewardsUiState(
            isLoading = false,
            beansCountValue = data.beansBalance.toString(),
            beansSubtitle = "Available now: ${data.availableBeansForNewRewards} • Reserved in cart: ${data.reservedBeans}",
            boosterProgress = data.boosterProgress,
            boosterProgressText = BeansBoosterManager.progressStatusText(
                data.boosterProgress,
                data.pendingBoosters
            ),
            rewards = buildRewardItems(data)
        )
    }

    private fun buildRewardItems(data: CustomerRewardsScreenData): List<RewardUiModel> {
        val items = mutableListOf<RewardUiModel>()

        items += RewardUiModel(
            id = "BEAN_BOOSTER",
            title = "5 Bean Booster",
            description = "Every 10 earned beans unlock a +5 bean booster.",
            beansLabel = BeansBoosterManager.rewardMetaLine(data.boosterProgress, data.pendingBoosters),
            actionLabel = if (data.pendingBoosters > 0) "Claim +5 beans" else "Keep collecting beans",
            enabled = data.pendingBoosters > 0
        )

        items += RewardUiModel(
            id = "FREE_COFFEE",
            title = "Free Coffee",
            description = "Choose one crafted coffee reward.",
            beansLabel = "15 beans",
            actionLabel = if (data.availableBeansForNewRewards >= 15) "Choose reward" else "Need more beans",
            enabled = data.availableBeansForNewRewards >= 15
        )

        items += RewardUiModel(
            id = "FREE_CAKE",
            title = "Free Cake",
            description = "Choose one crafted cake reward.",
            beansLabel = "18 beans",
            actionLabel = if (data.availableBeansForNewRewards >= 18) "Choose reward" else "Need more beans",
            enabled = data.availableBeansForNewRewards >= 18
        )

        if (data.rewardProductNames.contains("KoffeeCraft Mug")) {
            items += RewardUiModel(
                id = "MUG",
                title = "KoffeeCraft Mug",
                description = "Premium crafted mug with KoffeeCraft branding.",
                beansLabel = "125 beans",
                actionLabel = if (data.availableBeansForNewRewards >= 125) "Add to cart" else "Need more beans",
                enabled = data.availableBeansForNewRewards >= 125
            )
        }

        if (data.rewardProductNames.contains("KoffeeCraft Teddy Bear")) {
            items += RewardUiModel(
                id = "TEDDY",
                title = "KoffeeCraft Teddy Bear",
                description = "Soft teddy bear with KoffeeCraft branding.",
                beansLabel = "250 beans",
                actionLabel = if (data.availableBeansForNewRewards >= 250) "Add to cart" else "Need more beans",
                enabled = data.availableBeansForNewRewards >= 250
            )
        }

        if (data.rewardProductNames.contains("1kg Crafted Coffee Beans")) {
            items += RewardUiModel(
                id = "BEANS_1KG",
                title = "1kg Crafted Coffee Beans",
                description = "One kilogram of crafted KoffeeCraft coffee beans.",
                beansLabel = "370 beans",
                actionLabel = if (data.availableBeansForNewRewards >= 370) "Add to cart" else "Need more beans",
                enabled = data.availableBeansForNewRewards >= 370
            )
        }

        return items
    }

    class Factory(
        private val repository: CustomerRewardsRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CustomerRewardsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CustomerRewardsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}