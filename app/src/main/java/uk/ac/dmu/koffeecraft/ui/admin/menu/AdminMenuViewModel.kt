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
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.repository.AdminMenuRepository

class AdminMenuViewModel(
    private val repository: AdminMenuRepository
) : ViewModel() {

    private val validator = AdminMenuProductValidator()

    private val _uiState = MutableStateFlow(AdminMenuUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AdminMenuUiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

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
}