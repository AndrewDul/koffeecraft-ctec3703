package uk.ac.dmu.koffeecraft.ui.admin.menu

import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.entities.AddOn
import uk.ac.dmu.koffeecraft.data.entities.AddOnAllergenCrossRef
import uk.ac.dmu.koffeecraft.data.entities.Allergen
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.entities.ProductAllergenCrossRef
import uk.ac.dmu.koffeecraft.data.repository.AdminMenuRepository
import java.util.Locale

class AdminMenuAllergensController(
    private val fragment: Fragment,
    private val repository: AdminMenuRepository
) {

    fun showManageAllergensDialog(
        product: Product,
        onSaved: () -> Unit
    ) {
        val dialogView = fragment.layoutInflater.inflate(R.layout.dialog_manage_allergens_hub, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvManageAllergensTitle)
        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tvManageAllergensSubtitle)
        val btnProductAllergens = dialogView.findViewById<MaterialButton>(R.id.btnProductAllergens)
        val btnExtraAllergens = dialogView.findViewById<MaterialButton>(R.id.btnExtraAllergens)
        val btnAllergenLibrary = dialogView.findViewById<MaterialButton>(R.id.btnAllergenLibrary)

        tvTitle.text = "Manage allergens for ${product.name}"
        tvSubtitle.text = if (product.isMerch) {
            "This product can use direct allergen disclosures. Extra allergens are not used for merch items."
        } else {
            "Manage direct product allergens, review allergen links for extras, or open the shared allergen library."
        }

        if (product.isMerch) {
            btnExtraAllergens.isEnabled = false
            btnExtraAllergens.alpha = 0.55f
            btnExtraAllergens.text = "Extra allergens not used for merch"
        } else {
            btnExtraAllergens.isEnabled = true
            btnExtraAllergens.alpha = 1f
            btnExtraAllergens.text = "Manage extra allergens"
        }

        btnProductAllergens.setOnClickListener {
            showManageProductAllergensDialog(product, onSaved)
        }

        btnExtraAllergens.setOnClickListener {
            if (!product.isMerch) {
                showSelectExtraForAllergensDialog(product, onSaved)
            }
        }

        btnAllergenLibrary.setOnClickListener {
            showAllergenLibraryDialog(onSaved)
        }

        MaterialAlertDialogBuilder(fragment.requireContext())
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showManageProductAllergensDialog(
        product: Product,
        onSaved: () -> Unit
    ) {
        fragment.viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val allergens = repository.allergenDao().getAll()
            val selectedIds = repository.allergenDao().getProductAllergenIds(product.productId).toSet()

            withContext(Dispatchers.Main) {
                if (!fragment.isAdded) return@withContext

                showAllergenChecklistDialog(
                    title = "Product allergens",
                    subtitle = "Select the allergens that apply directly to ${product.name}.",
                    allergens = allergens,
                    selectedIds = selectedIds,
                    emptyMessage = "No allergens are available in the library yet. Add your first allergen to start linking product disclosures.",
                    onOpenLibrary = {
                        showAllergenLibraryDialog {
                            onSaved()
                            showManageProductAllergensDialog(product, onSaved)
                        }
                    },
                    onSaveSelection = { checkedIds ->
                        fragment.viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                            val refs = checkedIds.map { allergenId ->
                                ProductAllergenCrossRef(
                                    productId = product.productId,
                                    allergenId = allergenId
                                )
                            }

                            repository.allergenDao().deleteProductRefs(product.productId)
                            if (refs.isNotEmpty()) {
                                repository.allergenDao().insertProductRefs(refs)
                            }

                            withContext(Dispatchers.Main) {
                                if (!fragment.isAdded) return@withContext
                                Toast.makeText(
                                    fragment.requireContext(),
                                    "Product allergens updated.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onSaved()
                            }
                        }
                    }
                )
            }
        }
    }

    private fun showSelectExtraForAllergensDialog(
        product: Product,
        onSaved: () -> Unit
    ) {
        val dialogView = fragment.layoutInflater.inflate(R.layout.dialog_select_extra_for_allergens, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvExtraAllergensTitle)
        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tvExtraAllergensSubtitle)
        val btnOpenLibrary = dialogView.findViewById<MaterialButton>(R.id.btnOpenAllergenLibraryForExtras)
        val tvEmpty = dialogView.findViewById<TextView>(R.id.tvExtraAllergensEmpty)
        val container = dialogView.findViewById<LinearLayout>(R.id.containerExtraAllergens)

        tvTitle.text = "Extra allergens for ${product.name}"
        tvSubtitle.text = "Choose an assigned extra and manage the allergen disclosures linked to it."

        fun refreshExtraAllergenContent() {
            fragment.viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val addOns = repository.addOnDao().getAssignedForProduct(product.productId)
                val allergenMap = addOns.associateWith { addOn ->
                    repository.allergenDao().getForAddOn(addOn.addOnId)
                }

                withContext(Dispatchers.Main) {
                    if (!fragment.isAdded) return@withContext

                    container.removeAllViews()
                    tvEmpty.visibility = if (addOns.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE

                    addOns.forEach { addOn ->
                        val linkedAllergens = allergenMap[addOn].orEmpty()

                        val itemView = LayoutInflater.from(fragment.requireContext())
                            .inflate(R.layout.item_admin_extra_allergen_entry, container, false)

                        val tvExtraName = itemView.findViewById<TextView>(R.id.tvExtraAllergenName)
                        val tvExtraMeta = itemView.findViewById<TextView>(R.id.tvExtraAllergenMeta)
                        val tvExtraState = itemView.findViewById<TextView>(R.id.tvExtraAllergenState)
                        val btnManage = itemView.findViewById<MaterialButton>(R.id.btnManageExtraAllergens)

                        tvExtraName.text = addOn.name
                        tvExtraMeta.text = String.format(
                            Locale.UK,
                            "£%.2f • %d kcal • %s",
                            addOn.price,
                            addOn.estimatedCalories,
                            if (addOn.isActive) "Active" else "Disabled"
                        )
                        tvExtraState.text = if (linkedAllergens.isEmpty()) {
                            "No allergens linked yet"
                        } else {
                            linkedAllergens.joinToString(", ") { it.name }
                        }

                        btnManage.setOnClickListener {
                            showManageAddOnAllergensDialog(addOn) {
                                refreshExtraAllergenContent()
                                onSaved()
                            }
                        }

                        container.addView(itemView)
                    }
                }
            }
        }

        btnOpenLibrary.setOnClickListener {
            showAllergenLibraryDialog {
                refreshExtraAllergenContent()
                onSaved()
            }
        }

        val dialog = MaterialAlertDialogBuilder(fragment.requireContext())
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .create()

        dialog.setOnShowListener {
            refreshExtraAllergenContent()
        }

        dialog.show()
    }

    private fun showManageAddOnAllergensDialog(
        addOn: AddOn,
        onSaved: () -> Unit
    ) {
        fragment.viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val allergens = repository.allergenDao().getAll()
            val selectedIds = repository.allergenDao().getAddOnAllergenIds(addOn.addOnId).toSet()

            withContext(Dispatchers.Main) {
                if (!fragment.isAdded) return@withContext

                showAllergenChecklistDialog(
                    title = "${addOn.name} allergens",
                    subtitle = "Select the allergens that apply to this extra.",
                    allergens = allergens,
                    selectedIds = selectedIds,
                    emptyMessage = "No allergens are available in the library yet. Add your first allergen before linking it to this extra.",
                    onOpenLibrary = {
                        showAllergenLibraryDialog {
                            onSaved()
                            showManageAddOnAllergensDialog(addOn, onSaved)
                        }
                    },
                    onSaveSelection = { checkedIds ->
                        fragment.viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                            val refs = checkedIds.map { allergenId ->
                                AddOnAllergenCrossRef(
                                    addOnId = addOn.addOnId,
                                    allergenId = allergenId
                                )
                            }

                            repository.allergenDao().deleteAddOnRefs(addOn.addOnId)
                            if (refs.isNotEmpty()) {
                                repository.allergenDao().insertAddOnRefs(refs)
                            }

                            withContext(Dispatchers.Main) {
                                if (!fragment.isAdded) return@withContext
                                Toast.makeText(
                                    fragment.requireContext(),
                                    "Extra allergens updated.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onSaved()
                            }
                        }
                    }
                )
            }
        }
    }

    private fun showAllergenChecklistDialog(
        title: String,
        subtitle: String,
        allergens: List<Allergen>,
        selectedIds: Set<Long>,
        emptyMessage: String,
        onOpenLibrary: () -> Unit,
        onSaveSelection: (Set<Long>) -> Unit
    ) {
        val dialogView = fragment.layoutInflater.inflate(R.layout.dialog_manage_allergen_selection, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvAllergenSelectionTitle)
        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tvAllergenSelectionSubtitle)
        val btnOpenLibrary = dialogView.findViewById<MaterialButton>(R.id.btnOpenAllergenLibrary)
        val tvEmpty = dialogView.findViewById<TextView>(R.id.tvAllergenSelectionEmpty)
        val container = dialogView.findViewById<LinearLayout>(R.id.containerAllergenSelection)

        tvTitle.text = title
        tvSubtitle.text = subtitle
        tvEmpty.text = emptyMessage

        val checkedIds = selectedIds.toMutableSet()

        btnOpenLibrary.setOnClickListener {
            onOpenLibrary()
        }

        container.removeAllViews()
        tvEmpty.visibility = if (allergens.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE

        allergens.forEach { allergen ->
            val itemView = LayoutInflater.from(fragment.requireContext())
                .inflate(R.layout.item_admin_allergen_toggle, container, false)

            val tvName = itemView.findViewById<TextView>(R.id.tvAllergenName)
            val tvMeta = itemView.findViewById<TextView>(R.id.tvAllergenMeta)
            val cbAssigned = itemView.findViewById<CheckBox>(R.id.cbAllergenAssigned)

            val isSelected = checkedIds.contains(allergen.allergenId)

            tvName.text = allergen.name
            tvMeta.text = if (isSelected) {
                "Currently linked"
            } else {
                "Available in allergen library"
            }

            cbAssigned.isChecked = isSelected
            cbAssigned.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    checkedIds.add(allergen.allergenId)
                    tvMeta.text = "Currently linked"
                } else {
                    checkedIds.remove(allergen.allergenId)
                    tvMeta.text = "Available in allergen library"
                }
            }

            itemView.setOnClickListener {
                cbAssigned.isChecked = !cbAssigned.isChecked
            }

            container.addView(itemView)
        }

        MaterialAlertDialogBuilder(fragment.requireContext())
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                onSaveSelection(checkedIds.toSet())
            }
            .show()
    }

    private fun showAllergenLibraryDialog(
        onSaved: () -> Unit
    ) {
        val dialogView = fragment.layoutInflater.inflate(R.layout.dialog_allergen_library, null)

        val btnAddAllergen = dialogView.findViewById<MaterialButton>(R.id.btnAddAllergenFromLibrary)
        val tvEmpty = dialogView.findViewById<TextView>(R.id.tvAllergenLibraryEmpty)
        val container = dialogView.findViewById<LinearLayout>(R.id.containerAllergenLibrary)

        fun refreshLibraryContent() {
            fragment.viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val allergens = repository.allergenDao().getAll()

                withContext(Dispatchers.Main) {
                    if (!fragment.isAdded) return@withContext

                    container.removeAllViews()
                    tvEmpty.visibility = if (allergens.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE

                    allergens.forEach { allergen ->
                        val itemView = LayoutInflater.from(fragment.requireContext())
                            .inflate(R.layout.item_admin_allergen_library, container, false)

                        val tvName = itemView.findViewById<TextView>(R.id.tvLibraryAllergenName)
                        val tvMeta = itemView.findViewById<TextView>(R.id.tvLibraryAllergenMeta)

                        tvName.text = allergen.name
                        tvMeta.text = "Available in the shared allergen library"

                        container.addView(itemView)
                    }
                }
            }
        }

        btnAddAllergen.setOnClickListener {
            showAddAllergenDialog {
                refreshLibraryContent()
                onSaved()
            }
        }

        val dialog = MaterialAlertDialogBuilder(fragment.requireContext())
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .create()

        dialog.setOnShowListener {
            refreshLibraryContent()
        }

        dialog.show()
    }

    private fun showAddAllergenDialog(
        onSaved: () -> Unit
    ) {
        val dialogView = fragment.layoutInflater.inflate(R.layout.dialog_allergen_form, null)
        val tilAllergenName = dialogView.findViewById<TextInputLayout>(R.id.tilAllergenName)
        val etAllergenName = dialogView.findViewById<TextInputEditText>(R.id.etAllergenName)

        val dialog = MaterialAlertDialogBuilder(fragment.requireContext())
            .setTitle("Add allergen")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                tilAllergenName.error = null

                val name = etAllergenName.text?.toString()?.trim().orEmpty()

                if (name.isBlank()) {
                    tilAllergenName.error = "Enter allergen name"
                    return@setOnClickListener
                }

                fragment.viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    val result = repository.allergenDao().insert(Allergen(name = name))

                    withContext(Dispatchers.Main) {
                        if (!fragment.isAdded) return@withContext

                        if (result == -1L) {
                            Toast.makeText(
                                fragment.requireContext(),
                                "This allergen already exists.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                fragment.requireContext(),
                                "Allergen added.",
                                Toast.LENGTH_SHORT
                            ).show()
                            dialog.dismiss()
                            onSaved()
                        }
                    }
                }
            }
        }

        dialog.show()
    }
}