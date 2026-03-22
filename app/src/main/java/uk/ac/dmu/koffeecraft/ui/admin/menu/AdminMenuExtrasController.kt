package uk.ac.dmu.koffeecraft.ui.admin.menu

import android.view.LayoutInflater
import android.view.View
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
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.entities.ProductAddOnCrossRef
import uk.ac.dmu.koffeecraft.data.repository.AdminMenuRepository
import java.util.Locale

class AdminMenuExtrasController(
    private val fragment: Fragment,
    private val repository: AdminMenuRepository
) {

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
        val tvAssignedEmpty = dialogView.findViewById<TextView>(R.id.tvAssignedEmpty)
        val tvLibraryEmpty = dialogView.findViewById<TextView>(R.id.tvLibraryEmpty)
        val containerAssignedExtras =
            dialogView.findViewById<LinearLayout>(R.id.containerAssignedExtras)
        val containerLibraryExtras =
            dialogView.findViewById<LinearLayout>(R.id.containerLibraryExtras)

        tvTitle.text = "Manage extras for ${product.name}"
        tvSubtitle.text = if (product.isCoffee) {
            "Keep coffee extras organised, assign only what this drink should offer, and manage the shared library professionally."
        } else {
            "Keep cake extras organised, assign only what this cake should offer, and manage the shared library professionally."
        }

        fun refreshExtrasContent() {
            populateManageExtrasDialog(
                product = product,
                assignedContainer = containerAssignedExtras,
                libraryContainer = containerLibraryExtras,
                tvAssignedEmpty = tvAssignedEmpty,
                tvLibraryEmpty = tvLibraryEmpty,
                onChanged = {
                    refreshExtrasContent()
                    onSaved()
                }
            )
        }

        btnCreateExtra.setOnClickListener {
            showAddOnFormDialog(product.productFamily, null) {
                refreshExtrasContent()
                onSaved()
            }
        }

        val dialog = MaterialAlertDialogBuilder(fragment.requireContext())
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .create()

        dialog.setOnShowListener {
            refreshExtrasContent()
        }

        dialog.show()
    }

    private fun populateManageExtrasDialog(
        product: Product,
        assignedContainer: LinearLayout,
        libraryContainer: LinearLayout,
        tvAssignedEmpty: TextView,
        tvLibraryEmpty: TextView,
        onChanged: () -> Unit
    ) {
        fragment.viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val allAddOns = repository.addOnDao().getAllByCategory(product.productFamily)
            val assignedAddOns = repository.addOnDao().getAssignedForProduct(product.productId)
            val assignedIds = assignedAddOns.map { it.addOnId }.toSet()
            val unassignedLibraryAddOns = allAddOns.filterNot { assignedIds.contains(it.addOnId) }

            withContext(Dispatchers.Main) {
                if (!fragment.isAdded) return@withContext

                assignedContainer.removeAllViews()
                libraryContainer.removeAllViews()

                tvAssignedEmpty.visibility =
                    if (assignedAddOns.isEmpty()) View.VISIBLE else View.GONE
                tvLibraryEmpty.visibility =
                    if (unassignedLibraryAddOns.isEmpty()) View.VISIBLE else View.GONE

                assignedAddOns.forEach { addOn ->
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
                            fragment.viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                repository.addOnDao().deleteProductRef(product.productId, addOn.addOnId)

                                withContext(Dispatchers.Main) {
                                    if (!fragment.isAdded) return@withContext
                                    Toast.makeText(
                                        fragment.requireContext(),
                                        "\"${addOn.name}\" removed from ${product.name}.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onChanged()
                                }
                            }
                        },
                        secondaryText = "Edit",
                        onSecondary = {
                            showAddOnFormDialog(addOn.category, addOn) {
                                onChanged()
                            }
                        },
                        tertiaryText = if (addOn.isActive) "Disable" else "Enable",
                        onTertiary = {
                            fragment.viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                repository.addOnDao().setActive(addOn.addOnId, !addOn.isActive)

                                withContext(Dispatchers.Main) {
                                    if (!fragment.isAdded) return@withContext
                                    Toast.makeText(
                                        fragment.requireContext(),
                                        if (addOn.isActive) {
                                            "\"${addOn.name}\" disabled."
                                        } else {
                                            "\"${addOn.name}\" enabled."
                                        },
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onChanged()
                                }
                            }
                        }
                    )
                }

                unassignedLibraryAddOns.forEach { addOn ->
                    if (addOn.isActive) {
                        addManageExtraCard(
                            container = libraryContainer,
                            addOn = addOn,
                            stateText = "Available in library",
                            primaryText = "Assign",
                            onPrimary = {
                                fragment.viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                    repository.addOnDao().insertProductRefs(
                                        listOf(
                                            ProductAddOnCrossRef(
                                                productId = product.productId,
                                                addOnId = addOn.addOnId
                                            )
                                        )
                                    )

                                    withContext(Dispatchers.Main) {
                                        if (!fragment.isAdded) return@withContext
                                        Toast.makeText(
                                            fragment.requireContext(),
                                            "\"${addOn.name}\" assigned to ${product.name}.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onChanged()
                                    }
                                }
                            },
                            secondaryText = "Edit",
                            onSecondary = {
                                showAddOnFormDialog(addOn.category, addOn) {
                                    onChanged()
                                }
                            },
                            tertiaryText = "Disable",
                            onTertiary = {
                                fragment.viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                    repository.addOnDao().setActive(addOn.addOnId, false)

                                    withContext(Dispatchers.Main) {
                                        if (!fragment.isAdded) return@withContext
                                        Toast.makeText(
                                            fragment.requireContext(),
                                            "\"${addOn.name}\" disabled.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onChanged()
                                    }
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
                                fragment.viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                    repository.addOnDao().setActive(addOn.addOnId, true)

                                    withContext(Dispatchers.Main) {
                                        if (!fragment.isAdded) return@withContext
                                        Toast.makeText(
                                            fragment.requireContext(),
                                            "\"${addOn.name}\" enabled.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onChanged()
                                    }
                                }
                            },
                            secondaryText = "Edit",
                            onSecondary = {
                                showAddOnFormDialog(addOn.category, addOn) {
                                    onChanged()
                                }
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
                                        fragment.viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                            repository.addOnDao().deleteRefsForAddOn(addOn.addOnId)
                                            repository.allergenDao().deleteAddOnRefs(addOn.addOnId)
                                            repository.addOnDao().deleteById(addOn.addOnId)

                                            withContext(Dispatchers.Main) {
                                                if (!fragment.isAdded) return@withContext
                                                Toast.makeText(
                                                    fragment.requireContext(),
                                                    "\"${addOn.name}\" deleted permanently.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                onChanged()
                                            }
                                        }
                                    }
                                    .show()
                            }
                        )
                    }
                }
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
        category: String,
        existing: AddOn?,
        onSaved: () -> Unit
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
                    tilName.error = "Enter extra name"
                    hasError = true
                }

                if (price == null || price < 0.0) {
                    tilPrice.error = "Enter a valid price"
                    hasError = true
                }

                if (calories == null || calories < 0) {
                    tilCalories.error = "Enter valid calories"
                    hasError = true
                }

                if (hasError) return@setOnClickListener

                val validatedPrice = price ?: return@setOnClickListener
                val validatedCalories = calories ?: return@setOnClickListener

                fragment.viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    if (existing == null) {
                        repository.addOnDao().insert(
                            AddOn(
                                name = name,
                                category = category,
                                price = validatedPrice,
                                estimatedCalories = validatedCalories,
                                isActive = isActive
                            )
                        )
                    } else {
                        repository.addOnDao().update(
                            existing.copy(
                                name = name,
                                price = validatedPrice,
                                estimatedCalories = validatedCalories,
                                isActive = isActive
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                        if (!fragment.isAdded) return@withContext
                        Toast.makeText(
                            fragment.requireContext(),
                            if (existing == null) "Extra created." else "Extra updated.",
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
}