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
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.entities.ProductOption
import uk.ac.dmu.koffeecraft.data.repository.AdminMenuRepository

class AdminMenuOptionsController(
    private val fragment: Fragment,
    private val repository: AdminMenuRepository
) {

    fun showManageSizesDialog(
        product: Product,
        onSaved: () -> Unit
    ) {
        val dialogView = fragment.layoutInflater.inflate(R.layout.dialog_manage_sizes, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvManageSizesTitle)
        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tvManageSizesSubtitle)
        val btnAddSizeOption = dialogView.findViewById<MaterialButton>(R.id.btnAddSizeOption)
        val tvSizesEmpty = dialogView.findViewById<TextView>(R.id.tvSizesEmpty)
        val containerSizeOptions = dialogView.findViewById<LinearLayout>(R.id.containerSizeOptions)

        tvTitle.text = "Manage sizes for ${product.name}"
        tvSubtitle.text = if (product.isCoffee) {
            "Review, edit, and add drink size options for this product."
        } else {
            "Review, edit, and add portion options for this product."
        }

        fun refreshSizesContent() {
            populateManageSizesDialog(
                product = product,
                container = containerSizeOptions,
                tvEmpty = tvSizesEmpty,
                onSaved = onSaved
            )
        }

        btnAddSizeOption.setOnClickListener {
            showOptionFormDialog(product, null) {
                refreshSizesContent()
                onSaved()
            }
        }

        val dialog = MaterialAlertDialogBuilder(fragment.requireContext())
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .create()

        dialog.setOnShowListener {
            refreshSizesContent()
        }

        dialog.show()
    }

    private fun populateManageSizesDialog(
        product: Product,
        container: LinearLayout,
        tvEmpty: TextView,
        onSaved: () -> Unit
    ) {
        fragment.viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val options = repository.productOptionDao().getForProduct(product.productId)

            withContext(Dispatchers.Main) {
                if (!fragment.isAdded) return@withContext

                container.removeAllViews()
                tvEmpty.visibility = if (options.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE

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
                        showOptionFormDialog(product, option) {
                            populateManageSizesDialog(product, container, tvEmpty, onSaved)
                            onSaved()
                        }
                    }

                    btnDeleteSize.setOnClickListener {
                        MaterialAlertDialogBuilder(fragment.requireContext())
                            .setTitle("Delete size option?")
                            .setMessage("This will remove \"${option.displayLabel}\" from ${product.name}.")
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Delete") { _, _ ->
                                fragment.viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                    repository.productOptionDao().deleteById(option.optionId)
                                    withContext(Dispatchers.Main) {
                                        if (!fragment.isAdded) return@withContext
                                        Toast.makeText(
                                            fragment.requireContext(),
                                            "Size option deleted.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        populateManageSizesDialog(product, container, tvEmpty, onSaved)
                                        onSaved()
                                    }
                                }
                            }
                            .show()
                    }

                    container.addView(itemView)
                }
            }
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
        tvUnitHint.text = "Unit: $unit"

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
                    tilDisplayLabel.error = "Enter a label"
                    hasError = true
                }

                if (sizeValue == null || sizeValue <= 0) {
                    tilSizeValue.error = "Enter a valid size"
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

                fragment.viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    if (isDefault) {
                        repository.productOptionDao().clearDefaultForProduct(product.productId)
                    }

                    val optionName = existing?.optionName ?: toOptionKey(displayLabel)

                    if (existing == null) {
                        repository.productOptionDao().insert(
                            ProductOption(
                                productId = product.productId,
                                optionName = optionName,
                                displayLabel = displayLabel,
                                sizeValue = validatedSize,
                                sizeUnit = unit,
                                extraPrice = validatedExtraPrice,
                                estimatedCalories = validatedCalories,
                                isDefault = isDefault
                            )
                        )
                    } else {
                        repository.productOptionDao().update(
                            existing.copy(
                                displayLabel = displayLabel,
                                sizeValue = validatedSize,
                                sizeUnit = unit,
                                extraPrice = validatedExtraPrice,
                                estimatedCalories = validatedCalories,
                                isDefault = isDefault
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                        if (!fragment.isAdded) return@withContext
                        Toast.makeText(
                            fragment.requireContext(),
                            if (existing == null) "Size option added." else "Size option updated.",
                            Toast.LENGTH_SHORT
                        ).show()
                        dialog.dismiss()
                        onSaved()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun toOptionKey(displayLabel: String): String {
        return displayLabel
            .trim()
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
    }
}