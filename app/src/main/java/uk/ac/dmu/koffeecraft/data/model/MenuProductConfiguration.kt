package uk.ac.dmu.koffeecraft.data.model

import uk.ac.dmu.koffeecraft.data.entities.AddOn
import uk.ac.dmu.koffeecraft.data.entities.Allergen
import uk.ac.dmu.koffeecraft.data.entities.ProductOption

data class MenuProductConfiguration(
    val options: List<ProductOption> = emptyList(),
    val addOns: List<AddOn> = emptyList(),
    val baseAllergens: List<Allergen> = emptyList(),
    val addOnAllergens: Map<Long, List<Allergen>> = emptyMap(),
    val defaultOptionId: Long? = null
)