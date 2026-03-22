package uk.ac.dmu.koffeecraft.ui.menu

import uk.ac.dmu.koffeecraft.data.entities.AddOn
import uk.ac.dmu.koffeecraft.data.entities.Allergen
import uk.ac.dmu.koffeecraft.data.entities.ProductOption

data class ProductCardUiState(
    val options: List<ProductOption> = emptyList(),
    val addOns: List<AddOn> = emptyList(),
    val baseAllergens: List<Allergen> = emptyList(),
    val addOnAllergens: Map<Long, List<Allergen>> = emptyMap(),
    val selectedOptionId: Long? = null,
    val selectedAddOnIds: Set<Long> = emptySet(),
    val isLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val savePresetEnabled: Boolean = false
)