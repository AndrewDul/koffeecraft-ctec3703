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
import uk.ac.dmu.koffeecraft.data.entities.Product
import java.util.Locale

class AdminMenuExtrasController(
    private val fragment: Fragment,
    private val viewModel: AdminMenuViewModel
) {

    private var isObserving = false
    private var activeProduct: Product? = null
    private var onSavedCallback: (() -> Unit)? = null

    private var tvAssignedEmpty: TextView? = null
    private var tvLibraryEmpty: TextView? = null
    private var containerAssignedExtras: LinearLayout? = null
    private var containerLibraryExtras: LinearLayout? = null

    fun showManageExtrasDialog(
        product: Product,
        onSaved: () -> Unit
    ) {
        if (!product.isCoffee && !product.isCake) {
            Toast.makeText(
                fragment.requireContext(),
                "Extras are only available for coffee and cake products.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val dialogView = fragment.layoutInflater.inflate(R.layout.dialog_manage_extras, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvManageExtrasTitle)
        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tvManageExtrasSubtitle)
        val btnCreateExtra = dialogView.findViewById<MaterialButton>(R.id.btnCreateExtra)
        val localTvAssignedEmpty = dialogView.findViewById<TextView>(R.id.tvAssignedEmpty)
        val localTvLibraryEmpty = dialogView.findViewById<TextView>(R.id.tvLibraryEmpty)
        val localContainerAssignedExtras =
            dialogView.findViewById<LinearLayout>(R.id.containerAssignedExtras)
        val localContainerLibraryExtras =
            dialogView.findViewById<LinearLayout>(R.id.containerLibraryExtras)

        activeProduct = product
        onSavedCallback = onSaved
        tvAssignedEmpty = localTvAssignedEmpty
        tvLibraryEmpty = localTvLibraryEmpty
        containerAssignedExtras = localContainerAssignedExtras
        containerLibraryExtras = localContainerLibraryExtras

        tvTitle.text = "Manage extras for ${product.name}"
        tvSubtitle.text = if (product.isCoffee) {
            "Keep coffee extras organised, assign only what this drink should offer, and manage the shared library professionally."
        } else {
            "Keep cake extras organised, assign only what this cake should offer, and manage the shared library professionally."
        }

        btnCreateExtra.setOnClickListener {
            showAddOnFormDialog(
                product = product,
                category = product.productFamily,
                existing = null
            )
        }

        val dialog = MaterialAlertDialogBuilder(fragment.requireContext())
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .create()

        dialog.setOnShowListener {
            bindLoadingState()
            viewModel.loadProductExtras(product)
        }

        dialog.setOnDismissListener {
            clearDialogRefs()
        }

        ensureObserver()
        dialog.show()
    }

    private fun ensureObserver() {
        if (isObserving) return
        isObserving = true

        fragment.viewLifecycleOwner.lifecycleScope.launch {
            fragment.viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.productExtrasState.collect { state ->
                    val currentProduct = activeProduct ?: return@collect
                    if (state.productId != currentProduct.productId) return@collect

                    renderExtras(
                        product = currentProduct,
                        assignedExtras = state.assignedExtras,
                        libraryExtras = state.libraryExtras,
                        isLoading = state.isLoading
                    )
                }
            }
        }
    }

    private fun renderExtras(
        product: Product,
        assignedExtras: List<AddOn>,
        libraryExtras: List<AddOn>,
        isLoading: Boolean
    ) {
        val assignedContainer = containerAssignedExtras ?: return
        val libraryContainer = containerLibraryExtras ?: return
        val assignedEmpty = tvAssignedEmpty ?: return
        val libraryEmpty = tvLibraryEmpty ?: return

        assignedContainer.removeAllViews()
        libraryContainer.removeAllViews()

        if (isLoading) {
            assignedEmpty.visibility = View.VISIBLE
            libraryEmpty.visibility = View.VISIBLE
            assignedEmpty.text = "Loading..."
            libraryEmpty.text = "Loading..."
            return
        }

        assignedEmpty.visibility = if (assignedExtras.isEmpty()) View.VISIBLE else View.GONE
        libraryEmpty.visibility = if (libraryExtras.isEmpty()) View.VISIBLE else View.GONE

        assignedEmpty.text = "No extras assigned yet."
        libraryEmpty.text = "No extras available in the library."

        assignedExtras.forEach { addOn ->
            val stateText = if (addOn.isActive) {
                "Assigned • Active"
            } else {
                "Assigned • Disabled in library"
            }

            addManageExtraCard(
                container = assignedContainer,
                addOn = addOn,
                stateText = stateText,
                primaryText = "Remove",
                onPrimary = {
                    viewModel.removeProductExtra(product, addOn) {
                        onSavedCallback?.invoke()
                    }
                },
                secondaryText = "Edit",
                onSecondary = {
                    showAddOnFormDialog(
                        product = product,
                        category = addOn.category,
                        existing = addOn
                    )
                },
                tertiaryText = if (addOn.isActive) "Disable" else "Enable",
                onTertiary = {
                    viewModel.setProductExtraActive(
                        product = product,
                        addOn = addOn,
                        isActive = !addOn.isActive
                    ) {
                        onSavedCallback?.invoke()
                    }
                }
            )
        }

        libraryExtras.forEach { addOn ->
            if (addOn.isActive) {
                addManageExtraCard(
                    container = libraryContainer,
                    addOn = addOn,
                    stateText = "Available in library",
                    primaryText = "Assign",
                    onPrimary = {
                        viewModel.assignProductExtra(product, addOn) {
                            onSavedCallback?.invoke()
                        }
                    },
                    secondaryText = "Edit",
                    onSecondary = {
                        showAddOnFormDialog(
                            product = product,
                            category = addOn.category,
                            existing = addOn
                        )
                    },
                    tertiaryText = "Disable",
                    onTertiary = {
                        viewModel.setProductExtraActive(
                            product = product,
                            addOn = addOn,
                            isActive = false
                        ) {
                            onSavedCallback?.invoke()
                        }
                    }
                )
            } else {
                addManageExtraCard(
                    container = libraryContainer,
                    addOn = addOn,
                    stateText = "Disabled in library",
                    primaryText = "Enable",
                    onPrimary = {
                        viewModel.setProductExtraActive(
                            product = product,
                            addOn = addOn,
                            isActive = true
                        ) {
                            onSavedCallback?.invoke()
                        }
                    },
                    secondaryText = "Edit",
                    onSecondary = {
                        showAddOnFormDialog(
                            product = product,
                            category = addOn.category,
                            existing = addOn
                        )
                    },
                    tertiaryText = "Delete",
                    onTertiary = {
                        MaterialAlertDialogBuilder(fragment.requireContext())
                            .setTitle("Delete this extra permanently?")
                            .setMessage(
                                "This will permanently remove \"${addOn.name}\" from the library and remove its links to products and allergens. Use enable if you may need it again later."
                            )
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Delete") { _, _ ->
                                viewModel.deleteProductExtraPermanently(product, addOn) {
                                    onSavedCallback?.invoke()
                                }
                            }
                            .show()
                    }
                )
            }
        }
    }

    private fun addManageExtraCard(
        container: LinearLayout,
        addOn: AddOn,
        stateText: String,
        primaryText: String,
        onPrimary: () -> Unit,
        secondaryText: String,
        onSecondary: () -> Unit,
        tertiaryText: String?,
        onTertiary: (() -> Unit)?
    ) {
        val itemView = LayoutInflater.from(fragment.requireContext())
            .inflate(R.layout.item_admin_manage_extra, container, false)

        val tvExtraName = itemView.findViewById<TextView>(R.id.tvExtraName)
        val tvExtraMeta = itemView.findViewById<TextView>(R.id.tvExtraMeta)
        val tvExtraState = itemView.findViewById<TextView>(R.id.tvExtraState)
        val btnPrimaryAction = itemView.findViewById<MaterialButton>(R.id.btnPrimaryAction)
        val btnSecondaryAction = itemView.findViewById<MaterialButton>(R.id.btnSecondaryAction)
        val btnTertiaryAction = itemView.findViewById<MaterialButton>(R.id.btnTertiaryAction)

        tvExtraName.text = addOn.name
        tvExtraMeta.text = String.format(
            Locale.UK,
            "£%.2f • %d kcal",
            addOn.price,
            addOn.estimatedCalories
        )
        tvExtraState.text = stateText

        btnPrimaryAction.text = primaryText
        btnPrimaryAction.setOnClickListener { onPrimary() }

        btnSecondaryAction.text = secondaryText
        btnSecondaryAction.setOnClickListener { onSecondary() }

        if (tertiaryText.isNullOrBlank() || onTertiary == null) {
            btnTertiaryAction.visibility = View.GONE
        } else {
            btnTertiaryAction.visibility = View.VISIBLE
            btnTertiaryAction.text = tertiaryText
            btnTertiaryAction.setOnClickListener { onTertiary() }
        }

        container.addView(itemView)
    }

    private fun showAddOnFormDialog(
        product: Product,
        category: String,
        existing: AddOn?
    ) {
        val dialogView = fragment.layoutInflater.inflate(R.layout.dialog_add_on_form, null)

        val tilName = dialogView.findViewById<TextInputLayout>(R.id.tilAddOnName)
        val tilPrice = dialogView.findViewById<TextInputLayout>(R.id.tilAddOnPrice)
        val tilCalories = dialogView.findViewById<TextInputLayout>(R.id.tilAddOnCalories)

        val etName = dialogView.findViewById<TextInputEditText>(R.id.etAddOnName)
        val etPrice = dialogView.findViewById<TextInputEditText>(R.id.etAddOnPrice)
        val etCalories = dialogView.findViewById<TextInputEditText>(R.id.etAddOnCalories)
        val cbActive = dialogView.findViewById<CheckBox>(R.id.cbAddOnActive)

        if (existing != null) {
            etName.setText(existing.name)
            etPrice.setText(existing.price.toString())
            etCalories.setText(existing.estimatedCalories.toString())
            cbActive.isChecked = existing.isActive
        } else {
            cbActive.isChecked = true
            etPrice.setText("0.30")
            etCalories.setText("25")
        }

        val title = if (existing == null) {
            when (category.uppercase(Locale.UK)) {
                "COFFEE" -> "Create coffee extra"
                "CAKE" -> "Create cake extra"
                else -> "Create extra"
            }
        } else {
            "Edit extra"
        }

        val dialog = MaterialAlertDialogBuilder(fragment.requireContext())
            .setTitle(title)
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton(if (existing == null) "Save extra" else "Save changes", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                tilName.error = null
                tilPrice.error = null
                tilCalories.error = null

                val name = etName.text?.toString()?.trim().orEmpty()
                val price = etPrice.text?.toString()?.trim().orEmpty().toDoubleOrNull()
                val calories = etCalories.text?.toString()?.trim().orEmpty().toIntOrNull()
                val isActive = cbActive.isChecked

                var hasError = false

                if (name.isBlank()) {
                    tilName.error = "Enter the name of the add-on"
                    hasError = true
                }

                if (price == null || price < 0.0) {
                    tilPrice.error = "Enter a valid add-on price"
                    hasError = true
                }

                if (calories == null || calories < 0) {
                    tilCalories.error = "Enter valid calories"
                    hasError = true
                }

                if (hasError) return@setOnClickListener

                val validatedPrice = price ?: return@setOnClickListener
                val validatedCalories = calories ?: return@setOnClickListener

                viewModel.saveProductExtra(
                    category = category,
                    existing = existing,
                    name = name,
                    price = validatedPrice,
                    estimatedCalories = validatedCalories,
                    isActive = isActive,
                    product = product
                ) {
                    dialog.dismiss()
                    onSavedCallback?.invoke()
                }
            }
        }

        dialog.show()
    }

    private fun bindLoadingState() {
        tvAssignedEmpty?.visibility = View.VISIBLE
        tvLibraryEmpty?.visibility = View.VISIBLE
        tvAssignedEmpty?.text = "Loading..."
        tvLibraryEmpty?.text = "Loading..."
        containerAssignedExtras?.removeAllViews()
        containerLibraryExtras?.removeAllViews()
    }

    private fun clearDialogRefs() {
        activeProduct = null
        onSavedCallback = null
        tvAssignedEmpty = null
        tvLibraryEmpty = null
        containerAssignedExtras = null
        containerLibraryExtras = null
    }
}