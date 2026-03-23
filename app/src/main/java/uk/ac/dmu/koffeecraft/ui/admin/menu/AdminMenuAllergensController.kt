package uk.ac.dmu.koffeecraft.ui.admin.menu

import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.entities.AddOn
import uk.ac.dmu.koffeecraft.data.entities.Allergen
import uk.ac.dmu.koffeecraft.data.entities.Product
import java.util.Locale

class AdminMenuAllergensController(
    private val fragment: Fragment,
    private val viewModel: AdminMenuViewModel
) {

    private data class PendingProductSelection(
        val product: Product,
        val onSaved: () -> Unit
    )

    private data class PendingAddOnSelection(
        val addOn: AddOn,
        val onSaved: () -> Unit
    )

    private var isObserving = false

    private var activeExtraProduct: Product? = null
    private var activeExtraOnSaved: (() -> Unit)? = null
    private var tvExtraEmpty: TextView? = null
    private var containerExtraAllergens: LinearLayout? = null

    private var activeLibraryOnSaved: (() -> Unit)? = null
    private var tvLibraryEmpty: TextView? = null
    private var containerLibrary: LinearLayout? = null

    private var pendingProductSelection: PendingProductSelection? = null
    private var pendingAddOnSelection: PendingAddOnSelection? = null

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

        ensureObserver()

        MaterialAlertDialogBuilder(fragment.requireContext())
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun ensureObserver() {
        if (isObserving) return
        isObserving = true

        fragment.viewLifecycleOwner.lifecycleScope.launch {
            fragment.viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.extraAllergensState.collect { state ->
                        val product = activeExtraProduct ?: return@collect
                        if (state.productId != product.productId) return@collect

                        renderExtraAllergenEntries(
                            product = product,
                            entries = state.entries,
                            isLoading = state.isLoading
                        )
                    }
                }

                launch {
                    viewModel.allergenLibraryState.collect { state ->
                        renderAllergenLibrary(
                            allergens = state.allergens,
                            isLoading = state.isLoading
                        )
                    }
                }

                launch {
                    viewModel.productAllergenSelectionState.collect { state ->
                        val pending = pendingProductSelection ?: return@collect
                        if (state.productId != pending.product.productId || state.isLoading) {
                            return@collect
                        }

                        pendingProductSelection = null

                        showAllergenChecklistDialog(
                            title = "Product allergens",
                            subtitle = "Select the allergens that apply directly to ${pending.product.name}.",
                            allergens = state.allergens,
                            selectedIds = state.selectedIds,
                            emptyMessage = "No allergens are available in the library yet. Add your first allergen to start linking product disclosures.",
                            onOpenLibrary = {
                                showAllergenLibraryDialog {
                                    pending.onSaved()
                                    showManageProductAllergensDialog(
                                        product = pending.product,
                                        onSaved = pending.onSaved
                                    )
                                }
                            },
                            onSaveSelection = { checkedIds ->
                                viewModel.saveProductAllergenSelection(
                                    productId = pending.product.productId,
                                    allergenIds = checkedIds
                                ) {
                                    pending.onSaved()
                                }
                            }
                        )
                    }
                }

                launch {
                    viewModel.addOnAllergenSelectionState.collect { state ->
                        val pending = pendingAddOnSelection ?: return@collect
                        if (state.addOnId != pending.addOn.addOnId || state.isLoading) {
                            return@collect
                        }

                        pendingAddOnSelection = null

                        showAllergenChecklistDialog(
                            title = "${pending.addOn.name} allergens",
                            subtitle = "Select the allergens that apply to this extra.",
                            allergens = state.allergens,
                            selectedIds = state.selectedIds,
                            emptyMessage = "No allergens are available in the library yet. Add your first allergen before linking it to this extra.",
                            onOpenLibrary = {
                                showAllergenLibraryDialog {
                                    pending.onSaved()
                                    showManageAddOnAllergensDialog(
                                        addOn = pending.addOn,
                                        onSaved = pending.onSaved
                                    )
                                }
                            },
                            onSaveSelection = { checkedIds ->
                                viewModel.saveAddOnAllergenSelection(
                                    addOnId = pending.addOn.addOnId,
                                    allergenIds = checkedIds
                                ) {
                                    pending.onSaved()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun showManageProductAllergensDialog(
        product: Product,
        onSaved: () -> Unit
    ) {
        pendingProductSelection = PendingProductSelection(
            product = product,
            onSaved = onSaved
        )
        viewModel.loadProductAllergenSelection(product)
    }

    private fun showSelectExtraForAllergensDialog(
        product: Product,
        onSaved: () -> Unit
    ) {
        val dialogView =
            fragment.layoutInflater.inflate(R.layout.dialog_select_extra_for_allergens, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvExtraAllergensTitle)
        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tvExtraAllergensSubtitle)
        val btnOpenLibrary =
            dialogView.findViewById<MaterialButton>(R.id.btnOpenAllergenLibraryForExtras)
        val localTvEmpty = dialogView.findViewById<TextView>(R.id.tvExtraAllergensEmpty)
        val localContainer =
            dialogView.findViewById<LinearLayout>(R.id.containerExtraAllergens)

        activeExtraProduct = product
        activeExtraOnSaved = onSaved
        tvExtraEmpty = localTvEmpty
        containerExtraAllergens = localContainer

        tvTitle.text = "Extra allergens for ${product.name}"
        tvSubtitle.text = "Choose an assigned extra and manage the allergen disclosures linked to it."

        btnOpenLibrary.setOnClickListener {
            showAllergenLibraryDialog {
                onSaved()
                viewModel.loadExtraAllergenEntries(product)
            }
        }

        val dialog = MaterialAlertDialogBuilder(fragment.requireContext())
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .create()

        dialog.setOnShowListener {
            bindExtraAllergensLoadingState()
            viewModel.loadExtraAllergenEntries(product)
        }

        dialog.setOnDismissListener {
            clearExtraAllergenRefs()
        }

        ensureObserver()
        dialog.show()
    }

    private fun renderExtraAllergenEntries(
        product: Product,
        entries: List<AdminMenuExtraAllergenEntryUiModel>,
        isLoading: Boolean
    ) {
        val emptyView = tvExtraEmpty ?: return
        val container = containerExtraAllergens ?: return

        container.removeAllViews()

        if (isLoading) {
            emptyView.visibility = View.VISIBLE
            emptyView.text = "Loading..."
            return
        }

        emptyView.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        emptyView.text = "No extras assigned yet."

        entries.forEach { entry ->
            val addOn = entry.addOn

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
            tvExtraState.text = entry.linkedAllergensText

            btnManage.setOnClickListener {
                showManageAddOnAllergensDialog(addOn) {
                    activeExtraOnSaved?.invoke()
                    viewModel.loadExtraAllergenEntries(product)
                }
            }

            container.addView(itemView)
        }
    }

    private fun showManageAddOnAllergensDialog(
        addOn: AddOn,
        onSaved: () -> Unit
    ) {
        pendingAddOnSelection = PendingAddOnSelection(
            addOn = addOn,
            onSaved = onSaved
        )
        viewModel.loadAddOnAllergenSelection(addOn)
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
        val dialogView =
            fragment.layoutInflater.inflate(R.layout.dialog_manage_allergen_selection, null)

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
        tvEmpty.visibility = if (allergens.isEmpty()) View.VISIBLE else View.GONE

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

        val btnAddAllergen =
            dialogView.findViewById<MaterialButton>(R.id.btnAddAllergenFromLibrary)
        val localTvEmpty = dialogView.findViewById<TextView>(R.id.tvAllergenLibraryEmpty)
        val localContainer =
            dialogView.findViewById<LinearLayout>(R.id.containerAllergenLibrary)

        activeLibraryOnSaved = onSaved
        tvLibraryEmpty = localTvEmpty
        containerLibrary = localContainer

        btnAddAllergen.setOnClickListener {
            showAddAllergenDialog {
                activeLibraryOnSaved?.invoke()
                viewModel.loadAllergenLibrary()
            }
        }

        val dialog = MaterialAlertDialogBuilder(fragment.requireContext())
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .create()

        dialog.setOnShowListener {
            bindAllergenLibraryLoadingState()
            viewModel.loadAllergenLibrary()
        }

        dialog.setOnDismissListener {
            clearLibraryRefs()
        }

        ensureObserver()
        dialog.show()
    }

    private fun renderAllergenLibrary(
        allergens: List<Allergen>,
        isLoading: Boolean
    ) {
        val emptyView = tvLibraryEmpty ?: return
        val container = containerLibrary ?: return

        container.removeAllViews()

        if (isLoading) {
            emptyView.visibility = View.VISIBLE
            emptyView.text = "Loading..."
            return
        }

        emptyView.visibility = if (allergens.isEmpty()) View.VISIBLE else View.GONE

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

                viewModel.createAllergen(name) { created ->
                    if (created) {
                        dialog.dismiss()
                        onSaved()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun bindExtraAllergensLoadingState() {
        tvExtraEmpty?.visibility = View.VISIBLE
        tvExtraEmpty?.text = "Loading..."
        containerExtraAllergens?.removeAllViews()
    }

    private fun bindAllergenLibraryLoadingState() {
        tvLibraryEmpty?.visibility = View.VISIBLE
        tvLibraryEmpty?.text = "Loading..."
        containerLibrary?.removeAllViews()
    }

    private fun clearExtraAllergenRefs() {
        activeExtraProduct = null
        activeExtraOnSaved = null
        tvExtraEmpty = null
        containerExtraAllergens = null
    }

    private fun clearLibraryRefs() {
        activeLibraryOnSaved = null
        tvLibraryEmpty = null
        containerLibrary = null
    }
}