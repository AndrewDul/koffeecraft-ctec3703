package uk.ac.dmu.koffeecraft.ui.admin.menu

data class AdminMenuProductFormData(
    val name: String,
    val description: String,
    val priceText: String,
    val productFamily: String,
    val rewardEnabled: Boolean,
    val isNew: Boolean,
    val imageKey: String?,
    val customImagePath: String?
)