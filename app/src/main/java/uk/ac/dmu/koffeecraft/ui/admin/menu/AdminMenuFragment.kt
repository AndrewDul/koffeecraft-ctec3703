package uk.ac.dmu.koffeecraft.ui.admin.menu

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.Product
import com.google.android.material.button.MaterialButtonToggleGroup

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
        val cbIsNew = dialogView.findViewById<android.widget.CheckBox>(R.id.cbIsNew)

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

                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    if (existing == null) {
                        db.productDao().insert(
                            Product(
                                name = name,
                                category = category,
                                description = description,
                                price = price!!,
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
                                price = price!!,
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
