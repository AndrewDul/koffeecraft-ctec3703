package uk.ac.dmu.koffeecraft.ui.menu

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
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

    private lateinit var tvFilterCoffee: TextView
    private lateinit var tvFilterCake: TextView

    private var currentCategory: String = "COFFEE"
    private var currentProducts: List<Product> = emptyList()
    private var favouriteIds: Set<Long> = emptySet()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)
        vm = ViewModelProvider(this, MenuViewModelFactory(db.productDao()))[MenuViewModel::class.java]

        tvFilterCoffee = view.findViewById(R.id.tvFilterCoffee)
        tvFilterCake = view.findViewById(R.id.tvFilterCake)

        val rv = view.findViewById<RecyclerView>(R.id.rvProducts)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.setHasFixedSize(false)
        rv.clipToPadding = false

        adapter = ProductAdapter(
            scope = viewLifecycleOwner.lifecycleScope,
            db = db,
            appContext = requireContext().applicationContext,
            items = emptyList(),
            favouriteIds = emptySet(),
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

        tvFilterCoffee.setOnClickListener { vm.setCategory("COFFEE") }
        tvFilterCake.setOnClickListener { vm.setCategory("CAKE") }

        vm.start()

        viewLifecycleOwner.lifecycleScope.launch {
            vm.state.collect { state ->
                currentCategory = state.category
                currentProducts = state.products
                renderProducts()
                renderCategoryChips()
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

    private fun renderCategoryChips() {
        styleFilterChip(tvFilterCoffee, currentCategory == "COFFEE")
        styleFilterChip(tvFilterCake, currentCategory == "CAKE")
    }

    private fun styleFilterChip(view: TextView, selected: Boolean) {
        if (selected) {
            view.setBackgroundResource(R.drawable.bg_orders_filter_chip_selected)
            view.setTextColor(Color.parseColor("#2E2018"))
        } else {
            view.setBackgroundResource(R.drawable.bg_orders_filter_chip)
            view.setTextColor(Color.parseColor("#6E5A4D"))
        }
    }
}