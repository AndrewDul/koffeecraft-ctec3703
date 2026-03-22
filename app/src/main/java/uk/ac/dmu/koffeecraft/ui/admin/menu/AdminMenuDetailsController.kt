package uk.ac.dmu.koffeecraft.ui.admin.menu

import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.repository.AdminMenuRepository

class AdminMenuDetailsController(
    private val fragment: Fragment,
    private val repository: AdminMenuRepository,
    private val onManageSizes: (Product, () -> Unit) -> Unit,
    private val onManageExtras: (Product, () -> Unit) -> Unit,
    private val onManageAllergens: (Product, () -> Unit) -> Unit
) {

    fun show(product: Product) {
        val dialogView = fragment.layoutInflater
            .inflate(R.layout.dialog_admin_product_details, null)

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

        val dialog = MaterialAlertDialogBuilder(fragment.requireContext())
            .setTitle("Product details")
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .create()

        btnManageSizes.setOnClickListener {
            if (!supportsCustomisation) return@setOnClickListener
            onManageSizes(product) {
                loadProductDetails(product, tvOptions, tvAddOns, tvAllergens)
            }
        }

        btnManageExtras.setOnClickListener {
            if (!supportsCustomisation) return@setOnClickListener
            onManageExtras(product) {
                loadProductDetails(product, tvOptions, tvAddOns, tvAllergens)
            }
        }

        btnManageAllergens.setOnClickListener {
            onManageAllergens(product) {
                loadProductDetails(product, tvOptions, tvAddOns, tvAllergens)
            }
        }

        dialog.show()
        loadProductDetails(product, tvOptions, tvAddOns, tvAllergens)
    }

    private fun loadProductDetails(
        product: Product,
        tvOptions: TextView,
        tvAddOns: TextView,
        tvAllergens: TextView
    ) {
        fragment.viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val allergens = repository
                .allergenDao()
                .getForProduct(product.productId)

            val optionsText: String
            val addOnsText: String

            if (product.isMerch) {
                optionsText = "Not applicable for merch products."
                addOnsText = "Not applicable for merch products."
            } else {
                val options = repository
                    .productOptionDao()
                    .getForProduct(product.productId)

                val assignedAddOns = repository
                    .addOnDao()
                    .getAssignedForProduct(product.productId)

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
                if (!fragment.isAdded) return@withContext
                tvOptions.text = optionsText
                tvAddOns.text = addOnsText
                tvAllergens.text = allergensText
            }
        }
    }
}