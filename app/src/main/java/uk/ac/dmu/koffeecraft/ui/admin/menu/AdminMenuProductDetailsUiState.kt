package uk.ac.dmu.koffeecraft.ui.admin.menu

data class AdminMenuProductDetailsUiState(
    val productId: Long? = null,
    val isLoading: Boolean = false,
    val optionsText: String = "",
    val addOnsText: String = "",
    val allergensText: String = ""
)