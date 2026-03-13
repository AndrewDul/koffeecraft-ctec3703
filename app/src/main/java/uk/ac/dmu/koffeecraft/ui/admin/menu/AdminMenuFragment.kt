package uk.ac.dmu.koffeecraft.ui.admin.menu

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.AddOn
import uk.ac.dmu.koffeecraft.data.entities.AddOnAllergenCrossRef
import uk.ac.dmu.koffeecraft.data.entities.Allergen
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.entities.ProductAddOnCrossRef
import uk.ac.dmu.koffeecraft.data.entities.ProductAllergenCrossRef
import uk.ac.dmu.koffeecraft.data.entities.ProductOption

class AdminMenuFragment : Fragment(R.layout.fragment_admin_menu) {

    private lateinit var adapter: AdminProductsAdapter
    private lateinit var tvEmpty: TextView

    private var allProducts: List<Product> = emptyList()
    private var currentFilter: CategoryFilter = CategoryFilter.ALL

    private enum class CategoryFilter {
        ALL,
        COFFEE,
        CAKE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvAdminProducts)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        val fab = view.findViewById<FloatingActionButton>(R.id.fabAddProduct)
        val toggleGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.toggleCategoryFilter)

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            currentFilter = when (checkedId) {
                R.id.btnFilterCoffee -> CategoryFilter.COFFEE
                R.id.btnFilterCake -> CategoryFilter.CAKE
                else -> CategoryFilter.ALL
            }

            applyCategoryFilter()
        }

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        rv.layoutManager = LinearLayoutManager(requireContext())

        adapter = AdminProductsAdapter(
            items = emptyList(),
            onToggle = { product ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    db.productDao().setActive(product.productId, !product.isActive)
                }
            },
            onEdit = { product ->
                showProductDialog(db, existing = product)
            },
            onDelete = { product ->
                showDeleteDialog(db, product)
            },
            onDetails = { product ->
                showProductDetailsDialog(db, product)
            }
        )

        rv.adapter = adapter

        fab.setOnClickListener {
            showProductDialog(db, existing = null)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            db.productDao().observeAll().collect { products ->
                allProducts = products
                applyCategoryFilter()
            }
        }
    }

    private fun showProductDialog(
        db: KoffeeCraftDatabase,
        existing: Product?
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_product_form, null)

        val tilName = dialogView.findViewById<TextInputLayout>(R.id.tilName)
        val tilDescription = dialogView.findViewById<TextInputLayout>(R.id.tilDescription)
        val tilPrice = dialogView.findViewById<TextInputLayout>(R.id.tilPrice)

        val etName = dialogView.findViewById<TextInputEditText>(R.id.etName)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.etDescription)
        val etPrice = dialogView.findViewById<TextInputEditText>(R.id.etPrice)
        val spCategory = dialogView.findViewById<Spinner>(R.id.spCategory)
        val cbIsNew = dialogView.findViewById<CheckBox>(R.id.cbIsNew)

        val categories = listOf("COFFEE", "CAKE")
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spCategory.adapter = spinnerAdapter

        if (existing != null) {
            etName.setText(existing.name)
            etDescription.setText(existing.description)
            etPrice.setText(existing.price.toString())
            cbIsNew.isChecked = existing.isNew

            val selectedIndex = categories.indexOf(existing.category).takeIf { it >= 0 } ?: 0
            spCategory.setSelection(selectedIndex)
        }

        val isEdit = existing != null

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (isEdit) "Edit product" else "Add product")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton(if (isEdit) "Save" else "Add", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                tilName.error = null
                tilDescription.error = null
                tilPrice.error = null

                val name = etName.text?.toString()?.trim().orEmpty()
                val description = etDescription.text?.toString()?.trim().orEmpty()
                val priceText = etPrice.text?.toString()?.trim().orEmpty()
                val category = spCategory.selectedItem?.toString().orEmpty()
                val isNew = cbIsNew.isChecked

                var hasError = false

                if (name.isBlank()) {
                    tilName.error = "Enter product name"
                    hasError = true
                }

                if (description.isBlank()) {
                    tilDescription.error = "Enter product description"
                    hasError = true
                }

                val price = priceText.toDoubleOrNull()
                if (price == null || price <= 0.0) {
                    tilPrice.error = "Enter a valid price"
                    hasError = true
                }

                if (hasError) return@setOnClickListener
                val validatedPrice = price ?: return@setOnClickListener

                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    if (existing == null) {
                        db.productDao().insert(
                            Product(
                                name = name,
                                category = category,
                                description = description,
                                price = validatedPrice,
                                isActive = true,
                                isNew = isNew,
                                imageKey = null
                            )
                        )
                    } else {
                        db.productDao().update(
                            existing.copy(
                                name = name,
                                category = category,
                                description = description,
                                price = validatedPrice,
                                isNew = isNew
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                        if (!isAdded) return@withContext
                        Toast.makeText(
                            requireContext(),
                            if (existing == null) "Product added" else "Product updated",
                            Toast.LENGTH_SHORT
                        ).show()
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun showDeleteDialog(
        db: KoffeeCraftDatabase,
        product: Product
    ) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete product?")
            .setMessage("This will permanently remove \"${product.name}\" from the menu.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    db.productDao().deleteById(product.productId)
                }
            }
            .show()
    }

    private fun showProductDetailsDialog(
        db: KoffeeCraftDatabase,
        product: Product
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_admin_product_details, null)

        val tvProductName = dialogView.findViewById<TextView>(R.id.tvProductName)
        val tvProductMeta = dialogView.findViewById<TextView>(R.id.tvProductMeta)
        val tvDescription = dialogView.findViewById<TextView>(R.id.tvDescription)
        val tvOptions = dialogView.findViewById<TextView>(R.id.tvOptions)
        val tvAddOns = dialogView.findViewById<TextView>(R.id.tvAddOns)
        val tvAllergens = dialogView.findViewById<TextView>(R.id.tvAllergens)
        val btnManageSizes = dialogView.findViewById<MaterialButton>(R.id.btnManageSizes)
        val btnManageExtras = dialogView.findViewById<MaterialButton>(R.id.btnManageExtras)
        val btnManageAllergens = dialogView.findViewById<MaterialButton>(R.id.btnManageAllergens)

        tvProductName.text = product.name
        tvProductMeta.text = buildString {
            append(product.category.replaceFirstChar { it.uppercase() })
            append(" • ")
            append("From £%.2f".format(product.price))
            append(" • ")
            append(if (product.isActive) "Active" else "Disabled")
        }
        tvDescription.text = product.description

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Product details")
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .create()

        btnManageSizes.setOnClickListener {
            showManageSizesDialog(db, product) {
                loadProductDetails(db, product, tvOptions, tvAddOns, tvAllergens)
            }
        }

        btnManageExtras.setOnClickListener {
            showManageExtrasDialog(db, product) {
                loadProductDetails(db, product, tvOptions, tvAddOns, tvAllergens)
            }
        }

        btnManageAllergens.setOnClickListener {
            showManageAllergensDialog(db, product) {
                loadProductDetails(db, product, tvOptions, tvAddOns, tvAllergens)
            }
        }

        dialog.show()
        loadProductDetails(db, product, tvOptions, tvAddOns, tvAllergens)
    }

    private fun loadProductDetails(
        db: KoffeeCraftDatabase,
        product: Product,
        tvOptions: TextView,
        tvAddOns: TextView,
        tvAllergens: TextView
    ) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val options = db.productOptionDao().getForProduct(product.productId)
            val assignedAddOns = db.addOnDao().getAssignedForProduct(product.productId)
            val allergens = db.allergenDao().getForProduct(product.productId)

            val optionsText = if (options.isEmpty()) {
                "No size / portion options configured yet."
            } else {
                options.joinToString("\n\n") { option ->
                    buildString {
                        append("• ")
                        append(option.displayLabel)
                        append(" — ")
                        append(option.sizeValue)
                        append(option.sizeUnit.lowercase())
                        append("\n")
                        append("  Extra price: £%.2f".format(option.extraPrice))
                        append(" • ")
                        append(option.estimatedCalories)
                        append(" kcal")
                        if (option.isDefault) {
                            append(" • Default")
                        }
                    }
                }
            }

            val addOnsText = if (assignedAddOns.isEmpty()) {
                "No extras assigned yet."
            } else {
                assignedAddOns.joinToString("\n\n") { addOn ->
                    buildString {
                        append("• ")
                        append(addOn.name)
                        append("\n")
                        append("  £%.2f".format(addOn.price))
                        append(" • ")
                        append(addOn.estimatedCalories)
                        append(" kcal")
                        if (!addOn.isActive) {
                            append(" • Disabled")
                        }
                    }
                }
            }

            val allergensText = if (allergens.isEmpty()) {
                "No allergens listed."
            } else {
                allergens.joinToString(", ") { it.name }
            }

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                tvOptions.text = optionsText
                tvAddOns.text = addOnsText
                tvAllergens.text = allergensText
            }
        }
    }

    private fun showManageSizesDialog(
        db: KoffeeCraftDatabase,
        product: Product,
        onSaved: () -> Unit
    ) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val options = db.productOptionDao().getForProduct(product.productId)

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext

                val labels = options.map {
                    buildString {
                        append(it.displayLabel)
                        append(" • ")
                        append(it.sizeValue)
                        append(it.sizeUnit.lowercase())
                        if (it.extraPrice > 0.0) {
                            append(" • +£%.2f".format(it.extraPrice))
                        }
                        if (it.isDefault) {
                            append(" • Default")
                        }
                    }
                }.toMutableList()

                labels.add("+ Add new size option")

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Manage sizes")
                    .setItems(labels.toTypedArray()) { _, which ->
                        if (which == options.size) {
                            showOptionFormDialog(db, product, null, onSaved)
                        } else {
                            showSizeOptionActionsDialog(db, product, options[which], onSaved)
                        }
                    }
                    .setNegativeButton("Close", null)
                    .show()
            }
        }
    }

    private fun showSizeOptionActionsDialog(
        db: KoffeeCraftDatabase,
        product: Product,
        option: ProductOption,
        onSaved: () -> Unit
    ) {
        val actions = arrayOf("Edit size option", "Delete size option")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(option.displayLabel)
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> showOptionFormDialog(db, product, option, onSaved)
                    1 -> {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Delete size option?")
                            .setMessage("This will remove \"${option.displayLabel}\" from ${product.name}.")
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Delete") { _, _ ->
                                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                    db.productOptionDao().deleteById(option.optionId)
                                    withContext(Dispatchers.Main) {
                                        if (!isAdded) return@withContext
                                        Toast.makeText(requireContext(), "Size option deleted.", Toast.LENGTH_SHORT).show()
                                        onSaved()
                                    }
                                }
                            }
                            .show()
                    }
                }
            }
            .show()
    }

    private fun showOptionFormDialog(
        db: KoffeeCraftDatabase,
        product: Product,
        existing: ProductOption?,
        onSaved: () -> Unit
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_product_option_form, null)

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

        val unit = if (product.category.equals("COFFEE", ignoreCase = true)) "ML" else "G"
        tvUnitHint.text = "Unit: $unit"

        if (existing != null) {
            etDisplayLabel.setText(existing.displayLabel)
            etSizeValue.setText(existing.sizeValue.toString())
            etExtraPrice.setText(existing.extraPrice.toString())
            etCalories.setText(existing.estimatedCalories.toString())
            cbIsDefault.isChecked = existing.isDefault
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
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

                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    if (isDefault) {
                        db.productOptionDao().clearDefaultForProduct(product.productId)
                    }

                    val optionName = existing?.optionName ?: toOptionKey(displayLabel)

                    if (existing == null) {
                        db.productOptionDao().insert(
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
                        db.productOptionDao().update(
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
                        if (!isAdded) return@withContext
                        Toast.makeText(
                            requireContext(),
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

    private fun showManageExtrasDialog(
        db: KoffeeCraftDatabase,
        product: Product,
        onSaved: () -> Unit
    ) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val allAddOns = db.addOnDao().getAllByCategory(product.category)
            val assignedIds = db.addOnDao().getAssignedIdsForProduct(product.productId).toSet()

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext

                val checked = BooleanArray(allAddOns.size) { index ->
                    assignedIds.contains(allAddOns[index].addOnId)
                }

                val labels = allAddOns.map { addOn ->
                    buildString {
                        append(addOn.name)
                        append(" • £%.2f".format(addOn.price))
                        append(" • ")
                        append(addOn.estimatedCalories)
                        append(" kcal")
                        if (!addOn.isActive) {
                            append(" • Disabled")
                        }
                    }
                }.toTypedArray()

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Assign extras")
                    .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                        checked[which] = isChecked
                    }
                    .setNegativeButton("Cancel", null)
                    .setNeutralButton("Extra library") { _, _ ->
                        showAddOnLibraryDialog(db, product.category) {
                            onSaved()
                            showManageExtrasDialog(db, product, onSaved)
                        }
                    }
                    .setPositiveButton("Save") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                            val refs = allAddOns.mapIndexedNotNull { index, addOn ->
                                if (checked[index]) {
                                    ProductAddOnCrossRef(
                                        productId = product.productId,
                                        addOnId = addOn.addOnId
                                    )
                                } else {
                                    null
                                }
                            }

                            db.addOnDao().deleteRefsForProduct(product.productId)
                            if (refs.isNotEmpty()) {
                                db.addOnDao().insertProductRefs(refs)
                            }

                            withContext(Dispatchers.Main) {
                                if (!isAdded) return@withContext
                                Toast.makeText(requireContext(), "Extras updated.", Toast.LENGTH_SHORT).show()
                                onSaved()
                            }
                        }
                    }
                    .show()
            }
        }
    }

    private fun showAddOnLibraryDialog(
        db: KoffeeCraftDatabase,
        category: String,
        onSaved: () -> Unit
    ) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val addOns = db.addOnDao().getAllByCategory(category)

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext

                val labels = addOns.map { addOn ->
                    buildString {
                        append(addOn.name)
                        append(" • £%.2f".format(addOn.price))
                        append(" • ")
                        append(addOn.estimatedCalories)
                        append(" kcal")
                        append(" • ")
                        append(if (addOn.isActive) "Active" else "Disabled")
                    }
                }.toMutableList()

                labels.add("+ Add new extra")

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("${category.replaceFirstChar { it.uppercase() }} extras")
                    .setItems(labels.toTypedArray()) { _, which ->
                        if (which == addOns.size) {
                            showAddOnFormDialog(db, category, null, onSaved)
                        } else {
                            showAddOnActionsDialog(db, addOns[which], onSaved)
                        }
                    }
                    .setNegativeButton("Close", null)
                    .show()
            }
        }
    }

    private fun showAddOnActionsDialog(
        db: KoffeeCraftDatabase,
        addOn: AddOn,
        onSaved: () -> Unit
    ) {
        val actions = arrayOf(
            "Edit extra",
            if (addOn.isActive) "Disable extra" else "Enable extra",
            "Delete extra"
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(addOn.name)
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> showAddOnFormDialog(db, addOn.category, addOn, onSaved)
                    1 -> {
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                            db.addOnDao().setActive(addOn.addOnId, !addOn.isActive)
                            withContext(Dispatchers.Main) {
                                if (!isAdded) return@withContext
                                Toast.makeText(
                                    requireContext(),
                                    if (addOn.isActive) "Extra disabled." else "Extra enabled.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onSaved()
                            }
                        }
                    }

                    2 -> {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Delete extra?")
                            .setMessage("This will permanently remove \"${addOn.name}\".")
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Delete") { _, _ ->
                                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                    db.addOnDao().deleteRefsForAddOn(addOn.addOnId)
                                    db.allergenDao().deleteAddOnRefs(addOn.addOnId)
                                    db.addOnDao().deleteById(addOn.addOnId)

                                    withContext(Dispatchers.Main) {
                                        if (!isAdded) return@withContext
                                        Toast.makeText(requireContext(), "Extra deleted.", Toast.LENGTH_SHORT).show()
                                        onSaved()
                                    }
                                }
                            }
                            .show()
                    }
                }
            }
            .show()
    }

    private fun showAddOnFormDialog(
        db: KoffeeCraftDatabase,
        category: String,
        existing: AddOn?,
        onSaved: () -> Unit
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_on_form, null)

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
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existing == null) "Add extra" else "Edit extra")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton(if (existing == null) "Add" else "Save", null)
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

                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    if (existing == null) {
                        db.addOnDao().insert(
                            AddOn(
                                name = name,
                                category = category,
                                price = validatedPrice,
                                estimatedCalories = validatedCalories,
                                isActive = isActive
                            )
                        )
                    } else {
                        db.addOnDao().update(
                            existing.copy(
                                name = name,
                                price = validatedPrice,
                                estimatedCalories = validatedCalories,
                                isActive = isActive
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                        if (!isAdded) return@withContext
                        Toast.makeText(
                            requireContext(),
                            if (existing == null) "Extra added." else "Extra updated.",
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

    private fun showManageAllergensDialog(
        db: KoffeeCraftDatabase,
        product: Product,
        onSaved: () -> Unit
    ) {
        val actions = arrayOf(
            "Manage product allergens",
            "Manage extra allergens",
            "Allergen library"
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Manage allergens")
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> showManageProductAllergensDialog(db, product, onSaved)
                    1 -> showSelectExtraForAllergensDialog(db, product, onSaved)
                    2 -> showAllergenLibraryDialog(db, onSaved)
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showManageProductAllergensDialog(
        db: KoffeeCraftDatabase,
        product: Product,
        onSaved: () -> Unit
    ) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val allergens = db.allergenDao().getAll()
            val selectedIds = db.allergenDao().getProductAllergenIds(product.productId).toSet()

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext

                if (allergens.isEmpty()) {
                    Toast.makeText(requireContext(), "No allergens in library yet.", Toast.LENGTH_SHORT).show()
                    showAllergenLibraryDialog(db, onSaved)
                    return@withContext
                }

                val checked = BooleanArray(allergens.size) { index ->
                    selectedIds.contains(allergens[index].allergenId)
                }

                val labels = allergens.map { it.name }.toTypedArray()

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Product allergens")
                    .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                        checked[which] = isChecked
                    }
                    .setNegativeButton("Cancel", null)
                    .setNeutralButton("Allergen library") { _, _ ->
                        showAllergenLibraryDialog(db) {
                            onSaved()
                            showManageProductAllergensDialog(db, product, onSaved)
                        }
                    }
                    .setPositiveButton("Save") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                            val refs = allergens.mapIndexedNotNull { index, allergen ->
                                if (checked[index]) {
                                    ProductAllergenCrossRef(
                                        productId = product.productId,
                                        allergenId = allergen.allergenId
                                    )
                                } else {
                                    null
                                }
                            }

                            db.allergenDao().deleteProductRefs(product.productId)
                            if (refs.isNotEmpty()) {
                                db.allergenDao().insertProductRefs(refs)
                            }

                            withContext(Dispatchers.Main) {
                                if (!isAdded) return@withContext
                                Toast.makeText(requireContext(), "Product allergens updated.", Toast.LENGTH_SHORT).show()
                                onSaved()
                            }
                        }
                    }
                    .show()
            }
        }
    }

    private fun showSelectExtraForAllergensDialog(
        db: KoffeeCraftDatabase,
        product: Product,
        onSaved: () -> Unit
    ) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val addOns = db.addOnDao().getAssignedForProduct(product.productId)

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext

                if (addOns.isEmpty()) {
                    Toast.makeText(requireContext(), "No extras assigned to this product yet.", Toast.LENGTH_SHORT).show()
                    return@withContext
                }

                val labels = addOns.map { addOn ->
                    buildString {
                        append(addOn.name)
                        if (!addOn.isActive) {
                            append(" • Disabled")
                        }
                    }
                }.toTypedArray()

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Choose extra")
                    .setItems(labels) { _, which ->
                        showManageAddOnAllergensDialog(db, addOns[which], onSaved)
                    }
                    .setNegativeButton("Close", null)
                    .show()
            }
        }
    }

    private fun showManageAddOnAllergensDialog(
        db: KoffeeCraftDatabase,
        addOn: AddOn,
        onSaved: () -> Unit
    ) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val allergens = db.allergenDao().getAll()
            val selectedIds = db.allergenDao().getAddOnAllergenIds(addOn.addOnId).toSet()

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext

                if (allergens.isEmpty()) {
                    Toast.makeText(requireContext(), "No allergens in library yet.", Toast.LENGTH_SHORT).show()
                    showAllergenLibraryDialog(db, onSaved)
                    return@withContext
                }

                val checked = BooleanArray(allergens.size) { index ->
                    selectedIds.contains(allergens[index].allergenId)
                }

                val labels = allergens.map { it.name }.toTypedArray()

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("${addOn.name} allergens")
                    .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                        checked[which] = isChecked
                    }
                    .setNegativeButton("Cancel", null)
                    .setNeutralButton("Allergen library") { _, _ ->
                        showAllergenLibraryDialog(db) {
                            onSaved()
                            showManageAddOnAllergensDialog(db, addOn, onSaved)
                        }
                    }
                    .setPositiveButton("Save") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                            val refs = allergens.mapIndexedNotNull { index, allergen ->
                                if (checked[index]) {
                                    AddOnAllergenCrossRef(
                                        addOnId = addOn.addOnId,
                                        allergenId = allergen.allergenId
                                    )
                                } else {
                                    null
                                }
                            }

                            db.allergenDao().deleteAddOnRefs(addOn.addOnId)
                            if (refs.isNotEmpty()) {
                                db.allergenDao().insertAddOnRefs(refs)
                            }

                            withContext(Dispatchers.Main) {
                                if (!isAdded) return@withContext
                                Toast.makeText(requireContext(), "Extra allergens updated.", Toast.LENGTH_SHORT).show()
                                onSaved()
                            }
                        }
                    }
                    .show()
            }
        }
    }

    private fun showAllergenLibraryDialog(
        db: KoffeeCraftDatabase,
        onSaved: () -> Unit
    ) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val allergens = db.allergenDao().getAll()

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext

                val labels = allergens.map { it.name }.toMutableList()
                labels.add("+ Add new allergen")

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Allergen library")
                    .setItems(labels.toTypedArray()) { _, which ->
                        if (which == allergens.size) {
                            showAddAllergenDialog(db, onSaved)
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Allergen available: ${allergens[which].name}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .setNegativeButton("Close", null)
                    .show()
            }
        }
    }

    private fun showAddAllergenDialog(
        db: KoffeeCraftDatabase,
        onSaved: () -> Unit
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_allergen_form, null)
        val tilAllergenName = dialogView.findViewById<TextInputLayout>(R.id.tilAllergenName)
        val etAllergenName = dialogView.findViewById<TextInputEditText>(R.id.etAllergenName)

        val dialog = MaterialAlertDialogBuilder(requireContext())
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

                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    val result = db.allergenDao().insert(Allergen(name = name))

                    withContext(Dispatchers.Main) {
                        if (!isAdded) return@withContext

                        if (result == -1L) {
                            Toast.makeText(
                                requireContext(),
                                "This allergen already exists.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                requireContext(),
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

    private fun toOptionKey(displayLabel: String): String {
        val normalized = displayLabel
            .trim()
            .uppercase(Locale.UK)
            .replace("[^A-Z0-9]+".toRegex(), "_")
            .trim('_')

        return if (normalized.isBlank()) {
            "OPTION_${System.currentTimeMillis()}"
        } else {
            normalized
        }
    }

    private fun applyCategoryFilter() {
        val filteredProducts = when (currentFilter) {
            CategoryFilter.ALL -> allProducts
            CategoryFilter.COFFEE -> allProducts.filter { it.category.equals("COFFEE", ignoreCase = true) }
            CategoryFilter.CAKE -> allProducts.filter { it.category.equals("CAKE", ignoreCase = true) }
        }

        adapter.submitList(filteredProducts)
        tvEmpty.visibility = if (filteredProducts.isEmpty()) View.VISIBLE else View.GONE
    }
}