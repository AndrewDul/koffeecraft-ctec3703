package uk.ac.dmu.koffeecraft.ui.admin.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.data.entities.AddOn
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.entities.ProductOption
import uk.ac.dmu.koffeecraft.data.repository.AdminMenuRepository
import java.util.Locale

class AdminMenuViewModel(
    private val repository: AdminMenuRepository
) : ViewModel() {

    private val validator = AdminMenuProductValidator()

    private val _uiState = MutableStateFlow(AdminMenuUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AdminMenuUiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private val _productDetailsState = MutableStateFlow(AdminMenuProductDetailsUiState())
    val productDetailsState = _productDetailsState.asStateFlow()

    private val _productOptionsState = MutableStateFlow(AdminMenuProductOptionsUiState())
    val productOptionsState = _productOptionsState.asStateFlow()

    private val _productExtrasState = MutableStateFlow(AdminMenuProductExtrasUiState())
    val productExtrasState = _productExtrasState.asStateFlow()

    private val _extraAllergensState = MutableStateFlow(AdminMenuExtraAllergensUiState())
    val extraAllergensState = _extraAllergensState.asStateFlow()

    private val _productAllergenSelectionState =
        MutableStateFlow(AdminMenuProductAllergenSelectionUiState())
    val productAllergenSelectionState = _productAllergenSelectionState.asStateFlow()

    private val _addOnAllergenSelectionState =
        MutableStateFlow(AdminMenuAddOnAllergenSelectionUiState())
    val addOnAllergenSelectionState = _addOnAllergenSelectionState.asStateFlow()

    private val _allergenLibraryState = MutableStateFlow(AdminMenuAllergenLibraryUiState())
    val allergenLibraryState = _allergenLibraryState.asStateFlow()

    init {
        observeProducts()
    }

    private fun observeProducts() {
        viewModelScope.launch {
            repository.observeProducts().collectLatest { products ->
                _uiState.update { state ->
                    state.copy(
                        allProducts = products,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun setFilter(filter: AdminMenuCategoryFilter) {
        _uiState.update { state ->
            state.copy(currentFilter = filter)
        }
    }

    fun toggleProductActive(product: Product) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setProductActive(
                productId = product.productId,
                isActive = !product.isActive
            )

            _events.tryEmit(
                AdminMenuUiEvent.Message(
                    if (product.isActive) "Product disabled" else "Product enabled"
                )
            )
        }
    }

    fun archiveProduct(product: Product) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.archiveProduct(product.productId)
            _events.tryEmit(AdminMenuUiEvent.Message("Product archived"))
        }
    }

    fun restoreProduct(product: Product) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.restoreProduct(product.productId)
            _events.tryEmit(AdminMenuUiEvent.Message("Product restored"))
        }
    }

    fun saveProduct(
        existing: Product?,
        formData: AdminMenuProductFormData
    ) {
        val validation = validator.validate(formData)

        if (!validation.isValid) {
            _events.tryEmit(AdminMenuUiEvent.ProductValidationFailed(validation))
            return
        }

        val validatedPrice = validation.validatedPrice ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val normalizedName = formData.name.trim()
            val normalizedDescription = formData.description.trim()
            val normalizedFamily = formData.productFamily.trim()

            val savedProduct = if (existing == null) {
                repository.createProduct(
                    name = normalizedName,
                    productFamily = normalizedFamily,
                    description = normalizedDescription,
                    price = validatedPrice,
                    rewardEnabled = formData.rewardEnabled,
                    isNew = formData.isNew
                )
            } else {
                repository.updateProduct(
                    existing = existing,
                    name = normalizedName,
                    productFamily = normalizedFamily,
                    description = normalizedDescription,
                    price = validatedPrice,
                    rewardEnabled = formData.rewardEnabled,
                    isNew = formData.isNew
                )
            }

            if (savedProduct == null) {
                _events.tryEmit(AdminMenuUiEvent.Message("Unable to save product."))
            } else {
                _events.tryEmit(
                    AdminMenuUiEvent.ProductSaved(
                        product = savedProduct,
                        created = existing == null
                    )
                )
            }
        }
    }

    fun loadProductDetails(product: Product) {
        _productDetailsState.value = AdminMenuProductDetailsUiState(
            productId = product.productId,
            isLoading = true,
            optionsText = "Loading...",
            addOnsText = "Loading...",
            allergensText = "Loading..."
        )

        viewModelScope.launch(Dispatchers.IO) {
            val allergens = repository.getAllergensForProduct(product.productId)

            val optionsText: String
            val addOnsText: String

            if (product.isMerch) {
                optionsText = "Not applicable for merch products."
                addOnsText = "Not applicable for merch products."
            } else {
                val options = repository.getOptionsForProduct(product.productId)
                val assignedAddOns = repository.getAssignedAddOnsForProduct(product.productId)

                optionsText = if (options.isEmpty()) {
                    "No size / portion options configured yet."
                } else {
                    options.joinToString("\n\n") { option ->
                        buildString {
                            append("• ")
                            append(option.displayLabel)
                            append(" — ")
                            append(option.sizeValue)
                            append(option.sizeUnit.lowercase())
                            append("\n")
                            append(
                                String.format(
                                    Locale.UK,
                                    "  Extra price: £%.2f",
                                    option.extraPrice
                                )
                            )
                            append(" • ")
                            append(option.estimatedCalories)
                            append(" kcal")
                            if (option.isDefault) {
                                append(" • Default")
                            }
                        }
                    }
                }

                addOnsText = if (assignedAddOns.isEmpty()) {
                    "No extras assigned yet."
                } else {
                    assignedAddOns.joinToString("\n\n") { addOn ->
                        buildString {
                            append("• ")
                            append(addOn.name)
                            append("\n")
                            append(
                                String.format(
                                    Locale.UK,
                                    "  £%.2f",
                                    addOn.price
                                )
                            )
                            append(" • ")
                            append(addOn.estimatedCalories)
                            append(" kcal")
                            if (!addOn.isActive) {
                                append(" • Disabled")
                            }
                        }
                    }
                }
            }

            val allergensText = if (allergens.isEmpty()) {
                "No allergens listed."
            } else {
                allergens.joinToString(", ") { it.name }
            }

            _productDetailsState.value = AdminMenuProductDetailsUiState(
                productId = product.productId,
                isLoading = false,
                optionsText = optionsText,
                addOnsText = addOnsText,
                allergensText = allergensText
            )
        }
    }

    fun loadProductOptions(product: Product) {
        _productOptionsState.value = AdminMenuProductOptionsUiState(
            productId = product.productId,
            isLoading = true,
            options = emptyList()
        )

        refreshProductOptions(product.productId)
    }

    fun deleteProductOption(
        product: Product,
        option: ProductOption,
        onCompleted: (() -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteOptionById(option.optionId)
            refreshProductOptionsInternal(product.productId)
            _events.tryEmit(AdminMenuUiEvent.Message("Size option deleted."))

            withContext(Dispatchers.Main) {
                onCompleted?.invoke()
            }
        }
    }

    fun saveProductOption(
        product: Product,
        existing: ProductOption?,
        displayLabel: String,
        sizeValue: Int,
        sizeUnit: String,
        extraPrice: Double,
        estimatedCalories: Int,
        isDefault: Boolean,
        onCompleted: (() -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val optionName = existing?.optionName ?: toOptionKey(displayLabel)

            repository.saveProductOption(
                productId = product.productId,
                existing = existing,
                optionName = optionName,
                displayLabel = displayLabel,
                sizeValue = sizeValue,
                sizeUnit = sizeUnit,
                extraPrice = extraPrice,
                estimatedCalories = estimatedCalories,
                isDefault = isDefault
            )

            refreshProductOptionsInternal(product.productId)

            _events.tryEmit(
                AdminMenuUiEvent.Message(
                    if (existing == null) "Size option added." else "Size option updated."
                )
            )

            withContext(Dispatchers.Main) {
                onCompleted?.invoke()
            }
        }
    }

    fun loadProductExtras(product: Product) {
        _productExtrasState.value = AdminMenuProductExtrasUiState(
            productId = product.productId,
            isLoading = true,
            assignedExtras = emptyList(),
            libraryExtras = emptyList()
        )

        refreshProductExtras(product.productId, product.productFamily)
    }

    fun assignProductExtra(
        product: Product,
        addOn: AddOn,
        onCompleted: (() -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.assignAddOnToProduct(product.productId, addOn.addOnId)
            refreshProductExtrasInternal(product.productId, product.productFamily)

            _events.tryEmit(
                AdminMenuUiEvent.Message("\"${addOn.name}\" assigned to ${product.name}.")
            )

            withContext(Dispatchers.Main) {
                onCompleted?.invoke()
            }
        }
    }

    fun removeProductExtra(
        product: Product,
        addOn: AddOn,
        onCompleted: (() -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeAddOnFromProduct(product.productId, addOn.addOnId)
            refreshProductExtrasInternal(product.productId, product.productFamily)

            _events.tryEmit(
                AdminMenuUiEvent.Message("\"${addOn.name}\" removed from ${product.name}.")
            )

            withContext(Dispatchers.Main) {
                onCompleted?.invoke()
            }
        }
    }

    fun setProductExtraActive(
        product: Product,
        addOn: AddOn,
        isActive: Boolean,
        onCompleted: (() -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setAddOnActive(addOn.addOnId, isActive)
            refreshProductExtrasInternal(product.productId, product.productFamily)

            _events.tryEmit(
                AdminMenuUiEvent.Message(
                    if (isActive) "\"${addOn.name}\" enabled."
                    else "\"${addOn.name}\" disabled."
                )
            )

            withContext(Dispatchers.Main) {
                onCompleted?.invoke()
            }
        }
    }

    fun deleteProductExtraPermanently(
        product: Product,
        addOn: AddOn,
        onCompleted: (() -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAddOnPermanently(addOn.addOnId)
            refreshProductExtrasInternal(product.productId, product.productFamily)

            _events.tryEmit(
                AdminMenuUiEvent.Message("\"${addOn.name}\" deleted permanently.")
            )

            withContext(Dispatchers.Main) {
                onCompleted?.invoke()
            }
        }
    }

    fun saveProductExtra(
        category: String,
        existing: AddOn?,
        name: String,
        price: Double,
        estimatedCalories: Int,
        isActive: Boolean,
        product: Product,
        onCompleted: (() -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveAddOn(
                existing = existing,
                name = name,
                category = category,
                price = price,
                estimatedCalories = estimatedCalories,
                isActive = isActive
            )

            refreshProductExtrasInternal(product.productId, product.productFamily)

            _events.tryEmit(
                AdminMenuUiEvent.Message(
                    if (existing == null) "Extra created." else "Extra updated."
                )
            )

            withContext(Dispatchers.Main) {
                onCompleted?.invoke()
            }
        }
    }

    fun loadProductAllergenSelection(product: Product) {
        _productAllergenSelectionState.value = AdminMenuProductAllergenSelectionUiState(
            productId = product.productId,
            isLoading = true,
            allergens = emptyList(),
            selectedIds = emptySet()
        )

        viewModelScope.launch(Dispatchers.IO) {
            val allergens = repository.getAllAllergens()
            val selectedIds = repository.getSelectedProductAllergenIds(product.productId)

            _productAllergenSelectionState.value = AdminMenuProductAllergenSelectionUiState(
                productId = product.productId,
                isLoading = false,
                allergens = allergens,
                selectedIds = selectedIds
            )
        }
    }

    fun saveProductAllergenSelection(
        productId: Long,
        allergenIds: Set<Long>,
        onCompleted: (() -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.replaceProductAllergenSelection(
                productId = productId,
                allergenIds = allergenIds
            )

            _events.tryEmit(AdminMenuUiEvent.Message("Product allergens updated."))

            withContext(Dispatchers.Main) {
                onCompleted?.invoke()
            }
        }
    }

    fun loadExtraAllergenEntries(product: Product) {
        _extraAllergensState.value = AdminMenuExtraAllergensUiState(
            productId = product.productId,
            isLoading = true,
            entries = emptyList()
        )

        viewModelScope.launch(Dispatchers.IO) {
            val addOns = repository.getAssignedAddOnsForProduct(product.productId)
            val allergenMap = addOns.associateWith { addOn ->
                repository.getAllergensForAddOn(addOn.addOnId)
            }

            val entries = addOns.map { addOn ->
                val linkedAllergens = allergenMap[addOn].orEmpty()

                AdminMenuExtraAllergenEntryUiModel(
                    addOn = addOn,
                    linkedAllergensText = if (linkedAllergens.isEmpty()) {
                        "No allergens linked yet"
                    } else {
                        linkedAllergens.joinToString(", ") { it.name }
                    }
                )
            }

            _extraAllergensState.value = AdminMenuExtraAllergensUiState(
                productId = product.productId,
                isLoading = false,
                entries = entries
            )
        }
    }

    fun loadAddOnAllergenSelection(addOn: AddOn) {
        _addOnAllergenSelectionState.value = AdminMenuAddOnAllergenSelectionUiState(
            addOnId = addOn.addOnId,
            isLoading = true,
            allergens = emptyList(),
            selectedIds = emptySet()
        )

        viewModelScope.launch(Dispatchers.IO) {
            val allergens = repository.getAllAllergens()
            val selectedIds = repository.getSelectedAddOnAllergenIds(addOn.addOnId)

            _addOnAllergenSelectionState.value = AdminMenuAddOnAllergenSelectionUiState(
                addOnId = addOn.addOnId,
                isLoading = false,
                allergens = allergens,
                selectedIds = selectedIds
            )
        }
    }

    fun saveAddOnAllergenSelection(
        addOnId: Long,
        allergenIds: Set<Long>,
        onCompleted: (() -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.replaceAddOnAllergenSelection(
                addOnId = addOnId,
                allergenIds = allergenIds
            )

            _events.tryEmit(AdminMenuUiEvent.Message("Extra allergens updated."))

            withContext(Dispatchers.Main) {
                onCompleted?.invoke()
            }
        }
    }

    fun loadAllergenLibrary() {
        _allergenLibraryState.value = AdminMenuAllergenLibraryUiState(
            isLoading = true,
            allergens = emptyList()
        )

        viewModelScope.launch(Dispatchers.IO) {
            val allergens = repository.getAllAllergens()

            _allergenLibraryState.value = AdminMenuAllergenLibraryUiState(
                isLoading = false,
                allergens = allergens
            )
        }
    }

    fun createAllergen(
        name: String,
        onCompleted: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.createAllergen(name)

            if (result == -1L) {
                _events.tryEmit(AdminMenuUiEvent.Message("This allergen already exists."))

                withContext(Dispatchers.Main) {
                    onCompleted?.invoke(false)
                }
                return@launch
            }

            refreshAllergenLibraryInternal()
            _events.tryEmit(AdminMenuUiEvent.Message("Allergen added."))

            withContext(Dispatchers.Main) {
                onCompleted?.invoke(true)
            }
        }
    }

    private fun refreshProductOptions(productId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            refreshProductOptionsInternal(productId)
        }
    }

    private suspend fun refreshProductOptionsInternal(productId: Long) {
        val options = repository.getOptionsForProduct(productId)

        _productOptionsState.value = AdminMenuProductOptionsUiState(
            productId = productId,
            isLoading = false,
            options = options
        )
    }

    private fun refreshProductExtras(productId: Long, category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            refreshProductExtrasInternal(productId, category)
        }
    }

    private suspend fun refreshProductExtrasInternal(productId: Long, category: String) {
        val allAddOns = repository.getAllAddOnsByCategory(category)
        val assignedAddOns = repository.getAssignedAddOnsForProduct(productId)
        val assignedIds = assignedAddOns.map { it.addOnId }.toSet()
        val libraryAddOns = allAddOns.filterNot { it.addOnId in assignedIds }

        _productExtrasState.value = AdminMenuProductExtrasUiState(
            productId = productId,
            isLoading = false,
            assignedExtras = assignedAddOns,
            libraryExtras = libraryAddOns
        )
    }

    private suspend fun refreshAllergenLibraryInternal() {
        val allergens = repository.getAllAllergens()

        _allergenLibraryState.value = AdminMenuAllergenLibraryUiState(
            isLoading = false,
            allergens = allergens
        )
    }

    private fun toOptionKey(displayLabel: String): String {
        return displayLabel
            .trim()
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
    }
}