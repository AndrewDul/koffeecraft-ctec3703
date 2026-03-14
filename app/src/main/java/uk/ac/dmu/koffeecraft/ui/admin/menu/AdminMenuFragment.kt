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
import android.view.LayoutInflater
import android.widget.LinearLayout
class AdminMenuFragment : Fragment(R.layout.fragment_admin_menu) {

    private lateinit var adapter: AdminProductsAdapter
    private lateinit var tvEmpty: TextView

    private var allProducts: List<Product> = emptyList()
    private var currentFilter: CategoryFilter = CategoryFilter.ALL

    private enum class CategoryFilter {
        ALL,
        COFFEE,
        CAKE,
        MERCH
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
                R.id.btnFilterMerch -> CategoryFilter.MERCH
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

        val toggleProductFamily =
            dialogView.findViewById<MaterialButtonToggleGroup>(R.id.toggleProductFamily)
        val switchRewardEnabled =
            dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchRewardEnabled)
        val cbIsNew = dialogView.findViewById<CheckBox>(R.id.cbIsNew)

        if (existing != null) {
            etName.setText(existing.name)
            etDescription.setText(existing.description)
            etPrice.setText(existing.price.toString())
            cbIsNew.isChecked = existing.isNew
            switchRewardEnabled.isChecked = existing.rewardEnabled

            val familyButtonId = when (existing.productFamily.uppercase(Locale.UK)) {
                "COFFEE" -> R.id.btnFamilyCoffee
                "CAKE" -> R.id.btnFamilyCake
                "MERCH" -> R.id.btnFamilyMerch
                else -> R.id.btnFamilyCoffee
            }
            toggleProductFamily.check(familyButtonId)
        } else {
            toggleProductFamily.check(R.id.btnFamilyCoffee)
            cbIsNew.isChecked = false
            switchRewardEnabled.isChecked = false
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
                val rewardEnabled = switchRewardEnabled.isChecked
                val isNew = cbIsNew.isChecked

                val productFamily = when (toggleProductFamily.checkedButtonId) {
                    R.id.btnFamilyCoffee -> "COFFEE"
                    R.id.btnFamilyCake -> "CAKE"
                    R.id.btnFamilyMerch -> "MERCH"
                    else -> ""
                }

                var hasError = false

                if (name.isBlank()) {
                    tilName.error = "Enter product name"
                    hasError = true
                }

                if (description.isBlank()) {
                    tilDescription.error = "Enter product description"
                    hasError = true
                }

                if (productFamily.isBlank()) {
                    Toast.makeText(requireContext(), "Choose a product family.", Toast.LENGTH_SHORT).show()
                    hasError = true
                }

                val price = priceText.toDoubleOrNull()
                if (price == null || price < 0.0) {
                    tilPrice.error = "Enter a valid price"
                    hasError = true
                }

                if (productFamily != "MERCH" && price != null && price <= 0.0) {
                    tilPrice.error = "Menu products must have a price above 0"
                    hasError = true
                }

                if (productFamily == "MERCH" && !rewardEnabled) {
                    Toast.makeText(
                        requireContext(),
                        "Merch products should be reward-enabled.",
                        Toast.LENGTH_SHORT
                    ).show()
                    hasError = true
                }

                if (hasError) return@setOnClickListener
                val validatedPrice = price ?: return@setOnClickListener

                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    var insertedOrUpdatedProduct: Product? = null

                    if (existing == null) {
                        val insertedId = db.productDao().insert(
                            Product(
                                name = name,
                                productFamily = productFamily,
                                description = description,
                                price = validatedPrice,
                                isActive = true,
                                isNew = isNew,
                                imageKey = null,
                                rewardEnabled = rewardEnabled
                            )
                        )
                        insertedOrUpdatedProduct = db.productDao().getById(insertedId)
                    } else {
                        val updated = existing.copy(
                            name = name,
                            productFamily = productFamily,
                            description = description,
                            price = validatedPrice,
                            isNew = isNew,
                            rewardEnabled = rewardEnabled
                        )
                        db.productDao().update(updated)
                        insertedOrUpdatedProduct = db.productDao().getById(existing.productId)
                    }

                    withContext(Dispatchers.Main) {
                        if (!isAdded) return@withContext

                        Toast.makeText(
                            requireContext(),
                            if (existing == null) "Product added" else "Product updated",
                            Toast.LENGTH_SHORT
                        ).show()

                        dialog.dismiss()

                        val savedProduct = insertedOrUpdatedProduct ?: return@withContext

                        if (existing == null && (savedProduct.isCoffee || savedProduct.isCake)) {
                            showProductDetailsDialog(db, savedProduct)
                        }
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
            append(product.familyLabel)
            append(" • ")
            append(product.listingLabel)
            append(" • ")
            if (!product.isMerch) {
                append("From £%.2f".format(product.price))
                append(" • ")
            }
            append(if (product.isActive) "Active" else "Disabled")
        }
        tvDescription.text = product.description

        val supportsCustomisation = product.isCoffee || product.isCake

        btnManageSizes.isEnabled = supportsCustomisation
        btnManageExtras.isEnabled = supportsCustomisation

        if (!supportsCustomisation) {
            btnManageSizes.text = "Sizes not used for merch"
            btnManageExtras.text = "Extras not used for merch"
            btnManageSizes.alpha = 0.55f
            btnManageExtras.alpha = 0.55f
        } else {
            btnManageSizes.text = "Manage sizes"
            btnManageExtras.text = "Manage extras"
            btnManageSizes.alpha = 1f
            btnManageExtras.alpha = 1f
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Product details")
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .create()

        btnManageSizes.setOnClickListener {
            if (!supportsCustomisation) return@setOnClickListener
            showManageSizesDialog(db, product) {
                loadProductDetails(db, product, tvOptions, tvAddOns, tvAllergens)
            }
        }

        btnManageExtras.setOnClickListener {
            if (!supportsCustomisation) return@setOnClickListener
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
            val allergens = db.allergenDao().getForProduct(product.productId)

            val optionsText: String
            val addOnsText: String

            if (product.isMerch) {
                optionsText = "Not applicable for merch products."
                addOnsText = "Not applicable for merch products."
            } else {
                val options = db.productOptionDao().getForProduct(product.productId)
                val assignedAddOns = db.addOnDao().getAssignedForProduct(product.productId)

                optionsText = if (options.isEmpty()) {
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

                addOnsText = if (assignedAddOns.isEmpty()) {
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

        val unit = if (product.isCoffee) "ML" else "G"
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
        if (!product.isCoffee && !product.isCake) {
            Toast.makeText(
                requireContext(),
                "Extras are only available for coffee and cake products.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_manage_extras, null)

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
                db = db,
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
            showAddOnFormDialog(db, product.productFamily, null) {
                refreshExtrasContent()
                onSaved()
            }
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .create()

        dialog.setOnShowListener {
            refreshExtrasContent()
        }

        dialog.show()
    }
    private fun populateManageExtrasDialog(
        db: KoffeeCraftDatabase,
        product: Product,
        assignedContainer: LinearLayout,
        libraryContainer: LinearLayout,
        tvAssignedEmpty: TextView,
        tvLibraryEmpty: TextView,
        onChanged: () -> Unit
    ) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val allAddOns = db.addOnDao().getAllByCategory(product.productFamily)
            val assignedAddOns = db.addOnDao().getAssignedForProduct(product.productId)
            val assignedIds = assignedAddOns.map { it.addOnId }.toSet()
            val unassignedLibraryAddOns = allAddOns.filterNot { assignedIds.contains(it.addOnId) }

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext

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
                            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                db.addOnDao().deleteProductRef(product.productId, addOn.addOnId)

                                withContext(Dispatchers.Main) {
                                    if (!isAdded) return@withContext
                                    Toast.makeText(
                                        requireContext(),
                                        "\"${addOn.name}\" removed from ${product.name}.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onChanged()
                                }
                            }
                        },
                        secondaryText = "Edit",
                        onSecondary = {
                            showAddOnFormDialog(db, addOn.category, addOn) {
                                onChanged()
                            }
                        },
                        tertiaryText = if (addOn.isActive) "Disable" else "Enable",
                        onTertiary = {
                            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                db.addOnDao().setActive(addOn.addOnId, !addOn.isActive)

                                withContext(Dispatchers.Main) {
                                    if (!isAdded) return@withContext
                                    Toast.makeText(
                                        requireContext(),
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
                                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                    db.addOnDao().insertProductRefs(
                                        listOf(
                                            ProductAddOnCrossRef(
                                                productId = product.productId,
                                                addOnId = addOn.addOnId
                                            )
                                        )
                                    )

                                    withContext(Dispatchers.Main) {
                                        if (!isAdded) return@withContext
                                        Toast.makeText(
                                            requireContext(),
                                            "\"${addOn.name}\" assigned to ${product.name}.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onChanged()
                                    }
                                }
                            },
                            secondaryText = "Edit",
                            onSecondary = {
                                showAddOnFormDialog(db, addOn.category, addOn) {
                                    onChanged()
                                }
                            },
                            tertiaryText = "Disable",
                            onTertiary = {
                                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                    db.addOnDao().setActive(addOn.addOnId, false)

                                    withContext(Dispatchers.Main) {
                                        if (!isAdded) return@withContext
                                        Toast.makeText(
                                            requireContext(),
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
                                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                    db.addOnDao().setActive(addOn.addOnId, true)

                                    withContext(Dispatchers.Main) {
                                        if (!isAdded) return@withContext
                                        Toast.makeText(
                                            requireContext(),
                                            "\"${addOn.name}\" enabled.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onChanged()
                                    }
                                }
                            },
                            secondaryText = "Edit",
                            onSecondary = {
                                showAddOnFormDialog(db, addOn.category, addOn) {
                                    onChanged()
                                }
                            },
                            tertiaryText = "Delete",
                            onTertiary = {
                                MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("Delete this extra permanently?")
                                    .setMessage(
                                        "This will permanently remove \"${addOn.name}\" from the library and remove its links to products and allergens. Use enable if you may need it again later."
                                    )
                                    .setNegativeButton("Cancel", null)
                                    .setPositiveButton("Delete") { _, _ ->
                                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                            db.addOnDao().deleteRefsForAddOn(addOn.addOnId)
                                            db.allergenDao().deleteAddOnRefs(addOn.addOnId)
                                            db.addOnDao().deleteById(addOn.addOnId)

                                            withContext(Dispatchers.Main) {
                                                if (!isAdded) return@withContext
                                                Toast.makeText(
                                                    requireContext(),
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
        val itemView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_admin_manage_extra, container, false)

        val tvExtraName = itemView.findViewById<TextView>(R.id.tvExtraName)
        val tvExtraMeta = itemView.findViewById<TextView>(R.id.tvExtraMeta)
        val tvExtraState = itemView.findViewById<TextView>(R.id.tvExtraState)
        val btnPrimaryAction = itemView.findViewById<MaterialButton>(R.id.btnPrimaryAction)
        val btnSecondaryAction = itemView.findViewById<MaterialButton>(R.id.btnSecondaryAction)
        val btnTertiaryAction = itemView.findViewById<MaterialButton>(R.id.btnTertiaryAction)

        tvExtraName.text = addOn.name
        tvExtraMeta.text = String.format(Locale.UK, "£%.2f • %d kcal", addOn.price, addOn.estimatedCalories)
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
    private fun showAddOnLibraryDialog(
        db: KoffeeCraftDatabase,
        category: String,
        onSaved: () -> Unit
    ) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val addOns = db.addOnDao().getAllByCategory(category)

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext

                val title = when (category.uppercase(Locale.UK)) {
                    "COFFEE" -> "Coffee extra library"
                    "CAKE" -> "Cake extra library"
                    "MERCH" -> "Merch extra library"
                    else -> "${category.replaceFirstChar { it.uppercase() }} extra library"
                }

                val labels = addOns.map { addOn ->
                    buildString {
                        append(addOn.name)
                        append("\n")
                        append("£%.2f".format(addOn.price))
                        append(" • ")
                        append(addOn.estimatedCalories)
                        append(" kcal")
                        append(" • ")
                        append(if (addOn.isActive) "Active" else "Disabled")
                    }
                }.toMutableList()

                labels.add("+ Create new extra")

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(title)
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
            "Edit details",
            if (addOn.isActive) "Disable extra" else "Enable extra",
            "Delete permanently"
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(addOn.name)
            .setMessage(
                buildString {
                    append("Price: £%.2f".format(addOn.price))
                    append("\n")
                    append("Calories: ${addOn.estimatedCalories} kcal")
                    append("\n")
                    append("Status: ")
                    append(if (addOn.isActive) "Active" else "Disabled")
                }
            )
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
                            .setTitle("Delete this extra permanently?")
                            .setMessage(
                                "This will permanently remove \"${addOn.name}\" from the library and remove its links to products and allergen assignments. Use disable if you may need it again later."
                            )
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Delete") { _, _ ->
                                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                    db.addOnDao().deleteRefsForAddOn(addOn.addOnId)
                                    db.allergenDao().deleteAddOnRefs(addOn.addOnId)
                                    db.addOnDao().deleteById(addOn.addOnId)

                                    withContext(Dispatchers.Main) {
                                        if (!isAdded) return@withContext
                                        Toast.makeText(
                                            requireContext(),
                                            "Extra deleted permanently.",
                                            Toast.LENGTH_SHORT
                                        ).show()
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

        val dialog = MaterialAlertDialogBuilder(requireContext())
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
            CategoryFilter.COFFEE -> allProducts.filter { it.isCoffee }
            CategoryFilter.CAKE -> allProducts.filter { it.isCake }
            CategoryFilter.MERCH -> allProducts.filter { it.isMerch }
        }

        adapter.submitList(filteredProducts)
        tvEmpty.visibility = if (filteredProducts.isEmpty()) View.VISIBLE else View.GONE
    }
}