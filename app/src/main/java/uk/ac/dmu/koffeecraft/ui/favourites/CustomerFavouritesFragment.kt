package uk.ac.dmu.koffeecraft.ui.favourites

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.dao.CustomerFavouritePresetCard
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.Favourite
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.session.SessionManager
import uk.ac.dmu.koffeecraft.ui.menu.ProductAdapter
import uk.ac.dmu.koffeecraft.ui.menu.ProductCustomizationBottomSheet
import uk.ac.dmu.koffeecraft.data.cart.CartManager

class CustomerFavouritesFragment : Fragment(R.layout.fragment_customer_favourites) {

    private lateinit var presetAdapter: CustomerFavouritePresetAdapter
    private lateinit var standardAdapter: ProductAdapter

    private lateinit var tvEmpty: TextView
    private lateinit var tvPresetSection: TextView
    private lateinit var tvStandardSection: TextView

    private var currentProducts: List<Product> = emptyList()
    private var currentPresets: List<CustomerFavouritePresetCard> = emptyList()
    private var favouriteIds: Set<Long> = emptySet()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val customerId = SessionManager.currentCustomerId
        if (customerId == null) {
            Toast.makeText(requireContext(), "Please sign in first.", Toast.LENGTH_SHORT).show()
            return
        }

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        tvEmpty = view.findViewById(R.id.tvEmpty)
        tvPresetSection = view.findViewById(R.id.tvPresetSection)
        tvStandardSection = view.findViewById(R.id.tvStandardSection)

        val rvPresets = view.findViewById<RecyclerView>(R.id.rvFavouritePresets)
        val rvProducts = view.findViewById<RecyclerView>(R.id.rvFavouriteProducts)

        rvPresets.layoutManager = LinearLayoutManager(requireContext())
        rvProducts.layoutManager = LinearLayoutManager(requireContext())
        rvPresets.isNestedScrollingEnabled = false
        rvProducts.isNestedScrollingEnabled = false

        presetAdapter = CustomerFavouritePresetAdapter(
            items = emptyList(),
            onOpen = { preset ->
                showPresetDetailsDialog(db, preset)
            },
            onBuyAgain = { preset ->
                buyPresetAgain(db, preset)
            }
        )

        standardAdapter = ProductAdapter(
            items = emptyList(),
            favouriteIds = emptySet(),
            onCustomizeClicked = { product ->
                ProductCustomizationBottomSheet.newInstance(product.productId)
                    .show(parentFragmentManager, "product_customize")
            },
            onFavouriteToggle = { product, shouldFavourite ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    if (shouldFavourite) {
                        db.favouriteDao().insert(Favourite(customerId = customerId, productId = product.productId))
                    } else {
                        db.favouriteDao().delete(customerId, product.productId)
                    }
                }
            }
        )

        rvPresets.adapter = presetAdapter
        rvProducts.adapter = standardAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            db.customerFavouritePresetDao().observePresetCardsForCustomer(customerId).collect { presets ->
                currentPresets = presets
                render()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            db.favouriteDao().observeFavouriteProductsForCustomer(customerId).collect { products ->
                currentProducts = products
                favouriteIds = products.map { it.productId }.toSet()
                render()
            }
        }
    }

    private fun render() {
        presetAdapter.submitList(currentPresets)
        standardAdapter.submitList(currentProducts)
        standardAdapter.updateFavouriteIds(favouriteIds)

        tvPresetSection.visibility = if (currentPresets.isEmpty()) View.GONE else View.VISIBLE
        tvStandardSection.visibility = if (currentProducts.isEmpty()) View.GONE else View.VISIBLE

        tvEmpty.visibility =
            if (currentPresets.isEmpty() && currentProducts.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showPresetDetailsDialog(
        db: KoffeeCraftDatabase,
        preset: CustomerFavouritePresetCard
    ) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(preset.productName)
            .setMessage(
                buildString {
                    append(preset.productDescription)
                    append("\n\n")
                    append("Size: ")
                    append(preset.optionLabel)
                    append(" • ")
                    append(preset.optionSizeValue)
                    append(preset.optionSizeUnit.lowercase())
                    append("\n")
                    append("Add-ons: ")
                    append(preset.addOnSummary ?: "None")
                    append("\n")
                    append("Calories: ")
                    append(preset.totalCalories)
                    append(" kcal")
                    append("\n")
                    append("Price: £")
                    append(String.format("%.2f", preset.totalPrice))
                }
            )
            .setNegativeButton("Close", null)
            .setNeutralButton("Remove") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    db.customerFavouritePresetDao().deleteAddOnRefsForPreset(preset.presetId)
                    db.customerFavouritePresetDao().deletePresetById(preset.presetId)
                }
            }
            .setPositiveButton("Buy again") { _, _ ->
                buyPresetAgain(db, preset)
            }
            .show()
    }

    private fun buyPresetAgain(
        db: KoffeeCraftDatabase,
        preset: CustomerFavouritePresetCard
    ) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val product = db.productDao().getById(preset.productId)
            val option = db.productOptionDao().getById(preset.optionId)
            val addOns = db.customerFavouritePresetDao().getAddOnsForPreset(preset.presetId)

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext

                if (product == null || option == null) {
                    Toast.makeText(
                        requireContext(),
                        "This saved configuration is no longer available.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@withContext
                }

                CartManager.addCustomisedProduct(
                    product = product,
                    option = option,
                    addOns = addOns
                )

                Toast.makeText(
                    requireContext(),
                    "Saved configuration added to cart.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}