package uk.ac.dmu.koffeecraft.ui.menu

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.cart.CartManager
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.Product

class MenuFragment : Fragment(R.layout.fragment_menu) {

    private lateinit var vm: MenuViewModel
    private lateinit var adapter: ProductAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)
        vm = ViewModelProvider(this, MenuViewModelFactory(db.productDao()))[MenuViewModel::class.java]

        val rv = view.findViewById<RecyclerView>(R.id.rvProducts)
        rv.layoutManager = LinearLayoutManager(requireContext())

        adapter = ProductAdapter(emptyList()) { product ->
            CartManager.add(product)
        }
        rv.adapter = adapter

        view.findViewById<Button>(R.id.btnCoffee).setOnClickListener { vm.setCategory("COFFEE") }
        view.findViewById<Button>(R.id.btnCake).setOnClickListener { vm.setCategory("CAKE") }

        // Cart navigation we add in next step (Etap 3.2) once CartFragment is added to nav_graph.
        // For now we just keep the button ready.

        vm.start()

        viewLifecycleOwner.lifecycleScope.launch {
            vm.state.collect { state ->
                adapter.submitList(state.products)
            }
        }
    }
}