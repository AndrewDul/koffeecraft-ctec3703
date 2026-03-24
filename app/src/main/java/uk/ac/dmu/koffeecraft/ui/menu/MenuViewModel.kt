package uk.ac.dmu.koffeecraft.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.model.MenuProductConfiguration
import uk.ac.dmu.koffeecraft.data.repository.MenuRepository
import uk.ac.dmu.koffeecraft.data.session.SessionRepository

class MenuViewModel(
    private val menuRepository: MenuRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    data class UiState(
        val category: String = "COFFEE",
        val products: List<Product> = emptyList(),
        val favouriteIds: Set<Long> = emptySet(),
        val cardStates: Map<Long, ProductCardUiState> = emptyMap(),
        val expandedProductId: Long? = null
    )

    sealed interface UiEffect {
        data class ShowMessage(val message: String) : UiEffect
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var categoryJob: Job? = null
    private var favouritesJob: Job? = null

    fun setCategory(category: String) {
        _state.value = _state.value.copy(
            category = category,
            expandedProductId = null
        )
        observeCategory(category)
    }

    fun start() {
        observeCategory(_state.value.category)
        observeFavourites()
    }

    fun toggleExpand(productId: Long) {
        val previousExpanded = _state.value.expandedProductId
        val newExpanded = if (previousExpanded == productId) null else productId

        _state.value = _state.value.copy(expandedProductId = newExpanded)

        if (newExpanded != null) {
            ensureCardStateLoaded(newExpanded)
        }
    }

    fun onFavouriteToggle(product: Product, shouldFavourite: Boolean) {
        val customerId = sessionRepository.currentCustomerId
        if (customerId == null) {
            sendMessage("Please sign in first.")
            return
        }

        viewModelScope.launch {
            if (shouldFavourite) {
                menuRepository.addFavourite(customerId, product.productId)
            } else {
                menuRepository.removeFavourite(customerId, product.productId)
            }
        }
    }

    fun onOptionSelected(productId: Long, optionId: Long) {
        val current = _state.value.cardStates[productId] ?: return
        val updated = current.copy(
            selectedOptionId = optionId
        )

        updateCardState(productId, updated)
    }

    fun onAddOnToggled(productId: Long, addOnId: Long, checked: Boolean) {
        val current = _state.value.cardStates[productId] ?: return
        val newIds = current.selectedAddOnIds.toMutableSet()

        if (checked) {
            newIds.add(addOnId)
        } else {
            newIds.remove(addOnId)
        }

        val updated = current.copy(
            selectedAddOnIds = newIds
        )

        updateCardState(productId, updated)
    }

    fun onSavePreset(product: Product) {
        val customerId = sessionRepository.currentCustomerId
        if (customerId == null) {
            sendMessage("Please sign in first.")
            return
        }

        val state = _state.value.cardStates[product.productId] ?: return
        val option = state.options.firstOrNull { it.optionId == state.selectedOptionId } ?: return

        if (!state.savePresetEnabled) return

        val selectedAddOns = state.addOns.filter { it.addOnId in state.selectedAddOnIds }

        viewModelScope.launch {
            menuRepository.saveFavouritePreset(
                customerId = customerId,
                product = product,
                option = option,
                selectedAddOns = selectedAddOns
            )
            sendMessage("Favourite combo saved.")
        }
    }

    fun onAddToCart(product: Product) {
        if (!product.isActive) {
            sendMessage("This product is currently unavailable.")
            return
        }

        val state = _state.value.cardStates[product.productId]
        if (state == null) {
            sendMessage("Product configuration is still loading.")
            return
        }

        val option = state.options.firstOrNull { it.optionId == state.selectedOptionId }
        if (option == null) {
            sendMessage("This product has no size configured yet. Please add a size in Admin Menu.")
            return
        }

        val selectedAddOns = state.addOns.filter { it.addOnId in state.selectedAddOnIds }

        menuRepository.addConfiguredProductToCart(
            product = product,
            option = option,
            selectedAddOns = selectedAddOns
        )

        sendMessage("Product added to cart.")
    }

    private fun observeCategory(category: String) {
        categoryJob?.cancel()
        categoryJob = viewModelScope.launch {
            menuRepository.observeProductsByCategory(category).collect { products ->
                val validIds = products.map { it.productId }.toSet()
                val filteredStates = _state.value.cardStates.filterKeys { it in validIds }

                _state.value = _state.value.copy(
                    products = products,
                    cardStates = filteredStates,
                    expandedProductId = _state.value.expandedProductId?.takeIf { it in validIds }
                )

                products.forEach { product ->
                    ensureCardStateLoaded(product.productId)
                }
            }
        }
    }

    private fun observeFavourites() {
        favouritesJob?.cancel()

        val customerId = sessionRepository.currentCustomerId
        if (customerId == null) {
            _state.value = _state.value.copy(favouriteIds = emptySet())
            return
        }

        favouritesJob = viewModelScope.launch {
            menuRepository.observeFavouriteProductIds(customerId).collect { ids ->
                _state.value = _state.value.copy(favouriteIds = ids.toSet())
            }
        }
    }

    private fun ensureCardStateLoaded(productId: Long) {
        val current = _state.value.cardStates[productId]
        if (current?.isLoaded == true || current?.isLoading == true) return

        val loadingState = (current ?: ProductCardUiState()).copy(isLoading = true)
        _state.value = _state.value.copy(
            cardStates = _state.value.cardStates + (productId to loadingState)
        )

        viewModelScope.launch {
            val configuration = menuRepository.loadProductConfiguration(productId)
            val loaded = configuration.toUiState()
            val currentProduct = _state.value.products.firstOrNull { it.productId == productId }

            val finalState = loaded.copy(
                selectedAddOnIds = emptySet(),
                savePresetEnabled = currentProduct?.let {
                    shouldEnableSavePresetButton(
                        product = it,
                        state = loaded
                    )
                } ?: false
            )

            _state.value = _state.value.copy(
                cardStates = _state.value.cardStates + (productId to finalState)
            )
        }
    }

    private fun updateCardState(productId: Long, newState: ProductCardUiState) {
        val product = _state.value.products.firstOrNull { it.productId == productId } ?: return

        val resolvedState = newState.copy(
            savePresetEnabled = shouldEnableSavePresetButton(product, newState)
        )

        _state.value = _state.value.copy(
            cardStates = _state.value.cardStates + (productId to resolvedState)
        )
    }

    private fun shouldEnableSavePresetButton(
        product: Product,
        state: ProductCardUiState
    ): Boolean {
        if (!product.isActive) return false
        if (sessionRepository.currentCustomerId == null) return false

        val defaultOptionId =
            state.options.firstOrNull { it.isDefault }?.optionId ?: state.options.firstOrNull()?.optionId

        val sizeChanged = state.selectedOptionId != null && state.selectedOptionId != defaultOptionId
        val hasAddOns = state.selectedAddOnIds.isNotEmpty()

        return sizeChanged || hasAddOns
    }

    private fun sendMessage(message: String) {
        viewModelScope.launch {
            _effects.send(UiEffect.ShowMessage(message))
        }
    }

    private fun MenuProductConfiguration.toUiState(): ProductCardUiState {
        return ProductCardUiState(
            options = options,
            addOns = addOns,
            baseAllergens = baseAllergens,
            addOnAllergens = addOnAllergens,
            selectedOptionId = defaultOptionId,
            selectedAddOnIds = emptySet(),
            isLoaded = true,
            isLoading = false,
            savePresetEnabled = false
        )
    }
}