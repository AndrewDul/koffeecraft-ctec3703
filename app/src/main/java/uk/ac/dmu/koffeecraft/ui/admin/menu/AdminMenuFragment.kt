package uk.ac.dmu.koffeecraft.ui.admin.menu

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.core.di.appContainer
import uk.ac.dmu.koffeecraft.data.entities.Product

class AdminMenuFragment : Fragment(R.layout.fragment_admin_menu) {

    private lateinit var viewModel: AdminMenuViewModel
    private lateinit var adapter: AdminProductsAdapter
    private lateinit var tvEmpty: TextView

    private lateinit var detailsController: AdminMenuDetailsController
    private lateinit var optionsController: AdminMenuOptionsController
    private lateinit var extrasController: AdminMenuExtrasController
    private lateinit var allergensController: AdminMenuAllergensController

    private var productDialog: AlertDialog? = null
    private var productDialogTilName: TextInputLayout? = null
    private var productDialogTilDescription: TextInputLayout? = null
    private var productDialogTilPrice: TextInputLayout? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvAdminProducts)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        val fab = view.findViewById<FloatingActionButton>(R.id.fabAddProduct)
        val toggleGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.toggleCategoryFilter)
        val cardMenuHeader = view.findViewById<View>(R.id.cardMenuHeader)
        val cardMenuFilters = view.findViewById<View>(R.id.cardMenuFilters)

        val repository = appContainer.adminMenuRepository

        viewModel = ViewModelProvider(
            this,
            AdminMenuViewModelFactory(repository)
        )[AdminMenuViewModel::class.java]

        optionsController = AdminMenuOptionsController(
            fragment = this,
            viewModel = viewModel
        )
        extrasController = AdminMenuExtrasController(
            fragment = this,
            viewModel = viewModel
        )
        allergensController = AdminMenuAllergensController(
            fragment = this,
            viewModel = viewModel
        )
        detailsController = AdminMenuDetailsController(
            fragment = this,
            viewModel = viewModel,
            onManageSizes = { product, onSaved ->
                optionsController.showManageSizesDialog(product, onSaved)
            },
            onManageExtras = { product, onSaved ->
                extrasController.showManageExtrasDialog(product, onSaved)
            },
            onManageAllergens = { product, onSaved ->
                allergensController.showManageAllergensDialog(product, onSaved)
            }
        )

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.isNestedScrollingEnabled = false

        adapter = AdminProductsAdapter(
            items = emptyList(),
            onToggle = { product ->
                viewModel.toggleProductActive(product)
            },
            onEdit = { product ->
                showProductDialog(existing = product)
            },
            onDelete = { product ->
                showDeleteDialog(product)
            },
            onDetails = { product ->
                detailsController.show(product)
            }
        )

        rv.adapter = adapter

        cardMenuHeader.setOnClickListener {
            adapter.collapseAll()
        }

        cardMenuFilters.setOnClickListener {
            adapter.collapseAll()
        }

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            adapter.collapseAll()

            val filter = when (checkedId) {
                R.id.btnFilterCoffee -> AdminMenuCategoryFilter.COFFEE
                R.id.btnFilterCake -> AdminMenuCategoryFilter.CAKE
                R.id.btnFilterMerch -> AdminMenuCategoryFilter.MERCH
                else -> AdminMenuCategoryFilter.ALL
            }

            viewModel.setFilter(filter)
        }

        fab.setOnClickListener {
            showProductDialog(existing = null)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                adapter.submitList(state.filteredProducts)
                tvEmpty.visibility = if (state.filteredProducts.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.events.collectLatest { event ->
                when (event) {
                    is AdminMenuUiEvent.Message -> {
                        Toast.makeText(requireContext(), event.text, Toast.LENGTH_SHORT).show()
                    }

                    is AdminMenuUiEvent.ProductValidationFailed -> {
                        showProductValidationErrors(event.result)
                    }

                    is AdminMenuUiEvent.ProductSaved -> {
                        productDialog?.dismiss()
                        Toast.makeText(
                            requireContext(),
                            if (event.created) "Product added." else "Product updated.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun showProductDialog(existing: Product?) {
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
            dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(
                R.id.switchRewardEnabled
            )
        val cbIsNew = dialogView.findViewById<CheckBox>(R.id.cbIsNew)

        if (existing != null) {
            etName.setText(existing.name)
            etDescription.setText(existing.description)
            etPrice.setText(existing.price.toString())
            cbIsNew.isChecked = existing.isNew
            switchRewardEnabled.isChecked = existing.rewardEnabled

            val familyButtonId = when (existing.productFamily.uppercase()) {
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

        productDialog = dialog
        productDialogTilName = tilName
        productDialogTilDescription = tilDescription
        productDialogTilPrice = tilPrice

        dialog.setOnDismissListener {
            clearProductDialogRefs()
        }

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                clearProductDialogErrors()

                val productFamily = when (toggleProductFamily.checkedButtonId) {
                    R.id.btnFamilyCoffee -> "COFFEE"
                    R.id.btnFamilyCake -> "CAKE"
                    R.id.btnFamilyMerch -> "MERCH"
                    else -> ""
                }

                val formData = AdminMenuProductFormData(
                    name = etName.text?.toString().orEmpty(),
                    description = etDescription.text?.toString().orEmpty(),
                    priceText = etPrice.text?.toString().orEmpty(),
                    productFamily = productFamily,
                    rewardEnabled = switchRewardEnabled.isChecked,
                    isNew = cbIsNew.isChecked
                )

                viewModel.saveProduct(
                    existing = existing,
                    formData = formData
                )
            }
        }

        dialog.show()
    }

    private fun showDeleteDialog(product: Product) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Archive product?")
            .setMessage(
                "\"${product.name}\" will be removed from active sale, " +
                        "but kept in the database so past orders and history remain intact."
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Archive") { _, _ ->
                viewModel.archiveProduct(product)
            }
            .show()
    }

    private fun showProductValidationErrors(result: AdminMenuProductValidationResult) {
        productDialogTilName?.error = result.nameError
        productDialogTilDescription?.error = result.descriptionError
        productDialogTilPrice?.error = result.priceError

        val message = result.generalMessage ?: "Please complete all required fields."
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun clearProductDialogErrors() {
        productDialogTilName?.error = null
        productDialogTilDescription?.error = null
        productDialogTilPrice?.error = null
    }

    private fun clearProductDialogRefs() {
        productDialog = null
        productDialogTilName = null
        productDialogTilDescription = null
        productDialogTilPrice = null
    }
}