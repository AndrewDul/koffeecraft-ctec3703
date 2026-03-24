package uk.ac.dmu.koffeecraft.ui.admin.menu

import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
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
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.entities.ProductOption

class AdminMenuOptionsController(
    private val fragment: Fragment,
    private val viewModel: AdminMenuViewModel
) {

    private var isObserving = false
    private var activeProduct: Product? = null

    private var tvSizesEmpty: TextView? = null
    private var containerSizeOptions: LinearLayout? = null

    fun showManageSizesDialog(
        product: Product,
        onSaved: () -> Unit
    ) {
        val dialogView = fragment.layoutInflater.inflate(R.layout.dialog_manage_sizes, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvManageSizesTitle)
        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tvManageSizesSubtitle)
        val btnAddSizeOption = dialogView.findViewById<MaterialButton>(R.id.btnAddSizeOption)
        val localTvSizesEmpty = dialogView.findViewById<TextView>(R.id.tvSizesEmpty)
        val localContainerSizeOptions =
            dialogView.findViewById<LinearLayout>(R.id.containerSizeOptions)

        activeProduct = product
        tvSizesEmpty = localTvSizesEmpty
        containerSizeOptions = localContainerSizeOptions

        tvTitle.text = "Manage sizes for ${product.name}"
        tvSubtitle.text = if (product.isCoffee) {
            "Review, edit, and add drink size options for this product."
        } else {
            "Review, edit, and add portion options for this product."
        }

        btnAddSizeOption.setOnClickListener {
            showOptionFormDialog(product, null, onSaved)
        }

        val dialog = MaterialAlertDialogBuilder(fragment.requireContext())
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .create()

        dialog.setOnShowListener {
            localTvSizesEmpty.visibility = View.GONE
            localTvSizesEmpty.text = "Loading..."
            viewModel.loadProductOptions(product)
        }

        dialog.setOnDismissListener {
            clearDialogRefs()
        }

        ensureObserver(onSaved)
        dialog.show()
    }

    private fun ensureObserver(onSaved: () -> Unit) {
        if (isObserving) return
        isObserving = true

        fragment.viewLifecycleOwner.lifecycleScope.launch {
            fragment.viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.productOptionsState.collect { state ->
                    val currentProduct = activeProduct ?: return@collect
                    if (state.productId != currentProduct.productId) return@collect

                    renderOptions(
                        product = currentProduct,
                        options = state.options,
                        isLoading = state.isLoading,
                        onSaved = onSaved
                    )
                }
            }
        }
    }

    private fun renderOptions(
        product: Product,
        options: List<ProductOption>,
        isLoading: Boolean,
        onSaved: () -> Unit
    ) {
        val container = containerSizeOptions ?: return
        val emptyView = tvSizesEmpty ?: return

        container.removeAllViews()

        if (isLoading) {
            emptyView.visibility = View.VISIBLE
            emptyView.text = "Loading..."
            return
        }

        if (options.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            emptyView.text = "No size options added yet."
            return
        }

        emptyView.visibility = View.GONE

        options.forEach { option ->
            val itemView = LayoutInflater.from(fragment.requireContext())
                .inflate(R.layout.item_admin_size_option, container, false)

            val tvSizeName = itemView.findViewById<TextView>(R.id.tvSizeName)
            val tvSizeMeta = itemView.findViewById<TextView>(R.id.tvSizeMeta)
            val tvSizeState = itemView.findViewById<TextView>(R.id.tvSizeState)
            val btnEditSize = itemView.findViewById<MaterialButton>(R.id.btnEditSize)
            val btnDeleteSize = itemView.findViewById<MaterialButton>(R.id.btnDeleteSize)

            tvSizeName.text = option.displayLabel
            tvSizeMeta.text = buildString {
                append(option.sizeValue)
                append(" ")
                append(option.sizeUnit.lowercase())
                append(" • ")
                append(
                    if (option.extraPrice > 0.0) {
                        "+£%.2f".format(option.extraPrice)
                    } else {
                        "Included"
                    }
                )
                append(" • ")
                append(option.estimatedCalories)
                append(" kcal")
            }

            tvSizeState.text = if (option.isDefault) {
                "Default option"
            } else {
                "Optional size"
            }

            btnEditSize.setOnClickListener {
                showOptionFormDialog(product, option, onSaved)
            }

            btnDeleteSize.setOnClickListener {
                MaterialAlertDialogBuilder(fragment.requireContext())
                    .setTitle("Delete size option?")
                    .setMessage("This will remove \"${option.displayLabel}\" from ${product.name}.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteProductOption(product, option) {
                            onSaved()
                        }
                    }
                    .show()
            }

            container.addView(itemView)
        }
    }

    private fun showOptionFormDialog(
        product: Product,
        existing: ProductOption?,
        onSaved: () -> Unit
    ) {
        val dialogView = fragment.layoutInflater.inflate(R.layout.dialog_product_option_form, null)

        val tilDisplayLabel = dialogView.findViewById<TextInputLayout>(R.id.tilDisplayLabel)
        val tilSizeValue = dialogView.findViewById<TextInputLayout>(R.id.tilSizeValue)
        val tilExtraPrice = dialogView.findViewById<TextInputLayout>(R.id.tilExtraPrice)
        val tilCalories = dialogView.findViewById<TextInputLayout>(R.id.tilCalories)

        val etDisplayLabel = dialogView.findViewById<TextInputEditText>(R.id.etDisplayLabel)
        val etSizeValue = dialogView.findViewById<TextInputEditText>(R.id.etSizeValue)
        val etExtraPrice = dialogView.findViewById<TextInputEditText>(R.id.etExtraPrice)
        val etCalories = dialogView.findViewById<TextInputEditText>(R.id.etCalories)
        val tvUnitHint = dialogView.findViewById<TextView>(R.id.tvUnitHint)
        val cbIsDefault = dialogView.findViewById<CheckBox>(R.id.cbIsDefault)

        val unit = if (product.isCoffee) "ML" else "G"
        tvUnitHint.text = if (product.isCoffee) {
            "Use millilitres for drink size."
        } else {
            "Use grams for cake portion weight."
        }

        tilDisplayLabel.hint = if (product.isCoffee) {
            "Size name"
        } else {
            "Portion name"
        }

        tilSizeValue.hint = if (product.isCoffee) {
            "Drink size in ml"
        } else {
            "Portion weight in g"
        }

        if (existing != null) {
            etDisplayLabel.setText(existing.displayLabel)
            etSizeValue.setText(existing.sizeValue.toString())
            etExtraPrice.setText(existing.extraPrice.toString())
            etCalories.setText(existing.estimatedCalories.toString())
            cbIsDefault.isChecked = existing.isDefault
        }

        val dialog = MaterialAlertDialogBuilder(fragment.requireContext())
            .setTitle(if (existing == null) "Add size option" else "Edit size option")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton(if (existing == null) "Add" else "Save", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                tilDisplayLabel.error = null
                tilSizeValue.error = null
                tilExtraPrice.error = null
                tilCalories.error = null

                val displayLabel = etDisplayLabel.text?.toString()?.trim().orEmpty()
                val sizeValue = etSizeValue.text?.toString()?.trim().orEmpty().toIntOrNull()
                val extraPrice = etExtraPrice.text?.toString()?.trim().orEmpty().toDoubleOrNull()
                val calories = etCalories.text?.toString()?.trim().orEmpty().toIntOrNull()
                val isDefault = cbIsDefault.isChecked

                var hasError = false

                if (displayLabel.isBlank()) {
                    tilDisplayLabel.error = if (product.isCoffee) {
                        "Enter a size name"
                    } else {
                        "Enter a portion name"
                    }
                    hasError = true
                }

                if (sizeValue == null || sizeValue <= 0) {
                    tilSizeValue.error = if (product.isCoffee) {
                        "Enter a valid size in ml"
                    } else {
                        "Enter a valid portion weight in g"
                    }
                    hasError = true
                }

                if (extraPrice == null || extraPrice < 0.0) {
                    tilExtraPrice.error = "Enter a valid extra price"
                    hasError = true
                }

                if (calories == null || calories < 0) {
                    tilCalories.error = "Enter valid calories"
                    hasError = true
                }

                if (hasError) return@setOnClickListener

                val validatedSize = sizeValue ?: return@setOnClickListener
                val validatedExtraPrice = extraPrice ?: return@setOnClickListener
                val validatedCalories = calories ?: return@setOnClickListener

                viewModel.saveProductOption(
                    product = product,
                    existing = existing,
                    displayLabel = displayLabel,
                    sizeValue = validatedSize,
                    sizeUnit = unit,
                    extraPrice = validatedExtraPrice,
                    estimatedCalories = validatedCalories,
                    isDefault = isDefault
                ) {
                    dialog.dismiss()
                    onSaved()
                }
            }
        }

        dialog.show()
    }

    private fun clearDialogRefs() {
        activeProduct = null
        tvSizesEmpty = null
        containerSizeOptions = null
    }
}