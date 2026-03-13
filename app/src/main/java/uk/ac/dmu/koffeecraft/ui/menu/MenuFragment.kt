package uk.ac.dmu.koffeecraft.ui.menu

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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

class MenuFragment : Fragment(R.layout.fragment_menu) {

    private lateinit var vm: MenuViewModel
    private lateinit var adapter: ProductAdapter

    private var currentProducts: List<Product> = emptyList()
    private var favouriteIds: Set<Long> = emptySet()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)
        vm = ViewModelProvider(this, MenuViewModelFactory(db.productDao()))[MenuViewModel::class.java]

        val rv = view.findViewById<RecyclerView>(R.id.rvProducts)
        rv.layoutManager = LinearLayoutManager(requireContext())

        adapter = ProductAdapter(
            items = emptyList(),
            favouriteIds = emptySet(),
            onCustomizeClicked = { product ->
                ProductCustomizationBottomSheet.newInstance(product.productId)
                    .show(parentFragmentManager, "product_customize")
            },
            onFavouriteToggle = { product, shouldFavourite ->
                val customerId = SessionManager.currentCustomerId
                if (customerId == null) {
                    Toast.makeText(requireContext(), "Please sign in first.", Toast.LENGTH_SHORT).show()
                    return@ProductAdapter
                }

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

        view.findViewById<Button>(R.id.btnCoffee).setOnClickListener { vm.setCategory("COFFEE") }
        view.findViewById<Button>(R.id.btnCake).setOnClickListener { vm.setCategory("CAKE") }

        vm.start()

        viewLifecycleOwner.lifecycleScope.launch {
            vm.state.collect { state ->
                currentProducts = state.products
                renderProducts()
            }
        }

        val customerId = SessionManager.currentCustomerId
        if (customerId != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                db.favouriteDao().observeFavouriteProductIdsForCustomer(customerId).collect { ids ->
                    favouriteIds = ids.toSet()
                    renderProducts()
                }
            }
        }
    }

    private fun renderProducts() {
        adapter.submitList(currentProducts)
        adapter.updateFavouriteIds(favouriteIds)
    }
}