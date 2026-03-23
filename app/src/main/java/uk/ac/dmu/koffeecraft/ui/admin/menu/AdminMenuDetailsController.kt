package uk.ac.dmu.koffeecraft.ui.admin.menu

import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.entities.Product

class AdminMenuDetailsController(
    private val fragment: Fragment,
    private val viewModel: AdminMenuViewModel,
    private val onManageSizes: (Product, () -> Unit) -> Unit,
    private val onManageExtras: (Product, () -> Unit) -> Unit,
    private val onManageAllergens: (Product, () -> Unit) -> Unit
) {

    private var isObserving = false
    private var activeProductId: Long? = null

    private var detailsDialog: AlertDialog? = null
    private var tvOptions: TextView? = null
    private var tvAddOns: TextView? = null
    private var tvAllergens: TextView? = null

    fun show(product: Product) {
        detailsDialog?.dismiss()

        val dialogView = fragment.layoutInflater
            .inflate(R.layout.dialog_admin_product_details, null)

        val tvProductName = dialogView.findViewById<TextView>(R.id.tvProductName)
        val tvProductMeta = dialogView.findViewById<TextView>(R.id.tvProductMeta)
        val tvDescription = dialogView.findViewById<TextView>(R.id.tvDescription)
        val localTvOptions = dialogView.findViewById<TextView>(R.id.tvOptions)
        val localTvAddOns = dialogView.findViewById<TextView>(R.id.tvAddOns)
        val localTvAllergens = dialogView.findViewById<TextView>(R.id.tvAllergens)
        val btnManageSizes = dialogView.findViewById<MaterialButton>(R.id.btnManageSizes)
        val btnManageExtras = dialogView.findViewById<MaterialButton>(R.id.btnManageExtras)
        val btnManageAllergens = dialogView.findViewById<MaterialButton>(R.id.btnManageAllergens)

        tvOptions = localTvOptions
        tvAddOns = localTvAddOns
        tvAllergens = localTvAllergens
        activeProductId = product.productId

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

        bindLoadingState()

        btnManageSizes.setOnClickListener {
            if (!supportsCustomisation) return@setOnClickListener
            onManageSizes(product) {
                viewModel.loadProductDetails(product)
            }
        }

        btnManageExtras.setOnClickListener {
            if (!supportsCustomisation) return@setOnClickListener
            onManageExtras(product) {
                viewModel.loadProductDetails(product)
            }
        }

        btnManageAllergens.setOnClickListener {
            onManageAllergens(product) {
                viewModel.loadProductDetails(product)
            }
        }

        detailsDialog = MaterialAlertDialogBuilder(fragment.requireContext())
            .setTitle("Product details")
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .create()

        detailsDialog?.setOnDismissListener {
            clearDialogRefs()
        }

        ensureObserver()
        detailsDialog?.show()
        viewModel.loadProductDetails(product)
    }

    private fun ensureObserver() {
        if (isObserving) return
        isObserving = true

        fragment.viewLifecycleOwner.lifecycleScope.launch {
            fragment.viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.productDetailsState.collect { state ->
                    if (state.productId != activeProductId) return@collect

                    tvOptions?.text = state.optionsText
                    tvAddOns?.text = state.addOnsText
                    tvAllergens?.text = state.allergensText
                }
            }
        }
    }

    private fun bindLoadingState() {
        tvOptions?.text = "Loading..."
        tvAddOns?.text = "Loading..."
        tvAllergens?.text = "Loading..."
    }

    private fun clearDialogRefs() {
        detailsDialog = null
        activeProductId = null
        tvOptions = null
        tvAddOns = null
        tvAllergens = null
    }
}