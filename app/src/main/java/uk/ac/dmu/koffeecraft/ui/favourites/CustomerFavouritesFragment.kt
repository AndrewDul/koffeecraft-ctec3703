package uk.ac.dmu.koffeecraft.ui.favourites

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.cart.CartManager
import uk.ac.dmu.koffeecraft.data.dao.CustomerFavouritePresetCard
import uk.ac.dmu.koffeecraft.data.dao.StandardFavouriteCard
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.session.SessionManager
import uk.ac.dmu.koffeecraft.ui.menu.ProductCustomizationBottomSheet

class CustomerFavouritesFragment : Fragment(R.layout.fragment_customer_favourites) {

    private lateinit var presetAdapter: CustomerFavouritePresetAdapter
    private lateinit var standardAdapter: StandardFavouriteAdapter

    private lateinit var tvEmpty: TextView
    private lateinit var tvPresetSection: TextView
    private lateinit var tvStandardSection: TextView

    private var currentProducts: List<StandardFavouriteCard> = emptyList()
    private var currentPresets: List<CustomerFavouritePresetCard> = emptyList()

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
        rvPresets.setHasFixedSize(false)
        rvProducts.setHasFixedSize(false)

        presetAdapter = CustomerFavouritePresetAdapter(
            items = emptyList(),
            onRemove = { preset ->
                removePreset(db, preset)
            },
            onBuyAgain = { preset ->
                buyPresetAgain(db, preset)
            }
        )

        standardAdapter = StandardFavouriteAdapter(
            items = emptyList(),
            onRemove = { card ->
                removeStandardFavourite(db, customerId, card)
            },
            onCustomize = { card ->
                ProductCustomizationBottomSheet.newInstance(card.productId)
                    .show(parentFragmentManager, "product_customize")
            },
            onBuyAgain = { card ->
                buyStandardFavouriteAgain(db, card)
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
            db.favouriteDao().observeStandardFavouriteCardsForCustomer(customerId).collect { products ->
                currentProducts = products
                render()
            }
        }
    }

    private fun render() {
        presetAdapter.submitList(currentPresets)
        standardAdapter.submitList(currentProducts)

        tvPresetSection.visibility = if (currentPresets.isEmpty()) View.GONE else View.VISIBLE
        tvStandardSection.visibility = if (currentProducts.isEmpty()) View.GONE else View.VISIBLE
        tvEmpty.visibility =
            if (currentPresets.isEmpty() && currentProducts.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun removePreset(
        db: KoffeeCraftDatabase,
        preset: CustomerFavouritePresetCard
    ) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            db.customerFavouritePresetDao().deleteAddOnRefsForPreset(preset.presetId)
            db.customerFavouritePresetDao().deletePresetById(preset.presetId)

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext

                Toast.makeText(
                    requireContext(),
                    "Saved favourite removed.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun removeStandardFavourite(
        db: KoffeeCraftDatabase,
        customerId: Long,
        card: StandardFavouriteCard
    ) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            db.favouriteDao().delete(customerId, card.productId)

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext

                Toast.makeText(
                    requireContext(),
                    "Favourite removed.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun buyStandardFavouriteAgain(
        db: KoffeeCraftDatabase,
        card: StandardFavouriteCard
    ) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val product: Product? = db.productDao().getById(card.productId)

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext

                if (product == null || !product.isActive) {
                    Toast.makeText(
                        requireContext(),
                        "This product is currently unavailable.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@withContext
                }

                CartManager.add(product)

                Toast.makeText(
                    requireContext(),
                    "Favourite added to cart.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
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