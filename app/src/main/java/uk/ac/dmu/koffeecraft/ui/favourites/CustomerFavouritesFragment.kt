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
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.Favourite
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.session.SessionManager
import uk.ac.dmu.koffeecraft.ui.menu.ProductAdapter
import uk.ac.dmu.koffeecraft.ui.menu.ProductCustomizationBottomSheet

class CustomerFavouritesFragment : Fragment(R.layout.fragment_customer_favourites) {

    private lateinit var adapter: ProductAdapter
    private lateinit var tvEmpty: TextView

    private var currentProducts: List<Product> = emptyList()
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
        val rv = view.findViewById<RecyclerView>(R.id.rvFavouriteProducts)
        rv.layoutManager = LinearLayoutManager(requireContext())

        adapter = ProductAdapter(
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

        rv.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            db.favouriteDao().observeFavouriteProductsForCustomer(customerId).collect { products ->
                currentProducts = products
                favouriteIds = products.map { it.productId }.toSet()
                render()
            }
        }
    }

    private fun render() {
        adapter.submitList(currentProducts)
        adapter.updateFavouriteIds(favouriteIds)
        tvEmpty.visibility = if (currentProducts.isEmpty()) View.VISIBLE else View.GONE
    }
}