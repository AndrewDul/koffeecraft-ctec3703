package uk.ac.dmu.koffeecraft.ui.admin.menu

import uk.ac.dmu.koffeecraft.data.entities.AddOn

data class AdminMenuExtraAllergenEntryUiModel(
    val addOn: AddOn,
    val linkedAllergensText: String
)