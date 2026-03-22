package uk.ac.dmu.koffeecraft.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.entities.AddOn
import uk.ac.dmu.koffeecraft.data.entities.Allergen
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.entities.ProductOption
import uk.ac.dmu.koffeecraft.data.repository.ProductCustomizationActionResult
import uk.ac.dmu.koffeecraft.data.repository.ProductCustomizationData
import uk.ac.dmu.koffeecraft.data.repository.ProductCustomizationRepository
import uk.ac.dmu.koffeecraft.data.session.SessionManager
import java.util.Locale

data class ProductCustomizationOptionUi(
    val optionId: Long,
    val label: String,
    val selected: Boolean
)

data class ProductCustomizationAddOnUi(
    val addOnId: Long,
    val label: String,
    val selected: Boolean
)

data class ProductCustomizationUiState(
    val isLoading: Boolean = false,
    val title: String = "",
    val description: String = "",
    val optionItems: List<ProductCustomizationOptionUi> = emptyList(),
    val addOnItems: List<ProductCustomizationAddOnUi> = emptyList(),
    val allergensText: String = "No allergens listed",
    val caloriesText: String = "0 kcal",
    val totalText: String = "£0.00",
    val savePresetVisible: Boolean = true,
    val savePresetEnabled: Boolean = false,
    val addToCartText: String = "Add to cart"
)

class ProductCustomizationViewModel(
    private val repository: ProductCustomizationRepository
) : ViewModel() {

    sealed interface UiEffect {
        data class ShowMessage(val message: String) : UiEffect
        data object Dismiss : UiEffect
    }

    private val _state = MutableStateFlow(ProductCustomizationUiState(isLoading = true))
    val state: StateFlow<ProductCustomizationUiState> = _state

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var rewardMode: Boolean = false
    private var rewardType: String? = null
    private var rewardBeansCost: Int = 0

    private var product: Product? = null
    private var options: List<ProductOption> = emptyList()
    private var addOns: List<AddOn> = emptyList()
    private var baseAllergens: List<Allergen> = emptyList()
    private var addOnAllergens: Map<Long, List<Allergen>> = emptyMap()

    private var selectedOption: ProductOption? = null
    private val selectedAddOnIds = mutableSetOf<Long>()

    fun start(
        productId: Long,
        rewardMode: Boolean,
        rewardType: String?,
        rewardBeansCost: Int
    ) {
        this.rewardMode = rewardMode
        this.rewardType = rewardType
        this.rewardBeansCost = rewardBeansCost

        _state.value = _state.value.copy(isLoading = true)

        viewModelScope.launch {
            val data = repository.loadData(productId)
            if (data == null) {
                _state.value = _state.value.copy(isLoading = false)
                _effects.send(UiEffect.ShowMessage("This product is no longer available."))
                _effects.send(UiEffect.Dismiss)
                return@launch
            }

            applyLoadedData(data)
            publishState()
        }
    }

    fun selectOption(optionId: Long) {
        selectedOption = options.firstOrNull { it.optionId == optionId } ?: return
        publishState()
    }

    fun toggleAddOn(addOnId: Long, isChecked: Boolean) {
        if (isChecked) {
            selectedAddOnIds.add(addOnId)
        } else {
            selectedAddOnIds.remove(addOnId)
        }
        publishState()
    }

    fun savePreset() {
        val customerId = SessionManager.currentCustomerId
        val safeProduct = product
        val safeOption = selectedOption

        if (customerId == null) {
            viewModelScope.launch {
                _effects.send(UiEffect.ShowMessage("Please sign in first."))
            }
            return
        }

        if (safeProduct == null || safeOption == null) {
            viewModelScope.launch {
                _effects.send(UiEffect.ShowMessage("Please choose a size first."))
            }
            return
        }

        val selectedAddOns = addOns.filter { selectedAddOnIds.contains(it.addOnId) }

        viewModelScope.launch {
            when (
                val result = repository.savePreset(
                    customerId = customerId,
                    product = safeProduct,
                    option = safeOption,
                    selectedAddOns = selectedAddOns,
                    rewardMode = rewardMode
                )
            ) {
                is ProductCustomizationActionResult.Success -> {
                    _effects.send(UiEffect.ShowMessage(result.message))
                }

                is ProductCustomizationActionResult.Error -> {
                    _effects.send(UiEffect.ShowMessage(result.message))
                }
            }
        }
    }

    fun addToCart() {
        val safeProduct = product
        val safeOption = selectedOption

        if (safeProduct == null || safeOption == null) {
            viewModelScope.launch {
                _effects.send(UiEffect.ShowMessage("Please choose a size first."))
            }
            return
        }

        val selectedAddOns = addOns.filter { selectedAddOnIds.contains(it.addOnId) }

        viewModelScope.launch {
            when (
                val result = repository.addToCart(
                    product = safeProduct,
                    option = safeOption,
                    selectedAddOns = selectedAddOns,
                    rewardMode = rewardMode,
                    rewardType = rewardType,
                    rewardBeansCost = rewardBeansCost
                )
            ) {
                is ProductCustomizationActionResult.Success -> {
                    _effects.send(UiEffect.ShowMessage(result.message))
                    _effects.send(UiEffect.Dismiss)
                }

                is ProductCustomizationActionResult.Error -> {
                    _effects.send(UiEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun applyLoadedData(data: ProductCustomizationData) {
        product = data.product
        options = data.options
        addOns = data.addOns
        baseAllergens = data.baseAllergens
        addOnAllergens = data.addOnAllergens
        selectedOption = options.firstOrNull { it.isDefault } ?: options.firstOrNull()
        selectedAddOnIds.clear()
    }

    private fun publishState() {
        val safeProduct = product
        val option = selectedOption

        if (safeProduct == null || option == null) {
            _state.value = ProductCustomizationUiState(isLoading = false)
            return
        }

        val selectedAddOns = addOns.filter { selectedAddOnIds.contains(it.addOnId) }

        val basePrice = if (rewardMode) 0.0 else safeProduct.price
        val totalPrice = basePrice + option.extraPrice + selectedAddOns.sumOf { it.price }
        val totalCalories = option.estimatedCalories + selectedAddOns.sumOf { it.estimatedCalories }

        val allergenNames = (
                baseAllergens.map { it.name } +
                        selectedAddOns.flatMap { addOn ->
                            addOnAllergens[addOn.addOnId].orEmpty().map { it.name }
                        }
                ).distinct().sorted()

        _state.value = ProductCustomizationUiState(
            isLoading = false,
            title = safeProduct.name,
            description = if (rewardMode) {
                if (safeProduct.description.isBlank()) {
                    "Base item included. You only pay for size upgrades and extras."
                } else {
                    "${safeProduct.description}\n\nBase item included. You only pay for size upgrades and extras."
                }
            } else {
                safeProduct.description
            },
            optionItems = options.map { currentOption ->
                ProductCustomizationOptionUi(
                    optionId = currentOption.optionId,
                    label = buildString {
                        append(currentOption.displayLabel)
                        append(" • ")
                        append(currentOption.sizeValue)
                        append(currentOption.sizeUnit.lowercase(Locale.UK))
                        if (currentOption.extraPrice > 0.0) {
                            append(" (+£")
                            append(String.format(Locale.UK, "%.2f", currentOption.extraPrice))
                            append(")")
                        }
                    },
                    selected = option.optionId == currentOption.optionId
                )
            },
            addOnItems = addOns.map { addOn ->
                ProductCustomizationAddOnUi(
                    addOnId = addOn.addOnId,
                    label = "${addOn.name} +£${String.format(Locale.UK, "%.2f", addOn.price)}",
                    selected = selectedAddOnIds.contains(addOn.addOnId)
                )
            },
            allergensText = if (allergenNames.isEmpty()) {
                "No allergens listed"
            } else {
                allergenNames.joinToString(", ")
            },
            caloriesText = "$totalCalories kcal",
            totalText = String.format(Locale.UK, "£%.2f", totalPrice),
            savePresetVisible = !rewardMode,
            savePresetEnabled = shouldEnableSavePresetButton(),
            addToCartText = if (rewardMode) "Add reward to cart" else "Add to cart"
        )
    }

    private fun shouldEnableSavePresetButton(): Boolean {
        val defaultOptionId =
            options.firstOrNull { it.isDefault }?.optionId ?: options.firstOrNull()?.optionId
        val selectedOptionId = selectedOption?.optionId

        val sizeChanged = selectedOptionId != null && selectedOptionId != defaultOptionId
        val hasAddOns = selectedAddOnIds.isNotEmpty()

        return SessionManager.currentCustomerId != null && (sizeChanged || hasAddOns)
    }

    class Factory(
        private val repository: ProductCustomizationRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProductCustomizationViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ProductCustomizationViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}